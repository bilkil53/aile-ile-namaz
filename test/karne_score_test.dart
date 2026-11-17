import 'package:aile_ile_namaz/karne_badges.dart';
import 'package:aile_ile_namaz/models.dart';
import 'package:flutter_test/flutter_test.dart';

/// Karne puanı: dönem her zaman tam gün x 5 vakit; kaydı olmayana yanlış puan verilmez.
void main() {
  final now = DateTime(2026, 11, 17, 9); // bugün 17 Kasım, sabah
  DateTime day(int back) => DateTime(2026, 11, 17).subtract(Duration(days: back));
  String k(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  Map<String, PrayerStatus> all(PrayerStatus s) => {for (final p in prayerKeys) p: s};
  final week = [for (var i = 6; i >= 0; i--) day(i)];
  final month = [for (var i = 29; i >= 0; i--) day(i)];

  test('Hiç kayıt yoksa (misafir, yeni hesap) puan 0/0, ilk gün yok', () {
    expect(karneFirstMarkedDay({}), isNull);
    expect(karneScore({}, week, now: now), (0, 0));
    expect(karneScore({k(day(1)): all(PrayerStatus.none)}, week, now: now), (0, 0));
  });

  test('Hafta 7x5=35, ay 30x5=150 vakit; işaretsizler kılınmadı sayılır', () {
    final data = {k(day(2)): all(PrayerStatus.prayed), k(day(1)): all(PrayerStatus.prayed)};
    expect(karneFirstMarkedDay(data), k(day(2)));
    expect(karneScore(data, week, now: now), (10, 35));
    expect(karneScore(data, month, now: now), (10, 150));
  });

  test('Her vakitte payda gün sayısı (hafta 7), bugün dahil', () {
    final data = {k(day(1)): {...all(PrayerStatus.none), 'fajr': PrayerStatus.prayed}};
    for (final p in prayerKeys) {
      expect(karneScore(data, week, key: p, now: now).$2, 7);
    }
    expect(karneScore(data, week, key: 'fajr', now: now), (1, 7));
  });

  test('Bugünün 5 vakti baştan paydadadır', () {
    final data = {k(day(0)): {...all(PrayerStatus.none), 'fajr': PrayerStatus.prayed}};
    expect(karneScore(data, week, now: now), (1, 35));
  });

  test('Muaf sayılmaz ve vakit bazlı puan çalışır', () {
    final data = {
      k(day(1)): {...all(PrayerStatus.prayed), 'asr': PrayerStatus.excused},
    };
    expect(karneScore(data, week, now: now), (4, 34));
    expect(karneScore(data, week, key: 'asr', now: now), (0, 6));
    expect(karneScore(data, week, key: 'fajr', now: now), (1, 7));
  });
}
