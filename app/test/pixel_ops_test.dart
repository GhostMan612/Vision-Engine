// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart';
import 'package:vision_engine/features/manip/pixel_ops.dart';

Uint8List synthJpeg() => encodeJpg(Image(width: 100, height: 50), quality: 90);

void main() {
  group('cropJpeg', () {
    test('returns smaller encoded jpg', () {
      final Uint8List bytes = synthJpeg();
      final Uint8List? out =
          cropJpeg(bytes, left: 10, top: 5, width: 20, height: 10);
      expect(out, isNotNull);
      final Image? decoded = decodeJpg(out!);
      expect(decoded, isNotNull);
      expect(decoded!.width, 20);
      expect(decoded.height, 10);
    });

    test('returns null for undecodable input', () {
      expect(cropJpeg(Uint8List.fromList(const <int>[1, 2, 3]),
          left: 0, top: 0, width: 1, height: 1), isNull);
    });
  });

  group('resizeJpeg', () {
    test('scales the width down', () {
      final Uint8List bytes = synthJpeg();
      final Uint8List? out = resizeJpeg(bytes, width: 50);
      expect(out, isNotNull);
      final Image? decoded = decodeJpg(out!);
      expect(decoded, isNotNull);
      expect(decoded!.width, 50);
    });

    test('returns null for undecodable input', () {
      expect(resizeJpeg(Uint8List.fromList(const <int>[9, 9]), width: 10), isNull);
    });
  });

  group('defaultEditCopy', () {
    test('appends _edit and keeps the extension', () {
      expect(defaultEditCopy(File('/t/IMG_a.jpg'))!.path, '/t/IMG_a_edit.jpg');
    });

    test('appends _edit without extension', () {
      expect(defaultEditCopy(File('/t/noext'))!.path, '/t/noext_edit');
    });
  });
}
