// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:media_library/media_library.dart';
import 'package:metadata_core/metadata_core.dart';
import 'package:test/test.dart';

MediaSource _photoSource() => MediaSource(
      id: '/t/IMG_a.jpg',
      displayName: 'IMG_a.jpg',
      kind: MediaKind.photo,
      filePath: '/t/IMG_a.jpg',
      mime: 'image/jpeg',
      sizeBytes: 10,
      modifiedMs: 20,
    );

void main() {
  group('MediaSource', () {
    test('requires an identity transport', () {
      expect(
        () => MediaSource(
          id: 'x',
          displayName: 'x',
          kind: MediaKind.photo,
        ),
        throwsArgumentError,
      );
      expect(
        MediaSource(
          id: 'content://media/1',
          displayName: 'a.jpg',
          kind: MediaKind.photo,
          documentUri: 'content://media/1',
        ).documentUri,
        'content://media/1',
      );
    });

    test('round-trips through JSON', () {
      final MediaSource source = _photoSource();
      final MediaSource back = MediaSource.fromJson(
        Map<String, dynamic>.of(source.toJson()),
      );
      expect(back.id, source.id);
      expect(back.kind, MediaKind.photo);
      expect(back.mime, 'image/jpeg');
      expect(back.sizeBytes, 10);
    });

    test('unknown kind strings stay unsupported, never crash', () {
      final Map<String, dynamic> json =
          Map<String, dynamic>.of(_photoSource().toJson());
      json['kind'] = 'hologram';
      expect(MediaSource.fromJson(json).kind, MediaKind.unsupported);
    });
  });

  group('MediaRecord', () {
    test('photo factory composes core model with record state', () {
      final MediaRecord record = MediaRecord.fromPhoto(
        source: _photoSource(),
        sortIndex: 3,
        photo: photoMetaFromJson(<String, dynamic>{
          'orientation': <String, dynamic>{
            'value': 6,
            'provenance': 'exif',
          },
          'widthRaw': <String, dynamic>{
            'value': 3000,
            'provenance': 'exif',
          },
          'heightRaw': <String, dynamic>{
            'value': 4000,
            'provenance': 'exif',
          },
        }),
        status: ExtractStatus.ok,
        warnings: const <String>['w'],
      );
      expect(record.kind, MediaKind.photo);
      expect(record.photo?.width, 4000);
      expect(record.sortIndex, 3);
      expect(record.thumb.status, ThumbStatus.pending);
    });

    test('unsupported factory carries status and skips thumbs', () {
      final MediaRecord record = MediaRecord.unsupported(
        source: MediaSource(
          id: '/t/n.txt',
          displayName: 'n.txt',
          kind: MediaKind.unsupported,
          filePath: '/t/n.txt',
        ),
        sortIndex: 0,
      );
      expect(record.status, ExtractStatus.unsupported);
      expect(record.thumb.status, ThumbStatus.unavailable);
      expect(record.photo, isNull);
      expect(record.video, isNull);
    });

    test('round-trips through JSON with thumb state', () {
      final MediaRecord record = MediaRecord.fromPhoto(
        source: _photoSource(),
        sortIndex: 1,
        photo: photoMetaFromJson(<String, dynamic>{}),
        status: ExtractStatus.partial,
      );
      final MediaRecord back = MediaRecord.fromJson(
        Map<String, dynamic>.of(record.toJson()),
      );
      expect(back.source.id, '/t/IMG_a.jpg');
      expect(back.status, ExtractStatus.partial);
      expect(back.photo?.orientation.provenance, Provenance.unknown);
      expect(back.thumb.status, ThumbStatus.pending);
    });
  });
}
