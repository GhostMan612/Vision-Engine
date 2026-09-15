// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'package:flutter/material.dart';

import '../../settings_store.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key, required this.settings});

  final SettingsStore settings;

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late final TextEditingController _prefixes;

  @override
  void initState() {
    super.initState();
    _prefixes =
        TextEditingController(text: widget.settings.prefixes.join(', '));
  }

  @override
  void dispose() {
    _prefixes.dispose();
    super.dispose();
  }

  Future<void> _savePrefixes() async {
    final List<String> values = _prefixes.text
        .split(',')
        .map((String s) => s.trim())
        .where((String s) => s.isNotEmpty)
        .toList();
    await widget.settings.setPrefixes(values);
    if (!mounted) {
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text('Prefixes saved: ${values.join(', ')}')),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        TextField(
          controller: _prefixes,
          decoration: const InputDecoration(
            labelText: 'Prefixes (comma-separated)',
            helperText: 'Default: IMG_, VID_. Leading matches only.',
          ),
        ),
        const SizedBox(height: 8),
        ElevatedButton(
          onPressed: _savePrefixes,
          child: const Text('Save prefixes'),
        ),
        SwitchListTile(
          title: const Text('Case-sensitive match'),
          subtitle: const Text('Off = img_ and vid_ also match.'),
          value: widget.settings.caseSensitive,
          onChanged: (bool value) async {
            await widget.settings.setCaseSensitive(value);
            if (!mounted) {
              return;
            }
            setState(() {});
          },
        ),
        SwitchListTile(
          title: const Text('Include subfolders'),
          subtitle: const Text('Off = top folder only.'),
          value: widget.settings.recursive,
          onChanged: (bool value) async {
            await widget.settings.setRecursive(value);
            if (!mounted) {
              return;
            }
            setState(() {});
          },
        ),
      ],
    );
  }
}
