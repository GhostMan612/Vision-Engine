// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:math' as math;
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'package:tflite_flutter/tflite_flutter.dart';

typedef MaskFn = Future<Uint8List?> Function(
  Float32List rgb,
  int width,
  int height,
);

Future<Uint8List?> tfliteMask(
  Float32List rgb,
  int width,
  int height,
) async {
  final Interpreter? interpreter = await _createInterpreter();
  if (interpreter == null) {
    return heuristicMask(rgb, width, height);
  }
  try {
    final List<int> inShape = interpreter.getInputTensor(0).shape;
    final List<int> outShape = interpreter.getOutputTensor(0).shape;
    if (inShape.length != 4 || outShape.length != 4) {
      return heuristicMask(rgb, width, height);
    }
    final int inLen = inShape[1] * inShape[2] * inShape[3];
    if (inLen != rgb.length) {
      return heuristicMask(rgb, width, height);
    }
    final int outLen = outShape[1] * outShape[2] * outShape[3];
    final Float32List raw = Float32List(outLen);
    interpreter.run(rgb, raw);

    final int maskW = outShape[2];
    final int maskH = outShape[1];
    final int channels = outShape[3];
    final Uint8List mask = Uint8List(maskW * maskH);
    for (int i = 0; i < mask.length; i++) {
      double confidence;
      if (channels >= 2) {
        final double bg = raw[i * channels];
        final double fg = raw[i * channels + 1];
        confidence = 1 / (1 + math.exp(bg - fg));
      } else {
        confidence = raw[i];
      }
      if (confidence.isNaN) {
        confidence = 0;
      }
      mask[i] = (confidence.clamp(0.0, 1.0) * 255).round();
    }
    if (maskW == width && maskH == height) {
      return mask;
    }
    final Uint8List resized = Uint8List(width * height);
    for (int y = 0; y < height; y++) {
      final int sy = (y * maskH / height).clamp(0, maskH - 1).toInt();
      for (int x = 0; x < width; x++) {
        final int sx = (x * maskW / width).clamp(0, maskW - 1).toInt();
        resized[y * width + x] = mask[sy * maskW + sx];
      }
    }
    return resized;
  } catch (_) {
    return heuristicMask(rgb, width, height);
  } finally {
    interpreter.close();
  }
}

Future<Interpreter?> _createInterpreter() async {
  try {
    final ByteData asset =
        await rootBundle.load('assets/models/selfie_segmenter.tflite');
    return Interpreter.fromBuffer(Uint8List.sublistView(asset));
  } catch (_) {
    return null;
  }
}

Uint8List heuristicMask(Float32List rgb, int width, int height) {
  final Uint8List visited = Uint8List(width * height);
  final List<int> queue = <int>[];
  void seed(int x, int y) {
    final int i = y * width + x;
    if (visited[i] != 0) {
      return;
    }
    visited[i] = 1;
    queue.add(i);
  }

  for (int x = 0; x < width; x++) {
    seed(x, 0);
    seed(x, height - 1);
  }
  for (int y = 0; y < height; y++) {
    seed(0, y);
    seed(width - 1, y);
  }
  for (int head = 0; head < queue.length; head++) {
    final int i = queue[head];
    final int x = i % width;
    final int y = i ~/ width;
    final int r = (rgb[i * 3] * 255).round();
    final int g = (rgb[i * 3 + 1] * 255).round();
    final int b = (rgb[i * 3 + 2] * 255).round();
    void visit(int nx, int ny) {
      if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
        return;
      }
      final int ni = ny * width + nx;
      if (visited[ni] != 0) {
        return;
      }
      final int dr = (rgb[ni * 3] * 255).round() - r;
      final int dg = (rgb[ni * 3 + 1] * 255).round() - g;
      final int db = (rgb[ni * 3 + 2] * 255).round() - b;
      if ((dr.abs() + dg.abs() + db.abs()) <= 45) {
        visited[ni] = 1;
        queue.add(ni);
      }
    }

    visit(x - 1, y);
    visit(x + 1, y);
    visit(x, y - 1);
    visit(x, y + 1);
  }
  final Uint8List mask = Uint8List(width * height);
  for (int i = 0; i < mask.length; i++) {
    mask[i] = visited[i] == 0 ? 255 : 0;
  }
  return mask;
}
