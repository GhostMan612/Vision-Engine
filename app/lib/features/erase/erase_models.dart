// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
class EraseReceipt {
  const EraseReceipt({
    required this.changed,
    required this.verified,
    this.outPath = '',
    this.warnings = const <String>[],
  });

  final bool changed;
  final bool verified;
  final String outPath;
  final List<String> warnings;
}
