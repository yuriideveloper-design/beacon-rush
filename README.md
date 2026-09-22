# Beacon Rush

`Beacon Rush` — Android-игра на Flutter: за 60 секунд игрок запускает сигнальные ракеты по кораблям, удерживает комбо и набирает рекорд. Вся игровая графика рисуется через `CustomPainter`; рекорд и настройки сохраняются в `SharedPreferences`.

## Требования

- Flutter 3.44.1 или новее
- Dart 3.12.0 или новее
- Android SDK 36
- JDK 17

## Запуск и проверка

```bash
flutter pub get
flutter analyze
flutter test
flutter run
```

В проекте не добавлен отдельный набор прикладных тестов; `flutter test` запускает только уже имеющиеся тесты. Для ручной проверки пройдите полный 60-секундный раунд, проверьте попадание, промах, сброс комбо, объявление Rush на 45-й секунде, итоговый счёт, Retry/Menu и сохранение настроек после перезапуска.

## Release-сборки Android

```bash
flutter build apk --release
flutter build appbundle --release
```

Результаты:

```text
build/app/outputs/flutter-apk/app-release.apk
build/app/outputs/bundle/release/app-release.aab
```

Подпись release использует корневые файлы `upload-keystore.jks` и `keystore.properties`. Не добавляйте их содержимое в систему контроля версий. Минификация и shrink ресурсов включаются флагом `-PenableReleaseShrink=true`.
