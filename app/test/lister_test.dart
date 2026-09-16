// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:media_library/media_library.dart';
import 'package:vision_engine/library/media_lister.dart';

void _file(Directory dir, String name, [List<int> bytes = const <int>[1]]) {
  File('${dir.path}${Platform.pathSeparator}$name')
      .writeAsBytesSync(bytes);
}

void main() {
  group('listMediaSources', () {
    test('mixed collection classifies with boundary respected', () {
      final Directory dir = Directory.systemTemp.createTempSync('ve-ls-');
      try {
        _file(dir, 'VID_b.mp4');
        _file(dir, 'note.txt');
        _file(dir, 'IMG_a.jpg');
        final Directory sub = Directory(
          '${dir.path}${Platform.pathSeparator}sub',
        )..createSync();
        _file(sub, 'nested.jpg');
        final List<MediaSource> found = listMediaSources(dir);
        expect(
          found.map((MediaSource s) => s.displayName),
          <String>['IMG_a.jpg', 'note.txt', 'VID_b.mp4'],
        );
        expect(found[0].kind, MediaKind.photo);
        expect(found[0].mime, 'image/jpeg');
        expect(found[1].kind, MediaKind.unsupported);
        expect(found[1].mime, isNull);
        expect(found[2].kind, MediaKind.video);
        expect(found[0].sizeBytes, 1);
        expect(found[0].modifiedMs, isNotNull);
      } finally {
        dir.deleteSync(recursive: true);
      }
    });

    test('empty and missing directories yield empty lists', () {
      final Directory dir = Directory.systemTemp.createTempSync('ve-ls-');
      try {
        expect(listMediaSources(dir), isEmpty);
        expect(
          listMediaSources(
            Directory('${dir.path}${Platform.pathSeparator}nope'),
          ),
          isEmpty,
        );
      } finally {
        dir.deleteSync(recursive: true);
      }
    });

    test('directories themselves are never records', () {
      final Directory dir = Directory.systemTemp.createTempSync('ve-ls-');
      try {
        Directory('${dir.path}${Platform.pathSeparator}sub').createSync();
        expect(listMediaSources(dir), isEmpty);
      } finally {
        dir.deleteSync(recursive: true);
      }
    });
  });
}
