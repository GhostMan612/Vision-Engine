// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:vision_engine/adapters/manip_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late ManipAdapter adapter;
  late List<String> callsMade;
  late Map<String, Object?> Function(Map<String, Object?> args) script;

  const MethodChannel channel = MethodChannel(ManipAdapter.channelName);

  setUp(() {
    callsMade = <String>[];
    script = (_) => const <String, dynamic>{
          'changed': true,
          'verified': true,
          'warnings': <String>[],
        };
    adapter = ManipAdapter(channel: channel);
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall call) async {
      callsMade.add(call.method);
      final Map<String, Object?> args =
          (call.arguments as Map).cast<String, Object?>();
      return script(args);
    });
  });

  tearDown(() {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, null);
  });

  test('shiftDateTime calls the channel with minutes', () async {
    final ManipReceipt receipt = await adapter.shiftDateTime('/t/IMG_a.jpg', minutes: -30);
    expect(callsMade, contains('shiftDateTime'));
    expect(receipt.changed, isTrue);
    expect(receipt.verified, isTrue);
  });

  test('stripGps and setOrientation reach the channel', () async {
    await adapter.stripGps('/t/a.jpg');
    await adapter.setOrientation('/t/a.jpg', orientation: 3);
    expect(callsMade, <String>['stripGps', 'setOrientation']);
  });

  test('receipt surfaces warnings from the channel', () async {
    script = (_) => const <String, dynamic>{
          'changed': true,
          'verified': false,
          'warnings': <String>['read-back mismatch'],
        };
    final ManipReceipt receipt = await adapter.setOrientation('/t/a.jpg', orientation: 3);
    expect(receipt.verified, isFalse);
    expect(receipt.warnings, contains('read-back mismatch'));
  });

  test('channel failure becomes unreadable receipt', () async {
    TestDefaultBinaryMessengerBinding.instance.defaultBinaryMessenger
        .setMockMethodCallHandler(channel, (MethodCall call) async {
      throw PlatformException(code: 'BAD_ARGS', message: 'nope');
    });
    final ManipReceipt receipt = await adapter.stripGps('/t/a.jpg');
    expect(receipt.verified, isFalse);
    expect(receipt.warnings, anyElement(contains('nope')));
  });
}
