import 'package:flutter/services.dart';
import 'dart:async';
import 'dart:io';

class EpubReaderService {
  static const MethodChannel _channel = MethodChannel(
    'com.example.epub_reader_kit/reader',
  );
  static final StreamController<int> _pageChangeController =
      StreamController<int>.broadcast();
  static bool _callbackInitialized = false;

  static Stream<int> get onPageChanged => _pageChangeController.stream;

  static void _ensureCallbackHandler() {
    if (_callbackInitialized) return;
    _callbackInitialized = true;
    _channel.setMethodCallHandler((call) async {
      if (call.method == 'onEpubPageChanged') {
        final raw = (call.arguments as Map?)?['percentage'];
        final percent = raw is num
            ? raw.toInt().clamp(0, 100)
            : int.tryParse(raw?.toString() ?? '')?.clamp(0, 100);
        if (percent != null) {
          _pageChangeController.add(percent);
        }
      }
    });
  }

  /// Opens EPUB from local file path.
  static Future<bool> openBook(String bookPath, {String? sourceKey}) async {
    _ensureCallbackHandler();
    try {
      final result = await _channel.invokeMethod<bool>('openBook', {
        'bookPath': bookPath,
        'sourceKey': sourceKey ?? 'local:$bookPath',
      });
      return result ?? false;
    } on MissingPluginException {
      throw PlatformException(
        code: 'MISSING_NATIVE_HANDLER',
        message:
            'Native plugin not registered. Rebuild the Android app and ensure epub_reader_kit is included as a dependency.',
      );
    }
  }

  /// Opens EPUB from local file path.
  static Future<bool> openFromFilePath(String filePath) async {
    final file = File(filePath);
    if (!await file.exists()) {
      throw FileSystemException('EPUB file not found', filePath);
    }
    return openBook(filePath, sourceKey: 'local:$filePath');
  }

  /// Opens remote EPUB URL directly with native import/open flow.
  static Future<bool> openFromUrl(String epubUrl) async {
    _ensureCallbackHandler();
    final uri = Uri.tryParse(epubUrl);
    if (uri == null) {
      throw const FormatException('Invalid EPUB URL');
    }
    try {
      final result = await _channel.invokeMethod<bool>('openBookFromUrl', {
        'epubUrl': epubUrl,
        'sourceKey': 'remote:$epubUrl',
      });
      return result ?? false;
    } on MissingPluginException {
      throw PlatformException(
        code: 'MISSING_NATIVE_HANDLER',
        message:
            'Native plugin not registered. Rebuild the Android app and ensure epub_reader_kit is included as a dependency.',
      );
    }
  }

  /// Unified API: pass either [epubUrl] or [filePath] (exactly one).
  static Future<bool> open({String? epubUrl, String? filePath}) async {
    _ensureCallbackHandler();
    final trimmedUrl = epubUrl?.trim();
    final trimmedPath = filePath?.trim();
    final hasUrl = trimmedUrl != null && trimmedUrl.isNotEmpty;
    final hasPath = trimmedPath != null && trimmedPath.isNotEmpty;

    if (hasUrl == hasPath) {
      throw ArgumentError('Pass exactly one of epubUrl or filePath.');
    }

    if (hasUrl) {
      return openFromUrl(trimmedUrl);
    }
    return openFromFilePath(trimmedPath!);
  }

  /// One-call API for clients. Opens the EPUB in native reader.
  static Future<bool> readBook({String? epubUrl, String? filePath}) {
    return open(epubUrl: epubUrl, filePath: filePath);
  }

  /// Returns reading progress percentage (0-100) for a previously opened source.
  ///
  /// [bookSource] can be:
  /// - raw local file path
  /// - raw remote URL
  /// - explicit source key (`local:<path>` or `remote:<url>`)
  static Future<int> getProgress(String bookSource) async {
    final source = bookSource.trim();
    if (source.isEmpty) return 0;
    _ensureCallbackHandler();

    final result = await _channel.invokeMethod<int>('getReadingProgress', {
      'bookSource': source,
    });
    return (result ?? 0).clamp(0, 100);
  }
}
