// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:media_library/media_library.dart';
import 'package:rename_core/rename_core.dart' show basenameOf;

import '../adapters/metadata_adapter.dart' as meta;

List<MediaSource> listMediaSources(Directory dir) {
  final List<MediaSource> found = <MediaSource>[];
  final List<FileSystemEntity> entities;
  try {
    entities = dir.listSync(followLinks: false);
  } on FileSystemException {
    return <MediaSource>[];
  }
  for (final FileSystemEntity entity in entities) {
    if (entity is! File) {
      continue;
    }
    final String path = entity.path;
    final String name = basenameOf(path);
    int? sizeBytes;
    int? modifiedMs;
    try {
      final FileStat stat = entity.statSync();
      sizeBytes = stat.size;
      modifiedMs = stat.modified.millisecondsSinceEpoch;
    } on FileSystemException {
      sizeBytes = null;
      modifiedMs = null;
    }
    final meta.MediaTypeInfo? type = meta.MetadataAdapter.typeOf(path);
    if (type == null) {
      found.add(
        MediaSource(
          id: path,
          displayName: name,
          kind: MediaKind.unsupported,
          filePath: path,
          sizeBytes: sizeBytes,
          modifiedMs: modifiedMs,
        ),
      );
    } else {
      found.add(
        MediaSource(
          id: path,
          displayName: name,
          kind: type.kind == meta.MediaKind.photo
              ? MediaKind.photo
              : MediaKind.video,
          filePath: path,
          mime: type.mime,
          sizeBytes: sizeBytes,
          modifiedMs: modifiedMs,
        ),
      );
    }
  }
  return found;
}
