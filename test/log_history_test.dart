import 'package:aile_ile_namaz/log.dart';
import 'package:aile_ile_namaz/settings.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await AppSettings.init();
  });

  test('geçmiş en yeni başta, sınırı aşmaz, konu ve son hata korunur', () async {
    for (var i = 0; i < Log.maxHistory + 5; i++) {
      Log.warn(i.isEven ? 'pro' : 'kaza', 'hata $i');
    }
    final h = Log.history();
    expect(h.length, Log.maxHistory);
    expect(h.first, endsWith('pro · hata ${Log.maxHistory + 4}'));
    expect(h.last, endsWith('kaza · hata 5'));
    expect(Log.last('pro'), endsWith('hata ${Log.maxHistory + 4}'));
  });

  test('temizle geçmişi boşaltır', () async {
    Log.warn('pro', 'x');
    await Log.clearHistory();
    expect(Log.history(), isEmpty);
  });
}
