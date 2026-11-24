import 'package:aile_ile_namaz/widgets/qibla_card.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  const still = [120.0, 120.5, 119.5, 120.2, 119.8];

  test('normal alan, sabit ölçüm, iki kaynak uyumlu: sorun yok', () {
    expect(compassIssue(field: 45, recent: still, heading: 120, fused: 122), CompassIssue.none);
  });

  test('birleşik sensör yoksa yalnız alan ve kararlılığa bakılır', () {
    expect(compassIssue(field: 45, recent: still, heading: 120), CompassIssue.none);
  });

  test('alan gücü Dünya aralığı dışında: parazit', () {
    expect(compassIssue(field: 90, recent: still, heading: 120, fused: 120), CompassIssue.interference);
    expect(compassIssue(field: 10, recent: still, heading: 120, fused: 120), CompassIssue.interference);
  });

  test('iki kaynak 5°ten fazla ayrışınca: kalibrasyon', () {
    expect(compassIssue(field: 45, recent: still, heading: 120, fused: 130), CompassIssue.mismatch);
  });

  test('359° ile 1° arası fark 2° sayılır', () {
    expect(compassIssue(field: 45, recent: const [359, 0, 1, 359.5, 0.5], heading: 359, fused: 1), CompassIssue.none);
  });

  test('dağınık ölçüm: kararsız (ayrışma kontrolünden önce)', () {
    expect(compassIssue(field: 45, recent: const [100, 120, 140, 110, 130], heading: 120, fused: 150), CompassIssue.unstable);
  });

  test('yayılım: aynı yönler 0, 0° geçişinde bozulmaz', () {
    expect(headingSpread(const [50, 50, 50]), closeTo(0, 0.01));
    expect(headingSpread(const [358, 2]), closeTo(2, 0.1));
  });
}
