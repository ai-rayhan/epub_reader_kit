import 'package:file_picker/file_picker.dart';
import 'package:flutter/material.dart';

import 'epub_reader_service.dart';

class EpubReaderLauncher extends StatefulWidget {
  const EpubReaderLauncher({super.key});

  @override
  State<EpubReaderLauncher> createState() => _EpubReaderLauncherState();
}

class _EpubReaderLauncherState extends State<EpubReaderLauncher> {
  final TextEditingController _urlController = TextEditingController();
  bool _isWorking = false;
  String _status = 'Paste EPUB URL or pick a local EPUB file';

  @override
  void dispose() {
    _urlController.dispose();
    super.dispose();
  }

  Future<void> _downloadAndOpen() async {
    final url = _urlController.text.trim();
    if (url.isEmpty) {
      setState(() => _status = 'Please enter an EPUB URL');
      return;
    }

    final uri = Uri.tryParse(url);
    if (uri == null ||
        !(uri.hasScheme && (uri.scheme == 'http' || uri.scheme == 'https'))) {
      setState(() => _status = 'Invalid URL');
      return;
    }

    setState(() {
      _isWorking = true;
      _status = 'Downloading EPUB...';
    });

    try {
      setState(() => _status = 'Opening in reader...');
      final ok = await EpubReaderService.openFromUrl(url);
      setState(() => _status = ok ? 'Opened in reader' : 'Failed to open book');
    } catch (e) {
      setState(() => _status = 'Error: $e');
    } finally {
      if (mounted) {
        setState(() => _isWorking = false);
      }
    }
  }

  Future<void> _pickAndOpenLocalFile() async {
    setState(() {
      _isWorking = true;
      _status = 'Selecting EPUB file...';
    });

    try {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: const ['epub'],
      );

      if (result == null || result.files.single.path == null) {
        setState(() => _status = 'File selection canceled');
        return;
      }

      final filePath = result.files.single.path!;
      setState(() => _status = 'Opening selected EPUB...');

      final ok = await EpubReaderService.openFromFilePath(filePath);
      setState(() => _status = ok ? 'Opened in reader' : 'Failed to open book');
    } catch (e) {
      setState(() => _status = 'Error: $e');
    } finally {
      if (mounted) {
        setState(() => _isWorking = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const Text(
          'Open from URL',
          style: TextStyle(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _urlController,
          keyboardType: TextInputType.url,
          decoration: const InputDecoration(
            hintText: 'https://example.com/book.epub',
            border: OutlineInputBorder(),
          ),
        ),
        const SizedBox(height: 12),
        ElevatedButton(
          onPressed: _isWorking ? null : _downloadAndOpen,
          child: _isWorking
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(strokeWidth: 2),
                )
              : const Text('Download and Open'),
        ),
        const SizedBox(height: 20),
        const Divider(),
        const SizedBox(height: 12),
        const Text(
          'Open local EPUB file',
          style: TextStyle(fontWeight: FontWeight.w600),
        ),
        const SizedBox(height: 8),
        OutlinedButton(
          onPressed: _isWorking ? null : _pickAndOpenLocalFile,
          child: const Text('Pick EPUB File and Open'),
        ),
        const SizedBox(height: 12),
        Text(_status),
      ],
    );
  }
}
