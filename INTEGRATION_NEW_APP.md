# Integrate `epub_reader_kit` In A New Flutter App

This guide shows the minimum setup to use `epub_reader_kit` in a fresh Flutter app.

## 1. Add dependency

In your app `pubspec.yaml`:

```yaml
dependencies:
  flutter:
    sdk: flutter
  epub_reader_kit:
    path: ../epub_reader_kit
```

Then run:

```bash
flutter pub get
```

## 2. Android Gradle setup

In `android/app/build.gradle.kts`:

```kotlin
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")
}
```

## 3. Android manifest setup

In `android/app/src/main/AndroidManifest.xml`:

- add `tools` namespace on `<manifest>`
- set application name to plugin runtime app class

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name="com.example.epub_reader_kit.reader.EpubReaderKitApp"
        tools:replace="android:name"
        ...>
    </application>
</manifest>
```

If your app already has a custom `Application`, make it inherit from:

`com.example.epub_reader_kit.reader.EpubReaderKitApp`

## 4. Use from Dart

```dart
import 'package:epub_reader_kit/epub_reader_kit.dart';
```

Open from URL:

```dart
await EpubReaderService.readBook(
  epubUrl: 'https://www.gutenberg.org/ebooks/11.epub.noimages',
);
```

Open from local file:

```dart
await EpubReaderService.readBook(
  filePath: '/storage/emulated/0/Download/book.epub',
);
```

Or use quick launcher widget:

```dart
const EpubReaderLauncher();
```

## 5. Verify

```bash
flutter analyze
flutter run -d <device_id>
```

Test flow:

1. Paste EPUB URL
2. Tap open button
3. Reader UI should open natively

## Notes

- This plugin is Android-focused for native reader flow.
- Method channel used by plugin: `com.example.epub_reader_kit/reader`
