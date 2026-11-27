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
  String _musicPath = '';
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

  Future<void> _pickMusic() async {
    final List<PlatformFile> result = await FilePickerPlatform.instance
        .pickFiles(type: FileType.audio);
    if (result.isNotEmpty && result.first.path != null) {
      setState(() {
        _musicPath = result.first.path!;
      });
    }
  }

  Future<void> _runMp4() async {
    if (_photos.isEmpty || _running) {
      return;
    }
    setState(() {
      _running = true;
    });
    try {
      final int dot = _photos.first.lastIndexOf('.');
      final String out = dot < 0
          ? '${_photos.first}_slideshow.mp4'
          : '${_photos.first.substring(0, dot)}_slideshow.mp4';
      final SlideshowReceipt r = await _service.buildMp4(
        _photos,
        outPath: out,
        musicPath: _musicPath.isEmpty ? null : _musicPath,
      );
      setState(() {
        _receipt = r;
        _log.insert(
          0,
          'mp4: ${r.changed ? 'built' : 'failed'}, frames=${r.frames}, verified=${r.verified}',
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
            label: const Text('Build GIF (_edit copy)'),
          ),
          const SizedBox(height: 8),
          ListTile(
            title: const Text('Music (optional, user-supplied)'),
            subtitle: Text(_musicPath.isEmpty ? 'none' : _musicPath),
            trailing: const Icon(Icons.audiotrack),
            onTap: _pickMusic,
          ),
          const SizedBox(height: 8),
          FilledButton.icon(
            onPressed: _runMp4,
            icon: const Icon(Icons.movie),
            label: const Text('Build MP4 (ffmpeg, ADR-009)'),
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
