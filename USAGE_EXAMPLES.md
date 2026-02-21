# Usage Examples

This file shows practical ways to use `epub_reader_kit` in Flutter apps.

## 1. Minimal call (recommended)

```dart
import 'package:epub_reader_kit/epub_reader_kit.dart';

await EpubReaderService.readBook(
  epubUrl: 'https://www.gutenberg.org/ebooks/11.epub.noimages',
);
```

## 2. Open local EPUB file

```dart
import 'package:epub_reader_kit/epub_reader_kit.dart';

await EpubReaderService.readBook(
  filePath: '/storage/emulated/0/Download/book.epub',
);
```

## 3. Use the launcher widget

```dart
import 'package:flutter/material.dart';
import 'package:epub_reader_kit/epub_reader_kit.dart';

class ReaderHomePage extends StatelessWidget {
  const ReaderHomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return const Scaffold(
      body: Padding(
        padding: EdgeInsets.all(16),
        child: EpubReaderLauncher(),
      ),
    );
  }
}
```

## 4. Error handling pattern

```dart
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:epub_reader_kit/epub_reader_kit.dart';

Future<void> openBookSafe({String? url, String? path}) async {
  try {
    await EpubReaderService.readBook(epubUrl: url, filePath: path);
  } on PlatformException catch (e) {
    // Native plugin/runtime issues.
    print('PlatformException: ${e.code} ${e.message}');
  } on HttpException catch (e) {
    // Download failed.
    print('HttpException: ${e.message}');
  } on FileSystemException catch (e) {
    // Local file missing.
    print('FileSystemException: ${e.path}');
  } on FormatException catch (e) {
    // Invalid URL.
    print('FormatException: ${e.message}');
  } on ArgumentError catch (e) {
    // Both/none of epubUrl and filePath provided.
    print('ArgumentError: ${e.message}');
  }
}
```
