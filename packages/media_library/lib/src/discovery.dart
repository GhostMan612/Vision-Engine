// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'media_source.dart';

int _compareSources(MediaSource a, MediaSource b) {
  final int byFolded = a.displayName
      .toLowerCase()
      .compareTo(b.displayName.toLowerCase());
  if (byFolded != 0) {
    return byFolded;
  }
  final int byName = a.displayName.compareTo(b.displayName);
  if (byName != 0) {
    return byName;
  }
  return a.id.compareTo(b.id);
}

List<MediaSource> dedupeSources(List<MediaSource> found) {
  final Set<String> seen = <String>{};
  final List<MediaSource> out = <MediaSource>[];
  for (final MediaSource source in found) {
    if (seen.add(source.id)) {
      out.add(source);
    }
  }
  return out;
}

List<MediaSource> sortSources(List<MediaSource> found) {
  final List<MediaSource> ordered = List<MediaSource>.of(found);
  ordered.sort(_compareSources);
  return ordered;
}

List<MediaSource> planDiscovery(List<MediaSource> found) {
  return sortSources(dedupeSources(found));
}
