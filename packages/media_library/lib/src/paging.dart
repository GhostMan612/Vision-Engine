// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
List<T> pageOf<T>(List<T> ordered, {required int offset, required int limit}) {
  if (offset < 0 || limit <= 0 || offset >= ordered.length) {
    return <T>[];
  }
  final int end =
      offset + limit > ordered.length ? ordered.length : offset + limit;
  return ordered.sublist(offset, end);
}
