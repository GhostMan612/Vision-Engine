// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:vision_engine/features/slideshow/slideshow_service.dart';

Future<File> _stage(String asset, Directory dir, String name) async {
  final ByteData data = await rootBundle.load('assets/fixtures/$asset');
  final File file = File('${dir.path}${Platform.pathSeparator}$name');
  await file.writeAsBytes(
    data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes),
  );
  return file;
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  test('ffmpeg_kit_flutter encodes a real MP4 on device', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-mp4-dev-');
    try {
      final File a = await _stage('ve_plain.png', dir, 'a.png');
      final File b = await _stage('ve_thumb.jpg', dir, 'b.jpg');
      final String out = '${dir.path}${Platform.pathSeparator}out.mp4';
      final SlideshowReceipt r = await const SlideshowService().buildMp4(
        <String>[a.path, b.path],
        outPath: out,
        durationMs: 1000,
      );
      expect(r.changed, isTrue, reason: r.warnings.join('; '));
      expect(r.verified, isTrue);
      final File encoded = File(out);
      expect(encoded.lengthSync() > 1000, isTrue);
      final List<int> head = encoded.readAsBytesSync().sublist(0, 12);
      expect(String.fromCharCodes(head.sublist(4, 8)), 'ftyp');
    } finally {
      await dir.delete(recursive: true);
    }
  });
}
