import 'dart:math';
import 'package:flutter/material.dart';
import '../game/game_controller.dart';
import '../game/game_rules.dart';
import '../models/game_models.dart';

class NightScenePainter extends CustomPainter {
  NightScenePainter({this.game, this.menu = false});
  final GameController? game;
  final bool menu;
  @override
  void paint(Canvas c, Size size) {
    final p = Paint();
    p.color = const Color(0xFF11151B);
    c.drawRect(Offset.zero & size, p);
    p.color = const Color(0xFF43242A);
    c.drawRect(
      Rect.fromLTWH(0, size.height * .72, size.width, size.height * .28),
      p,
    );
    p.color = const Color(0xFF92524E).withValues(alpha: .28);
    c.drawRect(
      Rect.fromLTWH(0, size.height * .66, size.width, size.height * .08),
      p,
    );
    p.strokeWidth = 1;
    p.color = Colors.white.withValues(alpha: .025);
    for (double y = 4; y < size.height; y += 7) {
      c.drawLine(Offset(0, y), Offset(size.width, y), p);
    }
    final ships = menu
        ? [
            const Ship(id: 0, lane: Lane.far, x: .25, direction: 1),
            const Ship(id: 1, lane: Lane.mid, x: .74, direction: -1),
          ]
        : (game?.ships ?? []);
    for (final s in ships) {
      _ship(c, size, s);
    }
    _launcher(c, size);
    final g = game;
    if (g != null) {
      final f = g.flare;
      if (f != null) {
        p.style = PaintingStyle.stroke;
        p.strokeCap = StrokeCap.round;
        for (var i = 1; i < f.trail.length; i++) {
          p.strokeWidth = 2 + i / f.trail.length * 5;
          p.color = const Color(
            0xFFFF7B72,
          ).withValues(alpha: i / f.trail.length * .65);
          c.drawLine(_o(f.trail[i - 1], size), _o(f.trail[i], size), p);
        }
        p.style = PaintingStyle.fill;
        p.color = const Color(0xFFFFE5E2);
        c.drawCircle(
          _o(f.position, size),
          max(5, size.shortestSide * GameRules.flareRadius),
          p,
        );
      }
      if (g.aim case final a?) {
        p.style = PaintingStyle.stroke;
        p.strokeWidth = 2;
        p.color = const Color(0xFFFFC2BC).withValues(alpha: .8);
        c.drawLine(_o(const Offset(.5, .96), size), _o(a, size), p);
      }
      final event = g.feedback;
      if (event != null && event.kind != FeedbackKind.rush) {
        final center = _o(event.position, size);
        p.style = PaintingStyle.stroke;
        p.strokeWidth = 3;
        p.color = event.kind == FeedbackKind.hit
            ? const Color(0xFFFFC2BC)
            : const Color(0xFFFF7B72);
        final radius = event.kind == FeedbackKind.hit ? 22.0 : 13.0;
        c.drawCircle(center, radius, p);
        if (event.kind == FeedbackKind.hit) {
          for (var i = 0; i < 8; i++) {
            final angle = i * pi / 4;
            final direction = Offset(cos(angle), sin(angle));
            c.drawLine(center + direction * 27, center + direction * 38, p);
          }
        } else {
          c.drawLine(
            center + const Offset(-9, -9),
            center + const Offset(9, 9),
            p,
          );
          c.drawLine(
            center + const Offset(9, -9),
            center + const Offset(-9, 9),
            p,
          );
        }
      }
    }
  }

  Offset _o(Offset o, Size s) => Offset(o.dx * s.width, o.dy * s.height);
  void _ship(Canvas c, Size size, Ship s) {
    final r = GameRules.shipHull(s);
    final rect = Rect.fromLTWH(
      r.left * size.width,
      r.top * size.height,
      r.width * size.width,
      r.height * size.height,
    );
    // The visible silhouette fills the same hull rectangle used by collision.
    // This keeps a shot at any painted hull pixel fair and deterministic.
    c.drawRRect(
      RRect.fromRectAndRadius(rect, Radius.circular(rect.height * .16)),
      Paint()
        ..color = s.flash > 0
            ? const Color(0xFFFFC2BC)
            : const Color(0xFF080A0D),
    );
    c.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(
          rect.left + rect.width * .38,
          rect.top,
          rect.width * .28,
          rect.height * .28,
        ),
        Radius.circular(rect.height * .08),
      ),
      Paint()..color = const Color(0xFF080A0D),
    );
  }

  void _launcher(Canvas c, Size s) {
    final p = Paint()..color = const Color(0xFF080A0D);
    c.drawPath(
      Path()
        ..moveTo(s.width * .43, s.height)
        ..lineTo(s.width * .48, s.height * .87)
        ..lineTo(s.width * .52, s.height * .87)
        ..lineTo(s.width * .57, s.height)
        ..close(),
      p,
    );
    c.drawCircle(
      Offset(s.width * .5, s.height * .88),
      6,
      p..color = const Color(0xFFE53935),
    );
  }

  @override
  bool shouldRepaint(covariant NightScenePainter old) => true;
}
