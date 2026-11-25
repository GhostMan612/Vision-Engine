// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:image/image.dart';

Uint8List? cropJpeg(
  Uint8List bytes, {
  required int left,
  required int top,
  required int width,
  required int height,
  int quality = 90,
}) {
  try {
    final Image? src = decodeJpg(bytes);
    if (src == null) {
      return null;
    }
    final Image cropped = copyCrop(src, x: left, y: top, width: width, height: height);
    return encodeJpg(cropped, quality: quality);
  } catch (_) {
    return null;
  }
}

Uint8List? resizeJpeg(
  Uint8List bytes, {
  int? width,
  int? height,
  int quality = 85,
}) {
  try {
    final Image? src = decodeJpg(bytes);
    if (src == null) {
      return null;
    }
    final Image resized = copyResize(src, width: width, height: height);
    return encodeJpg(resized, quality: quality);
  } catch (_) {
    return null;
  }
}

File? defaultEditCopy(File source) {
  final String path = source.path;
  if (path.isEmpty) {
    return null;
  }
  final String dir = source.parent.path;
  final int dot = path.lastIndexOf('.');
  final String base = dot < 0 ? path.substring(dir.length) : path.substring(dir.length, dot);
  final String ext = dot < 0 ? '' : path.substring(dot);
  return File('$dir${base}_edit$ext');
}
