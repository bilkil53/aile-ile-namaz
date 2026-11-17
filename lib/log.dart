import 'package:flutter/foundation.dart';

import 'privacy.dart';
import 'settings.dart';

/// Sessizce yutulan hataların kaydı: konsola yazılır ve son hata (konu başına) telefonda saklanır.
/// "Bildirim izinleri" ekranı bildirim hatalarını buradan gösterir.
/// Ayrıca tüm konuların son [maxHistory] hatası sırayla tutulur (Ayarlar → Hata kaydı).
class Log {
  static String _k(String tag) => 'log.last.$tag';
  static const _kHist = 'log.history';
  static const maxHistory = 50;

  static void warn(String tag, Object e) {
    debugPrint('[$tag] ${scrubSensitive(e.toString())}');
    try {
      final now = DateTime.now();
      final t = '${now.day.toString().padLeft(2, '0')}.${now.month.toString().padLeft(2, '0')} '
          '${now.hour.toString().padLeft(2, '0')}:${now.minute.toString().padLeft(2, '0')}';
      final msg = scrubSensitive(e.toString());
      final short = msg.length > 160 ? '${msg.substring(0, 160)}…' : msg;
      AppSettings.prefs.setString(_k(tag), '$t · $short');
      final h = [..._hist(), '$t · $tag · $short'];
      AppSettings.prefs.setStringList(_kHist, h.length > maxHistory ? h.sublist(h.length - maxHistory) : h);
    } catch (_) {}
  }

  static List<String> _hist() => AppSettings.prefs.getStringList(_kHist) ?? const [];

  /// Son hatalar, en yenisi başta.
  static List<String> history() {
    try {
      return _hist().reversed.toList();
    } catch (_) {
      return const [];
    }
  }

  static Future<void> clearHistory() async {
    try {
      await AppSettings.prefs.remove(_kHist);
    } catch (_) {}
  }

  static String? last(String tag) {
    try {
      return AppSettings.prefs.getString(_k(tag));
    } catch (_) {
      return null;
    }
  }

  static Future<void> clear(String tag) async {
    try {
      await AppSettings.prefs.remove(_k(tag));
    } catch (_) {}
  }
}
