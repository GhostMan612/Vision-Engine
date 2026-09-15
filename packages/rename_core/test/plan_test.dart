// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:rename_core/rename_core.dart';
import 'package:test/test.dart';

RenameEntry file(String path, String name) =>
    RenameEntry(path: path, name: name);

void main() {
  group('buildPlan', () {
    test('skips directories and non-files without touching them', () {
      final RenameReport report = buildPlan(
        entries: <RenameEntry>[
          const RenameEntry(path: '/t/sub', name: 'sub', isDir: true),
          const RenameEntry(
            path: '/t/IMG_link.jpg',
            name: 'IMG_link.jpg',
            isFile: false,
          ),
          file('/t/IMG_ok.jpg', 'IMG_ok.jpg'),
        ],
        existingPaths: <String>{
          '/t/sub',
          '/t/IMG_link.jpg',
          '/t/IMG_ok.jpg',
        },
      );
      expect(report.scanned, 3);
      expect(report.skippedIsDir, 1);
      expect(report.skippedNoPrefix, 1);
      expect(
        report.planned.map((RenameOp op) => op.dstPath),
        <String>['/t/ok.jpg'],
      );
    });

    test('collision with unlisted on-disk target blocks the rename', () {
      final RenameReport report = buildPlan(
        entries: <RenameEntry>[file('/t/IMG_d.jpg', 'IMG_d.jpg')],
        existingPaths: <String>{'/t/IMG_d.jpg', '/t/d.jpg'},
      );
      expect(report.planned, isEmpty);
      expect(
        report.skippedCollision,
        <String>['IMG_d.jpg -> d.jpg (target exists)'],
      );
    });

    test('chains through batch sources are planned, not collisions', () {
      final RenameReport report = buildPlan(
        entries: <RenameEntry>[
          file('/t/IMG_VID_a.jpg', 'IMG_VID_a.jpg'),
          file('/t/VID_a.jpg', 'VID_a.jpg'),
        ],
        existingPaths: <String>{'/t/IMG_VID_a.jpg', '/t/VID_a.jpg'},
      );
      expect(
        report.planned.map((RenameOp op) => op.oldName),
        <String>['IMG_VID_a.jpg', 'VID_a.jpg'],
      );
      expect(report.skippedCollision, isEmpty);
    });

    test('staying files block their own names', () {
      final RenameReport report = buildPlan(
        entries: <RenameEntry>[
          file('/t/IMG_x.jpg', 'IMG_x.jpg'),
          file('/t/x.jpg', 'x.jpg'),
        ],
        existingPaths: <String>{'/t/IMG_x.jpg', '/t/x.jpg'},
      );
      expect(report.planned, isEmpty);
      expect(
        report.skippedCollision,
        <String>['IMG_x.jpg -> x.jpg (target exists)'],
      );
    });

    test('recursive mode enumerates by full path', () {
      final RenameReport report = buildPlan(
        entries: <RenameEntry>[
          file('/t/z/IMG_c.jpg', 'IMG_c.jpg'),
          file('/t/z/c.jpg', 'c.jpg'),
          file('/t/a/IMG_k.jpg', 'IMG_k.jpg'),
          file('/t/a/k.jpg', 'k.jpg'),
        ],
        existingPaths: <String>{
          '/t/z/IMG_c.jpg',
          '/t/z/c.jpg',
          '/t/a/IMG_k.jpg',
          '/t/a/k.jpg',
        },
        recursive: true,
      );
      expect(report.planned, isEmpty);
      expect(
        report.skippedCollision,
        <String>[
          'IMG_k.jpg -> k.jpg (target exists)',
          'IMG_c.jpg -> c.jpg (target exists)',
        ],
      );
    });
  });
}
