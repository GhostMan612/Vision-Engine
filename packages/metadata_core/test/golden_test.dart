// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:convert';
import 'dart:io';

import 'package:metadata_core/metadata_core.dart';
import 'package:test/test.dart';

Map<String, dynamic> _load(String name) {
  return jsonDecode(
    File('test/golden/$name.json').readAsStringSync(),
  ) as Map<String, dynamic>;
}

Map<String, dynamic> _withoutExpected(Map<String, dynamic> golden) {
  final Map<String, dynamic> copy = Map<String, dynamic>.of(golden);
  copy.remove('expectedWidth');
  copy.remove('expectedHeight');
  return copy;
}

void main() {
  group('golden fixtures', () {
    test('missing keys decode to unknown (channel-emitter contract)', () {
      final PhotoMeta photo = photoMetaFromJson(<String, dynamic>{});
      expect(photo.orientation.value, isNull);
      expect(photo.orientation.provenance, Provenance.unknown);
      expect(photo.width, isNull);
      final VideoMeta video = videoMetaFromJson(<String, dynamic>{});
      expect(video.durationMs.value, isNull);
      expect(video.durationMs.provenance, Provenance.unknown);
      expect(video.width, isNull);
    });

    test('photo_exif decodes with provenance and corrected dims', () {
      final Map<String, dynamic> golden = _load('photo_exif');
      final PhotoMeta meta =
          photoMetaFromJson(_withoutExpected(golden));
      expect(meta.datetimeOriginal.value, '2026:09:14 11:05:23');
      expect(meta.datetimeOriginal.provenance, Provenance.exif);
      expect(meta.gpsLatitude.value, closeTo(44.9778, 0.0001));
      expect(meta.orientation.value, 6);
      expect(meta.fileSizeBytes.provenance, Provenance.filesystem);
      expect(meta.width, golden['expectedWidth']);
      expect(meta.height, golden['expectedHeight']);
      expect(photoMetaToJson(meta), _withoutExpected(golden));
    });

    test('video_meta decodes with unknown location intact', () {
      final Map<String, dynamic> golden = _load('video_meta');
      final VideoMeta meta =
          videoMetaFromJson(_withoutExpected(golden));
      expect(meta.durationMs.value, 8340);
      expect(meta.rotation.value, 90);
      expect(meta.locationLatitude.value, isNull);
      expect(meta.locationLatitude.provenance, Provenance.unknown);
      expect(meta.codec.value, 'avc1');
      expect(meta.width, golden['expectedWidth']);
      expect(meta.height, golden['expectedHeight']);
      expect(videoMetaToJson(meta), _withoutExpected(golden));
    });

    test('missing keys decode to unknown (channel-emitter contract)', () {
      final PhotoMeta photo = photoMetaFromJson(<String, dynamic>{});
      expect(photo.orientation.value, isNull);
      expect(photo.orientation.provenance, Provenance.unknown);
      expect(photo.width, isNull);
      final VideoMeta video = videoMetaFromJson(<String, dynamic>{});
      expect(video.durationMs.value, isNull);
      expect(video.durationMs.provenance, Provenance.unknown);
      expect(video.width, isNull);
    });

    test('orientation and rotation vectors', () {
      final Map<String, dynamic> golden = _load('orientation_vectors');
      final Map<String, dynamic> photoBase =
          (golden['photoBase'] as Map).cast<String, dynamic>();
      for (final dynamic raw in golden['photoVectors'] as List<dynamic>) {
        final Map<String, dynamic> vector =
            (raw as Map).cast<String, dynamic>();
        final Map<String, dynamic> input =
            Map<String, dynamic>.of(photoBase);
        input['orientation'] = <String, dynamic>{
          'value': vector['orientation'],
          'provenance': 'exif',
        };
        final PhotoMeta meta = photoMetaFromJson(input);
        expect(meta.width, vector['width'],
            reason: 'orientation ${vector['orientation']} width');
        expect(meta.height, vector['height'],
            reason: 'orientation ${vector['orientation']} height');
      }
      final Map<String, dynamic> videoBase =
          (golden['videoBase'] as Map).cast<String, dynamic>();
      for (final dynamic raw in golden['videoVectors'] as List<dynamic>) {
        final Map<String, dynamic> vector =
            (raw as Map).cast<String, dynamic>();
        final Map<String, dynamic> input =
            Map<String, dynamic>.of(videoBase);
        input['rotation'] = <String, dynamic>{
          'value': vector['rotation'],
          'provenance': 'container',
        };
        final VideoMeta meta = videoMetaFromJson(input);
        expect(meta.width, vector['width'],
            reason: 'rotation ${vector['rotation']} width');
        expect(meta.height, vector['height'],
            reason: 'rotation ${vector['rotation']} height');
      }
    });
  });
}
