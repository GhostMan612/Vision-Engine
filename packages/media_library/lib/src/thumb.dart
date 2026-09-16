// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
enum ThumbStatus { pending, ready, unavailable, failed }

class ThumbInfo {
  const ThumbInfo(
    this.status, {
    this.key,
    this.width,
    this.height,
    this.reason,
  });

  final ThumbStatus status;
  final String? key;
  final int? width;
  final int? height;
  final String? reason;

  Map<String, dynamic> toJson() => <String, dynamic>{
        'status': status.name,
        'key': key,
        'width': width,
        'height': height,
        'reason': reason,
      };

  static ThumbInfo fromJson(Map<String, dynamic> json) {
    ThumbStatus status = ThumbStatus.pending;
    for (final ThumbStatus candidate in ThumbStatus.values) {
      if (candidate.name == json['status']) {
        status = candidate;
      }
    }
    return ThumbInfo(
      status,
      key: json['key'] as String?,
      width: json['width'] as int?,
      height: json['height'] as int?,
      reason: json['reason'] as String?,
    );
  }
}
