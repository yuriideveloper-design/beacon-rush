# Android release signing

This project is prepared to build signed release APK and AAB files. The Android upload
key and its signing properties are included in this downloaded project ZIP.

Signing files:
- upload-keystore.jks
- keystore.properties

Build a signed Android App Bundle for Google Play:

```bash
flutter build appbundle --release
```

AAB output:

```text
build/app/outputs/bundle/release/app-release.aab
```

Build a signed release APK:

```bash
flutter build apk --release
```

APK output:

```text
build/app/outputs/flutter-apk/app-release.apk
```

Keep the keystore and properties file private and backed up. All future Google Play
updates must use this same upload key unless the upload key is reset in Play Console.
For native Kotlin builds on Windows, use `gradlew.bat` instead of `./gradlew`.

## Google Play publication checklist

- Upload the signed AAB for production; use the APK only for direct/local testing.
- Keep the application ID and upload key unchanged, and increase `versionCode` for every update.
- If `app-icons/android/GOOGLE_PLAY_ASSETS.md` exists, upload its store icon and feature graphic
  in Play Console; these listing graphics are intentionally not embedded into the AAB.
- Add current phone screenshots and localized app name, short description, and full description.
- Complete App content declarations (privacy policy, Data safety, ads, app access, content rating,
  target audience, and any category-specific declarations) so they match the actual generated app.
- Test the release from an internal testing track before promoting it to production.
