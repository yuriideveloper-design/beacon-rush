import 'dart:math';

import 'package:flutter/scheduler.dart';
import 'package:flutter/services.dart';
import 'package:flutter/widgets.dart';
import '../models/game_models.dart';
import 'game_rules.dart';

class GameController extends ChangeNotifier with WidgetsBindingObserver {
  GameController({
    required this.settings,
    required this.onFinished,
    int seed = 7319,
  }) : _random = Random(seed) {
    WidgetsBinding.instance.addObserver(this);
    _clock.start();
    _ticker = Ticker(_tick)..start();
  }
  final GameSettings settings;
  final Future<void> Function(ScoreBreakdown) onFinished;
  final Random _random;
  late final Ticker _ticker;
  final Stopwatch _clock = Stopwatch();
  Duration _last = Duration.zero;
  final List<Ship> ships = [];
  Flare? flare;
  FeedbackEvent? feedback;
  Offset? aim;
  double elapsed = 0, _nextSpawn = .7, _feedbackUntil = 0;
  int score = 0,
      streak = 0,
      hits = 0,
      misses = 0,
      maxCombo = 0,
      basePoints = 0,
      distanceBonus = 0,
      timingBonus = 0,
      _shipId = 0,
      _eventId = 0;
  bool runResolved = false, rushAnnounced = false;
  int get multiplier => GameRules.multiplier(streak);
  double get comboProgress => switch (streak) {
    0 || 2 || 4 => 0,
    1 || 3 || 5 => .5,
    _ => 1,
  };
  int get secondsLeft => max(0, (GameRules.duration - elapsed).ceil());
  double get progress => secondsLeft / GameRules.duration;
  String get timerText =>
      '${secondsLeft ~/ 60}:${(secondsLeft % 60).toString().padLeft(2, '0')}';

  void _tick(Duration stamp) {
    if (runResolved) return;
    final target = (_clock.elapsedMicroseconds / 1e6).clamp(
      0.0,
      GameRules.duration,
    );
    var pending = target - elapsed;
    while (pending > 0) {
      final step = min(pending, 1 / 60);
      elapsed += step;
      _simulate(step);
      pending -= step;
    }
    elapsed = target;
    if (elapsed >= 45 && !rushAnnounced) {
      rushAnnounced = true;
      _emit(FeedbackKind.rush, 'RUSH!', const Offset(.5, .4));
    }
    if (feedback != null && elapsed >= _feedbackUntil) feedback = null;
    if (elapsed >= GameRules.duration) {
      _finish();
      return;
    }
    _last = stamp;
    notifyListeners();
  }

  void _simulate(double dt) {
    while (_nextSpawn <= elapsed) {
      _spawn();
      _nextSpawn += GameRules.spawnInterval(_nextSpawn);
    }
    for (var i = ships.length - 1; i >= 0; i--) {
      final s = ships[i];
      final x = s.x + s.direction * GameRules.speed(s.lane, elapsed) * dt;
      if (x < -.15 || x > 1.15) {
        ships.removeAt(i);
      } else {
        ships[i] = s.copyWith(x: x, flash: max(0, s.flash - dt));
      }
    }
    final f = flare;
    if (f != null) {
      final velocity = f.velocity + Offset(0, 1.18 * dt);
      final next = f.position + velocity * dt;
      final trail = [...f.trail, next];
      if (trail.length > 14) trail.removeAt(0);
      flare = f.copyWith(
        previous: f.position,
        position: next,
        velocity: velocity,
        age: f.age + dt,
        trail: trail,
      );
      for (final ship in List<Ship>.of(ships)) {
        if (GameRules.segmentHitsShip(f.position, next, ship)) {
          _hit(ship, next);
          return;
        }
      }
      if (next.dy > 1.08 || next.dx < -.05 || next.dx > 1.05 || f.age > 2.4) {
        _miss(next);
      }
    }
  }

  void _spawn() {
    final lane = Lane.values[_random.nextInt(3)],
        dir = _random.nextBool() ? 1 : -1;
    ships.add(
      Ship(
        id: _shipId++,
        lane: lane,
        x: dir == 1 ? -.12 : 1.12,
        direction: dir,
      ),
    );
  }

  void beginAim(Offset p) {
    if (runResolved || flare != null || p.dy < .12) return;
    aim = p;
    notifyListeners();
  }

  void updateAim(Offset p) {
    if (aim == null) return;
    aim = Offset(p.dx.clamp(0, 1), p.dy.clamp(.05, .95));
    notifyListeners();
  }

  void launch() {
    final start = aim;
    if (start == null || runResolved || flare != null) return;
    aim = null;
    final launcher = const Offset(.5, .96), drag = launcher - start;
    if (drag.dy <= .035 || drag.distance < .06) {
      notifyListeners();
      return;
    }
    final power = drag.distance.clamp(.10, .48);
    final direction = drag / drag.distance;
    flare = Flare(
      position: launcher,
      previous: launcher,
      velocity: direction * (1.25 + power * 2.1),
      age: 0,
      trail: [launcher],
    );
    _feedback(SystemSoundType.click);
    notifyListeners();
  }

  void cancelAim() {
    if (aim == null) return;
    aim = null;
    notifyListeners();
  }

  void _hit(Ship ship, Offset at) {
    final precision = GameRules.precision(ship, at),
        adjusted = GameRules.timingPoints(ship, precision),
        mult = multiplier,
        award = adjusted * mult;
    basePoints += 100 * mult;
    distanceBonus += (GameRules.baseFor(ship) - 100) * mult;
    timingBonus += (adjusted - GameRules.baseFor(ship)) * mult;
    score += award;
    hits++;
    streak++;
    maxCombo = max(maxCombo, streak);
    ships.remove(ship);
    flare = null;
    _emit(FeedbackKind.hit, '+$award ×$mult', at);
    _feedback(SystemSoundType.alert);
  }

  void _miss(Offset at) {
    if (flare == null) return;
    flare = null;
    misses++;
    streak = 0;
    _emit(FeedbackKind.miss, 'Miss — combo reset', at);
    _feedback(SystemSoundType.click);
  }

  void _emit(FeedbackKind kind, String text, Offset at) {
    feedback = FeedbackEvent(kind, text, at, _eventId++);
    _feedbackUntil = elapsed + (kind == FeedbackKind.rush ? 1.35 : .85);
  }

  void _feedback(SystemSoundType sound) {
    if (settings.sound) SystemSound.play(sound);
    if (settings.haptics) HapticFeedback.lightImpact();
  }

  Future<void> _finish() async {
    if (runResolved) return;
    runResolved = true;
    elapsed = GameRules.duration;
    _ticker.stop();
    _clock.stop();
    aim = null;
    flare = null;
    notifyListeners();
    final b = ScoreBreakdown(
      total: score,
      hits: hits,
      misses: misses,
      maxCombo: maxCombo,
      basePoints: basePoints,
      distanceBonus: distanceBonus,
      timingBonus: timingBonus,
      isNewBest: false,
    );
    await onFinished(b);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed && !runResolved) {
      _tick(_last);
    }
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _ticker.dispose();
    super.dispose();
  }
}
