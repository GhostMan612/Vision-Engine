// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:media_library/media_library.dart';
import 'package:metadata_core/metadata_core.dart';

import '../adapters/metadata_adapter.dart' as meta;
import 'media_lister.dart';
import 'thumb_store.dart';

class LibraryPipeline {
  LibraryPipeline({meta.MetadataAdapter? metadata, ThumbStore? thumbs})
      : _metadata = metadata ?? meta.MetadataAdapter(),
        _thumbs = thumbs ?? ThumbStore();

  final meta.MetadataAdapter _metadata;
  final ThumbStore _thumbs;

  ThumbStore get thumbs => _thumbs;

  Future<List<MediaSource>> discover(Directory dir) async {
    return planDiscovery(listMediaSources(dir));
  }

  Future<List<MediaRecord>> loadPage({
    required List<MediaSource> sources,
    required int offset,
    required int limit,
  }) async {
    final List<MediaSource> page =
        pageOf(sources, offset: offset, limit: limit);
    final List<MediaRecord> records = <MediaRecord>[];
    for (int i = 0; i < page.length; i++) {
      records.add(await _recordFor(page[i], offset + i));
    }
    return records;
  }

  Future<MediaRecord> _recordFor(MediaSource source, int sortIndex) async {
    switch (source.kind) {
      case MediaKind.photo:
        if (source.filePath == null) {
          return MediaRecord.fromPhoto(
            source: source,
            sortIndex: sortIndex,
            photo: photoMetaFromJson(<String, dynamic>{}),
            status: ExtractStatus.unreadable,
            warnings: const <String>['no local path'],
          );
        }
        final meta.PhotoResult result =
            await _metadata.probePhoto(source.filePath!);
        return MediaRecord.fromPhoto(
          source: source,
          sortIndex: sortIndex,
          photo: result.meta,
          status: result.status,
          warnings: result.warnings,
        );
      case MediaKind.video:
        if (source.filePath == null) {
          return MediaRecord.fromVideo(
            source: source,
            sortIndex: sortIndex,
            video: videoMetaFromJson(<String, dynamic>{}),
            status: ExtractStatus.unreadable,
            warnings: const <String>['no local path'],
          );
        }
        final meta.VideoResult result =
            await _metadata.probeVideo(source.filePath!);
        return MediaRecord.fromVideo(
          source: source,
          sortIndex: sortIndex,
          video: result.meta,
          status: result.status,
          warnings: result.warnings,
        );
      case MediaKind.unsupported:
        return MediaRecord.unsupported(
          source: source,
          sortIndex: sortIndex,
        );
    }
  }

  Future<ThumbFetch> thumbnailFor(MediaRecord record) {
    if (record.source.filePath == null) {
      return Future<ThumbFetch>.value(
        const ThumbFetch(
          ThumbInfo(ThumbStatus.unavailable, reason: 'no local path'),
          null,
        ),
      );
    }
    switch (record.kind) {
      case MediaKind.photo:
        return _thumbs.fetchPhoto(record.source);
      case MediaKind.video:
        return _thumbs.fetchVideo(record.source);
      case MediaKind.unsupported:
        return Future<ThumbFetch>.value(
          const ThumbFetch(
            ThumbInfo(ThumbStatus.unavailable, reason: 'unsupported type'),
            null,
          ),
        );
    }
  }
}
