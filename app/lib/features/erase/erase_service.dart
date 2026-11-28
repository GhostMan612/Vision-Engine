// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:image/image.dart';

import '../../settings_store.dart';
import 'erase_models.dart';
import 'tflite_mask.dart';

export 'erase_models.dart';
export 'tflite_mask.dart';

typedef EraseFn = Future<Uint8List?> Function(String endpoint, Uint8List inBytes);

enum EraseEngine { onDevice, cloud }

class EraseService {
  EraseService({
    required this.settings,
    EraseFn? eraseFn,
    MaskFn? maskFn,
    this.engine = EraseEngine.onDevice,
    this.modelSide = 256,
  })  : _eraseFn = eraseFn ?? _defaultErase,
        _maskFn = maskFn ?? tfliteMask;

  final SettingsStore settings;
  final EraseFn _eraseFn;
  final MaskFn _maskFn;
  final EraseEngine engine;
  final int modelSide;

  static Future<Uint8List?> _defaultErase(
    String endpoint,
    Uint8List bytes,
  ) async {
    final HttpClient client = HttpClient();
    try {
      final HttpClientRequest request =
          await client.postUrl(Uri.parse(endpoint));
      request.headers.set(HttpHeaders.contentTypeHeader, ContentType.binary.toString());
      request.add(bytes);
      final HttpClientResponse response = await request.close();
      if (response.statusCode != HttpStatus.ok) {
        return null;
      }
      final List<int> out = await response.fold<List<int>>(
        <int>[],
        (List<int> a, List<int> b) => a..addAll(b),
      );
      return Uint8List.fromList(out);
    } finally {
      client.close();
    }
  }

  Future<EraseReceipt> eraseBackground(
    String path, {
    bool? consent,
    EraseEngine? engine,
  }) async {
    final EraseEngine selected = engine ?? this.engine;
    if (selected == EraseEngine.cloud) {
      return _eraseCloud(path, consent);
    }
    return _eraseOnDevice(path);
  }

  Future<EraseReceipt> _eraseOnDevice(String path) async {
    try {
      final Uint8List sourceBytes = await File(path).readAsBytes();
      final Image? original = _decodeImage(sourceBytes);
      if (original == null) {
        return const EraseReceipt(
          changed: false,
          verified: false,
          warnings: <String>['source is not a decodable JPEG or PNG'],
        );
      }
      final Image resized = copyResize(
        original,
        width: modelSide,
        height: modelSide,
      );
      final Uint8List rgbBytes =
          resized.getBytes(order: ChannelOrder.rgb);
      final Float32List modelInput = Float32List(rgbBytes.length);
      for (int i = 0; i < rgbBytes.length; i++) {
        modelInput[i] = rgbBytes[i] / 255;
      }
      final Uint8List? mask =
          await _maskFn(modelInput, modelSide, modelSide);
      if (mask == null || mask.length != modelSide * modelSide) {
        return const EraseReceipt(
          changed: false,
          verified: false,
          warnings: <String>[
            'on-device model unavailable; source untouched (no upload)'
          ],
        );
      }
      final Uint8List rgba = original.getBytes(order: ChannelOrder.rgba);
      final int w = original.width;
      final int h = original.height;
      for (int y = 0; y < h; y++) {
        final int sy = (y * modelSide / h).clamp(0, modelSide - 1).toInt();
        for (int x = 0; x < w; x++) {
          final int sx = (x * modelSide / w).clamp(0, modelSide - 1).toInt();
          rgba[(y * w + x) * 4 + 3] = mask[sy * modelSide + sx];
        }
      }
      final Image cutout = Image.fromBytes(
        width: w,
        height: h,
        bytes: rgba.buffer,
        numChannels: 4,
        order: ChannelOrder.rgba,
      );
      final Uint8List png = encodePng(cutout);
      if (png.isEmpty) {
        return const EraseReceipt(
          changed: false,
          verified: false,
          warnings: <String>['encoder returned no bytes; source untouched'],
        );
      }
      final String target = _editPath(path, '.png');
      await File(target).writeAsBytes(png);
      final Uint8List after = await File(path).readAsBytes();
      final bool sourceIntact = _sameBytes(sourceBytes, after);
      return EraseReceipt(
        changed: true,
        verified: sourceIntact &&
            File(target).existsSync() &&
            File(target).lengthSync() == png.length,
        outPath: target,
        warnings: sourceIntact
            ? const <String>[]
            : const <String>['WARNING: source bytes changed'],
      );
    } catch (e) {
      return EraseReceipt(
        changed: false,
        verified: false,
        warnings: <String>['on-device erase failed: $e'],
      );
    }
  }

  Future<EraseReceipt> _eraseCloud(String path, bool? consent) async {
    final File source = File(path);
    final String endpoint = settings.eraserEndpoint.trim();
    if (endpoint.isEmpty) {
      return const EraseReceipt(
        changed: false,
        verified: false,
        warnings: <String>['no endpoint configured — set one in Settings'],
      );
    }
    final bool allowed = consent ?? settings.eraserConsent;
    if (!allowed) {
      return const EraseReceipt(
        changed: false,
        verified: false,
        warnings: <String>['consent required — enable Eraser in Settings'],
      );
    }
    try {
      final Uint8List inBytes = await source.readAsBytes();
      final Uint8List? out = await _eraseFn(endpoint, inBytes);
      if (out == null || out.isEmpty) {
        return const EraseReceipt(
          changed: false,
          verified: false,
          warnings: <String>['endpoint returned no image; source untouched'],
        );
      }
      final int dot = path.lastIndexOf('.');
      final String ext = dot < 0 ? '' : path.substring(dot);
      final String target = _editPath(path, ext);
      File(target).writeAsBytesSync(out);
      return EraseReceipt(
        changed: true,
        verified: File(target).existsSync(),
        outPath: target,
      );
    } catch (_) {
      return const EraseReceipt(
        changed: false,
        verified: false,
        warnings: <String>['offline or request failed; source untouched'],
      );
    }
  }

  static Image? _decodeImage(Uint8List bytes) {
    try {
      final Image? jpeg = decodeJpg(bytes);
      if (jpeg != null) {
        return jpeg;
      }
    } catch (_) {}
    try {
      return decodePng(bytes);
    } catch (_) {
      return null;
    }
  }

  static String _editPath(String path, String extension) {
    final int dot = path.lastIndexOf('.');
    final String base = dot < 0 ? path : path.substring(0, dot);
    return '$base$_editSuffix$extension';
  }

  static const String _editSuffix = '_edit';

  static bool _sameBytes(List<int> a, List<int> b) {
    if (a.length != b.length) {
      return false;
    }
    for (int i = 0; i < a.length; i++) {
      if (a[i] != b[i]) {
        return false;
      }
    }
    return true;
  }
}
