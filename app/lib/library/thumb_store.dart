// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';
import 'dart:ui' as ui;

import 'package:media_library/media_library.dart';

import '../adapters/metadata_adapter.dart';

class ThumbFetch {
  const ThumbFetch(this.info, this.bytes);

  final ThumbInfo info;
  final Uint8List? bytes;
}

class ThumbStore {
  ThumbStore({
    MetadataAdapter? metadata,
    ThumbnailCache? cache,
    this.maxDimension = 256,
    this.maxSourceBytes = 64 * 1024 * 1024,
  })  : _metadata = metadata ?? MetadataAdapter(),
        _cache = cache ?? ThumbnailCache();

  final MetadataAdapter _metadata;
  final ThumbnailCache _cache;
  final int maxDimension;
  final int maxSourceBytes;
  final Map<String, Future<ThumbFetch>> _inflight =
      <String, Future<ThumbFetch>>{};

  ThumbnailCache get cache => _cache;

  String cacheKey(MediaSource source) => thumbKey(
        sourceId: source.id,
        sizeBytes: source.sizeBytes,
        modifiedMs: source.modifiedMs,
        maxDimension: maxDimension,
      );

  Future<ThumbFetch> fetchPhoto(MediaSource source) {
    if (source.filePath == null) {
      return Future<ThumbFetch>.value(
        const ThumbFetch(
          ThumbInfo(ThumbStatus.unavailable, reason: 'no local path'),
          null,
        ),
      );
    }
    final String key = cacheKey(source);
    return _dedupe(key, () => _decodePhoto(File(source.filePath!), key));
  }

  Future<ThumbFetch> fetchVideo(MediaSource source) {
    if (source.filePath == null) {
      return Future<ThumbFetch>.value(
        const ThumbFetch(
          ThumbInfo(ThumbStatus.unavailable, reason: 'no local path'),
          null,
        ),
      );
    }
    final String key = cacheKey(source);
    return _dedupe(key, () => _decodeVideo(source.filePath!, key));
  }

  Future<ThumbFetch> _dedupe(
    String key,
    Future<ThumbFetch> Function() work,
  ) {
    final Future<ThumbFetch>? existing = _inflight[key];
    if (existing != null) {
      return existing;
    }
    final Future<ThumbFetch> future = work().whenComplete(() {
      _inflight.remove(key);
    });
    _inflight[key] = future;
    return future;
  }

  Future<ThumbFetch> _decodePhoto(File file, String key) async {
    final CachedThumb? hit = _cache.get(key);
    if (hit != null) {
      return ThumbFetch(
        ThumbInfo(
          ThumbStatus.ready,
          key: key,
          width: hit.width,
          height: hit.height,
        ),
        hit.bytes,
      );
    }
    int? size;
    try {
      size = file.lengthSync();
    } on FileSystemException {
      size = null;
    }
    if (size != null && size > maxSourceBytes) {
      return const ThumbFetch(
        ThumbInfo(ThumbStatus.unavailable, reason: 'source too large'),
        null,
      );
    }
    late final Uint8List bytes;
    try {
      bytes = await file.readAsBytes();
    } on FileSystemException catch (e) {
      return ThumbFetch(
        ThumbInfo(
          ThumbStatus.unavailable,
          reason: 'unreadable: ${e.message}',
        ),
        null,
      );
    }
    try {
      final ui.Codec codec = await ui.instantiateImageCodec(
        bytes,
        targetWidth: maxDimension,
      );
      final ui.FrameInfo frame = await codec.getNextFrame();
      final int width = frame.image.width;
      final int height = frame.image.height;
      final ByteData? data = await frame.image.toByteData(
        format: ui.ImageByteFormat.png,
      );
      frame.image.dispose();
      codec.dispose();
      if (data == null) {
        return const ThumbFetch(
          ThumbInfo(ThumbStatus.failed, reason: 'encode failed'),
          null,
        );
      }
      final Uint8List png = data.buffer
          .asUint8List(data.offsetInBytes, data.lengthInBytes);
      _cache.put(
        CachedThumb(key: key, bytes: png, width: width, height: height),
      );
      return ThumbFetch(
        ThumbInfo(
          ThumbStatus.ready,
          key: key,
          width: width,
          height: height,
        ),
        png,
      );
    } on Exception {
      return const ThumbFetch(
        ThumbInfo(ThumbStatus.failed, reason: 'decode failed'),
        null,
      );
    }
  }

  Future<ThumbFetch> _decodeVideo(String path, String key) async {
    final CachedThumb? hit = _cache.get(key);
    if (hit != null) {
      return ThumbFetch(
        ThumbInfo(
          ThumbStatus.ready,
          key: key,
          width: hit.width,
          height: hit.height,
        ),
        hit.bytes,
      );
    }
    final FrameFetch frame = await _metadata.getVideoFrame(path);
    if (!frame.isReady) {
      return ThumbFetch(
        ThumbInfo(
          ThumbStatus.unavailable,
          reason: frame.reason ?? 'no frame',
        ),
        null,
      );
    }
    final Uint8List bytes = frame.bytes!;
    _cache.put(
      CachedThumb(
        key: key,
        bytes: bytes,
        width: frame.width!,
        height: frame.height!,
      ),
    );
    return ThumbFetch(
      ThumbInfo(
        ThumbStatus.ready,
        key: key,
        width: frame.width,
        height: frame.height,
      ),
      bytes,
    );
  }
}
