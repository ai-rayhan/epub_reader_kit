# Installation Complete

`epub_reader_kit` is now a Flutter plugin with built-in Android native implementation.

## 1. Add dependency

```yaml
dependencies:
  epub_reader_kit:
    path: packages/epub_reader_kit
```

```bash
flutter pub get
```

## 2. Call one function

```dart
await EpubReaderService.readBook(
  epubUrl: 'https://www.gutenberg.org/ebooks/11.epub.noimages',
);
```

or

```dart
await EpubReaderService.readBook(
  filePath: '/storage/emulated/0/Download/book.epub',
);
```

## 3. Rebuild app

```bash
flutter clean
flutter pub get
flutter run
```

## Runtime requirement

If your app uses a custom Android `Application` class, ensure it is compatible with `org.cis_india.wsreader.WikisourceReader` runtime (recommended: subclass it).
