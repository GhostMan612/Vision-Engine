// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:metadata_core/metadata_core.dart';
import 'package:test/test.dart';

void main() {
  group('parseProvenance', () {
    test('maps the four known names', () {
      expect(parseProvenance('exif'), Provenance.exif);
      expect(parseProvenance('container'), Provenance.container);
      expect(parseProvenance('filesystem'), Provenance.filesystem);
      expect(parseProvenance('unknown'), Provenance.unknown);
    });

    test('falls back to unknown on anything else', () {
      expect(parseProvenance('EXIF'), Provenance.unknown);
      expect(parseProvenance(''), Provenance.unknown);
      expect(parseProvenance(null), Provenance.unknown);
      expect(parseProvenance(42), Provenance.unknown);
      expect(parseProvenance('made-up-source'), Provenance.unknown);
    });
  });

  group('MetaField', () {
    test('carries value with provenance', () {
      const MetaField<String> field =
          MetaField<String>('2026:09:14 11:05:23', Provenance.exif);
      expect(field.value, '2026:09:14 11:05:23');
      expect(field.provenance, Provenance.exif);
      expect(field.isKnown, isTrue);
    });

    test('unknown provenance is never known', () {
      const MetaField<String> field =
          MetaField<String>('x', Provenance.unknown);
      expect(field.isKnown, isFalse);
      const MetaField<String> empty =
          MetaField<String>(null, Provenance.unknown);
      expect(empty.isKnown, isFalse);
    });
  });
}
