// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'field.dart';
import 'photo_meta.dart';
import 'provenance.dart';
import 'video_meta.dart';

Provenance parseProvenance(Object? raw) {
  for (final Provenance candidate in Provenance.values) {
    if (candidate.name == raw) {
      return candidate;
    }
  }
  return Provenance.unknown;
}

MetaField<String> stringField(Map<String, dynamic> json, String key) {
  final Object? node = json[key];
  if (node is Map<String, dynamic>) {
    final Object? value = node['value'];
    return MetaField<String>(
      value is String ? value : null,
      parseProvenance(node['provenance']),
    );
  }
  return const MetaField<String>(null, Provenance.unknown);
}

MetaField<int> intField(Map<String, dynamic> json, String key) {
  final Object? node = json[key];
  if (node is Map<String, dynamic>) {
    final Object? value = node['value'];
    return MetaField<int>(
      value is int ? value : null,
      parseProvenance(node['provenance']),
    );
  }
  return const MetaField<int>(null, Provenance.unknown);
}

MetaField<double> doubleField(Map<String, dynamic> json, String key) {
  final Object? node = json[key];
  if (node is Map<String, dynamic>) {
    final Object? value = node['value'];
    return MetaField<double>(
      value is num ? value.toDouble() : null,
      parseProvenance(node['provenance']),
    );
  }
  return const MetaField<double>(null, Provenance.unknown);
}

Map<String, dynamic> fieldNode<T>(MetaField<T> field) => <String, dynamic>{
      'value': field.value,
      'provenance': field.provenance.name,
    };

PhotoMeta photoMetaFromJson(Map<String, dynamic> json) => PhotoMeta(
      datetimeOriginal: stringField(json, 'datetimeOriginal'),
      gpsLatitude: doubleField(json, 'gpsLatitude'),
      gpsLongitude: doubleField(json, 'gpsLongitude'),
      orientation: intField(json, 'orientation'),
      make: stringField(json, 'make'),
      model: stringField(json, 'model'),
      exposureTime: stringField(json, 'exposureTime'),
      widthRaw: intField(json, 'widthRaw'),
      heightRaw: intField(json, 'heightRaw'),
      fileSizeBytes: intField(json, 'fileSizeBytes'),
      mime: stringField(json, 'mime'),
    );

Map<String, dynamic> photoMetaToJson(PhotoMeta meta) => <String, dynamic>{
      'datetimeOriginal': fieldNode(meta.datetimeOriginal),
      'gpsLatitude': fieldNode(meta.gpsLatitude),
      'gpsLongitude': fieldNode(meta.gpsLongitude),
      'orientation': fieldNode(meta.orientation),
      'make': fieldNode(meta.make),
      'model': fieldNode(meta.model),
      'exposureTime': fieldNode(meta.exposureTime),
      'widthRaw': fieldNode(meta.widthRaw),
      'heightRaw': fieldNode(meta.heightRaw),
      'fileSizeBytes': fieldNode(meta.fileSizeBytes),
      'mime': fieldNode(meta.mime),
    };

VideoMeta videoMetaFromJson(Map<String, dynamic> json) => VideoMeta(
      durationMs: intField(json, 'durationMs'),
      widthRaw: intField(json, 'widthRaw'),
      heightRaw: intField(json, 'heightRaw'),
      rotation: intField(json, 'rotation'),
      creationTime: stringField(json, 'creationTime'),
      locationLatitude: doubleField(json, 'locationLatitude'),
      locationLongitude: doubleField(json, 'locationLongitude'),
      codec: stringField(json, 'codec'),
      fileSizeBytes: intField(json, 'fileSizeBytes'),
      mime: stringField(json, 'mime'),
    );

Map<String, dynamic> videoMetaToJson(VideoMeta meta) => <String, dynamic>{
      'durationMs': fieldNode(meta.durationMs),
      'widthRaw': fieldNode(meta.widthRaw),
      'heightRaw': fieldNode(meta.heightRaw),
      'rotation': fieldNode(meta.rotation),
      'creationTime': fieldNode(meta.creationTime),
      'locationLatitude': fieldNode(meta.locationLatitude),
      'locationLongitude': fieldNode(meta.locationLongitude),
      'codec': fieldNode(meta.codec),
      'fileSizeBytes': fieldNode(meta.fileSizeBytes),
      'mime': fieldNode(meta.mime),
    };
