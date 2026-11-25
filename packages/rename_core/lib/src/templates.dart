// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'model.dart';
import 'plan.dart';

String templateName(String originalName, DateTime time, {String suffix = ''}) {
  final int dot = originalName.lastIndexOf('.');
  final String stamp = suffix.isEmpty
      ? _formatStamp(time)
      : '${_formatStamp(time)}_$suffix';
  if (dot <= 0) {
    return stamp;
  }
  return '$stamp${originalName.substring(dot)}';
}

String _formatStamp(DateTime t) {
  String two(int v) => v.toString().padLeft(2, '0');
  return '${t.year}${two(t.month)}${two(t.day)}_${two(t.hour)}${two(t.minute)}${two(t.second)}';
}

RenameReport buildTemplates({
  required List<RenameEntry> entries,
  required Set<String> existingPaths,
  DateTime? now,
  String suffix = '',
}) {
  final DateTime stamp = now ?? DateTime.now();
  return buildPlan(
    entries: entries,
    existingPaths: existingPaths,
    nameMapper: (RenameEntry entry) => templateName(
      entry.name,
      stamp,
      suffix: suffix,
    ),
  );
}
