// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:typed_data';

class CachedThumb {
  const CachedThumb({
    required this.key,
    required this.bytes,
    required this.width,
    required this.height,
  });

  final String key;
  final Uint8List bytes;
  final int width;
  final int height;

  int get byteSize => bytes.length;
}

String thumbKey({
  required String sourceId,
  required int? sizeBytes,
  required int? modifiedMs,
  required int maxDimension,
}) {
  final String canonical =
      '$sourceId|${sizeBytes ?? -1}|${modifiedMs ?? -1}|$maxDimension';
  int hash = 0xcbf29ce484222325;
  for (final int unit in canonical.codeUnits) {
    hash ^= unit;
    hash = (hash * 0x100000001b3) & 0xFFFFFFFFFFFFFFFF;
  }
  return hash.toRadixString(16).padLeft(16, '0');
}

class ThumbnailCache {
  ThumbnailCache({this.maxEntries = 100, this.maxBytes = 32 * 1024 * 1024});

  final int maxEntries;
  final int maxBytes;
  final Map<String, CachedThumb> _entries = <String, CachedThumb>{};
  final List<String> _order = <String>[];
  int hits = 0;
  int misses = 0;
  int evictions = 0;

  int get length => _entries.length;

  int get currentBytes {
    int total = 0;
    for (final CachedThumb entry in _entries.values) {
      total += entry.byteSize;
    }
    return total;
  }

  CachedThumb? get(String key) {
    final CachedThumb? hit = _entries[key];
    if (hit == null) {
      misses += 1;
      return null;
    }
    hits += 1;
    _order.remove(key);
    _order.add(key);
    return hit;
  }

  void put(CachedThumb thumb) {
    _order.remove(thumb.key);
    _entries.remove(thumb.key);
    _entries[thumb.key] = thumb;
    _order.add(thumb.key);
    while (_entries.length > maxEntries || currentBytes > maxBytes) {
      final String oldest = _order.removeAt(0);
      _entries.remove(oldest);
      evictions += 1;
    }
  }

  void clear() {
    _entries.clear();
    _order.clear();
  }
}
