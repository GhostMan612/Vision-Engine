// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:vision_engine/features/rename/dcim_rename_service.dart';

void main() {
  test('dry run changes nothing and reports planned count', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-dcim-dry-');
    try {
      await File('${dir.path}${Platform.pathSeparator}IMG_a.jpg')
          .writeAsBytes(<int>[1, 2, 3]);
      final DcimRenameService service = DcimRenameService(dir);
      final DcimProof p = await service.roundTrip(execute: false);
      expect(p.toRename, 1);
      expect(p.renamed, 0);
      expect(File('${dir.path}${Platform.pathSeparator}IMG_a.jpg').existsSync(), isTrue);
    } finally {
      await dir.delete(recursive: true);
    }
  });

  test('execute proves byte preservation, undo restores it', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-dcim-run-');
    try {
      final List<int> payload = List<int>.generate(4096, (int i) => i % 251);
      final File source =
          File('${dir.path}${Platform.pathSeparator}IMG_20261127_a.jpg');
      await source.writeAsBytes(payload);
      await File('${dir.path}${Platform.pathSeparator}keep.jpg')
          .writeAsBytes(<int>[9]);
      final DcimRenameService service = DcimRenameService(dir);

      final DcimProof done = await service.roundTrip(execute: true);
      expect(done.renamed, 1);
      expect(done.byteProofOk, isTrue);
      expect(done.byteProofPassed, 1);
      expect(done.byteProofFailed, 0);
      expect(
        File('${dir.path}${Platform.pathSeparator}20261127_a.jpg').existsSync(),
        isTrue,
      );
      expect(source.existsSync(), isFalse);

      final DcimProof undone = await service.undo(done.manifestPath);
      expect(undone.undone, 1);
      expect(undone.byteProofOk, isTrue);
      expect(await source.readAsBytes(), payload);
    } finally {
      await dir.delete(recursive: true);
    }
  });
}
