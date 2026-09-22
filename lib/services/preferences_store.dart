import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/game_models.dart';

class PreferencesStore {
  static const _best = 'beaconRush.bestScore',
      _sound = 'beaconRush.soundEnabled',
      _haptics = 'beaconRush.hapticsEnabled',
      _theme = 'beaconRush.themeMode';
  Future<(int, GameSettings)> load() async {
    final p = await SharedPreferences.getInstance();
    final raw = p.getString(_theme);
    final mode =
        ThemeMode.values.where((e) => e.name == raw).firstOrNull ??
        ThemeMode.system;
    return (
      p.getInt(_best) ?? 0,
      GameSettings(
        sound: p.getBool(_sound) ?? true,
        haptics: p.getBool(_haptics) ?? true,
        themeMode: mode,
      ),
    );
  }

  Future<void> saveBest(int v) async {
    final p = await SharedPreferences.getInstance();
    if (!await p.setInt(_best, v)) throw StateError('Best score was not saved');
  }

  Future<void> saveSettings(GameSettings s) async {
    final p = await SharedPreferences.getInstance();
    final ok = await Future.wait([
      p.setBool(_sound, s.sound),
      p.setBool(_haptics, s.haptics),
      p.setString(_theme, s.themeMode.name),
    ]);
    if (ok.contains(false)) throw StateError('Settings were not saved');
  }
}
