// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:media_library/media_library.dart';
import 'package:test/test.dart';

MediaSource _source(String id, String name) => MediaSource(
      id: id,
      displayName: name,
      kind: MediaKind.photo,
      filePath: id,
    );

void main() {
  group('planDiscovery', () {
    test('sorts case-insensitively with deterministic tiebreaks', () {
      final List<MediaSource> planned = planDiscovery(<MediaSource>[
        _source('/t/b.jpg', 'b.jpg'),
        _source('/t/A.jpg', 'A.jpg'),
        _source('/t/c.jpg', 'C.jpg'),
        _source('/t/a2.jpg', 'a.jpg'),
      ]);
      expect(
        planned.map((MediaSource s) => s.displayName),
        <String>['A.jpg', 'a.jpg', 'b.jpg', 'C.jpg'],
      );
    });

    test('deduplicates by id keeping the first record', () {
      final MediaSource first = _source('/t/a.jpg', 'a.jpg');
      final List<MediaSource> planned = planDiscovery(<MediaSource>[
        _source('/t/b.jpg', 'b.jpg'),
        first,
        MediaSource(
          id: '/t/a.jpg',
          displayName: 'a-renamed.jpg',
          kind: MediaKind.photo,
          filePath: '/t/a.jpg',
        ),
      ]);
      expect(planned.length, 2);
      expect(planned.first.displayName, 'a.jpg');
      expect(planned.last.displayName, 'b.jpg');
    });

    test('empty input stays empty', () {
      expect(planDiscovery(<MediaSource>[]), isEmpty);
    });

    test('identical names order by id', () {
      final List<MediaSource> planned = planDiscovery(<MediaSource>[
        _source('/t/z/same.jpg', 'same.jpg'),
        _source('/t/a/same.jpg', 'same.jpg'),
      ]);
      expect(planned.first.id, '/t/a/same.jpg');
    });
  });
}
