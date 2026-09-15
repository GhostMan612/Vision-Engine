// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:convert';
import 'dart:io';

import 'package:rename_core/rename_core.dart';
import 'package:test/test.dart';

const List<String> _goldens = <String>[
  'leading_only',
  'case_gate',
  'empty_skip',
  'collision_trio',
  'sort_determinism',
  'recursive',
];

void main() {
  group('golden fixtures', () {
    for (final String name in _goldens) {
      test(name, () {
        final Map<String, dynamic> golden = jsonDecode(
          File('test/golden/$name.json').readAsStringSync(),
        ) as Map<String, dynamic>;
        final List<RenameEntry> entries =
            (golden['entries'] as List<dynamic>).map((dynamic raw) {
          final Map<String, dynamic> map = raw as Map<String, dynamic>;
          return RenameEntry(
            path: map['path'] as String,
            name: map['name'] as String,
            isDir: (map['isDir'] as bool?) ?? false,
            isFile: (map['isFile'] as bool?) ?? true,
          );
        }).toList();
        final Set<String> existing =
            (golden['existing'] as List<dynamic>).cast<String>().toSet();
        final RenameReport report = buildPlan(
          entries: entries,
          existingPaths: existing,
          prefixes: (golden['prefixes'] as List<dynamic>).cast<String>(),
          caseSensitive: (golden['caseSensitive'] as bool?) ?? true,
          recursive: (golden['recursive'] as bool?) ?? false,
        );
        final List<List<String>> planned = report.planned
            .map((RenameOp op) => <String>[op.srcPath, op.dstPath])
            .toList();
        final List<dynamic> rawExpected =
            golden['expectedPlanned'] as List<dynamic>;
        expect(
          planned,
          rawExpected
              .map((dynamic pair) => (pair as List<dynamic>).cast<String>())
              .toList(),
        );
        expect(
          report.skippedCollision,
          (golden['expectedCollisions'] as List<dynamic>).cast<String>(),
        );
        final Map<String, dynamic> counts =
            golden['expectedCounts'] as Map<String, dynamic>;
        expect(report.scanned, counts['scanned']);
        expect(report.planned.length, counts['planned']);
        expect(report.skippedNoPrefix, counts['skippedNoPrefix']);
        expect(report.skippedIsDir, counts['skippedIsDir']);
        expect(report.skippedEmptyResult, counts['skippedEmptyResult']);
        expect(
          report.skippedCollision.length,
          counts['collisions'],
        );
      });
    }
  });
}
