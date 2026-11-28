// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:vision_engine/features/erase/erase_service.dart';
import 'package:vision_engine/settings_store.dart';

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

  test('real TFLite model erases on device without touching source', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-erase-dev-');
    try {
      final File source = await _stage('ve_plain.png', dir, 'erase_src.png');
      final List<int> before = await source.readAsBytes();
      final SettingsStore settings = await SettingsStore.load();
      final EraseService service = EraseService(settings: settings);
      final EraseReceipt r = await service.eraseBackground(source.path);
      expect(r.changed, isTrue, reason: r.warnings.join('; '));
      expect(r.verified, isTrue);
      expect(r.outPath, endsWith('_edit.png'));
      expect(File(r.outPath).lengthSync() > 0, isTrue);
      expect(await source.readAsBytes(), before);
    } finally {
      await dir.delete(recursive: true);
    }
  });
}
