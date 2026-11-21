import 'dart:io';

import 'package:aile_ile_namaz/dini_gunler.dart';
import 'package:aile_ile_namaz/settings.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:intl/intl.dart';
import 'package:shared_preferences/shared_preferences.dart';

/// Sahte takvim: 1448 tam, 1449'da Ramazan ve sonrası yok (sunucu 500 döner, gerçek durum).
int calls = 0;
Future<Map<String, dynamic>> fakeCalendar(String url) async {
  calls++;
  if (url.contains('/gToH/')) {
    return {
      'data': {
        'hijri': {'year': '1448'}
      }
    };
  }
  final p = RegExp(r'hToG/(\d+)-(\d+)-(\d+)').firstMatch(url)!;
  final day = int.parse(p[1]!), month = int.parse(p[2]!), year = int.parse(p[3]!);
  if (year == 1449 && month >= 9) throw const HttpException('HTTP 500');
  final g = DateTime(2026 + (year - 1448), month, day.clamp(1, 28));
  return {
    'data': {
      'gregorian': {'date': DateFormat('dd-MM-yyyy').format(g)}
    }
  };
}

void main() {
  setUp(() async {
    SharedPreferences.setMockInitialValues({});
    await AppSettings.init();
    DiniGunler.setEventsForTest([]);
    calls = 0;
  });
  tearDown(() {
    DiniGunler.fetchForTest = null;
    DiniGunler.setEventsForTest([]);
  });

  test('takvimde olmayan tarihler atlanır, alınanlar kaydedilir, sonraki açılışta yeniden istenmez', () async {
    DiniGunler.fetchForTest = fakeCalendar;
    await DiniGunler.ensure();
    final missing = diniGunler.where((g) => g.hMonth >= 9).length;
    expect(DiniGunler.events.length, diniGunler.length * 2 - missing);
    final first = calls;
    DiniGunler.setEventsForTest([]); // uygulama yeniden açıldı: liste önbellekten gelir
    await DiniGunler.ensure();
    expect(calls, first, reason: 'önbellek dolu: servise yeniden gidilmez');
    expect(DiniGunler.events.length, diniGunler.length * 2 - missing);
  });

  test('internet yoksa hiçbir şey kaydedilmez, sonra yeniden denenir', () async {
    DiniGunler.fetchForTest = (url) async {
      calls++;
      if (url.contains('/hToG/') && calls > 3) throw const SocketException('yok');
      return fakeCalendar(url);
    };
    await DiniGunler.ensure();
    expect(DiniGunler.events, isEmpty);
    expect(AppSettings.prefs.getString('diniGunler.v1'), isNull);
  });

  test('aynı anda gelen çağrılar tek yenileme yapar', () async {
    DiniGunler.fetchForTest = fakeCalendar;
    await Future.wait([DiniGunler.ensure(), DiniGunler.ensure(), DiniGunler.ensure()]);
    expect(calls, 1 + diniGunler.length * 2);
  });
}
