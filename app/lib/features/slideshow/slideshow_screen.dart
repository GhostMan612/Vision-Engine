// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:file_picker_platform_interface/file_picker_platform_interface.dart';
import 'package:flutter/material.dart';

import '../../adapters/metadata_adapter.dart';
import 'slideshow_service.dart';

class SlideshowScreen extends StatefulWidget {
  const SlideshowScreen({super.key});

  @override
  State<SlideshowScreen> createState() => _SlideshowScreenState();
}

class _SlideshowScreenState extends State<SlideshowScreen> {
  final SlideshowService _service = const SlideshowService();
  final List<String> _photos = <String>[];
  final List<String> _log = <String>[];
  SlideshowReceipt? _receipt;
  bool _running = false;

  Future<void> _pick() async {
    final List<PlatformFile> result = await FilePickerPlatform.instance
        .pickFiles(type: FileType.image);
    if (result.isEmpty) {
      return;
    }
    setState(() {
      for (final PlatformFile f in result) {
        if (f.path != null && !_photos.contains(f.path)) {
          _photos.add(f.path!);
        }
      }
      _receipt = null;
    });
  }

  Future<void> _run() async {
    if (_photos.isEmpty || _running) {
      return;
    }
    setState(() {
      _running = true;
    });
    try {
      final String out = File(_photos.first).existsSync()
          ? (_photos.first.lastIndexOf('.') < 0
                ? '${_photos.first}_slideshow.gif'
                : '${_photos.first.substring(0, _photos.first.lastIndexOf('.'))}_slideshow.gif')
          : 'slideshow_out.gif';
      final SlideshowReceipt r = await _service.buildSlideshow(
        _photos,
        outPath: out,
      );
      final String reRead = await MetadataAdapter().probePhoto(r.outPath).then((PhotoResult p) => p.status.name);
      setState(() {
        _receipt = r;
        _log.insert(
          0,
          'slideshow: ${r.changed ? 'built' : 'failed'}, frames=${r.frames}, re-read=$reRead',
        );
      });
    } catch (e) {
      setState(() {
        _log.insert(0, 'error: $e');
      });
    } finally {
      setState(() {
        _running = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Slideshow')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          const Text(
            'v1 (ADR-008): GIF slideshow from picked photos. MP4/music via ffmpeg deferred.',
            style: TextStyle(fontStyle: FontStyle.italic),
          ),
          const SizedBox(height: 8),
          ListTile(
            title: Text('Photos picked (${_photos.length})'),
            onTap: _pick,
            trailing: const Icon(Icons.add),
          ),
          if (_photos.isNotEmpty)
            for (final String p in _photos)
              ListTile(title: Text(p), leading: const Icon(Icons.photo_camera)),
          FilledButton.icon(
            onPressed: _run,
            icon: const Icon(Icons.play_circle),
            label: const Text('Build slideshow (_edit copy)'),
          ),
          const Divider(),
          if (_receipt != null)
            ListTile(
              title: Text('Result: ${_receipt!.changed ? 'changed' : 'no change'}'),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text('frames: ${_receipt!.frames}'),
                  for (final String w in _receipt!.warnings) Text('warn: $w'),
                  if (_receipt!.outPath.isNotEmpty) Text(_receipt!.outPath),
                ],
              ),
            ),
          const Divider(),
          ..._log.map((String line) => ListTile(title: Text(line))),
        ],
      ),
    );
  }
}
