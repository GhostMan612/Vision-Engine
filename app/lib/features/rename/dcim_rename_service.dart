// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:rename_core/rename_core.dart';

import '../../adapters/local_rename_adapter.dart';

class DcimProof {
  const DcimProof({
    required this.scanned,
    required this.toRename,
    required this.renamed,
    required this.skipped,
    required this.errors,
    required this.byteProofPassed,
    required this.byteProofFailed,
    required this.manifestPath,
    required this.undone,
  });

  final int scanned;
  final int toRename;
  final int renamed;
  final int skipped;
  final int errors;
  final int byteProofPassed;
  final int byteProofFailed;
  final String manifestPath;
  final int undone;

  bool get byteProofOk => toRename > 0 && byteProofFailed == 0;
}

class DcimRenameService {
  const DcimRenameService(
    this.directory, {
    this.prefixes = const <String>['IMG_', 'VID_'],
    this.caseSensitive = true,
  });

  final Directory directory;
  final List<String> prefixes;
  final bool caseSensitive;

  RenameReport preview() {
    return LocalRenameAdapter(directory).preview(
      prefixes: prefixes,
      caseSensitive: caseSensitive,
      recursive: false,
    );
  }

  Future<DcimProof> roundTrip({required bool execute}) async {
    final LocalRenameAdapter adapter = LocalRenameAdapter(directory);
    final RenameReport plan = adapter.preview(
      prefixes: prefixes,
      caseSensitive: caseSensitive,
      recursive: false,
    );
    if (!execute || plan.planned.isEmpty) {
      return DcimProof(
        scanned: plan.scanned,
        toRename: plan.toRename,
        renamed: 0,
        skipped: 0,
        errors: 0,
        byteProofPassed: 0,
        byteProofFailed: 0,
        manifestPath: '',
        undone: 0,
      );
    }
    final Map<String, int> before = <String, int>{};
    for (final RenameOp op in plan.planned) {
      before[op.srcPath] = await _fnv1a(File(op.srcPath));
    }
    final ExecuteResult result = adapter.execute(plan);
    int passed = 0;
    int failed = 0;
    for (final RenameOp op in plan.planned) {
      final File dst = File(op.dstPath);
      if (!dst.existsSync()) {
        continue;
      }
      final int? expected = before[op.srcPath];
      final int now = await _fnv1a(dst);
      if (expected != null && now == expected) {
        passed += 1;
      } else {
        failed += 1;
      }
    }
    final String manifestPath =
        '${directory.path}${Platform.pathSeparator}'
        'renames_${DateTime.now().millisecondsSinceEpoch}.csv';
    File(manifestPath).writeAsStringSync(
      manifestToCsv(plan.planned, status: 'renamed'),
    );
    return DcimProof(
      scanned: plan.scanned,
      toRename: plan.toRename,
      renamed: result.renamed,
      skipped: result.skipped,
      errors: result.errors.length,
      byteProofPassed: passed,
      byteProofFailed: failed,
      manifestPath: manifestPath,
      undone: 0,
    );
  }

  Future<DcimProof> undo(String manifestPath) async {
    final List<ManifestRow> rows =
        parseManifestCsv(File(manifestPath).readAsStringSync());
    final Map<String, int> before = <String, int>{};
    for (final ManifestRow row in rows) {
      final File src = File(row.newPath);
      if (src.existsSync()) {
        before[row.originalPath] = await _fnv1a(src);
      }
    }
    final UndoResult result =
        LocalRenameAdapter(directory).undoRows(rows);
    int passed = 0;
    int failed = 0;
    for (final ManifestRow row in rows) {
      final File restored = File(row.originalPath);
      if (!restored.existsSync()) {
        continue;
      }
      final int? expected = before[row.originalPath];
      if (expected != null && await _fnv1a(restored) == expected) {
        passed += 1;
      } else {
        failed += 1;
      }
    }
    return DcimProof(
      scanned: rows.length,
      toRename: rows.length,
      renamed: 0,
      skipped: result.skipped,
      errors: result.errors.length,
      byteProofPassed: passed,
      byteProofFailed: failed,
      manifestPath: manifestPath,
      undone: result.restored,
    );
  }

  static Future<int> _fnv1a(File file) async {
    int hash = 0xcbf29ce484222325;
    await for (final List<int> chunk in file.openRead()) {
      for (final int byte in chunk) {
        hash = (hash ^ byte) * 0x100000001b3;
        hash &= 0xffffffffffffffff;
      }
    }
    return hash;
  }
}
