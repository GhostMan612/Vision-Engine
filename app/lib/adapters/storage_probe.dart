// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:permission_handler/permission_handler.dart';
import 'package:rename_core/rename_core.dart';

import 'local_rename_adapter.dart';

class ProbeReport {
  const ProbeReport({required this.ok, required this.lines});

  final bool ok;
  final List<String> lines;

  String get text => lines.join('\n');
}

Future<String> permissionSummary() async {
  final PermissionStatus photos = await Permission.photos.status;
  final PermissionStatus videos = await Permission.videos.status;
  final PermissionStatus storage = await Permission.storage.status;
  return 'photos=${photos.name} videos=${videos.name} '
      'storage=${storage.name}';
}

Map<String, List<int>> _readFiles(Directory dir) {
  final Map<String, List<int>> out = <String, List<int>>{};
  for (final FileSystemEntity entity in dir.listSync()) {
    if (entity is File) {
      out[basenameOf(entity.path)] = entity.readAsBytesSync();
    }
  }
  return out;
}

bool _sameMultiset(Iterable<List<int>> a, Iterable<List<int>> b) {
  final List<String> left =
      a.map((List<int> bytes) => bytes.join(',')).toList()..sort();
  final List<String> right =
      b.map((List<int> bytes) => bytes.join(',')).toList()..sort();
  if (left.length != right.length) {
    return false;
  }
  for (int i = 0; i < left.length; i++) {
    if (left[i] != right[i]) {
      return false;
    }
  }
  return true;
}

Future<ProbeReport> runAppPrivateProbe(Directory base) async {
  final List<String> lines = <String>['scope=app-private'];
  bool ok = true;
  final Directory probe = Directory(
    '${base.path}${Platform.pathSeparator}'
    've_probe_${DateTime.now().millisecondsSinceEpoch}',
  );
  try {
    probe.createSync();
    final Map<String, List<int>> payloads = <String, List<int>>{
      'IMG_probe_a.jpg': <int>[0, 1, 2, 3],
      'VID_probe_b.mp4': <int>[4, 5, 6],
      'keep.jpg': <int>[7],
    };
    payloads.forEach((String name, List<int> bytes) {
      File('${probe.path}${Platform.pathSeparator}$name')
          .writeAsBytesSync(bytes);
    });
    final LocalRenameAdapter adapter = LocalRenameAdapter(probe);
    final RenameReport plan = adapter.preview(
      prefixes: defaultPrefixes,
      caseSensitive: true,
      recursive: false,
    );
    lines.add(
      'scanned=${plan.scanned} to_rename=${plan.toRename} '
      'clean=${plan.skippedNoPrefix}',
    );
    for (final RenameOp op in plan.planned) {
      lines.add('PLAN: ${op.oldName} -> ${op.newName}');
    }
    if (plan.toRename != 2) {
      ok = false;
      lines.add('UNEXPECTED plan size');
    }
    final Map<String, List<int>> before = _readFiles(probe);
    final ExecuteResult result = adapter.execute(plan);
    lines.add(
      'executed renamed=${result.renamed} '
      'skipped=${result.skipped} errors=${result.errors.length}',
    );
    final Map<String, List<int>> after = _readFiles(probe);
    final bool namesOk = after.length == 3 &&
        after.containsKey('probe_a.jpg') &&
        after.containsKey('probe_b.mp4') &&
        after.containsKey('keep.jpg');
    final bool bytesOk = _sameMultiset(before.values, after.values);
    lines.add('names_ok=$namesOk bytes_ok=$bytesOk');
    if (!namesOk || !bytesOk || result.renamed != 2) {
      ok = false;
    }
    final String manifest = manifestToCsv(plan.planned, status: 'renamed');
    final UndoResult undo =
        adapter.undoRows(parseManifestCsv(manifest));
    lines.add('undo_restored=${undo.restored}');
    if (undo.restored != 2) {
      ok = false;
    }
    final List<String> names = _readFiles(probe).keys.toList()..sort();
    lines.add('after_undo=$names');
    const List<String> expectedNames = <String>[
      'IMG_probe_a.jpg',
      'VID_probe_b.mp4',
      'keep.jpg',
    ];
    bool namesMatch = names.length == expectedNames.length;
    for (int i = 0; namesMatch && i < names.length; i++) {
      namesMatch = names[i] == expectedNames[i];
    }
    if (!namesMatch) {
      ok = false;
    }
  } on FileSystemException catch (e) {
    ok = false;
    lines.add('FILESYSTEM: ${e.message}');
  } on Object catch (e) {
    ok = false;
    lines.add('EXCEPTION: $e');
  } finally {
    if (probe.existsSync()) {
      probe.deleteSync(recursive: true);
    }
  }
  lines.add(ok ? 'APP_PRIVATE_PROBE: OK' : 'APP_PRIVATE_PROBE: FAIL');
  return ProbeReport(ok: ok, lines: lines);
}

Future<ProbeReport> runSharedFolderProbe(String dirPath) async {
  final List<String> lines = <String>['scope=shared target=$dirPath'];
  bool conclusive = false;
  final Directory root = Directory(dirPath);
  if (!root.existsSync()) {
    lines.add('target missing');
    lines.add('SHARED_PROBE: INCONCLUSIVE');
    return ProbeReport(ok: false, lines: lines);
  }
  final Directory probe = Directory(
    '${root.path}${Platform.pathSeparator}'
    'VE_PROBE_${DateTime.now().millisecondsSinceEpoch}',
  );
  try {
    probe.createSync();
    File('${probe.path}${Platform.pathSeparator}IMG_probe_a.jpg')
        .writeAsBytesSync(<int>[10, 11, 12]);
    File('${probe.path}${Platform.pathSeparator}VID_probe_b.mp4')
        .writeAsBytesSync(<int>[13, 14]);
    final LocalRenameAdapter adapter = LocalRenameAdapter(probe);
    final RenameReport plan = adapter.preview(
      prefixes: defaultPrefixes,
      caseSensitive: true,
      recursive: false,
    );
    lines.add('to_rename=${plan.toRename}');
    final ExecuteResult result = adapter.execute(plan);
    lines.add(
      'renamed=${result.renamed} errors=${result.errors.length}',
    );
    for (final String error in result.errors) {
      lines.add('ERROR: $error');
    }
    if (result.renamed == 2 && result.errors.isEmpty) {
      lines.add('RAW_RENAME: WORKS here');
    } else {
      lines.add(
        'RAW_RENAME: BLOCKED here (scoped-storage semantics, '
        'MediaStore path required)',
      );
    }
    final UndoResult undo = adapter.undoRows(
      parseManifestCsv(manifestToCsv(plan.planned, status: 'renamed')),
    );
    lines.add('undo_restored=${undo.restored}');
    conclusive = true;
  } on FileSystemException catch (e) {
    lines.add('FILESYSTEM: ${e.message}');
    conclusive = true;
  } on Object catch (e) {
    lines.add('EXCEPTION: $e');
  } finally {
    if (probe.existsSync()) {
      probe.deleteSync(recursive: true);
    }
  }
  final bool cleaned = !probe.existsSync();
  lines.add('cleaned=$cleaned');
  lines.add(
    conclusive && cleaned ? 'SHARED_PROBE: COMPLETE' : 'SHARED_PROBE: INCONCLUSIVE',
  );
  return ProbeReport(ok: conclusive && cleaned, lines: lines);
}
