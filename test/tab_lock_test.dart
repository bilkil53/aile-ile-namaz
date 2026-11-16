import 'package:aile_ile_namaz/screens/home_shell.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('Ücretsizde de tüm sekmelere girilir (içerik pasif görünür)', () {
    for (final i in [0, 1, 2, 3]) {
      expect(HomeShellTabs.locked(i, demo: true), isFalse, reason: 'sekme $i');
    }
  });

  test('Pro / denemede tüm sekmeler açık', () {
    for (final i in [0, 1, 2, 3]) {
      expect(HomeShellTabs.locked(i, demo: false), isFalse, reason: 'sekme $i');
    }
  });
}
