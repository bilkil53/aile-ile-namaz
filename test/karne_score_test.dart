import 'package:aile_ile_namaz/karne_badges.dart';
import 'package:aile_ile_namaz/models.dart';
import 'package:flutter_test/flutter_test.dart';

/// Karne puanı: kaydı olmayan kullanıcıya yanlış "kılınmadı" bilgisi verilmez.
void main() {
  final now = DateTime(2026, 11, 17, 9); // bugün 17 Kasım, sabah
  DateTime day(int back) => DateTime(2026, 11, 17).subtract(Duration(days: back));
  String k(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';
  Map<String, PrayerStatus> all(PrayerStatus s) => {for (final p in prayerKeys) p: s};
  final week = [for (var i = 6; i >= 0; i--) day(i)];

  test('Hiç kayıt yoksa (misafir, yeni hesap) puan 0/0, ilk gün yok', () {
    expect(karneFirstMarkedDay({}), isNull);
    expect(karneScore({}, week, now: now), (0, 0));
    expect(karneScore({k(day(1)): all(PrayerStatus.none)}, week, now: now), (0, 0));
  });

  test('İlk kayıt gününden önceki günler sayılmaz', () {
    final data = {k(day(2)): all(PrayerStatus.prayed), k(day(1)): all(PrayerStatus.prayed)};
    expect(karneFirstMarkedDay(data), k(day(2)));
    // 2 gün x 5 vakit; bugünün işaretsiz vakitleri ve öncesi sayılmaz
    expect(karneScore(data, week, now: now), (10, 10));
  });

  test('Geçmiş günde işaretsiz vakit kılınmadı sayılır', () {
    final data = {
      k(day(1)): {...all(PrayerStatus.none), 'fajr': PrayerStatus.prayed},
    };
    expect(karneScore(data, week, now: now), (1, 5));
  });

  test('Bugünün işaretsiz vakitleri sayılmaz, işaretlenenler sayılır', () {
    final data = {k(day(0)): {...all(PrayerStatus.none), 'fajr': PrayerStatus.prayed}};
    expect(karneScore(data, week, now: now), (1, 1));
    final missed = {k(day(0)): {...all(PrayerStatus.none), 'fajr': PrayerStatus.missed}};
    expect(karneScore(missed, week, now: now), (0, 1));
  });

  test('Muaf sayılmaz ve vakit bazlı puan çalışır', () {
    final data = {
      k(day(1)): {...all(PrayerStatus.prayed), 'asr': PrayerStatus.excused},
    };
    expect(karneScore(data, week, now: now), (4, 4));
    expect(karneScore(data, week, key: 'asr', now: now), (0, 0));
    expect(karneScore(data, week, key: 'fajr', now: now), (1, 1));
  });
}
