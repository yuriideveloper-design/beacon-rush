import 'package:flutter/material.dart';
import '../models/game_models.dart';

class ResultsScreen extends StatelessWidget {
  const ResultsScreen({
    super.key,
    required this.breakdown,
    required this.onRetry,
    required this.onMenu,
  });
  final ScoreBreakdown breakdown;
  final VoidCallback onRetry, onMenu;
  @override
  Widget build(BuildContext context) {
    final t = Theme.of(context);
    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, _) {
        if (!didPop) onMenu();
      },
      child: Scaffold(
        body: SafeArea(
          child: LayoutBuilder(
            builder: (context, c) {
              final tight = c.maxHeight < 720;
              return Padding(
                padding: EdgeInsets.symmetric(
                  horizontal: c.maxWidth < 380 ? 18 : 28,
                  vertical: tight ? 12 : 24,
                ),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 460),
                    child: Column(
                      children: [
                        Expanded(
                          child: SingleChildScrollView(
                            child: Column(
                              children: [
                                SizedBox(height: tight ? 4 : 16),
                                Icon(
                                  Icons.flare,
                                  size: tight ? 42 : 54,
                                  color: t.colorScheme.primary,
                                ),
                                Text(
                                  'RUN OVER',
                                  style: t.textTheme.headlineLarge,
                                ),
                                if (breakdown.isNewBest)
                                  Chip(
                                    avatar: const Icon(
                                      Icons.workspace_premium,
                                      size: 18,
                                    ),
                                    label: const Text('NEW BEST'),
                                  ),
                                SizedBox(height: tight ? 8 : 16),
                                FittedBox(
                                  child: Text(
                                    '${breakdown.total}',
                                    style: t.textTheme.displayLarge?.copyWith(
                                      fontWeight: FontWeight.w900,
                                    ),
                                  ),
                                ),
                                Text(
                                  'FINAL SCORE',
                                  style: t.textTheme.labelLarge,
                                ),
                                SizedBox(height: tight ? 8 : 16),
                                const Divider(),
                                _row('Hits', '${breakdown.hits}'),
                                _row('Misses', '${breakdown.misses}'),
                                _row('Max combo', '${breakdown.maxCombo}'),
                                _row(
                                  'Signal points',
                                  '${breakdown.basePoints}',
                                ),
                                _row(
                                  'Distance bonus',
                                  _signed(breakdown.distanceBonus),
                                ),
                                _row(
                                  'Timing adjustment',
                                  _signed(breakdown.timingBonus),
                                ),
                                const Divider(),
                                _row(
                                  'Reconciled total',
                                  '${breakdown.basePoints + breakdown.distanceBonus + breakdown.timingBonus}',
                                  strong: true,
                                ),
                                SizedBox(height: tight ? 8 : 20),
                              ],
                            ),
                          ),
                        ),
                        SizedBox(
                          width: double.infinity,
                          child: FilledButton.icon(
                            onPressed: onRetry,
                            icon: const Icon(Icons.replay),
                            label: const Text('RETRY'),
                          ),
                        ),
                        const SizedBox(height: 10),
                        SizedBox(
                          width: double.infinity,
                          child: OutlinedButton.icon(
                            onPressed: onMenu,
                            icon: const Icon(Icons.home_outlined),
                            label: const Text('MENU'),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }

  static Widget _row(String a, String b, {bool strong = false}) => Padding(
    padding: const EdgeInsets.symmetric(vertical: 3),
    child: Row(
      children: [
        Expanded(child: Text(a)),
        const SizedBox(width: 12),
        Flexible(
          child: FittedBox(
            fit: BoxFit.scaleDown,
            alignment: Alignment.centerRight,
            child: Text(
              b,
              style: TextStyle(
                fontWeight: strong ? FontWeight.w900 : FontWeight.w600,
              ),
            ),
          ),
        ),
      ],
    ),
  );
  static String _signed(int n) => n > 0 ? '+$n' : '$n';
}
