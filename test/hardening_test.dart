import 'dart:io';

import 'package:aile_ile_namaz/prayer_times.dart';
import 'package:aile_ile_namaz/pro.dart';
import 'package:aile_ile_namaz/update_checker.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Vakit servisine giden koordinat yaklaşık 1 km\'ye yuvarlanır', () {
    expect(PrayerTimesService.coord(41.0082374), '41.01');
    expect(PrayerTimesService.coord(28.9783589), '28.98');
    expect(PrayerTimesService.coord(-33.86881), '-33.87');
  });

  test('Yerel vakit hesabı: vakitler sıralı ve makul (İstanbul, yaz ve kış)', () {
    for (final day in [DateTime(2026, 6, 21), DateTime(2026, 12, 21), DateTime(2026, 3, 20)]) {
      final t = PrayerTimesService.calculate(day, 41.0082, 28.9784);
      final list = [t.imsak, t.gunes, t.ogle, t.ikindi, t.aksam, t.yatsi];
      for (final v in list) {
        expect(RegExp(r'^\d{2}:\d{2}$').hasMatch(v), isTrue, reason: v);
      }
      final sorted = [...list]..sort();
      expect(list, sorted, reason: 'vakitler sırayla artmalı: $list');
      expect(t.ogle.compareTo('12:30') > 0 && t.ogle.compareTo('13:40') < 0, isTrue, reason: 'öğle ${t.ogle}');
    }
  });

  test('En düşük sürüm ayarı: bozuk ya da eksik veri zorlamaz', () {
    expect(UpdateChecker.parseMinBuild('{"min_build": 101}'), 101);
    expect(UpdateChecker.parseMinBuild('{"min_build": "101"}'), 0);
    expect(UpdateChecker.parseMinBuild('{"min_build": -5}'), 0);
    expect(UpdateChecker.parseMinBuild('{}'), 0);
    expect(UpdateChecker.parseMinBuild('<html>404</html>'), 0);
    expect(UpdateChecker.parseMinBuild(''), 0);
  });

  test('Yayın etiketinden derleme numarası: main v1.N, demo v11.N → N + 1; eski etiketler yok sayılır', () {
    expect(UpdateChecker.buildOfTag('v1.0'), 1);
    expect(UpdateChecker.buildOfTag('v1.10'), 11);
    expect(UpdateChecker.buildOfTag('v11.0'), 0); // demo etiketi main'e önerilmez
    expect(UpdateChecker.buildOfTag('v11.0', demo: true), 1);
    expect(UpdateChecker.buildOfTag('v11.12', demo: true), 13);
    expect(UpdateChecker.buildOfTag('v1.3', demo: true), 0); // main etiketi demoya önerilmez
    expect(UpdateChecker.buildOfTag('v1.0.30'), 0); // eski anahtarla imzalı yayın
    expect(UpdateChecker.buildOfTag('v1.0.29-demo', demo: true), 0);
    expect(UpdateChecker.buildOfTag('r105'), 0);
    expect(UpdateChecker.buildOfTag(''), 0);
    expect(UpdateChecker.buildOfTag('v1.0-beta'), 0);
  });

  test('İndirme adresi görülen yayın etiketine sabitlenir; etiket yoksa "en son" adresi', () {
    expect(UpdateChecker.assetBase('v1.17'), endsWith('/releases/download/v1.17'));
    expect(UpdateChecker.assetBase('v1.0.17'), endsWith('/releases/latest/download')); // eski anahtarlı yayın
    expect(UpdateChecker.assetBase(''), endsWith('/releases/latest/download'));
    expect(UpdateChecker.assetBase('r105'), endsWith('/releases/latest/download'));
    expect(UpdateChecker.assetBase('../kötü'), endsWith('/releases/latest/download'));
  });

  test('Play bağlantısı çalışan uygulamanın paket adından kurulur (demo ayrı paket)', () {
    expect(UpdateChecker.playUrl('com.bilkil53.aile_ile_namaz.demo'), endsWith('?id=com.bilkil53.aile_ile_namaz.demo'));
    expect(UpdateChecker.playUrl('com.bilkil53.aile_ile_namaz'), endsWith('?id=com.bilkil53.aile_ile_namaz'));
  });

  test('Kararlı kanalda Pro test işlevleri sunucuya hiç gitmez (Supabase açılmadan false döner)', () async {
    expect(kTestChannel, isFalse); // testler CHANNEL verilmeden = kararlı kanal
    expect(await Pro.testerStatus(), isFalse);
    expect(await Pro.testRestartTrial(), isFalse);
    expect(await Pro.expireTest(), isFalse);
    expect(await Pro.redeem(), isFalse);
  });

  test('CI test kodu denetimindeki yazılar kodda geçer (denetim eskimesin)', () {
    final wf = File('.github/workflows/build-apk.yml').readAsStringSync();
    final m = RegExp(r'for m in ([a-z_ ]+); do').firstMatch(wf);
    expect(m, isNotNull, reason: 'build-apk.yml test kodu denetimi yok');
    final markers = m!.group(1)!.trim().split(' ');
    expect(markers, containsAll(['am_i_pro_tester', 'test_restart_trial', 'expire_pro_test']));
    final src = File('lib/pro.dart').readAsStringSync();
    for (final x in markers) {
      expect(src.contains(x), isTrue, reason: 'denetim yazısı kodda yok: $x');
    }
  });
}
