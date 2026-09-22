import 'dart:math';
import 'dart:ui';
import '../models/game_models.dart';

class GameRules {
  static const duration = 60.0, flareRadius = 0.014;
  static const laneY = {Lane.near: .73, Lane.mid: .50, Lane.far: .29};
  static const laneValue = {Lane.near: 100, Lane.mid: 180, Lane.far: 300};
  static const laneSize = {
    Lane.near: Size(.18, .075),
    Lane.mid: Size(.14, .058),
    Lane.far: Size(.105, .045),
  };
  static int multiplier(int streak) => streak >= 6
      ? 4
      : streak >= 4
      ? 3
      : streak >= 2
      ? 2
      : 1;
  static int band(double elapsed) => min(3, elapsed ~/ 15);
  static double spawnInterval(double elapsed) =>
      [2.25, 1.75, 1.35, .95][band(elapsed)] / (elapsed >= 45 ? 2 : 1);
  static double speed(Lane lane, double elapsed) =>
      ([.075, .09, .11, .135][band(elapsed)]) *
      ({Lane.near: 1.0, Lane.mid: .82, Lane.far: .68}[lane]!);
  static Rect shipHull(Ship s) {
    final z = laneSize[s.lane]!;
    return Rect.fromCenter(
      center: Offset(s.x, laneY[s.lane]!),
      width: z.width,
      height: z.height,
    );
  }

  static bool segmentHitsShip(Offset a, Offset b, Ship ship) {
    final r = shipHull(ship).inflate(flareRadius);
    if (r.contains(a) || r.contains(b)) return true;
    final d = b - a;
    for (final edge in [
      [r.topLeft, r.topRight],
      [r.topRight, r.bottomRight],
      [r.bottomRight, r.bottomLeft],
      [r.bottomLeft, r.topLeft],
    ]) {
      final e = edge[1] - edge[0];
      final den = d.dx * e.dy - d.dy * e.dx;
      if (den.abs() < 1e-9) continue;
      final q = edge[0] - a;
      final t = (q.dx * e.dy - q.dy * e.dx) / den;
      final u = (q.dx * d.dy - q.dy * d.dx) / den;
      if (t >= 0 && t <= 1 && u >= 0 && u <= 1) return true;
    }
    return false;
  }

  static double precision(Ship ship, Offset hit) {
    final r = shipHull(ship);
    return (1 - (hit.dx - r.center.dx).abs() / (r.width / 2)).clamp(0, 1);
  }

  static int baseFor(Ship s) => laneValue[s.lane]!;
  static int timingPoints(Ship s, double precision) =>
      (baseFor(s) * (.55 + .45 * precision)).round();
}
