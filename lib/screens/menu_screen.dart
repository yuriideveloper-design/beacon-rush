import 'package:flutter/material.dart';
import '../painters/game_painter.dart';

class MenuScreen extends StatelessWidget {
  const MenuScreen({
    super.key,
    required this.bestScore,
    required this.onPlay,
    required this.onSettings,
  });
  final int bestScore;
  final VoidCallback onPlay, onSettings;
  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return Scaffold(
      body: SafeArea(
        child: LayoutBuilder(
          builder: (context, c) {
            final compact = c.maxWidth < 380;
            return Stack(
              children: [
                Positioned.fill(
                  child: ExcludeSemantics(
                    child: CustomPaint(painter: NightScenePainter(menu: true)),
                  ),
                ),
                Padding(
                  padding: EdgeInsets.fromLTRB(
                    compact ? 18 : 24,
                    12,
                    compact ? 18 : 24,
                    24,
                  ),
                  child: Column(
                    children: [
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          IconButton(
                            tooltip: 'Settings',
                            onPressed: onSettings,
                            icon: const Icon(Icons.settings_outlined),
                            style: IconButton.styleFrom(
                              backgroundColor: t.colorScheme.surface.withValues(
                                alpha: .88,
                              ),
                            ),
                          ),
                        ],
                      ),
                      const Spacer(),
                      FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          'BuІІ',
                          maxLines: 1,
                          style: t.textTheme.headlineLarge?.copyWith(
                            color: Colors.white,
                            fontSize: compact ? 42 : 50,
                            letterSpacing: 5,
                          ),
                        ),
                      ),
                      FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          'RUSH',
                          maxLines: 1,
                          style: t.textTheme.headlineLarge?.copyWith(
                            color: t.colorScheme.primary,
                            fontSize: compact ? 58 : 68,
                            letterSpacing: 2,
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      Text(
                        'BEST SCORE  •  $bestScore',
                        style: t.textTheme.titleMedium?.copyWith(
                          color: Colors.white,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const Spacer(flex: 2),
                      SizedBox(
                        width: minOf(c.maxWidth * .75, 320),
                        child: FilledButton.icon(
                          onPressed: onPlay,
                          icon: const Icon(Icons.local_fire_department),
                          label: const Text('PLAY'),
                        ),
                      ),
                      const SizedBox(height: 10),
                      Text(
                        'Drag to aim. Release to signal.',
                        style: t.textTheme.bodyMedium?.copyWith(
                          color: Colors.white70,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            );
          },
        ),
      ),
    );
  }

  double minOf(double a, double b) => a < b ? a : b;
}
