// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'model.dart';
import 'prefix.dart';

class _Candidate {
  const _Candidate(this.entry, this.stripped);

  final RenameEntry entry;
  final String stripped;
}

String _parentOf(RenameEntry entry) {
  return entry.path.substring(0, entry.path.length - entry.name.length);
}

RenameReport buildPlan({
  required List<RenameEntry> entries,
  required Set<String> existingPaths,
  List<String> prefixes = defaultPrefixes,
  bool caseSensitive = true,
  bool recursive = false,
}) {
  final RenameReport report = RenameReport();
  final List<RenameEntry> ordered = List<RenameEntry>.of(entries);
  if (recursive) {
    ordered.sort(
      (RenameEntry a, RenameEntry b) => a.path.compareTo(b.path),
    );
  } else {
    ordered.sort(
      (RenameEntry a, RenameEntry b) => a.name.compareTo(b.name),
    );
  }
  final Set<String> stayingPaths = <String>{};
  final List<_Candidate> candidates = <_Candidate>[];
  for (final RenameEntry entry in ordered) {
    report.scanned += 1;
    if (entry.isDir) {
      report.skippedIsDir += 1;
      continue;
    }
    if (!entry.isFile) {
      report.skippedNoPrefix += 1;
      continue;
    }
    final String? stripped = stripLeadingPrefix(
      entry.name,
      prefixes: prefixes,
      caseSensitive: caseSensitive,
    );
    if (stripped == null) {
      stayingPaths.add(entry.path);
      report.skippedNoPrefix += 1;
    } else if (stripped.isEmpty) {
      report.skippedEmptyResult += 1;
    } else {
      candidates.add(_Candidate(entry, stripped));
    }
  }
  final Set<String> candidateSrcs =
      candidates.map((c) => c.entry.path).toSet();
  final Set<String> seenDestinations = <String>{};
  for (final _Candidate candidate in candidates) {
    final String dst = _parentOf(candidate.entry) + candidate.stripped;
    if (existingPaths.contains(dst) && !candidateSrcs.contains(dst)) {
      report.skippedCollision.add(
        '${candidate.entry.name} -> ${candidate.stripped} (target exists)',
      );
      continue;
    }
    if (stayingPaths.contains(dst)) {
      report.skippedCollision.add(
        '${candidate.entry.name} -> ${candidate.stripped} (target exists)',
      );
      continue;
    }
    if (seenDestinations.contains(dst)) {
      report.skippedCollision.add(
        '${candidate.entry.name} -> ${candidate.stripped} '
        '(duplicate target in batch)',
      );
      continue;
    }
    seenDestinations.add(dst);
    report.planned.add(
      RenameOp(srcPath: candidate.entry.path, dstPath: dst),
    );
  }
  report.planned.sort((RenameOp a, RenameOp b) {
    final int byName = a.oldName.compareTo(b.oldName);
    if (byName != 0) {
      return byName;
    }
    return a.srcPath.compareTo(b.srcPath);
  });
  return report;
}
