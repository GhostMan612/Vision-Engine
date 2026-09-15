// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter_test/flutter_test.dart';
import 'package:rename_core/rename_core.dart';
import 'package:vision_engine/adapters/local_rename_adapter.dart';

List<String> _names(Directory dir) {
  final List<String> names = dir
      .listSync()
      .whereType<File>()
      .map((File f) => basenameOf(f.path))
      .toList()
    ..sort();
  return names;
}

void main() {
  test('preview-execute-manifest-undo round trip preserves bytes', () {
    final Directory dir = Directory.systemTemp.createTempSync('ve-flow-');
    try {
      File('${dir.path}${Platform.pathSeparator}IMG_a.jpg')
          .writeAsBytesSync(<int>[1, 2, 3]);
      File('${dir.path}${Platform.pathSeparator}VID_b.mp4')
          .writeAsBytesSync(<int>[4, 5]);
      File('${dir.path}${Platform.pathSeparator}keep.jpg')
          .writeAsBytesSync(<int>[6]);
      final LocalRenameAdapter adapter = LocalRenameAdapter(dir);
      final RenameReport plan = adapter.preview(
        prefixes: defaultPrefixes,
        caseSensitive: true,
        recursive: false,
      );
      expect(plan.toRename, 2);
      final ExecuteResult result = adapter.execute(plan);
      expect(result.renamed, 2);
      expect(result.errors, isEmpty);
      expect(_names(dir), <String>['a.jpg', 'b.mp4', 'keep.jpg']);
      expect(
        File('${dir.path}${Platform.pathSeparator}a.jpg').readAsBytesSync(),
        <int>[1, 2, 3],
      );
      final String csv = manifestToCsv(plan.planned, status: 'renamed');
      final UndoResult undo = adapter.undoRows(parseManifestCsv(csv));
      expect(undo.restored, 2);
      expect(undo.errors, isEmpty);
      expect(_names(dir), <String>['IMG_a.jpg', 'VID_b.mp4', 'keep.jpg']);
    } finally {
      dir.deleteSync(recursive: true);
    }
  });

  test('execute re-checks targets that appear mid-run', () {
    final Directory dir = Directory.systemTemp.createTempSync('ve-toctou-');
    try {
      File('${dir.path}${Platform.pathSeparator}IMG_x.jpg')
          .writeAsBytesSync(<int>[9]);
      final LocalRenameAdapter adapter = LocalRenameAdapter(dir);
      final RenameReport plan = adapter.preview(
        prefixes: defaultPrefixes,
        caseSensitive: true,
        recursive: false,
      );
      expect(plan.toRename, 1);
      File('${dir.path}${Platform.pathSeparator}x.jpg')
          .writeAsBytesSync(<int>[0]);
      final ExecuteResult result = adapter.execute(plan);
      expect(result.renamed, 0);
      expect(result.skipped, 1);
      expect(plan.collisionCount, 1);
    } finally {
      dir.deleteSync(recursive: true);
    }
  });
}
