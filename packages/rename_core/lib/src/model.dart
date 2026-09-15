// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
String basenameOf(String path) {
  final int slash = path.lastIndexOf('/');
  final int backslash = path.lastIndexOf('\\');
  final int cut = slash > backslash ? slash : backslash;
  return cut < 0 ? path : path.substring(cut + 1);
}

class RenameEntry {
  const RenameEntry({
    required this.path,
    required this.name,
    this.isDir = false,
    this.isFile = true,
  });

  final String path;
  final String name;
  final bool isDir;
  final bool isFile;
}

class RenameOp {
  const RenameOp({required this.srcPath, required this.dstPath});

  final String srcPath;
  final String dstPath;

  String get oldName => basenameOf(srcPath);
  String get newName => basenameOf(dstPath);
}

class RenameReport {
  int scanned = 0;
  List<RenameOp> planned = <RenameOp>[];
  int skippedNoPrefix = 0;
  int skippedIsDir = 0;
  int skippedEmptyResult = 0;
  List<String> skippedCollision = <String>[];

  int get toRename => planned.length;
  int get collisionCount => skippedCollision.length;
}

class ManifestRow {
  const ManifestRow({
    required this.originalName,
    required this.newName,
    required this.originalPath,
    required this.newPath,
    required this.contentUri,
    required this.status,
  });

  final String originalName;
  final String newName;
  final String originalPath;
  final String newPath;
  final String contentUri;
  final String status;
}
