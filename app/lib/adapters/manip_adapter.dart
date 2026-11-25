// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/services.dart';

import 'metadata_adapter.dart';
import '../features/manip/manip_models.dart';
import '../features/manip/pixel_ops.dart';

export '../features/manip/manip_models.dart';
export '../features/manip/pixel_ops.dart';

class ManipAdapter {
  ManipAdapter({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(channelName);

  static const String channelName = 'vision_engine/exif_ops';

  final MethodChannel _channel;

  Future<ManipReceipt> _exif(
    ManipKind kind,
    Map<String, Object?> args,
  ) async {
    try {
      final String method = args.remove('method') as String;
      final Map<Object?, Object?>? raw =
          await _channel.invokeMapMethod<String, Object?>(method, args);
      final List<String> warnings = <String>[];
      for (final Object? w in raw?['warnings'] as List? ?? const <Never>[]) {
        if (w is String && w.isNotEmpty) {
          warnings.add(w);
        }
      }
      return ManipReceipt(
        kind: kind,
        changed: raw?['changed'] == true,
        verified: raw?['verified'] == true,
        warnings: warnings,
      );
    } on PlatformException catch (e) {
      return ManipReceipt(
        kind: kind,
        changed: false,
        verified: false,
        warnings: <String>['${e.code}: ${e.message}'],
      );
    }
  }

  Future<ManipReceipt> shiftDateTime(
    String path, {
    required int minutes,
  }) {
    return _exif(ManipKind.shiftDateTime,
        <String, Object?>{'path': path, 'method': 'shiftDateTime', 'minutes': minutes});
  }

  Future<ManipReceipt> stripGps(String path) {
    return _exif(ManipKind.stripGps,
        <String, Object?>{'path': path, 'method': 'stripGps'});
  }

  Future<ManipReceipt> setOrientation(String path, {required int orientation}) {
    return _exif(ManipKind.setOrientation,
        <String, Object?>{'path': path, 'method': 'setOrientation', 'orientation': orientation});
  }

  Future<Uint8List?> crop(
    Uint8List bytes, {
    required int left,
    required int top,
    required int width,
    required int height,
  }) {
    return Future<Uint8List?>.sync(
      () => cropJpeg(bytes, left: left, top: top, width: width, height: height),
    );
  }

  Future<Uint8List?> resize(
    Uint8List bytes, {
    int? width,
    int? height,
    int quality = 85,
  }) {
    return Future<Uint8List?>.sync(
      () => resizeJpeg(bytes, width: width, height: height, quality: quality),
    );
  }

  String defaultEditCopyPath(String path) =>
      defaultEditCopy(File(path))?.path ?? path;

  Future<String> reRead(String path) async {
    try {
      final PhotoResult result = await MetadataAdapter().probePhoto(path);
      final List<String> out = <String>[
        if (result.status == ExtractStatus.unreadable) 'file unreadable',
        if (result.status == ExtractStatus.partial &&
            !result.meta.datetimeOriginal.isKnown)
          'datetime missing',
        ...result.warnings,
      ]..removeWhere((String w) => w.isEmpty);
      return out.isEmpty ? 'ok' : out.join('; ');
    } catch (_) {
      return 're-read failed';
    }
  }
}
