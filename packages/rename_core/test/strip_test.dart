// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:rename_core/rename_core.dart';
import 'package:test/test.dart';

void main() {
  group('stripLeadingPrefix', () {
    test('strips leading IMG_ and VID_', () {
      expect(
        stripLeadingPrefix('IMG_20260905_110523079_HDR.jpg'),
        '20260905_110523079_HDR.jpg',
      );
      expect(
        stripLeadingPrefix('VID_20260902_200800398.mp4'),
        '20260902_200800398.mp4',
      );
    });

    test('leaves mid-string prefixes alone', () {
      expect(stripLeadingPrefix('MY_IMG_foo.jpg'), isNull);
      expect(stripLeadingPrefix('XVID_trailer.mp4'), isNull);
      expect(stripLeadingPrefix('20200101_plain.jpg'), isNull);
    });

    test('is case-sensitive by default', () {
      expect(stripLeadingPrefix('img_lower.jpg'), isNull);
      expect(stripLeadingPrefix('vid_lower.mp4'), isNull);
      expect(stripLeadingPrefix('Img_mixed.jpg'), isNull);
    });

    test('matches case-insensitively with ignore-case', () {
      expect(
        stripLeadingPrefix('img_lower.jpg', caseSensitive: false),
        'lower.jpg',
      );
      expect(
        stripLeadingPrefix('Vid_mixed.mp4', caseSensitive: false),
        'mixed.mp4',
      );
      expect(
        stripLeadingPrefix('MY_IMG_foo.jpg', caseSensitive: false),
        isNull,
      );
    });

    test('returns empty string when nothing would remain', () {
      expect(stripLeadingPrefix('IMG_'), '');
      expect(stripLeadingPrefix('VID_'), '');
    });

    test('honours custom prefix lists', () {
      expect(
        stripLeadingPrefix('PANO_beach.jpg', prefixes: <String>['PANO_']),
        'beach.jpg',
      );
      expect(
        stripLeadingPrefix('IMG_beach.jpg', prefixes: <String>['PANO_']),
        isNull,
      );
    });

    test('preserves HDR and burst suffixes verbatim', () {
      expect(
        stripLeadingPrefix('IMG_20260905_110523079_HDR.jpg'),
        endsWith('_HDR.jpg'),
      );
      expect(
        stripLeadingPrefix('IMG_0001_BURST001_COVER.jpg'),
        '0001_BURST001_COVER.jpg',
      );
    });
  });
}
