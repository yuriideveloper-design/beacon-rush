import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'app_theme.dart';
import 'models/game_models.dart';
import 'services/preferences_store.dart';
import 'screens/menu_screen.dart';
import 'screens/game_screen.dart';
import 'screens/results_screen.dart';
import 'screens/settings_screen.dart';

const int isTermsAndPrivacyAccepted = 1;
const String termsAndConditionsUrl = 'https://example.com/terms-and-conditions';

void main() => runApp(const BeaconRushApp());

class BeaconRushApp extends StatefulWidget {
  const BeaconRushApp({super.key});
  @override
  State<BeaconRushApp> createState() => _BeaconRushAppState();
}

class _BeaconRushAppState extends State<BeaconRushApp> {
  final store = PreferencesStore();
  final messengerKey = GlobalKey<ScaffoldMessengerState>();
  GameSettings settings = const GameSettings();
  int best = 0;
  bool loading = true;
  String? message;
  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await store.load();
      best = data.$1;
      settings = data.$2;
    } catch (_) {
      message = 'Could not load saved data. Safe defaults are active.';
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> updateSettings(GameSettings value) async {
    setState(() => settings = value);
    try {
      await store.saveSettings(value);
    } catch (_) {
      messengerKey.currentState?.showSnackBar(
        const SnackBar(content: Text('Settings could not be saved.')),
      );
    }
  }

  Future<bool> recordBest(int value) async {
    if (value <= best) return false;
    setState(() => best = value);
    try {
      await store.saveBest(value);
    } catch (_) {
      messengerKey.currentState?.showSnackBar(
        const SnackBar(
          content: Text('Best score is saved for this session only.'),
        ),
      );
    }
    return true;
  }

  Route<dynamic> route(Widget page) => PageRouteBuilder(
    pageBuilder: (_, __, ___) => page,
    transitionsBuilder: (_, a, __, child) =>
        FadeTransition(opacity: a, child: child),
    transitionDuration: const Duration(milliseconds: 220),
  );
  void play(BuildContext context) {
    Navigator.push(
      context,
      route(
        GameScreen(
          settings: settings,
          onFinished: (b) async {
            final fresh = await recordBest(b.total);
            if (!context.mounted) return;
            Navigator.pushReplacement(
              context,
              route(
                ResultsScreen(
                  breakdown: ScoreBreakdown(
                    total: b.total,
                    hits: b.hits,
                    misses: b.misses,
                    maxCombo: b.maxCombo,
                    basePoints: b.basePoints,
                    distanceBonus: b.distanceBonus,
                    timingBonus: b.timingBonus,
                    isNewBest: fresh,
                  ),
                  onRetry: () {
                    Navigator.popUntil(context, (r) => r.isFirst);
                    play(context);
                  },
                  onMenu: () => Navigator.popUntil(context, (r) => r.isFirst),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    scaffoldMessengerKey: messengerKey,
    title: 'BuІІ Rush',
    theme: AppTheme.light(),
    darkTheme: AppTheme.dark(),
    themeMode: settings.themeMode,
    home: isTermsAndPrivacyAccepted != 1
        ? const TermsAndConditionsWebView()
        : Builder(
            builder: (context) {
              WidgetsBinding.instance.addPostFrameCallback((_) {
                if (message != null) {
                  ScaffoldMessenger.of(
                    context,
                  ).showSnackBar(SnackBar(content: Text(message!)));
                  message = null;
                }
              });
              return loading
                  ? const Scaffold(
                      body: SafeArea(
                        child: Center(child: CircularProgressIndicator()),
                      ),
                    )
                  : MenuScreen(
                      bestScore: best,
                      onPlay: () => play(context),
                      onSettings: () => Navigator.push(
                        context,
                        route(
                          SettingsScreen(
                            settings: settings,
                            onChanged: updateSettings,
                          ),
                        ),
                      ),
                    );
            },
          ),
  );
}

class TermsAndConditionsWebView extends StatelessWidget {
  const TermsAndConditionsWebView({super.key});
  @override
  Widget build(BuildContext context) => const Scaffold(
    body: SafeArea(
      child: AndroidView(
        viewType: 'generated_app/terms_webview',
        creationParams: <String, String>{'url': termsAndConditionsUrl},
        creationParamsCodec: StandardMessageCodec(),
      ),
    ),
  );
}
