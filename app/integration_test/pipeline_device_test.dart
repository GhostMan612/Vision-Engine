// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:media_library/media_library.dart';
import 'package:metadata_core/metadata_core.dart';
import 'package:path_provider/path_provider.dart';
import 'package:vision_engine/library/library_pipeline.dart';
import 'package:vision_engine/library/thumb_store.dart';

Future<File> _stageBytes(
  List<int> bytes,
  Directory dir,
  String name,
) async {
  final File file = File('${dir.path}${Platform.pathSeparator}$name');
  await file.writeAsBytes(bytes);
  return file;
}

Future<File> _stageAsset(String asset, Directory dir, String name) async {
  final ByteData data = await rootBundle.load('assets/fixtures/$asset');
  return _stageBytes(
    data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes),
    dir,
    name,
  );
}

String _snapshot(Directory root) {
  final List<String> parts = <String>[];
  for (final FileSystemEntity entity in root.listSync(recursive: true)) {
    if (entity is File) {
      final List<int> bytes = entity.readAsBytesSync();
      int checksum = bytes.length;
      for (final int byte in bytes) {
        checksum = ((checksum * 31) + byte) & 0xFFFFFFFF;
      }
      parts.add('${entity.path}:$checksum');
    }
  }
  parts.sort();
  return parts.join('|');
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  late Directory work;
  late LibraryPipeline pipeline;

  setUpAll(() async {
    final Directory base = await getTemporaryDirectory();
    work = Directory(
      '${base.path}${Platform.pathSeparator}VE_2C',
    );
    if (work.existsSync()) {
      work.deleteSync(recursive: true);
    }
    work.createSync(recursive: true);
    await _stageAsset('ve_thumb.jpg', work, 'IMG_t.jpg');
    await _stageAsset('ve_plain.png', work, 'shot.png');
    await _stageAsset('ve_minimal.mp4', work, 'VID_m.mp4');
    await _stageBytes(<int>[104, 105], work, 'note.txt');
    final Directory sub = Directory(
      '${work.path}${Platform.pathSeparator}sub',
    )..createSync();
    await _stageAsset('ve_thumb.jpg', sub, 'nested.jpg');
    pipeline = LibraryPipeline();
  });

  test('2C pipeline discovers, extracts, thumbs and cleans up', () async {
    final String before = _snapshot(work);
    final List<MediaSource> sources = await pipeline.discover(work);
    expect(
      sources.map((MediaSource s) => s.displayName),
      <String>['IMG_t.jpg', 'VID_m.mp4', 'note.txt', 'shot.png'],
    );
    expect(
      sources.map((MediaSource s) => s.kind),
      <MediaKind>[
        MediaKind.photo,
        MediaKind.video,
        MediaKind.unsupported,
        MediaKind.photo,
      ],
    );
    final List<MediaRecord> records = await pipeline.loadPage(
      sources: sources,
      offset: 0,
      limit: 10,
    );
    expect(records.length, 4);
    expect(records[0].photo?.orientation.value, 6);
    expect(records[0].photo?.width, 16);
    expect(records[0].photo?.height, 24);
    expect(records[1].video?.durationMs.value, 0);
    expect(records[2].status, ExtractStatus.unsupported);
    expect(records[3].status, ExtractStatus.partial);
    expect(records[3].photo?.orientation.value, isNull);
    final ThumbFetch thumb = await pipeline.thumbnailFor(records[0]);
    expect(thumb.info.status, ThumbStatus.ready);
    expect(thumb.info.width, 256);
    expect(thumb.info.height, 384);
    final ThumbFetch again = await pipeline.thumbnailFor(records[0]);
    expect(again.bytes, thumb.bytes);
    expect(pipeline.thumbs.cache.hits, greaterThanOrEqualTo(1));
    final ThumbFetch png = await pipeline.thumbnailFor(records[3]);
    expect(png.info.status, ThumbStatus.ready);
    expect(png.info.width, 256);
    expect(png.info.height, 256);
    final ThumbFetch video = await pipeline.thumbnailFor(records[1]);
    expect(video.info.status, ThumbStatus.unavailable);
    expect(video.bytes, isNull);
    expect(records[1].video?.durationMs.value, 0);
    final ThumbFetch text = await pipeline.thumbnailFor(records[2]);
    expect(text.info.status, ThumbStatus.unavailable);
    expect(_snapshot(work), before);
    work.deleteSync(recursive: true);
    expect(work.existsSync(), isFalse);
  });
}
