// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'media_kind.dart';

class MediaSource {
  MediaSource({
    required this.id,
    required this.displayName,
    required this.kind,
    this.filePath,
    this.documentUri,
    this.mime,
    this.sizeBytes,
    this.modifiedMs,
  }) {
    if (filePath == null && documentUri == null) {
      throw ArgumentError('MediaSource needs filePath or documentUri: $id');
    }
  }

  final String id;
  final String displayName;
  final MediaKind kind;
  final String? filePath;
  final String? documentUri;
  final String? mime;
  final int? sizeBytes;
  final int? modifiedMs;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'id': id,
        'displayName': displayName,
        'kind': kind.name,
        'filePath': filePath,
        'documentUri': documentUri,
        'mime': mime,
        'sizeBytes': sizeBytes,
        'modifiedMs': modifiedMs,
      };

  static MediaSource fromJson(Map<String, dynamic> json) {
    final Object? kindRaw = json['kind'];
    MediaKind kind = MediaKind.unsupported;
    for (final MediaKind candidate in MediaKind.values) {
      if (candidate.name == kindRaw) {
        kind = candidate;
      }
    }
    return MediaSource(
      id: json['id'] as String,
      displayName: json['displayName'] as String,
      kind: kind,
      filePath: json['filePath'] as String?,
      documentUri: json['documentUri'] as String?,
      mime: json['mime'] as String?,
      sizeBytes: json['sizeBytes'] as int?,
      modifiedMs: json['modifiedMs'] as int?,
    );
  }
}
