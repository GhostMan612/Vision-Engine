// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'model.dart';

const List<String> manifestFieldnames = <String>[
  'original_name',
  'new_name',
  'original_path',
  'new_path',
  'content_uri',
  'status',
];

String _escape(String value) {
  if (value.contains(',') ||
      value.contains('"') ||
      value.contains('\n') ||
      value.contains('\r')) {
    return '"${value.replaceAll('"', '""')}"';
  }
  return value;
}

String manifestToCsv(
  List<RenameOp> ops, {
  Map<String, String> contentUris = const <String, String>{},
  String status = 'planned',
}) {
  final StringBuffer sb = StringBuffer();
  sb.writeln(manifestFieldnames.join(','));
  for (final RenameOp op in ops) {
    sb.writeln(<String>[
      _escape(op.oldName),
      _escape(op.newName),
      _escape(op.srcPath),
      _escape(op.dstPath),
      _escape(contentUris[op.srcPath] ?? ''),
      _escape(status),
    ].join(','));
  }
  return sb.toString();
}

List<String> _splitCsvLine(String line) {
  final List<String> fields = <String>[];
  final StringBuffer current = StringBuffer();
  bool inQuotes = false;
  int i = 0;
  while (i < line.length) {
    final String ch = line[i];
    if (inQuotes) {
      if (ch == '"') {
        if (i + 1 < line.length && line[i + 1] == '"') {
          current.write('"');
          i += 2;
        } else {
          inQuotes = false;
          i += 1;
        }
      } else {
        current.write(ch);
        i += 1;
      }
    } else {
      if (ch == '"') {
        inQuotes = true;
        i += 1;
      } else if (ch == ',') {
        fields.add(current.toString());
        current.clear();
        i += 1;
      } else {
        current.write(ch);
        i += 1;
      }
    }
  }
  fields.add(current.toString());
  return fields;
}

List<ManifestRow> parseManifestCsv(String csv) {
  final List<String> lines = csv
      .replaceAll('\r\n', '\n')
      .split('\n')
      .where((String line) => line.isNotEmpty)
      .toList();
  if (lines.isEmpty) {
    return <ManifestRow>[];
  }
  final List<String> header = _splitCsvLine(lines.first);
  final List<ManifestRow> rows = <ManifestRow>[];
  for (final String line in lines.skip(1)) {
    final List<String> fields = _splitCsvLine(line);
    final Map<String, String> byName = <String, String>{};
    for (int i = 0; i < header.length && i < fields.length; i++) {
      byName[header[i]] = fields[i];
    }
    rows.add(ManifestRow(
      originalName: byName['original_name'] ?? '',
      newName: byName['new_name'] ?? '',
      originalPath: byName['original_path'] ?? '',
      newPath: byName['new_path'] ?? '',
      contentUri: byName['content_uri'] ?? '',
      status: byName['status'] ?? '',
    ));
  }
  return rows;
}

List<ManifestRow> undoOrder(List<ManifestRow> rows) {
  return rows.reversed.toList();
}
