// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import '../../settings_store.dart';

export 'erase_models.dart';
import 'erase_models.dart';

typedef EraseFn = Future<Uint8List?> Function(String endpoint, Uint8List inBytes);

class EraseService {
  EraseService({required this._settings, EraseFn? eraseFn})
      : _eraseFn = eraseFn ?? _defaultErase;

  final SettingsStore _settings;
  final EraseFn _eraseFn;
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

  Future<EraseReceipt> eraseBackground(String path, {bool? consent}) async {
    final File source = File(path);
    final String endpoint = _settings.eraserEndpoint.trim();
    if (endpoint.isEmpty) {
      return const EraseReceipt(
        changed: false,
        verified: false,
        warnings: <String>['no endpoint configured — set one in Settings'],
      );
    }
    final bool allowed = consent ?? _settings.eraserConsent;
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
      final String base = dot < 0 ? path : path.substring(0, dot);
      final String ext = dot < 0 ? '' : path.substring(dot);
      final String target = '${base}_edit$ext';
      File(target).writeAsBytesSync(out);
      return EraseReceipt(changed: true, verified: File(target).existsSync(), outPath: target);
    } catch (_) {
      return const EraseReceipt(
        changed: false,
        verified: false,
        warnings: <String>['offline or request failed; source untouched'],
      );
    }
  }
}
