// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:permission_handler/permission_handler.dart';

import '../../adapters/storage_probe.dart';

class ProbeScreen extends StatefulWidget {
  const ProbeScreen({super.key, required this.baseDir});

  final Directory baseDir;

  @override
  State<ProbeScreen> createState() => _ProbeScreenState();
}

class _ProbeScreenState extends State<ProbeScreen> {
  String _report = 'No probe run yet. Probes only touch files they create.';

  void _setReport(String value) {
    if (!mounted) {
      return;
    }
    setState(() {
      _report = value;
    });
  }

  Future<void> _runPrivate() async {
    _setReport('Running app-private probe...');
    final ProbeReport report = await runAppPrivateProbe(widget.baseDir);
    _setReport(report.text);
  }

  Future<void> _permissions() async {
    _setReport(await permissionSummary());
  }

  Future<void> _request() async {
    final Map<Permission, PermissionStatus> out =
        await <Permission>[Permission.photos, Permission.videos].request();
    if (!mounted) {
      return;
    }
    setState(() {
      _report = out.entries
          .map((MapEntry<Permission, PermissionStatus> e) =>
              '${e.key.value}:${e.value.name}')
          .join(' ');
    });
  }

  Future<void> _runShared() async {
    final String? picked = await FilePicker.getDirectoryPath(
      dialogTitle: 'Pick an EMPTY test folder (probe creates its own files)',
    );
    if (picked == null) {
      return;
    }
    _setReport('Running shared-folder probe in $picked ...');
    final ProbeReport report = await runSharedFolderProbe(picked);
    _setReport(report.text);
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          const Text(
            'D2 storage probe. App-private first, shared folder only on '
            'explicit pick. Only VE_PROBE files are ever written.',
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: <Widget>[
              ElevatedButton(
                onPressed: _runPrivate,
                child: const Text('Run app-private probe'),
              ),
              ElevatedButton(
                onPressed: _permissions,
                child: const Text('Check permission status'),
              ),
              ElevatedButton(
                onPressed: _request,
                child: const Text('Request media permission'),
              ),
              ElevatedButton(
                onPressed: _runShared,
                child: const Text('Pick folder + run shared probe'),
              ),
              OutlinedButton(
                onPressed: () {
                  Clipboard.setData(ClipboardData(text: _report));
                },
                child: const Text('Copy report'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SelectableText(_report),
        ],
      ),
    );
  }
}
