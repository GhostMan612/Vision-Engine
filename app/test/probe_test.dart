// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:vision_engine/adapters/storage_probe.dart';

void main() {
  test('app-private probe passes on host', () async {
    final Directory dir = Directory.systemTemp.createTempSync('ve-probe-');
    try {
      final ProbeReport report = await runAppPrivateProbe(dir);
      expect(report.ok, isTrue);
      expect(report.text, contains('APP_PRIVATE_PROBE: OK'));
    } finally {
      dir.deleteSync(recursive: true);
    }
  });

  test('shared-folder probe completes and cleans up on host', () async {
    final Directory dir = Directory.systemTemp.createTempSync('ve-shared-');
    try {
      final ProbeReport report = await runSharedFolderProbe(dir.path);
      expect(report.ok, isTrue);
      expect(report.text, contains('SHARED_PROBE: COMPLETE'));
      expect(dir.listSync(), isEmpty);
    } finally {
      dir.deleteSync(recursive: true);
    }
  });
}
