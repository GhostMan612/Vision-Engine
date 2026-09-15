// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:metadata_core/metadata_core.dart';
import 'package:test/test.dart';

PhotoMeta _photo({int? orientation, int? width, int? height}) {
  return PhotoMeta(
    datetimeOriginal:
        const MetaField<String>(null, Provenance.unknown),
    gpsLatitude: const MetaField<double>(null, Provenance.unknown),
    gpsLongitude: const MetaField<double>(null, Provenance.unknown),
    orientation: MetaField<int>(orientation, Provenance.exif),
    make: const MetaField<String>(null, Provenance.unknown),
    model: const MetaField<String>(null, Provenance.unknown),
    exposureTime: const MetaField<String>(null, Provenance.unknown),
    widthRaw: MetaField<int>(width, Provenance.exif),
    heightRaw: MetaField<int>(height, Provenance.exif),
    fileSizeBytes: const MetaField<int>(null, Provenance.unknown),
    mime: const MetaField<String>(null, Provenance.unknown),
  );
}

void main() {
  group('orientationSwapsAxes', () {
    test('orientations 1-4 keep axes', () {
      for (final int o in <int>[1, 2, 3, 4]) {
        expect(orientationSwapsAxes(o), isFalse);
      }
    });

    test('orientations 5-8 swap axes', () {
      for (final int o in <int>[5, 6, 7, 8]) {
        expect(orientationSwapsAxes(o), isTrue);
      }
    });

    test('null and out-of-range never swap', () {
      expect(orientationSwapsAxes(null), isFalse);
      expect(orientationSwapsAxes(0), isFalse);
      expect(orientationSwapsAxes(9), isFalse);
    });
  });

  group('PhotoMeta corrected dimensions', () {
    test('orientation 6 portrait reports display size', () {
      final PhotoMeta meta = _photo(orientation: 6, width: 3000, height: 4000);
      expect(meta.width, 4000);
      expect(meta.height, 3000);
    });

    test('orientation 1 keeps stored size', () {
      final PhotoMeta meta = _photo(orientation: 1, width: 3000, height: 4000);
      expect(meta.width, 3000);
      expect(meta.height, 4000);
    });

    test('missing raw dimensions yield null, never zero', () {
      final PhotoMeta meta = _photo(orientation: 6, width: null, height: 4000);
      expect(meta.width, isNull);
      expect(meta.height, isNull);
    });
  });
}
