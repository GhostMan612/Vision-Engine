// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';
import 'package:rename_core/rename_core.dart';

import 'dcim_rename_service.dart';

class DcimScreen extends StatefulWidget {
  const DcimScreen({super.key});

  @override
  State<DcimScreen> createState() => _DcimScreenState();
}

class _DcimScreenState extends State<DcimScreen> {
  String _folder = '';
  String _log = 'Pick a folder first. Preview is a dry run; Execute renames.';
  DcimProof? _proof;
  bool _running = false;

  Future<void> _pick() async {
    final String? picked = await FilePicker.getDirectoryPath(
      dialogTitle: 'Pick the DCIM folder to round-trip (camera prefixes only)',
    );
    if (!mounted || picked == null) {
      return;
    }
    setState(() {
      _folder = picked;
      _proof = null;
      _log = 'Picked $picked — run Preview before Execute.';
    });
  }

  DcimRenameService? _service() {
    if (_folder.isEmpty || !Directory(_folder).existsSync()) {
      return null;
    }
    return DcimRenameService(Directory(_folder));
  }

  Future<void> _preview() async {
    final DcimRenameService? service = _service();
    if (service == null) {
      setState(() => _log = 'No valid folder picked.');
      return;
    }
    final RenameReport plan = service.preview();
    setState(() {
      _log = 'DRY-RUN scanned=${plan.scanned} to_rename=${plan.toRename} '
          'clean=${plan.skippedNoPrefix} collisions=${plan.collisionCount}. '
          'Nothing changed.';
    });
  }

  Future<void> _execute() async {
    final DcimRenameService? service = _service();
    if (service == null || _running) {
      return;
    }
    setState(() => _running = true);
    final DcimProof proof = await service.roundTrip(execute: true);
    if (!mounted) {
      return;
    }
    setState(() {
      _running = false;
      _proof = proof;
      _log = 'EXECUTE renamed=${proof.renamed}/${proof.toRename} '
          'skipped=${proof.skipped} errors=${proof.errors} '
          'byteProof=${proof.byteProofPassed}passed/'
          '${proof.byteProofFailed}failed';
    });
  }

  Future<void> _undo() async {
    final DcimRenameService? service = _service();
    final String? manifest = _proof?.manifestPath;
    if (service == null || manifest == null || manifest.isEmpty || _running) {
      setState(() => _log = 'No manifest yet — Execute a rename first.');
      return;
    }
    setState(() => _running = true);
    final DcimProof proof = await service.undo(manifest);
    if (!mounted) {
      return;
    }
    setState(() {
      _running = false;
      _proof = proof;
      _log = 'UNDO restored=${proof.undone} skipped=${proof.skipped} '
          'errors=${proof.errors} '
          'byteProof=${proof.byteProofPassed}passed/'
          '${proof.byteProofFailed}failed';
    });
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        const Text(
          'DCIM round-trip leg (Phase 5): camera-prefix renames from a '
          'folder you pick explicitly. Execute re-reads every renamed file '
          'and compares a streamed FNV-1a fingerprint before/after; the '
          'source bytes must survive unchanged. Undo uses the CSV manifest.',
          style: TextStyle(fontStyle: FontStyle.italic),
        ),
        const SizedBox(height: 8),
        ListTile(
          title: const Text('Folder'),
          subtitle: Text(_folder.isEmpty ? 'not picked' : _folder),
          trailing: const Icon(Icons.folder_open),
          onTap: _pick,
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: <Widget>[
            OutlinedButton(
              onPressed: _preview,
              child: const Text('Preview (dry run)'),
            ),
            FilledButton(
              onPressed: _execute,
              child: const Text('Execute + byte proof'),
            ),
            OutlinedButton(onPressed: _undo, child: const Text('Undo')),
          ],
        ),
        const SizedBox(height: 12),
        Text(_log),
        if (_proof != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Text(
              'byte-proof OK: ${_proof!.byteProofOk}\n'
              'manifest: $_proof!.manifestPath',
            ),
          ),
      ],
    );
  }
}
