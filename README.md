# epub_reader_kit

Production Flutter plugin to open EPUB in native Wikisource/Readium Android reader.

## What you get

- One-call API: `EpubReaderService.readBook(...)`
- URL download + native open
- Local file open + native open
- No `MainActivity` method-channel code required

## Install

```yaml
dependencies:
  epub_reader_kit:
    path: packages/epub_reader_kit
```

```bash
flutter pub get
```

## Usage

```dart
await EpubReaderService.readBook(
  epubUrl: 'https://www.gutenberg.org/ebooks/11.epub.noimages',
);
```

```dart
await EpubReaderService.readBook(
  filePath: '/storage/emulated/0/Download/book.epub',
);
```

Or use ready widget:

```dart
const EpubReaderLauncher()
```

## Android notes

The plugin auto-registers its own native `openBook` channel handler:

`com.example.ebook_reader/wikisource`

`AndroidManifest` entries for reader activity/service/provider are shipped with the plugin and merged automatically.

If your app already defines a custom `Application`, make it compatible with `org.cis_india.wsreader.WikisourceReader` runtime (or subclass it).
