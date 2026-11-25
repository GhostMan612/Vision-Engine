// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:file_picker_platform_interface/file_picker_platform_interface.dart';
import 'package:flutter/material.dart';

import '../../settings_store.dart';
import 'erase_service.dart';

class EraseScreen extends StatefulWidget {
  const EraseScreen({required this.settings, super.key});

  final SettingsStore settings;

  @override
  State<EraseScreen> createState() => _EraseScreenState();
}

class _EraseScreenState extends State<EraseScreen> {
  late EraseService _service;
  late TextEditingController _endpointCtrl;
  final List<String> _log = <String>[];
  String _path = '';
  bool _consent = false;
  EraseReceipt? _receipt;
  bool _running = false;

  @override
  void initState() {
    super.initState();
    _service = EraseService(settings: widget.settings);
    _endpointCtrl = TextEditingController(text: widget.settings.eraserEndpoint);
    _consent = widget.settings.eraserConsent;
  }

  @override
  void dispose() {
    _endpointCtrl.dispose();
    super.dispose();
  }

  Future<void> _pick() async {
    final List<PlatformFile> result =
        await FilePickerPlatform.instance.pickFiles(type: FileType.image);
    if (result.isEmpty || result.first.path == null) {
      return;
    }
    setState(() {
      _path = result.first.path!;
    });
  }

  Future<void> _run() async {
    if (_path.isEmpty || _running) {
      return;
    }
    setState(() {
      _running = true;
    });
    final EraseReceipt receipt =
        await _service.eraseBackground(_path, consent: _consent);
    setState(() {
      _receipt = receipt;
      _log.insert(0, 'erase: ${receipt.changed ? 'changed' : 'no change'}');
      _running = false;
    });
  }

  Future<void> _saveSettings() async {
    await widget.settings.setEraserEndpoint(_endpointCtrl.text.trim());
    await widget.settings.setEraserConsent(_consent);
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Background Eraser')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: <Widget>[
          const Text(
            'Consent-gated eraser (ADR-007). Writes an `_edit` copy beside '
            'the source by default; the source is untouched unless you '
            'replace in place.',
            style: TextStyle(fontStyle: FontStyle.italic),
          ),
          const SizedBox(height: 8),
          ListTile(
            title: const Text('Source file'),
            subtitle: Text(_path.isEmpty ? 'not picked' : _path),
            trailing: const Icon(Icons.folder_open),
            onTap: _pick,
          ),
          TextField(
            decoration: const InputDecoration(
              labelText: 'Endpoint URL (device settings only)',
            ),
            controller: _endpointCtrl,
          ),
          const SizedBox(height: 8),
          SwitchListTile(
            title: const Text('I consent to sending this image to the endpoint'),
            value: _consent,
            onChanged: (bool? v) => setState(() => _consent = v ?? false),
          ),
          FilledButton.icon(
            onPressed: _run,
            icon: const Icon(Icons.auto_fix_high),
            label: const Text('Erase background'),
          ),
          const Divider(),
          if (_receipt != null)
            ListTile(
              title: Text('Result: ${_receipt!.changed ? 'changed' : 'no change'}'),
              subtitle: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  for (final String w in _receipt!.warnings) Text('warn: $w'),
                  if (_receipt!.outPath.isNotEmpty) Text(_receipt!.outPath),
                ],
              ),
            ),
          const Divider(),
          FilledButton(onPressed: _saveSettings, child: const Text('Save endpoint + consent')),
        ],
      ),
    );
  }
}
