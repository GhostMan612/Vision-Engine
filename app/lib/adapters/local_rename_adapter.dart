// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:rename_core/rename_core.dart';

class ExecuteResult {
  const ExecuteResult({
    required this.renamed,
    required this.skipped,
    required this.errors,
  });

  final int renamed;
  final int skipped;
  final List<String> errors;
}

class UndoResult {
  const UndoResult({
    required this.restored,
    required this.skipped,
    required this.errors,
  });

  final int restored;
  final int skipped;
  final List<String> errors;
}

class LocalRenameAdapter {
  const LocalRenameAdapter(this.directory);

  final Directory directory;

  List<RenameEntry> listEntries({required bool recursive}) {
    final List<RenameEntry> entries = <RenameEntry>[];
    for (final FileSystemEntity entity in directory.listSync(
      recursive: recursive,
      followLinks: false,
    )) {
      final String name = basenameOf(entity.path);
      if (entity is Directory) {
        entries.add(
          RenameEntry(path: entity.path, name: name, isDir: true, isFile: false),
        );
      } else if (entity is File) {
        entries.add(RenameEntry(path: entity.path, name: name));
      } else {
        entries.add(
          RenameEntry(path: entity.path, name: name, isFile: false),
        );
      }
    }
    return entries;
  }

  RenameReport preview({
    required List<String> prefixes,
    required bool caseSensitive,
    required bool recursive,
  }) {
    final List<RenameEntry> entries = listEntries(recursive: recursive);
    final Set<String> existing = <String>{};
    for (final RenameEntry entry in entries) {
      if (!entry.isDir && entry.isFile) {
        existing.add(entry.path);
      }
    }
    return buildPlan(
      entries: entries,
      existingPaths: existing,
      prefixes: prefixes,
      caseSensitive: caseSensitive,
      recursive: recursive,
    );
  }

  ExecuteResult execute(RenameReport report) {
    int renamed = 0;
    int skipped = 0;
    final List<String> errors = <String>[];
    for (final RenameOp op in report.planned) {
      if (File(op.dstPath).existsSync()) {
        report.skippedCollision.add(
          'SKIP (appeared during run): '
          '${op.oldName} -> ${op.newName} (target exists)',
        );
        skipped += 1;
        continue;
      }
      try {
        File(op.srcPath).renameSync(op.dstPath);
        renamed += 1;
      } on FileSystemException catch (e) {
        errors.add('ERROR: ${op.oldName} -> ${op.newName}: ${e.message}');
      }
    }
    return ExecuteResult(renamed: renamed, skipped: skipped, errors: errors);
  }

  UndoResult undoRows(List<ManifestRow> rows, {bool dryRun = false}) {
    int restored = 0;
    int skipped = 0;
    final List<String> errors = <String>[];
    for (final ManifestRow row in undoOrder(rows)) {
      final File src = File(row.newPath);
      final File dst = File(row.originalPath);
      if (!src.existsSync()) {
        skipped += 1;
        errors.add('SKIP (not found): ${row.newName}');
        continue;
      }
      if (dst.existsSync()) {
        skipped += 1;
        errors.add('SKIP (original already exists): ${row.originalName}');
        continue;
      }
      if (dryRun) {
        restored += 1;
        continue;
      }
      try {
        src.renameSync(dst.path);
        restored += 1;
      } on FileSystemException catch (e) {
        errors.add('ERROR restoring ${row.newName}: ${e.message}');
      }
    }
    return UndoResult(restored: restored, skipped: skipped, errors: errors);
  }
}
