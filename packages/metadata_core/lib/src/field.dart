// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'provenance.dart';

class MetaField<T> {
  const MetaField(this.value, this.provenance);

  final T? value;
  final Provenance provenance;

  bool get isKnown => value != null && provenance != Provenance.unknown;
}
