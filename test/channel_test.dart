import 'package:aile_ile_namaz/config.dart';
import 'package:flutter_test/flutter_test.dart';

/// Kanala (CHANNEL) bağlı ayarlar. Testler CHANNEL verilmeden çalışır: kararlı kanal değerleri beklenir.
void main() {
  test('Giriş dönüş adresi: kararlı kanalda demo şeması kullanılmaz', () {
    expect(AppConfig.authRedirect, 'io.supabase.aileilenamaz://login-callback/');
  });
}
