// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart';
import 'package:vision_engine/features/slideshow/slideshow_service.dart';

Uint8List synthJpg(int w, int h) => encodeJpg(Image(width: w, height: h), quality: 85);

void main() {
  test('buildSlideshow writes a gif with frames', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-slideshow-');
    try {
      final List<String> paths = <String>[];
      for (int i = 0; i < 3; i++) {
        final File f =
            File('${dir.path}${Platform.pathSeparator}p$i.jpg');
        await f.writeAsBytes(synthJpg(60, 40));
        paths.add(f.path);
      }
      final File out = File('${dir.path}${Platform.pathSeparator}out_slideshow.gif');
      final SlideshowReceipt r = await const SlideshowService()
          .buildSlideshow(paths, outPath: out.path);
      expect(r.changed, isTrue);
      expect(r.verified, isTrue);
      expect(r.frames, 3);
      expect(await out.exists(), isTrue);
    } finally {
      await dir.delete(recursive: true);
    }
  });

  test('empty pick is a clean no-change receipt', () async {
    final SlideshowReceipt r = await const SlideshowService()
        .buildSlideshow(<String>[], outPath: 'nope.gif');
    expect(r.changed, isFalse);
    expect(r.warnings, isNotEmpty);
  });

  test('buildMp4 empty pick is a clean no-change receipt', () async {
    final SlideshowReceipt r = await const SlideshowService()
        .buildMp4(<String>[], outPath: 'nope.mp4');
    expect(r.changed, isFalse);
    expect(r.warnings, isNotEmpty);
  });
}
