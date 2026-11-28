// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:vision_engine/features/rename/dcim_rename_service.dart';

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  test('on-device DCIM-shaped round-trip preserves bytes and undoes', () async {
    final Directory dir =
        await Directory.systemTemp.createTemp('ve-dcim-device-');
    try {
      final File source =
          File('${dir.path}${Platform.pathSeparator}IMG_20261127_dev.jpg');
      await source.writeAsBytes(
        List<int>.generate(8192, (int i) => i % 239),
      );
      final DcimRenameService service = DcimRenameService(dir);
      final DcimProof dry = await service.roundTrip(execute: false);
      expect(dry.toRename, 1);
      expect(dry.renamed, 0);

      final DcimProof done = await service.roundTrip(execute: true);
      expect(done.renamed, 1);
      expect(done.byteProofOk, isTrue);
      expect(done.byteProofFailed, 0);

      final DcimProof undone = await service.undo(done.manifestPath);
      expect(undone.undone, 1);
      expect(undone.byteProofOk, isTrue);
      expect(source.existsSync(), isTrue);
    } finally {
      await dir.delete(recursive: true);
    }
  });
}
