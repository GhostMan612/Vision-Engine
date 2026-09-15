// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:flutter/services.dart';
import 'package:metadata_core/metadata_core.dart';
import 'package:rename_core/rename_core.dart' show basenameOf;

enum MediaKind { photo, video }

enum ExtractStatus { ok, partial, unsupported, unreadable }

class PhotoResult {
  const PhotoResult({
    required this.meta,
    required this.status,
    required this.warnings,
  });

  final PhotoMeta meta;
  final ExtractStatus status;
  final List<String> warnings;
}

class VideoResult {
  const VideoResult({
    required this.meta,
    required this.status,
    required this.warnings,
  });

  final VideoMeta meta;
  final ExtractStatus status;
  final List<String> warnings;
}

class MediaTypeInfo {
  const MediaTypeInfo(this.mime, this.kind);

  final String mime;
  final MediaKind kind;
}

const Map<String, MediaTypeInfo> _mediaTypes =
    <String, MediaTypeInfo>{
  'jpg': MediaTypeInfo('image/jpeg', MediaKind.photo),
  'jpeg': MediaTypeInfo('image/jpeg', MediaKind.photo),
  'png': MediaTypeInfo('image/png', MediaKind.photo),
  'webp': MediaTypeInfo('image/webp', MediaKind.photo),
  'heic': MediaTypeInfo('image/heic', MediaKind.photo),
  'heif': MediaTypeInfo('image/heif', MediaKind.photo),
  'dng': MediaTypeInfo('image/x-adobe-dng', MediaKind.photo),
  'mp4': MediaTypeInfo('video/mp4', MediaKind.video),
  'm4v': MediaTypeInfo('video/x-m4v', MediaKind.video),
  'mov': MediaTypeInfo('video/quicktime', MediaKind.video),
  '3gp': MediaTypeInfo('video/3gpp', MediaKind.video),
  'mkv': MediaTypeInfo('video/x-matroska', MediaKind.video),
  'webm': MediaTypeInfo('video/webm', MediaKind.video),
};

final RegExp _iso6709 =
    RegExp(r'^([+-]\d+(?:\.\d+)?)([+-]\d+(?:\.\d+)?)$');

class _Decoded<T> {
  _Decoded(this.meta, this.warnings);

  final T meta;
  final List<String> warnings;
}

class MetadataAdapter {
  MetadataAdapter({MethodChannel? channel})
      : _channel = channel ?? const MethodChannel(channelName);

  static const String channelName = 'vision_engine/metadata';

  final MethodChannel _channel;

  static MediaTypeInfo? typeOf(String path) {
    final String name = basenameOf(path);
    final int dot = name.lastIndexOf('.');
    if (dot < 0 || dot == name.length - 1) {
      return null;
    }
    return _mediaTypes[name.substring(dot + 1).toLowerCase()];
  }

  static String? mimeOf(String path) => typeOf(path)?.mime;

  static MediaKind? kindOf(String path) => typeOf(path)?.kind;

  Future<PhotoResult> probePhoto(String path) async {
    final MediaTypeInfo? type = typeOf(path);
    if (type == null || type.kind != MediaKind.photo) {
      return PhotoResult(
        meta: photoMetaFromJson(<String, dynamic>{}),
        status: ExtractStatus.unsupported,
        warnings: const <String>['unsupported extension'],
      );
    }
    late final Map<String, dynamic> payload;
    try {
      final Map<Object?, Object?>? raw =
          await _channel.invokeMapMethod<String, Object?>(
        'probePhoto',
        <String, Object?>{'path': path},
      );
      payload = <String, dynamic>{};
      raw?.forEach((Object? key, Object? value) {
        if (key is String && value is Map) {
          payload[key] = value.cast<String, dynamic>();
        }
      });
    } on PlatformException catch (e) {
      return PhotoResult(
        meta: photoMetaFromJson(<String, dynamic>{}),
        status: ExtractStatus.unreadable,
        warnings: <String>['${e.code}: ${e.message}'],
      );
    }
    final _Decoded<PhotoMeta> decoded = _decodePhoto(payload, type.mime);
    return PhotoResult(
      meta: decoded.meta,
      status: _photoStatus(decoded.meta),
      warnings: decoded.warnings,
    );
  }

  Future<VideoResult> probeVideo(String path) async {
    final MediaTypeInfo? type = typeOf(path);
    if (type == null || type.kind != MediaKind.video) {
      return VideoResult(
        meta: videoMetaFromJson(<String, dynamic>{}),
        status: ExtractStatus.unsupported,
        warnings: const <String>['unsupported extension'],
      );
    }
    late final Map<String, dynamic> payload;
    try {
      final Map<Object?, Object?>? raw =
          await _channel.invokeMapMethod<String, Object?>(
        'probeVideo',
        <String, Object?>{'path': path},
      );
      payload = <String, dynamic>{};
      raw?.forEach((Object? key, Object? value) {
        if (key is String && value is Map) {
          payload[key] = value.cast<String, dynamic>();
        }
      });
    } on PlatformException catch (e) {
      return VideoResult(
        meta: videoMetaFromJson(<String, dynamic>{}),
        status: ExtractStatus.unreadable,
        warnings: <String>['${e.code}: ${e.message}'],
      );
    }
    final _Decoded<VideoMeta> decoded = _decodeVideo(payload, type.mime);
    return VideoResult(
      meta: decoded.meta,
      status: _videoStatus(decoded.meta),
      warnings: decoded.warnings,
    );
  }

  static void _sanitizeInt(
    Map<String, dynamic> json,
    String key,
    List<String> warnings, {
    bool allowZero = true,
  }) {
    final Object? node = json[key];
    if (node is! Map<String, dynamic>) {
      return;
    }
    final Object? value = node['value'];
    if (value is int) {
      if (!allowZero && value <= 0) {
        warnings.add('$key not legitimate: $value');
        json[key] = <String, dynamic>{
          'value': null,
          'provenance': 'unknown',
        };
      }
      return;
    }
    if (value is String) {
      final int? parsed = int.tryParse(value);
      if (parsed == null) {
        warnings.add('$key value ignored: $value');
        json[key] = <String, dynamic>{
          'value': null,
          'provenance': 'unknown',
        };
      } else if (!allowZero && parsed <= 0) {
        warnings.add('$key not legitimate: $value');
        json[key] = <String, dynamic>{
          'value': null,
          'provenance': 'unknown',
        };
      } else {
        node['value'] = parsed;
      }
      return;
    }
    if (value != null) {
      warnings.add('$key value ignored');
      json[key] = <String, dynamic>{
        'value': null,
        'provenance': 'unknown',
      };
    }
  }

  static _Decoded<PhotoMeta> _decodePhoto(
    Map<String, dynamic> payload,
    String mime,
  ) {
    final Map<String, dynamic> json =
        Map<String, dynamic>.of(payload);
    final List<String> warnings = <String>[];
    _sanitizeInt(json, 'orientation', warnings);
    _sanitizeInt(json, 'widthRaw', warnings, allowZero: false);
    _sanitizeInt(json, 'heightRaw', warnings, allowZero: false);
    json['mime'] = <String, dynamic>{
      'value': mime,
      'provenance': 'filesystem',
    };
    return _Decoded<PhotoMeta>(photoMetaFromJson(json), warnings);
  }

  static _Decoded<VideoMeta> _decodeVideo(
    Map<String, dynamic> payload,
    String mime,
  ) {
    final Map<String, dynamic> json =
        Map<String, dynamic>.of(payload);
    final List<String> warnings = <String>[];
    _sanitizeInt(json, 'durationMs', warnings);
    _sanitizeInt(json, 'widthRaw', warnings, allowZero: false);
    _sanitizeInt(json, 'heightRaw', warnings, allowZero: false);
    _sanitizeInt(json, 'rotation', warnings);
    _decodeIso6709(json, warnings);
    json['mime'] = <String, dynamic>{
      'value': mime,
      'provenance': 'filesystem',
    };
    return _Decoded<VideoMeta>(videoMetaFromJson(json), warnings);
  }

  static void _decodeIso6709(
    Map<String, dynamic> json,
    List<String> warnings,
  ) {
    final Object? node = json['location'];
    json.remove('location');
    if (node is! Map<String, dynamic>) {
      return;
    }
    final Object? value = node['value'];
    if (value is! String) {
      if (value != null) {
        warnings.add('location value ignored');
      }
      return;
    }
    final RegExpMatch? match = _iso6709.firstMatch(value.trim());
    final double? latitude =
        match == null ? null : double.tryParse(match.group(1)!);
    final double? longitude =
        match == null ? null : double.tryParse(match.group(2)!);
    if (latitude == null || longitude == null) {
      warnings.add('location value ignored: $value');
      return;
    }
    json['locationLatitude'] = <String, dynamic>{
      'value': latitude,
      'provenance': 'container',
    };
    json['locationLongitude'] = <String, dynamic>{
      'value': longitude,
      'provenance': 'container',
    };
  }

  static ExtractStatus _photoStatus(PhotoMeta meta) {
    final List<bool> known = <bool>[
      meta.datetimeOriginal.isKnown,
      meta.gpsLatitude.isKnown,
      meta.gpsLongitude.isKnown,
      meta.orientation.isKnown,
      meta.make.isKnown,
      meta.model.isKnown,
      meta.exposureTime.isKnown,
      meta.widthRaw.isKnown,
      meta.heightRaw.isKnown,
      meta.fileSizeBytes.isKnown,
    ];
    return known.any((bool k) => k)
        ? ExtractStatus.ok
        : ExtractStatus.partial;
  }

  static ExtractStatus _videoStatus(VideoMeta meta) {
    final List<bool> known = <bool>[
      meta.durationMs.isKnown,
      meta.widthRaw.isKnown,
      meta.heightRaw.isKnown,
      meta.rotation.isKnown,
      meta.creationTime.isKnown,
      meta.locationLatitude.isKnown,
      meta.locationLongitude.isKnown,
      meta.codec.isKnown,
      meta.fileSizeBytes.isKnown,
    ];
    return known.any((bool k) => k)
        ? ExtractStatus.ok
        : ExtractStatus.partial;
  }
}
