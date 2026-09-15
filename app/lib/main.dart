// ============================================================
// As Above, So Below. As Within, So Without.
// The Future Dictates the Past and the Past is Always Present.
// ============================================================
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:path_provider/path_provider.dart';

import 'adapters/local_rename_adapter.dart';
import 'features/probe/probe_screen.dart';
import 'features/rename/rename_screen.dart';
import 'features/settings/settings_screen.dart';
import 'settings_store.dart';

void main() {
  runApp(const VisionApp());
}

class VisionApp extends StatelessWidget {
  const VisionApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Vision Engine',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
        useMaterial3: true,
      ),
      home: const HomeShell(),
    );
  }
}

class _Boot {
  const _Boot(this.docs, this.adapter, this.settings);

  final Directory docs;
  final LocalRenameAdapter adapter;
  final SettingsStore settings;
}

class HomeShell extends StatefulWidget {
  const HomeShell({super.key});

  @override
  State<HomeShell> createState() => _HomeShellState();
}

class _HomeShellState extends State<HomeShell> {
  int _index = 0;
  _Boot? _boot;

  @override
  void initState() {
    super.initState();
    _start();
  }

  Future<void> _start() async {
    final Directory docs = await getApplicationDocumentsDirectory();
    final Directory demo = Directory(
      '${docs.path}${Platform.pathSeparator}vision_demo',
    );
    if (!demo.existsSync()) {
      demo.createSync(recursive: true);
    }
    final SettingsStore settings = await SettingsStore.load();
    if (!mounted) {
      return;
    }
    setState(() {
      _boot = _Boot(docs, LocalRenameAdapter(demo), settings);
    });
  }

  @override
  Widget build(BuildContext context) {
    final _Boot? boot = _boot;
    if (boot == null) {
      return const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      );
    }
    final List<Widget> tabs = <Widget>[
      RenameScreen(adapter: boot.adapter, settings: boot.settings),
      ProbeScreen(baseDir: boot.docs),
      SettingsScreen(settings: boot.settings),
    ];
    return Scaffold(
      appBar: AppBar(title: const Text('Vision Engine')),
      body: IndexedStack(index: _index, children: tabs),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (int value) {
          setState(() {
            _index = value;
          });
        },
        destinations: const <Widget>[
          NavigationDestination(
            icon: Icon(Icons.drive_file_rename_outline),
            label: 'Rename',
          ),
          NavigationDestination(
            icon: Icon(Icons.smartphone),
            label: 'Probe',
          ),
          NavigationDestination(
            icon: Icon(Icons.settings),
            label: 'Settings',
          ),
        ],
      ),
    );
  }
}
