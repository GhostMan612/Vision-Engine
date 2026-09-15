// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:metadata_core/metadata_core.dart';
import 'package:vision_engine/adapters/metadata_adapter.dart';

const MethodChannel _channel =
    MethodChannel(MetadataAdapter.channelName);

Map<String, dynamic> _node(Object? value, String provenance) =>
    <String, dynamic>{'value': value, 'provenance': provenance};

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late int calls;
  late Map<String, dynamic> Function(String path) script;

  setUp(() {
    calls = 0;
    script = (_) => <String, dynamic>{};
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, (MethodCall call) async {
      calls += 1;
      final Map<String, Object?> args =
          (call.arguments as Map).cast<String, Object?>();
      return script(args['path'] as String? ?? '');
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(_channel, null);
  });

  group('photo extraction', () {
    final Map<String, dynamic> full = <String, dynamic>{
      'datetimeOriginal': _node('2026:09:14 11:05:23', 'exif'),
      'gpsLatitude': _node(44.9778, 'exif'),
      'gpsLongitude': _node(-93.2650, 'exif'),
      'orientation': _node('6', 'exif'),
      'make': _node('SYNTHETIC', 'exif'),
      'model': _node('SAMPLE CAMERA A', 'exif'),
      'exposureTime': _node('1/120', 'exif'),
      'widthRaw': _node('3000', 'exif'),
      'heightRaw': _node('4000', 'exif'),
      'fileSizeBytes': _node(2450011, 'filesystem'),
    };

    test('full EXIF payload decodes with provenance', () async {
      script = (_) => full;
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.status, ExtractStatus.ok);
      expect(result.warnings, isEmpty);
      expect(result.meta.datetimeOriginal.value, '2026:09:14 11:05:23');
      expect(result.meta.datetimeOriginal.provenance, Provenance.exif);
      expect(result.meta.gpsLatitude.value, closeTo(44.9778, 0.0001));
      expect(result.meta.orientation.value, 6);
      expect(result.meta.fileSizeBytes.value, 2450011);
      expect(
        result.meta.fileSizeBytes.provenance,
        Provenance.filesystem,
      );
      expect(result.meta.mime.value, 'image/jpeg');
      expect(result.meta.mime.provenance, Provenance.filesystem);
      expect(result.meta.width, 4000);
      expect(result.meta.height, 3000);
    });

    test('empty payload stays partial with unknowns', () async {
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.status, ExtractStatus.partial);
      expect(result.meta.orientation.value, isNull);
      expect(
        result.meta.orientation.provenance,
        Provenance.unknown,
      );
      expect(result.meta.width, isNull);
    });

    test('junk orientation warns and leaves dims unswapped', () async {
      script = (_) => <String, dynamic>{
        'orientation': _node('sideways', 'exif'),
        'widthRaw': _node('3000', 'exif'),
        'heightRaw': _node('4000', 'exif'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.status, ExtractStatus.ok);
      expect(result.warnings.length, 1);
      expect(result.meta.orientation.value, isNull);
      expect(
        result.meta.orientation.provenance,
        Provenance.unknown,
      );
      expect(result.meta.width, 3000);
      expect(result.meta.height, 4000);
    });

    test('out-of-range orientation is kept raw, never swapped', () async {
      script = (_) => <String, dynamic>{
        'orientation': _node('9', 'exif'),
        'widthRaw': _node('3000', 'exif'),
        'heightRaw': _node('4000', 'exif'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.meta.orientation.value, 9);
      expect(result.meta.width, 3000);
      expect(result.meta.height, 4000);
    });

    test('zero dimensions are not legitimate', () async {
      script = (_) => <String, dynamic>{
        'widthRaw': _node('0', 'exif'),
        'heightRaw': _node('4000', 'exif'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.meta.widthRaw.value, isNull);
      expect(result.meta.width, isNull);
      expect(result.warnings.length, 1);
    });

    test('unsupported extension never reaches the channel', () async {
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/notes.txt');
      expect(result.status, ExtractStatus.unsupported);
      expect(calls, 0);
      expect(result.meta.mime.value, isNull);
    });

    test('video extension is unsupported for photo probe', () async {
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/VID_a.mp4');
      expect(result.status, ExtractStatus.unsupported);
      expect(calls, 0);
    });

    test('channel failure becomes unreadable', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(_channel, (MethodCall call) async {
        throw PlatformException(code: 'UNREADABLE', message: 'gone');
      });
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.status, ExtractStatus.unreadable);
      expect(result.warnings.single, contains('UNREADABLE'));
      expect(result.meta.make.value, isNull);
    });

    test('MIME lookup is case-insensitive', () async {
      script = (_) => <String, dynamic>{};
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_X.JPG');
      expect(result.meta.mime.value, 'image/jpeg');
      expect(calls, 1);
    });
  });

  group('video extraction', () {
    test('full container payload decodes with provenance', () async {
      script = (_) => <String, dynamic>{
        'durationMs': _node('8340', 'container'),
        'widthRaw': _node('1920', 'container'),
        'heightRaw': _node('1080', 'container'),
        'rotation': _node('90', 'container'),
        'creationTime': _node('2026-09-14T11:05:23.000', 'container'),
        'location': _node('+44.9778-093.2650', 'container'),
        'codec': _node('video/avc', 'container'),
        'fileSizeBytes': _node(11800211, 'filesystem'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/VID_a.mp4');
      expect(result.status, ExtractStatus.ok);
      expect(result.warnings, isEmpty);
      expect(result.meta.durationMs.value, 8340);
      expect(
        result.meta.durationMs.provenance,
        Provenance.container,
      );
      expect(result.meta.rotation.value, 90);
      expect(result.meta.locationLatitude.value, closeTo(44.9778, 0.0001));
      expect(result.meta.locationLongitude.value, closeTo(-93.265, 0.0001));
      expect(
        result.meta.locationLatitude.provenance,
        Provenance.container,
      );
      expect(result.meta.codec.value, 'video/avc');
      expect(result.meta.mime.value, 'video/mp4');
      expect(result.meta.width, 1080);
      expect(result.meta.height, 1920);
    });

    test('missing location stays unknown without warnings', () async {
      script = (_) => <String, dynamic>{
        'durationMs': _node('1000', 'container'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/VID_a.mp4');
      expect(result.status, ExtractStatus.ok);
      expect(result.warnings, isEmpty);
      expect(result.meta.locationLatitude.value, isNull);
      expect(
        result.meta.locationLatitude.provenance,
        Provenance.unknown,
      );
    });

    test('malformed location warns and stays unknown', () async {
      script = (_) => <String, dynamic>{
        'durationMs': _node('1000', 'container'),
        'location': _node('near the lake', 'container'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/VID_a.mp4');
      expect(result.warnings.length, 1);
      expect(result.meta.locationLatitude.value, isNull);
      expect(
        result.meta.locationLatitude.provenance,
        Provenance.unknown,
      );
    });

    test('junk duration and rotation warn', () async {
      script = (_) => <String, dynamic>{
        'durationMs': _node('long', 'container'),
        'rotation': _node('up', 'container'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/VID_a.mp4');
      expect(result.warnings.length, 2);
      expect(result.meta.durationMs.value, isNull);
      expect(result.meta.rotation.value, isNull);
      expect(result.status, ExtractStatus.partial);
    });

    test('unsupported extension never reaches the channel', () async {
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/IMG_a.jpg');
      expect(result.status, ExtractStatus.unsupported);
      expect(calls, 0);
    });

    test('channel failure becomes unreadable', () async {
      TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
          .setMockMethodCallHandler(_channel, (MethodCall call) async {
        throw PlatformException(code: 'UNREADABLE', message: 'gone');
      });
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/VID_a.mp4');
      expect(result.status, ExtractStatus.unreadable);
      expect(result.warnings.single, contains('UNREADABLE'));
    });
  });

  group('media type table', () {
    test('photo and video kinds resolve', () {
      expect(MetadataAdapter.kindOf('/t/a.jpg'), MediaKind.photo);
      expect(MetadataAdapter.kindOf('/t/a.HEIC'), MediaKind.photo);
      expect(MetadataAdapter.kindOf('/t/a.dng'), MediaKind.photo);
      expect(MetadataAdapter.kindOf('/t/a.mp4'), MediaKind.video);
      expect(MetadataAdapter.kindOf('/t/a.MOV'), MediaKind.video);
    });

    test('unknown, missing and bare extensions resolve to null', () {
      expect(MetadataAdapter.kindOf('/t/a.txt'), isNull);
      expect(MetadataAdapter.kindOf('/t/noext'), isNull);
      expect(MetadataAdapter.kindOf('/t/trailing.'), isNull);
      expect(MetadataAdapter.mimeOf('/t/a.txt'), isNull);
    });

    test('MIME values are deterministic', () {
      expect(MetadataAdapter.mimeOf('/t/a.jpeg'), 'image/jpeg');
      expect(MetadataAdapter.mimeOf('/t/a.heif'), 'image/heif');
      expect(MetadataAdapter.mimeOf('/t/a.mkv'), 'video/x-matroska');
    });
  });

  group('status semantics', () {
    test('orientation zero is unknown, never a declared value', () async {
      script = (_) => <String, dynamic>{
        'orientation': _node('0', 'exif'),
        'widthRaw': _node('3000', 'exif'),
        'heightRaw': _node('4000', 'exif'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/IMG_a.jpg');
      expect(result.meta.orientation.value, isNull);
      expect(
        result.meta.orientation.provenance,
        Provenance.unknown,
      );
      expect(result.warnings.length, 1);
      expect(result.meta.width, 3000);
      expect(result.meta.height, 4000);
    });

    test('filesystem facts alone stay partial (photo)', () async {
      script = (_) => <String, dynamic>{
        'fileSizeBytes': _node(454, 'filesystem'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final PhotoResult result =
          await adapter.probePhoto('/t/shot.png');
      expect(result.status, ExtractStatus.partial);
      expect(result.meta.fileSizeBytes.value, 454);
    });

    test('filesystem facts alone stay partial (video)', () async {
      script = (_) => <String, dynamic>{
        'fileSizeBytes': _node(150, 'filesystem'),
      };
      final MetadataAdapter adapter = MetadataAdapter();
      final VideoResult result =
          await adapter.probeVideo('/t/VID_a.mp4');
      expect(result.status, ExtractStatus.partial);
      expect(result.meta.fileSizeBytes.value, 150);
    });
  });
}
