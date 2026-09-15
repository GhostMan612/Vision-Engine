// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:rename_core/rename_core.dart';
import 'package:test/test.dart';

void main() {
  group('manifest codec', () {
    test('round-trips ops through CSV', () {
      final List<RenameOp> ops = <RenameOp>[
        const RenameOp(
          srcPath: '/t/IMG_a.jpg',
          dstPath: '/t/a.jpg',
        ),
        const RenameOp(
          srcPath: '/t/VID_b.mp4',
          dstPath: '/t/b.mp4',
        ),
      ];
      final String csv = manifestToCsv(
        ops,
        contentUris: <String, String>{'/t/IMG_a.jpg': 'content://media/11'},
      );
      final List<ManifestRow> rows = parseManifestCsv(csv);
      expect(rows.length, 2);
      expect(rows[0].originalName, 'IMG_a.jpg');
      expect(rows[0].newName, 'a.jpg');
      expect(rows[0].originalPath, '/t/IMG_a.jpg');
      expect(rows[0].newPath, '/t/a.jpg');
      expect(rows[0].contentUri, 'content://media/11');
      expect(rows[0].status, 'planned');
      expect(rows[1].contentUri, '');
    });

    test('quotes commas and double-quotes in names', () {
      final List<RenameOp> ops = <RenameOp>[
        const RenameOp(
          srcPath: '/t/IMG_a,"q".jpg',
          dstPath: '/t/a,"q".jpg',
        ),
      ];
      final List<ManifestRow> rows = parseManifestCsv(manifestToCsv(ops));
      expect(rows.length, 1);
      expect(rows.single.originalName, 'IMG_a,"q".jpg');
      expect(rows.single.newName, 'a,"q".jpg');
    });

    test('undoOrder replays newest first', () {
      final List<ManifestRow> rows = parseManifestCsv(manifestToCsv(
        <RenameOp>[
          const RenameOp(srcPath: '/t/IMG_a.jpg', dstPath: '/t/a.jpg'),
          const RenameOp(srcPath: '/t/IMG_b.jpg', dstPath: '/t/b.jpg'),
          const RenameOp(srcPath: '/t/IMG_c.jpg', dstPath: '/t/c.jpg'),
        ],
      ));
      final List<ManifestRow> undone = undoOrder(rows);
      expect(
        undone.map((ManifestRow r) => r.originalName),
        <String>['IMG_c.jpg', 'IMG_b.jpg', 'IMG_a.jpg'],
      );
    });

    test('empty csv parses to no rows', () {
      expect(parseManifestCsv(''), isEmpty);
      expect(parseManifestCsv('original_name,new_name\n'), isEmpty);
    });
  });
}
