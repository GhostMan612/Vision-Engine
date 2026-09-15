// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:metadata_core/metadata_core.dart';
import 'package:test/test.dart';

VideoMeta _video({int? rotation, int? width, int? height}) {
  return VideoMeta(
    durationMs: const MetaField<int>(null, Provenance.unknown),
    widthRaw: MetaField<int>(width, Provenance.container),
    heightRaw: MetaField<int>(height, Provenance.container),
    rotation: MetaField<int>(rotation, Provenance.container),
    creationTime: const MetaField<String>(null, Provenance.unknown),
    locationLatitude: const MetaField<double>(null, Provenance.unknown),
    locationLongitude: const MetaField<double>(null, Provenance.unknown),
    codec: const MetaField<String>(null, Provenance.unknown),
    fileSizeBytes: const MetaField<int>(null, Provenance.unknown),
    mime: const MetaField<String>(null, Provenance.unknown),
  );
}

void main() {
  group('rotationSwapsAxes', () {
    test('90 and 270 swap axes', () {
      expect(rotationSwapsAxes(90), isTrue);
      expect(rotationSwapsAxes(270), isTrue);
    });

    test('0, 180, null and anything else keep axes', () {
      expect(rotationSwapsAxes(0), isFalse);
      expect(rotationSwapsAxes(180), isFalse);
      expect(rotationSwapsAxes(null), isFalse);
      expect(rotationSwapsAxes(360), isFalse);
    });
  });

  group('VideoMeta corrected dimensions', () {
    test('rotation 90 portrait reports display size', () {
      final VideoMeta meta = _video(rotation: 90, width: 1920, height: 1080);
      expect(meta.width, 1080);
      expect(meta.height, 1920);
    });

    test('rotation 0 keeps stored size', () {
      final VideoMeta meta = _video(rotation: 0, width: 1920, height: 1080);
      expect(meta.width, 1920);
      expect(meta.height, 1080);
    });

    test('missing raw dimensions yield null, never zero', () {
      final VideoMeta meta = _video(rotation: 90, width: null, height: 1080);
      expect(meta.width, isNull);
      expect(meta.height, isNull);
    });
  });
}
