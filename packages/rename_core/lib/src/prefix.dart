// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
const List<String> defaultPrefixes = <String>['IMG_', 'VID_'];

String? stripLeadingPrefix(
  String filename, {
  List<String> prefixes = defaultPrefixes,
  bool caseSensitive = true,
}) {
  final String nameCmp = caseSensitive ? filename : filename.toUpperCase();
  for (final String prefix in prefixes) {
    final String prefixCmp = caseSensitive ? prefix : prefix.toUpperCase();
    if (nameCmp.startsWith(prefixCmp)) {
      final String stripped = filename.substring(prefix.length);
      if (stripped.isEmpty) {
        return '';
      }
      return stripped;
    }
  }
  return null;
}
