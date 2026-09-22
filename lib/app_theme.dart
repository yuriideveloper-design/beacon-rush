import 'package:flutter/material.dart';

class AppTheme {
  static const red = Color(0xFFE53935);
  static const coral = Color(0xFFFF7B72);
  static const charcoal = Color(0xFF1C1C1C);

  static ThemeData light() => _theme(
    ColorScheme(
      brightness: Brightness.light,
      primary: red,
      onPrimary: Colors.white,
      secondary: const Color(0xFF9D332E),
      onSecondary: Colors.white,
      tertiary: const Color(0xFF8B5A12),
      onTertiary: Colors.white,
      error: const Color(0xFFBA1A1A),
      onError: Colors.white,
      surface: Colors.white,
      onSurface: const Color(0xFF251918),
    ),
    const Color(0xFFFFF8F7),
  );

  static ThemeData dark() => _theme(
    const ColorScheme(
      brightness: Brightness.dark,
      primary: red,
      onPrimary: Colors.white,
      secondary: coral,
      onSecondary: Color(0xFF4A0705),
      tertiary: Color(0xFFFFC2BC),
      onTertiary: Color(0xFF54100C),
      error: coral,
      onError: Color(0xFF4A0705),
      surface: Color(0xFF292929),
      onSurface: Colors.white,
    ),
    charcoal,
  );

  static ThemeData _theme(ColorScheme scheme, Color background) => ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: background,
    textTheme: const TextTheme(
      headlineLarge: TextStyle(fontWeight: FontWeight.w900, letterSpacing: -1),
      titleLarge: TextStyle(fontWeight: FontWeight.w800),
    ),
    filledButtonTheme: FilledButtonThemeData(
      style: FilledButton.styleFrom(
        minimumSize: const Size(120, 52),
        textStyle: const TextStyle(fontWeight: FontWeight.w800),
      ),
    ),
    outlinedButtonTheme: OutlinedButtonThemeData(
      style: OutlinedButton.styleFrom(minimumSize: const Size(120, 52)),
    ),
    cardTheme: const CardThemeData(elevation: 0, margin: EdgeInsets.zero),
  );
}
