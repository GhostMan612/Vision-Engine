// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:media_library/media_library.dart';
import 'package:vision_engine/adapters/metadata_adapter.dart'
    hide MediaKind;
import 'package:vision_engine/library/library_pipeline.dart';
import 'package:vision_engine/library/thumb_store.dart';

const MethodChannel _channel =
    MethodChannel(MetadataAdapter.channelName);

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Directory? workDir;
  String? beforeHash;
  late List<String> calls;
  late LibraryPipeline pipeline;

  String treeSnapshot(Directory target) {
    final List<String> parts = <String>[];
    for (final FileSystemEntity entity in target.listSync(recursive: true)) {
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

  Directory stageTree() {
    final Directory fresh =
        Directory.systemTemp.createTempSync('ve-pipe-');
    File('${fresh.path}${Platform.pathSeparator}IMG_b.jpg')
        .writeAsBytesSync(
      File('assets/fixtures/ve_thumb.jpg').readAsBytesSync(),
    );
    File('${fresh.path}${Platform.pathSeparator}VID_a.mp4')
        .writeAsBytesSync(
      File('assets/fixtures/ve_minimal.mp4').readAsBytesSync(),
    );
    File('${fresh.path}${Platform.pathSeparator}note.txt')
        .writeAsStringSync('hello');
    final Directory sub = Directory(
      '${fresh.path}${Platform.pathSeparator}sub',
    )..createSync();
    File('${sub.path}${Platform.pathSeparator}nested.jpg').writeAsBytesSync(
      File('assets/fixtures/ve_thumb.jpg').readAsBytesSync(),
    );
    workDir = fresh;
    beforeHash = treeSnapshot(fresh);
    return fresh;
  }

  setUp(() {
    calls = <String>[];
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (MethodCall call) async {
      final Map<String, Object?> args =
          (call.arguments as Map).cast<String, Object?>();
      final String path = args['path'] as String? ?? '';
      calls.add('${call.method}:$path');
      if (call.method == 'probePhoto') {
        return <String, Object?>{
          'orientation': {'value': '6', 'provenance': 'exif'},
          'widthRaw': {'value': '24', 'provenance': 'exif'},
          'heightRaw': {'value': '16', 'provenance': 'exif'},
          'make': {'value': 'SYNTHETIC', 'provenance': 'exif'},
          'fileSizeBytes': {
            'value': File(path).lengthSync(),
            'provenance': 'filesystem',
          },
        };
      }
      if (call.method == 'probeVideo') {
        return <String, Object?>{
          'durationMs': {'value': '0', 'provenance': 'container'},
          'fileSizeBytes': {
            'value': File(path).lengthSync(),
            'provenance': 'filesystem',
          },
        };
      }
      throw PlatformException(code: 'UNAVAILABLE', message: 'no frame');
    });
    pipeline = LibraryPipeline();
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
    final Directory? target = workDir;
    final String? baseline = beforeHash;
    if (target != null && baseline != null) {
      expect(treeSnapshot(target), baseline);
      target.deleteSync(recursive: true);
    }
    workDir = null;
    beforeHash = null;
  });

  testWidgets('discover sorts, bounds and classifies',
      (WidgetTester tester) async {
    final Directory dir = stageTree();
    final List<MediaSource> sources = await pipeline.discover(dir);
    expect(
      sources.map((MediaSource s) => s.displayName),
      <String>['IMG_b.jpg', 'note.txt', 'VID_a.mp4'],
    );
    expect(
      sources.map((MediaSource s) => s.kind),
      <MediaKind>[MediaKind.photo, MediaKind.unsupported, MediaKind.video],
    );
  });

  testWidgets('loadPage builds records with sort positions',
      (WidgetTester tester) async {
    final Directory dir = stageTree();
    final List<MediaSource> sources = await pipeline.discover(dir);
    final List<MediaRecord> records =
        await pipeline.loadPage(sources: sources, offset: 0, limit: 10);
    expect(records.length, 3);
    expect(records[0].kind, MediaKind.photo);
    expect(records[0].status, ExtractStatus.ok);
    expect(records[0].photo?.width, 16);
    expect(records[0].photo?.height, 24);
    expect(records[0].sortIndex, 0);
    expect(records[1].kind, MediaKind.unsupported);
    expect(records[1].status, ExtractStatus.unsupported);
    expect(records[2].kind, MediaKind.video);
    expect(records[2].video?.durationMs.value, 0);
    expect(
      calls.where((String c) => c.startsWith('probePhoto')).length,
      1,
    );
    expect(
      calls.where((String c) => c.startsWith('probeVideo')).length,
      1,
    );
  });

  testWidgets('paging slices deterministically',
      (WidgetTester tester) async {
    final Directory dir = stageTree();
    final List<MediaSource> sources = await pipeline.discover(dir);
    final List<MediaRecord> page = await pipeline.loadPage(
      sources: sources,
      offset: 2,
      limit: 1,
    );
    expect(page.length, 1);
    expect(page.single.source.displayName, 'VID_a.mp4');
    expect(page.single.sortIndex, 2);
  });

  testWidgets('photo thumbnail decodes from staged bytes',
      (WidgetTester tester) async {
    final Directory dir = stageTree();
    final List<MediaSource> sources = await pipeline.discover(dir);
    final List<MediaRecord> records = await pipeline.loadPage(
      sources: sources,
      offset: 0,
      limit: 1,
    );
    final ThumbFetch fetch = (await tester.runAsync(
      () => pipeline.thumbnailFor(records.single),
    ))!;
    expect(fetch.info.status, ThumbStatus.ready);
    expect(fetch.info.width, 256);
    expect(fetch.info.height, 384);
    expect(fetch.bytes, isNotNull);
  });

  testWidgets('video and unsupported thumbs stay unavailable, records intact',
      (WidgetTester tester) async {
    final Directory dir = stageTree();
    final List<MediaSource> sources = await pipeline.discover(dir);
    final List<MediaRecord> records =
        await pipeline.loadPage(sources: sources, offset: 0, limit: 10);
    final ThumbFetch video = (await tester.runAsync(
      () => pipeline.thumbnailFor(records[2]),
    ))!;
    expect(video.info.status, ThumbStatus.unavailable);
    expect(video.bytes, isNull);
    expect(records[2].video?.durationMs.value, 0);
    final ThumbFetch text = (await tester.runAsync(
      () => pipeline.thumbnailFor(records[1]),
    ))!;
    expect(text.info.status, ThumbStatus.unavailable);
  });
}
