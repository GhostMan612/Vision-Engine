// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:rename_core/rename_core.dart';
import 'package:test/test.dart';

void main() {
  final DateTime stamp = DateTime(2026, 11, 24, 8, 5, 9);

  group('templateName', () {
    test('formats camera names as yyyyMMdd_HHmmss', () {
      expect(
        templateName('IMG_20230102_030405.jpg', stamp),
        '20261124_080509.jpg',
      );
    });

    test('keeps extension for non-prefixed names', () {
      expect(
        templateName('VID_20230102_030405.mp4', DateTime(2026, 1, 1)),
        '20260101_000000.mp4',
      );
    });

    test('appends optional suffix before the extension', () {
      expect(
        templateName('IMG_x.jpg', stamp, suffix: 'b'),
        '20261124_080509_b.jpg',
      );
    });
  });

  group('buildTemplates', () {
    final entries = <RenameEntry>[
      RenameEntry(path: '/t/IMG_a.jpg', name: 'IMG_a.jpg'),
      RenameEntry(path: '/t/IMG_b.jpg', name: 'IMG_b.jpg'),
    ];

    test('plans timestamp destinations via buildPlan', () {
      final RenameReport report = buildTemplates(
        entries: entries,
        existingPaths: <String>{},
        now: stamp,
      );
      expect(report.planned.length, 1);
      expect(report.planned.first.newName, '20261124_080509.jpg');
      expect(report.collisionCount, 1);
    });

    test('skips collisions like any plan', () {
      final RenameReport report = buildTemplates(
        entries: entries,
        existingPaths: <String>{'/t/20261124_080509.jpg'},
        now: stamp,
      );
      expect(report.toRename, 0);
      expect(report.collisionCount, 2);
    });

    test('flows optional suffix through the plan', () {
      final RenameReport report = buildTemplates(
        entries: entries,
        existingPaths: <String>{},
        now: stamp,
        suffix: 'day1',
      );
      expect(report.planned.first.newName, '20261124_080509_day1.jpg');
    });

    test('plans round-trip through manifest + undo codecs', () {
      final RenameReport report = buildTemplates(
        entries: <RenameEntry>[
          RenameEntry(path: '/t/IMG_1.jpg', name: 'IMG_1.jpg'),
        ],
        existingPaths: <String>{},
        now: stamp,
      );
      final String csv = manifestToCsv(report.planned, status: 'done');
      expect(csv, contains('original_name,new_name'));
      expect(parseManifestCsv(csv).length, 1);
      expect(undoOrder(parseManifestCsv(csv)).length, 1);
    });
  });
}
