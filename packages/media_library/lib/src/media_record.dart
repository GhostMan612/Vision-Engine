// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:metadata_core/metadata_core.dart';

import 'media_kind.dart';
import 'media_source.dart';
import 'thumb.dart';

class MediaRecord {
  const MediaRecord({
    required this.source,
    required this.kind,
    required this.status,
    required this.sortIndex,
    this.photo,
    this.video,
    this.warnings = const <String>[],
    this.thumb = const ThumbInfo(ThumbStatus.pending),
  });

  final MediaSource source;
  final MediaKind kind;
  final ExtractStatus status;
  final int sortIndex;
  final PhotoMeta? photo;
  final VideoMeta? video;
  final List<String> warnings;
  final ThumbInfo thumb;

  factory MediaRecord.fromPhoto({
    required MediaSource source,
    required int sortIndex,
    required PhotoMeta photo,
    required ExtractStatus status,
    List<String> warnings = const <String>[],
  }) {
    return MediaRecord(
      source: source,
      kind: MediaKind.photo,
      status: status,
      sortIndex: sortIndex,
      photo: photo,
      warnings: List<String>.unmodifiable(warnings),
    );
  }

  factory MediaRecord.fromVideo({
    required MediaSource source,
    required int sortIndex,
    required VideoMeta video,
    required ExtractStatus status,
    List<String> warnings = const <String>[],
  }) {
    return MediaRecord(
      source: source,
      kind: MediaKind.video,
      status: status,
      sortIndex: sortIndex,
      video: video,
      warnings: List<String>.unmodifiable(warnings),
    );
  }

  factory MediaRecord.unsupported({
    required MediaSource source,
    required int sortIndex,
    List<String> warnings = const <String>['unsupported type'],
  }) {
    return MediaRecord(
      source: source,
      kind: MediaKind.unsupported,
      status: ExtractStatus.unsupported,
      sortIndex: sortIndex,
      warnings: List<String>.unmodifiable(warnings),
      thumb: const ThumbInfo(
        ThumbStatus.unavailable,
        reason: 'unsupported type',
      ),
    );
  }

  Map<String, dynamic> toJson() => <String, dynamic>{
        'source': source.toJson(),
        'kind': kind.name,
        'status': status.name,
        'sortIndex': sortIndex,
        'photo': photo == null ? null : photoMetaToJson(photo!),
        'video': video == null ? null : videoMetaToJson(video!),
        'warnings': warnings,
        'thumb': thumb.toJson(),
      };

  static MediaRecord fromJson(Map<String, dynamic> json) {
    MediaKind kind = MediaKind.unsupported;
    for (final MediaKind candidate in MediaKind.values) {
      if (candidate.name == json['kind']) {
        kind = candidate;
      }
    }
    return MediaRecord(
      source: MediaSource.fromJson(
        (json['source'] as Map).cast<String, dynamic>(),
      ),
      kind: kind,
      status: _parseStatus(json['status']),
      sortIndex: json['sortIndex'] as int,
      photo: json['photo'] == null
          ? null
          : photoMetaFromJson(
              (json['photo'] as Map).cast<String, dynamic>(),
            ),
      video: json['video'] == null
          ? null
          : videoMetaFromJson(
              (json['video'] as Map).cast<String, dynamic>(),
            ),
      warnings: (json['warnings'] as List).cast<String>(),
      thumb: ThumbInfo.fromJson(
        (json['thumb'] as Map).cast<String, dynamic>(),
      ),
    );
  }

  static ExtractStatus _parseStatus(Object? raw) {
    for (final ExtractStatus candidate in ExtractStatus.values) {
      if (candidate.name == raw) {
        return candidate;
      }
    }
    return ExtractStatus.partial;
  }
}
