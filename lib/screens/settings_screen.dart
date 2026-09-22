import 'package:flutter/material.dart';
import '../models/game_models.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({
    super.key,
    required this.settings,
    required this.onChanged,
  });
  final GameSettings settings;
  final Future<void> Function(GameSettings) onChanged;
  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  late GameSettings value;
  @override
  void initState() {
    super.initState();
    value = widget.settings;
  }

  void change(GameSettings v) {
    setState(() => value = v);
    widget.onChanged(v);
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('Settings')),
    body: SafeArea(
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Text('FEEDBACK', style: Theme.of(context).textTheme.labelLarge),
          SwitchListTile(
            title: const Text('Sound'),
            subtitle: const Text('Launch, hit and miss cues'),
            secondary: const Icon(Icons.volume_up_outlined),
            value: value.sound,
            onChanged: (v) => change(value.copyWith(sound: v)),
          ),
          SwitchListTile(
            title: const Text('Haptics'),
            subtitle: const Text('Tactile signaling feedback'),
            secondary: const Icon(Icons.vibration),
            value: value.haptics,
            onChanged: (v) => change(value.copyWith(haptics: v)),
          ),
          const SizedBox(height: 24),
          Text('APPEARANCE', style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: 12),
          SegmentedButton<ThemeMode>(
            segments: const [
              ButtonSegment(
                value: ThemeMode.system,
                icon: Icon(Icons.brightness_auto),
                label: Text('System'),
              ),
              ButtonSegment(
                value: ThemeMode.light,
                icon: Icon(Icons.light_mode),
                label: Text('Light'),
              ),
              ButtonSegment(
                value: ThemeMode.dark,
                icon: Icon(Icons.dark_mode),
                label: Text('Dark'),
              ),
            ],
            selected: {value.themeMode},
            showSelectedIcon: false,
            onSelectionChanged: (s) =>
                change(value.copyWith(themeMode: s.first)),
          ),
        ],
      ),
    ),
  );
}
