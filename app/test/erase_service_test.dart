// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:image/image.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:vision_engine/features/erase/erase_service.dart';
import 'package:vision_engine/settings_store.dart';

Future<SettingsStore> settingsWith(Map<String, Object> values) async {
  SharedPreferences.setMockInitialValues(values);
  return SettingsStore.load();
}

void main() {
  setUpAll(() {
    SharedPreferences.setMockInitialValues(<String, Object>{});
  });

  test('no endpoint configured -> warning, source untouched', () async {
    final SettingsStore settings =
        await settingsWith(<String, Object>{});
    final EraseService service = EraseService(
      settings: settings,
      engine: EraseEngine.cloud,
    );
    final EraseReceipt r = await service.eraseBackground('C:/tmp/erase_a.jpg');
    expect(r.changed, isFalse);
    expect(r.verified, isFalse);
    expect(r.warnings, anyElement(contains('no endpoint')));
  });

  test('no consent -> warning, source untouched', () async {
    final SettingsStore settings = await settingsWith(<String, Object>{
      SettingsStore.keyEraserEndpoint: 'https://eraser.example/api',
      SettingsStore.keyEraserConsent: false,
    });
    final EraseService service = EraseService(
      settings: settings,
      engine: EraseEngine.cloud,
    );
    final EraseReceipt r = await service.eraseBackground('C:/tmp/erase_a.jpg');
    expect(r.changed, isFalse);
    expect(r.warnings, anyElement(contains('consent required')));
  });

  test('consent + endpoint + injected fn -> writes _edit copy', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-erase-');
    try {
      final File source = File('${dir.path}${Platform.pathSeparator}erase_b.jpg');
      await source.writeAsBytes(<int>[1, 2, 3, 4]);
      final SettingsStore settings = await settingsWith(<String, Object>{
        SettingsStore.keyEraserEndpoint: 'https://eraser.example/api',
        SettingsStore.keyEraserConsent: true,
      });
      final EraseService service = EraseService(
        settings: settings,
        engine: EraseEngine.cloud,
        eraseFn: (String endpoint, Uint8List inBytes) async =>
            Uint8List.fromList(inBytes.reversed.toList()),
      );
      final EraseReceipt r = await service.eraseBackground(source.path);
      expect(r.changed, isTrue);
      expect(r.verified, isTrue);
      final File out =
          File('${dir.path}${Platform.pathSeparator}erase_b_edit.jpg');
      expect(await out.readAsBytes(), <int>[4, 3, 2, 1]);
      expect(await source.readAsBytes(), <int>[1, 2, 3, 4]);
    } finally {
      await dir.delete(recursive: true);
    }
  });

  test('on-device mask -> _edit.png alpha cutout, source byte-identical',
      () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-erase-local-');
    try {
      final Image image = Image(width: 8, height: 8);
      for (int y = 0; y < 8; y++) {
        for (int x = 0; x < 8; x++) {
          image.setPixelRgba(x, y, 200, 20, 20, 255);
        }
      }
      final Uint8List sourceBytes = encodePng(image);
      final File source =
          File('${dir.path}${Platform.pathSeparator}local_a.png');
      await source.writeAsBytes(sourceBytes);
      final SettingsStore settings = await settingsWith(<String, Object>{});
      final EraseService service = EraseService(
        settings: settings,
        maskFn: (Float32List rgb, int w, int h) async {
          final Uint8List mask = Uint8List(w * h);
          for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
              mask[y * w + x] = x < w ~/ 2 ? 255 : 0;
            }
          }
          return mask;
        },
      );
      final EraseReceipt r = await service.eraseBackground(source.path);
      expect(r.changed, isTrue);
      expect(r.verified, isTrue);
      expect(r.outPath, endsWith('local_a_edit.png'));
      final Image out = decodePng(
        await File(r.outPath).readAsBytes(),
      )!;
      final Uint8List outBytes =
          out.getBytes(order: ChannelOrder.rgba);
      expect(outBytes[3], 255);
      expect(outBytes[(7 * 8 + 7) * 4 + 3], 0);
      expect(await source.readAsBytes(), sourceBytes);
    } finally {
      await dir.delete(recursive: true);
    }
  });

  test('on-device model unavailable -> no change, never uploads', () async {
    final Directory dir = await Directory.systemTemp.createTemp('ve-erase-offline-');
    try {
      final File source =
          File('${dir.path}${Platform.pathSeparator}local_b.png');
      await source.writeAsBytes(encodePng(Image(width: 4, height: 4)));
      final Uint8List before = await source.readAsBytes();
      final SettingsStore settings = await settingsWith(<String, Object>{
        SettingsStore.keyEraserEndpoint: 'https://eraser.example/api',
        SettingsStore.keyEraserConsent: true,
      });
      int calls = 0;
      final EraseService service = EraseService(
        settings: settings,
        eraseFn: (String endpoint, Uint8List bytes) async {
          calls += 1;
          return null;
        },
        maskFn: (Float32List rgb, int w, int h) async => null,
      );
      final EraseReceipt r = await service.eraseBackground(source.path);
      expect(r.changed, isFalse);
      expect(r.warnings, anyElement(contains('model unavailable')));
      expect(calls, 0);
      expect(await source.readAsBytes(), before);
    } finally {
      await dir.delete(recursive: true);
    }
  });
}
