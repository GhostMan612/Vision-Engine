// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:convert';
import 'dart:io';

import 'package:rename_core/rename_core.dart';

void _fail(String message) {
  stderr.writeln('dump_plan error: $message');
  exit(2);
}

void main(List<String> args) {
  String? entriesPath;
  String? existingPath;
  List<String> prefixes = defaultPrefixes;
  bool caseSensitive = true;
  bool recursive = false;
  for (int i = 0; i < args.length; i++) {
    final String arg = args[i];
    if (arg == '--entries' && i + 1 < args.length) {
      entriesPath = args[++i];
    } else if (arg == '--existing' && i + 1 < args.length) {
      existingPath = args[++i];
    } else if (arg == '--prefixes' && i + 1 < args.length) {
      prefixes = args[++i].split(',');
    } else if (arg == '--ignore-case') {
      caseSensitive = false;
    } else if (arg == '--recursive') {
      recursive = true;
    } else {
      _fail('unknown argument: $arg');
    }
  }
  if (entriesPath == null || existingPath == null) {
    _fail('usage: dump_plan.dart --entries <json> --existing <json> '
        '[--prefixes a,b] [--ignore-case] [--recursive]');
  }
  final List<dynamic> rawEntries =
      jsonDecode(File(entriesPath!).readAsStringSync()) as List<dynamic>;
  final List<dynamic> rawExisting =
      jsonDecode(File(existingPath!).readAsStringSync()) as List<dynamic>;
  final List<RenameEntry> entries = rawEntries.map((dynamic raw) {
    final Map<String, dynamic> map = raw as Map<String, dynamic>;
    return RenameEntry(
      path: map['path'] as String,
      name: map['name'] as String,
      isDir: (map['isDir'] as bool?) ?? false,
      isFile: (map['isFile'] as bool?) ?? true,
    );
  }).toList();
  final Set<String> existing =
      rawExisting.map((dynamic e) => e as String).toSet();
  final RenameReport report = buildPlan(
    entries: entries,
    existingPaths: existing,
    prefixes: prefixes,
    caseSensitive: caseSensitive,
    recursive: recursive,
  );
  final Map<String, dynamic> out = <String, dynamic>{
    'planned': report.planned
        .map((RenameOp op) => <String>[op.srcPath, op.dstPath])
        .toList(),
    'collisions': report.skippedCollision,
    'counts': <String, int>{
      'scanned': report.scanned,
      'planned': report.planned.length,
      'skippedNoPrefix': report.skippedNoPrefix,
      'skippedIsDir': report.skippedIsDir,
      'skippedEmptyResult': report.skippedEmptyResult,
      'collisions': report.skippedCollision.length,
    },
  };
  stdout.write(jsonEncode(out));
}
