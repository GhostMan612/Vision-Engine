// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:integration_test/integration_test.dart';
import 'package:metadata_core/metadata_core.dart';
import 'package:path_provider/path_provider.dart';
import 'package:vision_engine/adapters/metadata_adapter.dart';

Future<File> _stage(String asset, Directory dir, String name) async {
  final ByteData data = await rootBundle.load('assets/fixtures/$asset');
  final File file = File('${dir.path}${Platform.pathSeparator}$name');
  await file.writeAsBytes(
    data.buffer.asUint8List(data.offsetInBytes, data.lengthInBytes),
  );
  return file;
}

void main() {
  IntegrationTestWidgetsFlutterBinding.ensureInitialized();

  late Directory work;
  late MetadataAdapter adapter;

  setUpAll(() async {
    final Directory base = await getTemporaryDirectory();
    work = Directory(
      '${base.path}${Platform.pathSeparator}ve-extract-device',
    );
    if (work.existsSync()) {
      work.deleteSync(recursive: true);
    }
    work.createSync(recursive: true);
    adapter = MetadataAdapter();
  });

  tearDownAll(() {
    if (work.existsSync()) {
      work.deleteSync(recursive: true);
    }
  });

  test('synthetic EXIF photo extracts exactly', () async {
    final File file = await _stage('ve_exif.jpg', work, 'IMG_probe.jpg');
    final PhotoResult result = await adapter.probePhoto(file.path);
    expect(result.status, ExtractStatus.ok);
    expect(result.warnings, isEmpty);
    final PhotoMeta meta = result.meta;
    expect(meta.datetimeOriginal.value, '2026:09:14 11:05:23');
    expect(meta.datetimeOriginal.provenance, Provenance.exif);
    expect(meta.make.value, 'SYNTHETIC');
    expect(meta.model.value, 'SAMPLE CAM');
    expect(meta.orientation.value, 6);
    expect(meta.exposureTime.value, '0.008333333333333333');
    expect(meta.exposureTime.provenance, Provenance.exif);
    expect(meta.widthRaw.value, 3000);
    expect(meta.heightRaw.value, 4000);
    expect(meta.width, 4000);
    expect(meta.height, 3000);
    expect(meta.gpsLatitude.value, isNotNull);
    expect(
      (meta.gpsLatitude.value! - 44.9778).abs() < 0.002,
      isTrue,
    );
    expect(
      (meta.gpsLongitude.value! + 93.2650).abs() < 0.002,
      isTrue,
    );
    expect(meta.mime.value, 'image/jpeg');
    expect(meta.fileSizeBytes.value, file.lengthSync());
  });

  test('EXIF-less PNG yields unknowns without crashing', () async {
    final File file = await _stage('ve_plain.png', work, 'shot.png');
    final PhotoResult result = await adapter.probePhoto(file.path);
    expect(result.meta.datetimeOriginal.value, isNull);
    expect(
      result.meta.datetimeOriginal.provenance,
      Provenance.unknown,
    );
    expect(result.meta.orientation.value, isNull);
    expect(result.meta.width, isNull);
    expect(result.meta.mime.value, 'image/png');
    expect(result.meta.fileSizeBytes.value, file.lengthSync());
  });

  test('missing file is unreadable', () async {
    final PhotoResult result = await adapter.probePhoto(
      '${work.path}${Platform.pathSeparator}nope.jpg',
    );
    expect(result.status, ExtractStatus.unreadable);
    expect(result.meta.make.value, isNull);
  });

  test('corrupt file completes gracefully', () async {
    final File file = File(
      '${work.path}${Platform.pathSeparator}empty.jpg',
    );
    file.writeAsBytesSync(<int>[]);
    final PhotoResult result = await adapter.probePhoto(file.path);
    expect(
      <ExtractStatus>[ExtractStatus.partial, ExtractStatus.unreadable],
      contains(result.status),
    );
    expect(photoMetaToJson(result.meta)['make'], isNotNull);
  });

  test('minimal container completes with deterministic shape', () async {
    final File file = await _stage('ve_minimal.mp4', work, 'VID_probe.mp4');
    final VideoResult result = await adapter.probeVideo(file.path);
    expect(result.meta.fileSizeBytes.value, file.lengthSync());
    expect(result.meta.mime.value, 'video/mp4');
    for (final Map<String, dynamic> node in <Map<String, dynamic>>[
      videoMetaToJson(result.meta)['codec'] as Map<String, dynamic>,
      videoMetaToJson(result.meta)['durationMs'] as Map<String, dynamic>,
    ]) {
      expect(
        <String>['container', 'unknown'],
        contains(node['provenance']),
      );
    }
    expect(result.meta.durationMs.value, 0);
    expect(
      result.meta.durationMs.provenance,
      Provenance.container,
    );
  });
}
