# Flutter generated-project guide

This project uses Flutter's standard Material 3 widgets. Do not add a separate
design system unless the user explicitly requests one.

The Android project uses Android Gradle Plugin 9 Built-in Kotlin. Do not apply
`org.jetbrains.kotlin.android` or `kotlin-android`, set
`android.builtInKotlin=false`, add the legacy `android.kotlinOptions` DSL, or
declare KGP even with `apply false`. Keep Java `compileOptions`
`targetCompatibility = JavaVersion.VERSION_17`; AGP 9 Built-in Kotlin inherits
that target without a separate top-level `kotlin.compilerOptions` block. Keep
`android.newDsl=false` while the current Flutter Gradle plugin requires it.
The Terms fallback intentionally uses the SDK `AndroidView` and native Android
`WebView`; do not add `webview_flutter` just to replace it.

## UI contract

- Keep `MaterialApp` or `MaterialApp.router` at the application root.
- Keep `ThemeData(useMaterial3: true)` with deliberate light and dark
  `ColorScheme` values.
- Prefer Flutter SDK widgets such as `Scaffold`, `NavigationBar`, `AppBar`,
  `Card`, `FilledButton`, `OutlinedButton`, `TextField`, `Dialog`, `Chip`,
  `Slider`, `Switch`, and standard progress indicators.
- Use standard Flutter layout, scrolling, navigation, animation, semantics,
  focus, and form APIs.
- Label `IconButton` with `tooltip` or a surrounding `Semantics` widget.
  `semanticLabel` belongs to `Icon`, not `IconButton`.
- Do not create application-specific unit or widget tests. Preserve and run
  tests already present in an imported/edited project without adding a new
  suite.
- Add third-party packages only for capabilities absent from the SDK, not to
  replace the standard UI toolkit.
- Keep every visible control functional and include loading, empty, error,
  disabled, and success states where the product flow needs them.
- Adapt layouts with `LayoutBuilder`, `MediaQuery`, `Wrap`, flexible layout,
  and scrollable surfaces. Respect safe areas, keyboard insets, touch targets,
  large text, and narrow screens.
- Never hide layout failures by clipping required content, suppressing
  `FlutterError`, or weakening tests.
- Build a safe premium visual result: a product-specific focal area, strong
  hierarchy, balanced multi-hue palette, restrained tonal depth, and no generic
  gradient/card-grid template.
- For image-led products, use at most three relevant generated assets under
  `assets/images`, derive them from the original request, keep their longest edge
  at or below 1600px, declare every path, and provide graceful fallbacks without
  adding a decorative image package or external image API.
- Limit motion to two to four purposeful short SDK animations around 160-320ms.
  Avoid continuous decoration, runtime 3D, custom shaders, video backgrounds,
  and animation frameworks added only for polish.

Before finishing, run `flutter analyze` when permitted. Run `flutter test` only
when the project already contains tests.
