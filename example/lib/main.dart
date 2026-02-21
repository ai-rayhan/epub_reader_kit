import 'package:flutter/material.dart';
import 'package:epub_reader_kit/epub_reader_kit.dart';

void main() {
  runApp(const ExampleApp());
}

class ExampleApp extends StatelessWidget {
  const ExampleApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'EPUB Reader Kit Example',
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.teal),
      ),
      home: const ExampleHomePage(),
    );
  }
}

class ExampleHomePage extends StatefulWidget {
  const ExampleHomePage({super.key});

  @override
  State<ExampleHomePage> createState() => _ExampleHomePageState();
}

class _ExampleHomePageState extends State<ExampleHomePage> {
  final TextEditingController _urlController = TextEditingController();
  bool _isOpening = false;
  String _status = 'Enter an EPUB URL and tap Open';

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _openReader() async {
    final url = _urlController.text.trim();
    final uri = Uri.tryParse(url);
    final isHttp = uri != null && (uri.scheme == 'http' || uri.scheme == 'https');
    if (!isHttp) {
      setState(() => _status = 'Please enter a valid http/https EPUB URL');
      return;
    }

    setState(() {
      _isOpening = true;
      _status = 'Opening reader...';
    });

    try {
      final opened = await EpubReaderService.readBook(epubUrl: url);
      setState(() => _status = opened ? 'Reader opened' : 'Failed to open reader');
    } catch (e) {
      setState(() => _status = 'Error: $e');
    } finally {
      if (mounted) {
        setState(() => _isOpening = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('EPUB Reader Demo'),
      ),
      body: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              controller: _urlController,
              keyboardType: TextInputType.url,
              decoration: const InputDecoration(
                labelText: 'EPUB URL',
                hintText: 'https://example.com/book.epub',
                border: OutlineInputBorder(),
              ),
            ),
            const SizedBox(height: 12),
            ElevatedButton(
              onPressed: _isOpening ? null : _openReader,
              child: _isOpening
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    )
                  : const Text('Open Reader'),
            ),
            const SizedBox(height: 12),
            Text(_status),
          ],
        ),
      ),
    );
  }
}
