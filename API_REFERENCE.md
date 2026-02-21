# API Reference

## Import

```dart
import 'package:epub_reader_kit/epub_reader_kit.dart';
```

## `EpubReaderService`

### `readBook({String? epubUrl, String? filePath})`

Primary API for clients. Opens EPUB in native Android reader.

- Pass exactly one:
  - `epubUrl` for remote book
  - `filePath` for local book
- Returns: `Future<bool>`
- Throws:
  - `ArgumentError`
  - `FormatException`
  - `HttpException`
  - `FileSystemException`
  - `PlatformException`

### `open({String? epubUrl, String? filePath})`

Same behavior as `readBook`, kept as lower-level alias.

### `openFromUrl(String epubUrl)`

Downloads EPUB from URL then opens in native reader.

### `openFromFilePath(String filePath)`

Checks local file exists then opens in native reader.

### `openBook(String bookPath)`

Direct method-channel open call for a local EPUB path.

## `EpubReaderLauncher`

Ready-to-use widget with:

- URL input
- local file picker
- status/loading UI

Use when you need a quick reader launcher screen without custom UI work.
