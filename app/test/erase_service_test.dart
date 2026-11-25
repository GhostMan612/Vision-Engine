// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
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
    final EraseService service = EraseService(settings: settings);
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
    final EraseService service = EraseService(settings: settings);
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
}
