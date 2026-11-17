import 'package:flutter/material.dart';

import '../crash_report.dart';
import '../lang.dart';
import '../log.dart';
import '../theme.dart';

/// Telefonda tutulan son hatalar (Log.warn). Hiçbir şey otomatik gönderilmez:
/// kişi "Gönder"e basarsa mevcut hata raporu yoluyla gider.
class ErrorLogScreen extends StatefulWidget {
  const ErrorLogScreen({super.key});

  @override
  State<ErrorLogScreen> createState() => _ErrorLogScreenState();
}

class _ErrorLogScreenState extends State<ErrorLogScreen> {
  List<String> _lines = Log.history();
  bool _sending = false;

  void _say(String m) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(m)));

  Future<void> _send() async {
    setState(() => _sending = true);
    final ok = await CrashReport.sendLog(_lines);
    if (!mounted) return;
    setState(() => _sending = false);
    _say(ok
        ? t('Hata kaydı gönderildi, teşekkürler.', 'Error log sent, thank you.')
        : t('Gönderilemedi. Giriş yapmış ve internete bağlı olman gerekir.',
            'Could not send. You need to be signed in and online.'));
  }

  Future<void> _clear() async {
    await Log.clearHistory();
    if (mounted) setState(() => _lines = Log.history());
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: Text(t('Hata kaydı', 'Error log'))),
      body: SafeArea(
        top: false,
        child: _lines.isEmpty
            ? Center(
                child: Text(t('Kayıtlı hata yok.', 'No errors recorded.'),
                    style: TextStyle(color: AppColors.onBackgroundMuted)))
            : ListView(
                padding: const EdgeInsets.fromLTRB(16, 16, 16, 24),
                children: [
                  Text(
                      t('Son ${Log.maxHistory} hata bu telefonda tutulur, kendiliğinden gönderilmez. Kişisel bilgiler temizlenmiştir.',
                          'The last ${Log.maxHistory} errors are kept on this phone and never sent automatically. Personal data is removed.'),
                      style: TextStyle(color: AppColors.onBackgroundMuted)),
                  const SizedBox(height: 12),
                  Row(children: [
                    Expanded(
                      child: FilledButton.icon(
                        onPressed: _sending ? null : _send,
                        icon: const Icon(Icons.send),
                        label: Text(t('Gönder', 'Send')),
                      ),
                    ),
                    const SizedBox(width: 12),
                    OutlinedButton(onPressed: _sending ? null : _clear, child: Text(t('Temizle', 'Clear'))),
                  ]),
                  const SizedBox(height: 12),
                  for (final l in _lines)
                    Card(
                      color: AppColors.card,
                      child: Padding(
                        padding: const EdgeInsets.all(10),
                        child: SelectableText(l, style: TextStyle(fontSize: 13, color: AppColors.onCard)),
                      ),
                    ),
                ],
              ),
      ),
    );
  }
}
