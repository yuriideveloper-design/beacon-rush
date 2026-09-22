import 'package:flutter/material.dart';

import '../game/game_controller.dart';
import '../models/game_models.dart';
import '../painters/game_painter.dart';

class GameScreen extends StatefulWidget {
  const GameScreen({
    super.key,
    required this.settings,
    required this.onFinished,
  });
  final GameSettings settings;
  final Future<void> Function(ScoreBreakdown) onFinished;
  @override
  State<GameScreen> createState() => _GameScreenState();
}

class _GameScreenState extends State<GameScreen> {
  late final GameController game;
  @override
  void initState() {
    super.initState();
    game = GameController(
      settings: widget.settings,
      onFinished: widget.onFinished,
    );
  }

  @override
  void dispose() {
    game.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => PopScope(
    canPop: false,
    child: Scaffold(
      body: SafeArea(
        child: ListenableBuilder(
          listenable: game,
          builder: (context, _) => LayoutBuilder(
            builder: (context, constraints) {
              final hudHeight = constraints.maxHeight < 700 ? 104.0 : 116.0;
              return Column(
                children: [
                  SizedBox(
                    height: hudHeight,
                    child: _Hud(game: game),
                  ),
                  Expanded(
                    child: Semantics(
                      label:
                          'Signal field. Drag upward from the coastal launcher and release to fire.',
                      child: LayoutBuilder(
                        builder: (context, field) {
                          Offset normalize(Offset point) => Offset(
                            (point.dx / field.maxWidth).clamp(0.0, 1.0),
                            (point.dy / field.maxHeight).clamp(0.0, 1.0),
                          );
                          return GestureDetector(
                            behavior: HitTestBehavior.opaque,
                            onPanStart: (d) =>
                                game.beginAim(normalize(d.localPosition)),
                            onPanUpdate: (d) =>
                                game.updateAim(normalize(d.localPosition)),
                            onPanEnd: (_) => game.launch(),
                            onPanCancel: game.cancelAim,
                            child: Stack(
                              fit: StackFit.expand,
                              children: [
                                ExcludeSemantics(
                                  child: CustomPaint(
                                    painter: NightScenePainter(game: game),
                                  ),
                                ),
                                _Feedback(
                                  event: game.feedback,
                                  disable: MediaQuery.disableAnimationsOf(
                                    context,
                                  ),
                                ),
                              ],
                            ),
                          );
                        },
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
        ),
      ),
    ),
  );
}

class _Hud extends StatelessWidget {
  const _Hud({required this.game});
  final GameController game;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final urgent = game.secondsLeft <= 10;
    return Padding(
      padding: const EdgeInsets.all(10),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Card(
            color: theme.colorScheme.surfaceContainerHigh,
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
              child: Semantics(
                label:
                    '${game.timerText} remaining. Combo times ${game.multiplier}',
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TweenAnimationBuilder<double>(
                      key: ValueKey(game.secondsLeft),
                      tween: Tween(begin: urgent ? 1.08 : 1, end: 1),
                      duration: Duration(
                        milliseconds: MediaQuery.disableAnimationsOf(context)
                            ? 0
                            : 180,
                      ),
                      builder: (context, scale, child) => Transform.scale(
                        scale: scale,
                        alignment: Alignment.centerLeft,
                        child: child,
                      ),
                      child: Text(
                        game.timerText,
                        style: theme.textTheme.titleLarge?.copyWith(
                          color: urgent ? theme.colorScheme.error : null,
                          fontFeatures: const [FontFeature.tabularFigures()],
                        ),
                      ),
                    ),
                    SizedBox(
                      width: 112,
                      child: FittedBox(
                        alignment: Alignment.centerLeft,
                        fit: BoxFit.scaleDown,
                        child: Text(
                          'COMBO ${game.multiplier}×',
                          maxLines: 1,
                          style: theme.textTheme.labelLarge,
                        ),
                      ),
                    ),
                    SizedBox(
                      width: 112,
                      child: LinearProgressIndicator(
                        value: game.comboProgress,
                        semanticsLabel: 'Combo progress',
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          const Spacer(),
          Flexible(
            child: Semantics(
              label: 'Score ${game.score}',
              child: Card(
                color: theme.colorScheme.surfaceContainerHigh,
                child: Padding(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 14,
                    vertical: 12,
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.end,
                    children: [
                      Text('SCORE', style: theme.textTheme.labelSmall),
                      FittedBox(
                        fit: BoxFit.scaleDown,
                        child: Text(
                          _compact(game.score),
                          style: theme.textTheme.titleLarge,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  static String _compact(int value) =>
      value >= 100000 ? '${(value / 1000).toStringAsFixed(0)}K' : '$value';
}

class _Feedback extends StatelessWidget {
  const _Feedback({required this.event, required this.disable});
  final FeedbackEvent? event;
  final bool disable;

  @override
  Widget build(BuildContext context) {
    final current = event;
    return IgnorePointer(
      child: AnimatedSwitcher(
        duration: Duration(milliseconds: disable ? 0 : 240),
        transitionBuilder: (child, animation) => FadeTransition(
          opacity: animation,
          child: ScaleTransition(
            scale: Tween<double>(begin: .88, end: 1).animate(animation),
            child: child,
          ),
        ),
        child: current == null
            ? const SizedBox.shrink()
            : Align(
                key: ValueKey(current.serial),
                alignment: current.kind == FeedbackKind.rush
                    ? const Alignment(0, -.25)
                    : Alignment(
                        current.position.dx * 2 - 1,
                        current.position.dy * 2 - 1,
                      ),
                child: Semantics(
                  liveRegion: true,
                  label: current.text,
                  child: DecoratedBox(
                    decoration: BoxDecoration(
                      color: const Color(0xDD1C1C1C),
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(
                        color: current.kind == FeedbackKind.miss
                            ? const Color(0xFFFF7B72)
                            : const Color(0xFFFFC2BC),
                      ),
                    ),
                    child: Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 16,
                        vertical: 10,
                      ),
                      child: Text(
                        current.text,
                        textAlign: TextAlign.center,
                        style: Theme.of(context).textTheme.titleMedium
                            ?.copyWith(
                              color: Colors.white,
                              fontWeight: FontWeight.w900,
                            ),
                      ),
                    ),
                  ),
                ),
              ),
      ),
    );
  }
}
