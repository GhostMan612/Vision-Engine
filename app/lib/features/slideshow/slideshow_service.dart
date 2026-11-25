// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
export 'slideshow_models.dart';

import 'dart:io';
import 'dart:typed_data';

import 'package:image/image.dart';
import 'slideshow_models.dart';

class SlideshowService {
  const SlideshowService();

  Future<SlideshowReceipt> buildSlideshow(
    List<String> paths, {
    required String outPath,
    int durationMs = 3000,
  }) async {
    if (paths.isEmpty) {
      return const SlideshowReceipt(
        changed: false,
        verified: false,
        warnings: <String>['no photos picked'],
      );
    }
    try {
      final List<Image> frames = <Image>[];
      int width = 0;
      int height = 0;
      for (final String path in paths) {
        final Uint8List bytes = await File(path).readAsBytes();
        final Image? decoded = decodeJpg(bytes) ?? decodePng(bytes);
        if (decoded == null) {
          continue;
        }
        width = width == 0 ? decoded.width : max(width, decoded.width);
        height = height == 0 ? decoded.height : max(height, decoded.height);
        frames.add(decoded);
      }
      if (frames.isEmpty || width == 0) {
        return const SlideshowReceipt(
          changed: false,
          verified: false,
          warnings: <String>['no decodable photos'],
        );
      }
      final Image timeline = Image(width: width, height: height);
      timeline.loopCount = 0;
      timeline.frameDuration = durationMs;
      timeline.frames
        ..clear()
        ..addAll(frames.map((Image f) => copyResize(f, width: width)));
      final Uint8List out = encodeGif(timeline);
      File(outPath).writeAsBytesSync(out);
      return SlideshowReceipt(
        changed: out.isNotEmpty,
        verified: File(outPath).existsSync() && File(outPath).lengthSync() > 0,
        outPath: outPath,
        frames: frames.length,
      );
    } catch (e) {
      return SlideshowReceipt(
        changed: false,
        verified: false,
        warnings: <String>['slideshow failed: $e'],
      );
    }
  }
}

int max(int a, int b) => a > b ? a : b;
