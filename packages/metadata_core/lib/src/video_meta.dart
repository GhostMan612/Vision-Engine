// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'field.dart';

bool rotationSwapsAxes(int? rotation) =>
    rotation == 90 || rotation == 270;

class VideoMeta {
  const VideoMeta({
    required this.durationMs,
    required this.widthRaw,
    required this.heightRaw,
    required this.rotation,
    required this.creationTime,
    required this.locationLatitude,
    required this.locationLongitude,
    required this.codec,
    required this.fileSizeBytes,
    required this.mime,
  });

  final MetaField<int> durationMs;
  final MetaField<int> widthRaw;
  final MetaField<int> heightRaw;
  final MetaField<int> rotation;
  final MetaField<String> creationTime;
  final MetaField<double> locationLatitude;
  final MetaField<double> locationLongitude;
  final MetaField<String> codec;
  final MetaField<int> fileSizeBytes;
  final MetaField<String> mime;

  int? get width {
    final int? rawWidth = widthRaw.value;
    final int? rawHeight = heightRaw.value;
    if (rawWidth == null || rawHeight == null) {
      return null;
    }
    return rotationSwapsAxes(rotation.value) ? rawHeight : rawWidth;
  }

  int? get height {
    final int? rawWidth = widthRaw.value;
    final int? rawHeight = heightRaw.value;
    if (rawWidth == null || rawHeight == null) {
      return null;
    }
    return rotationSwapsAxes(rotation.value) ? rawWidth : rawHeight;
  }
}
