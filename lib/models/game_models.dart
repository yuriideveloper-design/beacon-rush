import 'package:flutter/material.dart';

enum Lane { near, mid, far }

enum FeedbackKind { hit, miss, rush }

class Ship {
  const Ship({
    required this.id,
    required this.lane,
    required this.x,
    required this.direction,
    this.flash = 0,
  });
  final int id;
  final Lane lane;
  final double x;
  final int direction;
  final double flash;
  Ship copyWith({double? x, double? flash}) => Ship(
    id: id,
    lane: lane,
    x: x ?? this.x,
    direction: direction,
    flash: flash ?? this.flash,
  );
}

class Flare {
  const Flare({
    required this.position,
    required this.previous,
    required this.velocity,
    required this.age,
    required this.trail,
  });
  final Offset position, previous, velocity;
  final double age;
  final List<Offset> trail;
  Flare copyWith({
    Offset? position,
    Offset? previous,
    Offset? velocity,
    double? age,
    List<Offset>? trail,
  }) => Flare(
    position: position ?? this.position,
    previous: previous ?? this.previous,
    velocity: velocity ?? this.velocity,
    age: age ?? this.age,
    trail: trail ?? this.trail,
  );
}

class FeedbackEvent {
  const FeedbackEvent(this.kind, this.text, this.position, this.serial);
  final FeedbackKind kind;
  final String text;
  final Offset position;
  final int serial;
}

class ScoreBreakdown {
  const ScoreBreakdown({
    required this.total,
    required this.hits,
    required this.misses,
    required this.maxCombo,
    required this.basePoints,
    required this.distanceBonus,
    required this.timingBonus,
    required this.isNewBest,
  });
  final int total,
      hits,
      misses,
      maxCombo,
      basePoints,
      distanceBonus,
      timingBonus;
  final bool isNewBest;
}

class GameSettings {
  const GameSettings({
    this.sound = true,
    this.haptics = true,
    this.themeMode = ThemeMode.system,
  });
  final bool sound, haptics;
  final ThemeMode themeMode;
  GameSettings copyWith({bool? sound, bool? haptics, ThemeMode? themeMode}) =>
      GameSettings(
        sound: sound ?? this.sound,
        haptics: haptics ?? this.haptics,
        themeMode: themeMode ?? this.themeMode,
      );
}
