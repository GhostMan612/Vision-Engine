// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:rename_core/rename_core.dart';

import '../../adapters/local_rename_adapter.dart';
import '../../settings_store.dart';

class RenameScreen extends StatefulWidget {
  const RenameScreen({
    super.key,
    required this.adapter,
    required this.settings,
  });

  final LocalRenameAdapter adapter;
  final SettingsStore settings;

  @override
  State<RenameScreen> createState() => _RenameScreenState();
}

class _RenameScreenState extends State<RenameScreen> {
  RenameReport? _plan;
  String _log = 'Dry-run is the default. Nothing changes until Execute.';
  String? _manifestPath;

  void _seed() {
    final Map<String, List<int>> seeds = <String, List<int>>{
      'IMG_demo_a.jpg': <int>[1, 2, 3],
      'VID_demo_b.mp4': <int>[4, 5],
      'demo_keep.jpg': <int>[6],
    };
    int created = 0;
    seeds.forEach((String name, List<int> bytes) {
      final File file = File(
        '${widget.adapter.directory.path}${Platform.pathSeparator}$name',
      );
      if (!file.existsSync()) {
        file.writeAsBytesSync(bytes);
        created += 1;
      }
    });
    setState(() {
      _log = 'Seeded $created demo file(s) in vision_demo.';
    });
  }

  void _preview() {
    final RenameReport plan = widget.adapter.preview(
      prefixes: widget.settings.prefixes,
      caseSensitive: widget.settings.caseSensitive,
      recursive: widget.settings.recursive,
    );
    setState(() {
      _plan = plan;
      _log = 'Scanned=${plan.scanned} to_rename=${plan.toRename} '
          'clean=${plan.skippedNoPrefix} dirs=${plan.skippedIsDir} '
          'collisions=${plan.collisionCount}. DRY-RUN — nothing changed.';
    });
  }

  Future<void> _execute() async {
    final RenameReport? plan = _plan;
    if (plan == null || plan.planned.isEmpty) {
      setState(() {
        _log = 'Nothing to execute — run Preview first.';
      });
      return;
    }
    final ExecuteResult result = widget.adapter.execute(plan);
    final String path =
        '${widget.adapter.directory.path}${Platform.pathSeparator}'
        'renames_${DateTime.now().millisecondsSinceEpoch}.csv';
    File(path).writeAsStringSync(
      manifestToCsv(plan.planned, status: 'renamed'),
    );
    if (!mounted) {
      return;
    }
    setState(() {
      _manifestPath = path;
      _log = 'DONE renamed=${result.renamed}/${plan.toRename} '
          'skipped=${result.skipped} errors=${result.errors.length} '
          'manifest=${basenameOf(path)}';
    });
  }

  void _undo() {
    final String? path = _manifestPath;
    if (path == null) {
      setState(() {
        _log = 'No manifest yet — execute a rename first.';
      });
      return;
    }
    final List<ManifestRow> rows =
        parseManifestCsv(File(path).readAsStringSync());
    final UndoResult undo = widget.adapter.undoRows(rows);
    setState(() {
      _log = 'UNDO restored=${undo.restored} skipped=${undo.skipped} '
          'errors=${undo.errors.length}';
    });
  }

  @override
  Widget build(BuildContext context) {
    final RenameReport? plan = _plan;
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: <Widget>[
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: <Widget>[
              ElevatedButton(
                onPressed: _seed,
                child: const Text('Seed demo files'),
              ),
              ElevatedButton(
                onPressed: _preview,
                child: const Text('Preview'),
              ),
              ElevatedButton(
                onPressed: _execute,
                child: const Text('Execute'),
              ),
              OutlinedButton(
                onPressed: _undo,
                child: const Text('Undo'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          SelectableText(_log),
          const SizedBox(height: 12),
          if (plan != null && plan.planned.isNotEmpty)
            ...plan.planned.map(
              (RenameOp op) => Text('${op.oldName} -> ${op.newName}'),
            ),
          if (plan != null)
            ...plan.skippedCollision.map(
              (String collision) => Text(
                'SKIP $collision',
                style: const TextStyle(color: Colors.orange),
              ),
            ),
        ],
      ),
    );
  }
}
