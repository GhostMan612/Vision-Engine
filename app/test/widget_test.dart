// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:vision_engine/adapters/local_rename_adapter.dart';
import 'package:vision_engine/features/rename/rename_screen.dart';
import 'package:vision_engine/settings_store.dart';

void main() {
  testWidgets('rename preview lists planned ops', (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues(<String, Object>{});
    late Directory dir;
    late SettingsStore settings;
    await tester.runAsync(() async {
      dir = await Directory.systemTemp.createTemp('ve-widget-');
      File('${dir.path}${Platform.pathSeparator}IMG_demo_a.jpg')
          .writeAsBytesSync(<int>[1]);
      settings = await SettingsStore.load();
    });
    try {
      await tester.pumpWidget(
        MaterialApp(
          home: RenameScreen(
            adapter: LocalRenameAdapter(dir),
            settings: settings,
          ),
        ),
      );
      await tester.tap(find.text('Preview'));
      await tester.pump();
      expect(find.text('IMG_demo_a.jpg -> demo_a.jpg'), findsOneWidget);
    } finally {
      dir.deleteSync(recursive: true);
    }
  });
}
