// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:typed_data';

import 'package:media_library/media_library.dart';
import 'package:test/test.dart';

CachedThumb _thumb(String key, int bytes) => CachedThumb(
      key: key,
      bytes: Uint8List(bytes),
      width: 4,
      height: 4,
    );

void main() {
  group('thumbKey', () {
    test('is stable for identical inputs', () {
      String key() => thumbKey(
            sourceId: '/t/a.jpg',
            sizeBytes: 10,
            modifiedMs: 20,
            maxDimension: 256,
          );
      expect(key(), key());
      expect(key().length, 16);
    });

    test('changes when version inputs change', () {
      String key({int? size, int? mtime, int dim = 256}) => thumbKey(
            sourceId: '/t/a.jpg',
            sizeBytes: size,
            modifiedMs: mtime,
            maxDimension: dim,
          );
      final String base = key(size: 10, mtime: 20);
      expect(key(size: 11, mtime: 20), isNot(base));
      expect(key(size: 10, mtime: 21), isNot(base));
      expect(key(size: 10, mtime: 20, dim: 128), isNot(base));
      expect(key(size: null, mtime: null), isNot(base));
    });
  });

  group('ThumbnailCache', () {
    test('hit and miss accounting', () {
      final ThumbnailCache cache = ThumbnailCache();
      expect(cache.get('a'), isNull);
      cache.put(_thumb('a', 8));
      expect(cache.get('a')?.byteSize, 8);
      expect(cache.hits, 1);
      expect(cache.misses, 1);
      expect(cache.evictions, 0);
    });

    test('evicts least-recently-used past max entries', () {
      final ThumbnailCache cache = ThumbnailCache(maxEntries: 2);
      cache.put(_thumb('a', 8));
      cache.put(_thumb('b', 8));
      expect(cache.get('a')?.key, 'a');
      cache.put(_thumb('c', 8));
      expect(cache.length, 2);
      expect(cache.get('b'), isNull);
      expect(cache.get('a')?.key, 'a');
      expect(cache.get('c')?.key, 'c');
      expect(cache.evictions, 1);
    });

    test('evicts past the byte budget', () {
      final ThumbnailCache cache = ThumbnailCache(maxBytes: 16);
      cache.put(_thumb('a', 8));
      cache.put(_thumb('b', 8));
      expect(cache.currentBytes, 16);
      cache.put(_thumb('c', 8));
      expect(cache.currentBytes <= 16, isTrue);
      expect(cache.evictions, greaterThan(0));
    });

    test('re-put refreshes without duplicating', () {
      final ThumbnailCache cache = ThumbnailCache();
      cache.put(_thumb('a', 8));
      cache.put(_thumb('a', 8));
      expect(cache.length, 1);
      expect(cache.evictions, 0);
    });
  });

  group('pageOf', () {
    test('slices deterministically with clamped edges', () {
      final List<int> all = <int>[0, 1, 2, 3, 4];
      expect(pageOf(all, offset: 1, limit: 2), <int>[1, 2]);
      expect(pageOf(all, offset: 4, limit: 9), <int>[4]);
      expect(pageOf(all, offset: 5, limit: 2), isEmpty);
      expect(pageOf(all, offset: -1, limit: 2), isEmpty);
      expect(pageOf(all, offset: 0, limit: 0), isEmpty);
    });
  });
}
