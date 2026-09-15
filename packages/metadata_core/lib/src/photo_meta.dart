// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'field.dart';

bool orientationSwapsAxes(int? orientation) =>
    orientation != null && orientation >= 5 && orientation <= 8;

class PhotoMeta {
  const PhotoMeta({
    required this.datetimeOriginal,
    required this.gpsLatitude,
    required this.gpsLongitude,
    required this.orientation,
    required this.make,
    required this.model,
    required this.exposureTime,
    required this.widthRaw,
    required this.heightRaw,
    required this.fileSizeBytes,
    required this.mime,
  });

  final MetaField<String> datetimeOriginal;
  final MetaField<double> gpsLatitude;
  final MetaField<double> gpsLongitude;
  final MetaField<int> orientation;
  final MetaField<String> make;
  final MetaField<String> model;
  final MetaField<String> exposureTime;
  final MetaField<int> widthRaw;
  final MetaField<int> heightRaw;
  final MetaField<int> fileSizeBytes;
  final MetaField<String> mime;

  int? get width {
    final int? rawWidth = widthRaw.value;
    final int? rawHeight = heightRaw.value;
    if (rawWidth == null || rawHeight == null) {
      return null;
    }
    return orientationSwapsAxes(orientation.value) ? rawHeight : rawWidth;
  }

  int? get height {
    final int? rawWidth = widthRaw.value;
    final int? rawHeight = heightRaw.value;
    if (rawWidth == null || rawHeight == null) {
      return null;
    }
    return orientationSwapsAxes(orientation.value) ? rawWidth : rawHeight;
  }
}
