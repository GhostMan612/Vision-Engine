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
import 'package:vision_engine/library/thumb_store.dart';

const MethodChannel _channel =
    MethodChannel(MetadataAdapter.channelName);

Uint8List _bytes(String asset) =>
    File('assets/fixtures/$asset').readAsBytesSync();

File _stage(Directory dir, String asset, String name) {
  final File file =
      File('${dir.path}${Platform.pathSeparator}$name');
  file.writeAsBytesSync(_bytes(asset));
  return file;
}

MediaSource _photoSource(File file, String name) => MediaSource(
      id: file.path,
      displayName: name,
      kind: MediaKind.photo,
      filePath: file.path,
    );

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  void mockFrames(Future<Map<String, Object?>?> Function(MethodCall) fn) {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, fn);
  }

  void unmock() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  }

  Directory workDir(String tag) {
    final Directory dir = Directory.systemTemp.createTempSync(tag);
    addTearDown(() => dir.deleteSync(recursive: true));
    return dir;
  }

  group('photo thumbnails', () {
    testWidgets('orientation-applied decode at target width with cache hit',
        (WidgetTester tester) async {
      final Directory dir = workDir('ve-th-');
      final File file = _stage(dir, 've_thumb.jpg', 'IMG_t.jpg');
      final ThumbStore store = ThumbStore();
      final MediaSource source = _photoSource(file, 'IMG_t.jpg');
      final ThumbFetch first =
          (await tester.runAsync(() => store.fetchPhoto(source)))!;
      expect(first.info.status, ThumbStatus.ready);
      expect(first.info.width, 256);
      expect(first.info.height, 384);
      expect(first.bytes, isNotNull);
      final ThumbFetch second =
          (await tester.runAsync(() => store.fetchPhoto(source)))!;
      expect(second.bytes, first.bytes);
      expect(store.cache.hits, 1);
      expect(store.cache.misses, 1);
    });

    testWidgets('small PNG scales to target width, aspect kept',
        (WidgetTester tester) async {
      final Directory dir = workDir('ve-th-');
      final File file = _stage(dir, 've_plain.png', 'shot.png');
      final ThumbStore store = ThumbStore();
      final ThumbFetch fetch = (await tester.runAsync(
        () => store.fetchPhoto(
          MediaSource(
            id: file.path,
            displayName: 'shot.png',
            kind: MediaKind.photo,
            filePath: file.path,
            mime: 'image/png',
          ),
        ),
      ))!;
      expect(fetch.info.status, ThumbStatus.ready);
      expect(fetch.info.width, 256);
      expect(fetch.info.height, 256);
    });

    testWidgets('scan-less JPEG fails without throwing',
        (WidgetTester tester) async {
      final Directory dir = workDir('ve-th-');
      final File file = _stage(dir, 've_exif.jpg', 'noscan.jpg');
      final ThumbStore store = ThumbStore();
      final ThumbFetch fetch = (await tester.runAsync(
        () => store.fetchPhoto(_photoSource(file, 'noscan.jpg')),
      ))!;
      expect(fetch.info.status, ThumbStatus.failed);
      expect(fetch.bytes, isNull);
    });

    testWidgets('missing file is unavailable',
        (WidgetTester tester) async {
      final ThumbStore store = ThumbStore();
      final ThumbFetch fetch = (await tester.runAsync(
        () => store.fetchPhoto(
          MediaSource(
            id: '/t/gone.jpg',
            displayName: 'gone.jpg',
            kind: MediaKind.photo,
            filePath: '/t/gone.jpg',
          ),
        ),
      ))!;
      expect(fetch.info.status, ThumbStatus.unavailable);
      expect(fetch.bytes, isNull);
    });

    testWidgets('oversize sources are skipped deterministically',
        (WidgetTester tester) async {
      final Directory dir = workDir('ve-th-');
      final File file = _stage(dir, 've_thumb.jpg', 'big.jpg');
      final ThumbStore store = ThumbStore(maxSourceBytes: 10);
      final ThumbFetch fetch = (await tester.runAsync(
        () => store.fetchPhoto(_photoSource(file, 'big.jpg')),
      ))!;
      expect(fetch.info.status, ThumbStatus.unavailable);
      expect(fetch.info.reason, 'source too large');
    });

    testWidgets('garbage bytes fail without throwing',
        (WidgetTester tester) async {
      final Directory dir = workDir('ve-th-');
      final File file = File(
        '${dir.path}${Platform.pathSeparator}junk.jpg',
      )..writeAsBytesSync(<int>[0, 1, 2, 3, 4, 5]);
      final ThumbStore store = ThumbStore();
      final ThumbFetch fetch = (await tester.runAsync(
        () => store.fetchPhoto(_photoSource(file, 'junk.jpg')),
      ))!;
      expect(fetch.info.status, ThumbStatus.failed);
    });
  });

  group('video thumbnails', () {
    testWidgets('scripted frame becomes ready and cached',
        (WidgetTester tester) async {
      final Uint8List frame = Uint8List.fromList(<int>[9, 9, 9]);
      mockFrames((MethodCall call) async {
        if (call.method == 'getVideoFrame') {
          return <String, Object?>{
            'bytes': frame,
            'width': 64,
            'height': 48,
          };
        }
        return null;
      });
      try {
        final ThumbStore store = ThumbStore();
        final MediaSource source = MediaSource(
          id: '/t/VID_a.mp4',
          displayName: 'VID_a.mp4',
          kind: MediaKind.video,
          filePath: '/t/VID_a.mp4',
        );
        final ThumbFetch first =
            (await tester.runAsync(() => store.fetchVideo(source)))!;
        expect(first.info.status, ThumbStatus.ready);
        expect(first.info.width, 64);
        expect(first.info.height, 48);
        expect(first.bytes, frame);
        final ThumbFetch second =
            (await tester.runAsync(() => store.fetchVideo(source)))!;
        expect(second.bytes, frame);
        expect(store.cache.hits, 1);
      } finally {
        unmock();
      }
    });

    testWidgets('frame failure stays unavailable with reason',
        (WidgetTester tester) async {
      mockFrames((MethodCall call) async {
        throw PlatformException(code: 'UNAVAILABLE', message: 'no frame');
      });
      try {
        final ThumbStore store = ThumbStore();
        final ThumbFetch fetch = (await tester.runAsync(
          () => store.fetchVideo(
            MediaSource(
              id: '/t/VID_a.mp4',
              displayName: 'VID_a.mp4',
              kind: MediaKind.video,
              filePath: '/t/VID_a.mp4',
            ),
          ),
        ))!;
        expect(fetch.info.status, ThumbStatus.unavailable);
        expect(fetch.info.reason, contains('UNAVAILABLE'));
        expect(fetch.bytes, isNull);
      } finally {
        unmock();
      }
    });

    testWidgets('concurrent duplicate fetches share one decode',
        (WidgetTester tester) async {
      int calls = 0;
      mockFrames((MethodCall call) async {
        calls += 1;
        await Future<void>.delayed(const Duration(milliseconds: 20));
        return <String, Object?>{
          'bytes': Uint8List.fromList(<int>[1]),
          'width': 2,
          'height': 2,
        };
      });
      try {
        final ThumbStore store = ThumbStore();
        final MediaSource source = MediaSource(
          id: '/t/VID_a.mp4',
          displayName: 'VID_a.mp4',
          kind: MediaKind.video,
          filePath: '/t/VID_a.mp4',
        );
        final List<ThumbFetch> both = (await tester.runAsync(
          () => Future.wait(<Future<ThumbFetch>>[
            store.fetchVideo(source),
            store.fetchVideo(source),
          ]),
        ))!;
        expect(both[0].info.status, ThumbStatus.ready);
        expect(both[1].info.status, ThumbStatus.ready);
        expect(calls, 1);
      } finally {
        unmock();
      }
    });
  });

  group('source without local path', () {
    testWidgets('never touches platform channels',
        (WidgetTester tester) async {
      int calls = 0;
      mockFrames((MethodCall call) async {
        calls += 1;
        return null;
      });
      try {
        final ThumbStore store = ThumbStore();
        final MediaSource source = MediaSource(
          id: 'content://media/1',
          displayName: 'a.jpg',
          kind: MediaKind.photo,
          documentUri: 'content://media/1',
        );
        expect(
          ((await tester.runAsync(() => store.fetchPhoto(source)))!)
              .info
              .status,
          ThumbStatus.unavailable,
        );
        expect(
          ((await tester.runAsync(() => store.fetchVideo(source)))!)
              .info
              .status,
          ThumbStatus.unavailable,
        );
        expect(calls, 0);
      } finally {
        unmock();
      }
    });
  });
}
