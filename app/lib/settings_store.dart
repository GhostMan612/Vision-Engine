// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:shared_preferences/shared_preferences.dart';

class SettingsStore {
  SettingsStore(this._prefs);

  final SharedPreferences _prefs;

  static const String keyPrefixes = 'prefixes';
  static const String keyCaseSensitive = 'case_sensitive';
  static const String keyRecursive = 'recursive';

  static Future<SettingsStore> load() async {
    final SharedPreferences prefs = await SharedPreferences.getInstance();
    return SettingsStore(prefs);
  }

  List<String> get prefixes {
    final String? raw = _prefs.getString(keyPrefixes);
    if (raw == null || raw.trim().isEmpty) {
      return <String>['IMG_', 'VID_'];
    }
    return raw
        .split(',')
        .map((String s) => s.trim())
        .where((String s) => s.isNotEmpty)
        .toList();
  }

  Future<void> setPrefixes(List<String> value) {
    return _prefs.setString(keyPrefixes, value.join(', '));
  }

  bool get caseSensitive => _prefs.getBool(keyCaseSensitive) ?? true;

  Future<void> setCaseSensitive(bool value) {
    return _prefs.setBool(keyCaseSensitive, value);
  }

  bool get recursive => _prefs.getBool(keyRecursive) ?? false;

  Future<void> setRecursive(bool value) {
    return _prefs.setBool(keyRecursive, value);
  }
}
