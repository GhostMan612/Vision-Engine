// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================

enum ManipKind { shiftDateTime, stripGps, setOrientation, crop, resize }

String kindName(ManipKind kind) => switch (kind) {
      ManipKind.shiftDateTime => 'Date shift',
      ManipKind.stripGps => 'GPS strip',
      ManipKind.setOrientation => 'Orientation fix',
      ManipKind.crop => 'Crop',
      ManipKind.resize => 'Resize',
    };

bool isExifKind(ManipKind kind) => switch (kind) {
      ManipKind.shiftDateTime ||
      ManipKind.stripGps ||
      ManipKind.setOrientation =>
        true,
      ManipKind.crop || ManipKind.resize => false,
    };

class ManipPlan {
  const ManipPlan({
    required this.kind,
    required this.sourcePath,
    required this.destinationPath,
    this.before,
    this.after,
    required this.overwriteInPlace,
  });

  final ManipKind kind;
  final String sourcePath;
  final String destinationPath;
  final String? before;
  final String? after;
  final bool overwriteInPlace;
}

class ManipReceipt {
  const ManipReceipt({
    required this.kind,
    required this.changed,
    required this.verified,
    this.warnings = const <String>[],
    this.reRead = '',
  });

  final ManipKind kind;
  final bool changed;
  final bool verified;
  final List<String> warnings;
  final String reRead;
}
