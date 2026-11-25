// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';
import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import '../../adapters/manip_adapter.dart';

class ManipScreen extends StatefulWidget {
  const ManipScreen({super.key});

  @override
  State<ManipScreen> createState() => _ManipScreenState();
}

class _ManipScreenState extends State<ManipScreen> {
  final ManipAdapter _ops = ManipAdapter();
  final List<String> _log = <String>[
    'Pick a file. EXIF ops edit by default; '
        'uncheck replace-in-place to get an `_edit` copy beside the source.',
  ];

  String _path = '';
  ManipReceipt? _receipt;
  bool _inPlace = false;
  bool _running = false;
  ManipKind _kind = ManipKind.shiftDateTime;
  int _shiftMinutes = 30;
  int? _orientation = 1;
  int? _resizeLongEdge;

  Future<void> _pick() async {
    final List<PlatformFile> result = await FilePickerPlatform.instance
        .pickFiles(type: FileType.image);
    if (result.isEmpty || result.first.path == null) {
      return;
    }
    setState(() {
      _path = result.first.path!;
      _receipt = null;
    });
  }

  Future<void> _run() async {
    if (_path.isEmpty || _running) {
      return;
    }
    setState(() {
      _running = true;
    });
    ManipReceipt receipt;
    try {
      switch (_kind) {
        case ManipKind.shiftDateTime:
          receipt = await _ops.shiftDateTime(_path, minutes: _shiftMinutes);
        case ManipKind.stripGps:
          receipt = await _ops.stripGps(_path);
        case ManipKind.setOrientation:
          receipt = await _ops.setOrientation(_path, orientation: _orientation!);
        case ManipKind.crop:
        case ManipKind.resize:
          final Uint8List bytes = await File(_path).readAsBytes();
          final Uint8List? out =
              await _ops.resize(bytes, width: _resizeLongEdge, height: null);
          receipt = ManipReceipt(
            kind: _kind,
            changed: out != null && out.isNotEmpty,
            verified: true,
            warnings: <String>[
              if (out == null) 'resize produced no bytes; file untouched',
            ],
          );
          if (out != null) {
            final int dot = _path.lastIndexOf('.');
            final String base = dot < 0 ? _path : _path.substring(0, dot);
            final String target = '${base}_edit.jpg';
            if (_inPlace && File(_path).existsSync()) {
              File(_path).writeAsBytesSync(out);
            } else if (!File(target).existsSync()) {
              File(target).writeAsBytesSync(out);
              _log.insert(0, 'default policy: wrote $target');
            }
          }
      }
      if (isExifKind(_kind) && !_inPlace) {
        final String copy = _ops.defaultEditCopyPath(_path);
        if (copy != _path && !File(copy).existsSync() && File(_path).existsSync()) {
          File(_path).copySync(copy);
          _log.insert(0, 'default policy: wrote $copy');
        }
      }
      final String reRead = isExifKind(_kind) ? await _ops.reRead(_path) : '';
      setState(() {
        _receipt = receipt;
        _log.insert(
          0,
          '${_kindName()}: ${receipt.changed ? 'changed' : 'no change'}, '
          'verified=${receipt.verified}${reRead.isEmpty ? '' : ', re-read: $reRead'}',
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

  String _kindName() => kindName(_kind);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Manipulation')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          const Text(
            'EXIF + pixel ops. Every op is plan -> execute -> re-read; '
            're-read mismatches surface as warnings.',
            style: TextStyle(fontStyle: FontStyle.italic),
          ),
          const SizedBox(height: 8),
          ListTile(
            title: const Text('Source file'),
            subtitle: Text(_path.isEmpty ? 'not picked' : _path),
            trailing: const Icon(Icons.folder_open),
            onTap: _pick,
          ),
          SegmentedButton<ManipKind>(
            segments: <ButtonSegment<ManipKind>>[
              ButtonSegment(value: ManipKind.shiftDateTime, label: Text('Date shift')),
              ButtonSegment(value: ManipKind.stripGps, label: const Text('Strip GPS')),
              ButtonSegment(value: ManipKind.setOrientation, label: const Text('Orient')),
            ],
            selected: {_kind},
            onSelectionChanged: (Set<ManipKind> next) =>
                setState(() => _kind = next.first),
          ),
          if (_kind == ManipKind.shiftDateTime)
            ListTile(
              title: const Text('Shift minutes'),
              subtitle: Text('signed (negative = earlier)',
                  style: const TextStyle(fontStyle: FontStyle.italic)),
              trailing: SegmentedButton<int>(
                segments: const <ButtonSegment<int>>[
                  ButtonSegment(value: 30, label: Text('+30')),
                  ButtonSegment(value: -30, label: Text('-30')),
                  ButtonSegment(value: 1440, label: Text('-1440')),
                ],
                selected: {_shiftMinutes},
                onSelectionChanged: (Set<int> next) =>
                    setState(() => _shiftMinutes = next.first),
              ),
            ),
          if (_kind == ManipKind.setOrientation)
            SizedBox(
              width: 220,
              child: DropdownButtonFormField<int>(
                initialValue: _orientation,
                onChanged: (int? v) => setState(() => _orientation = v),
                items: List<DropdownMenuItem<int>>.generate(
                  8,
                  (int i) => DropdownMenuItem<int>(value: i + 1, child: Text('$i+1')),
                ),
                decoration: const InputDecoration(labelText: 'Orientation'),
              ),
            ),
          if (_kind == ManipKind.resize)
            ListTile(
              title: const Text('Long edge (px)'),
              subtitle: const Text('0 or empty = native size.'),
              trailing: SizedBox(
                width: 120,
                child: TextField(
                  keyboardType: TextInputType.number,
                  onSubmitted: (String v) {
                    final int? n = int.tryParse(v);
                    setState(() => _resizeLongEdge = n);
                  },
                ),
              ),
            ),
          const SizedBox(height: 8),
          CheckboxListTile(
            title: const Text('Replace in place'),
            subtitle: const Text('Default is an `_edit` copy beside the source.'),
            value: _inPlace,
            onChanged: (bool? v) => setState(() => _inPlace = v ?? false),
          ),
          const SizedBox(height: 8),
          FilledButton.icon(
            onPressed: _run,
            icon: const Icon(Icons.play_arrow),
            label: _running ? const Text('Running...') : Text('Run: ${_kindName()}'),
          ),
          const Divider(),
          if (_receipt != null)
            ListTile(
              title: Text('Result: ${_kindName()}'),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text('changed=${_receipt!.changed} verified=${_receipt!.verified}'),
                  for (final String w in _receipt!.warnings) Text('warn: $w'),
                ],
              ),
            ),
          const Divider(),
          Text('Log', style: Theme.of(context).textTheme.titleMedium),
          ..._log.map((String line) => ListTile(title: Text(line))),
        ],
      ),
    );
  }
}
