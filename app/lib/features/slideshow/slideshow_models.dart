// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
class SlideshowReceipt {
  const SlideshowReceipt({
    required this.changed,
    required this.verified,
    this.outPath = '',
    this.warnings = const <String>[],
    this.frames = 0,
  });

  final bool changed;
  final bool verified;
  final String outPath;
  final List<String> warnings;
  final int frames;
}
