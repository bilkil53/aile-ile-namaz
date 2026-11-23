import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:sensors_plus/sensors_plus.dart';

import '../lang.dart';
import '../prayer_times.dart';
import '../settings.dart';
import '../theme.dart';
import 'qibla_map_screen.dart';

/// Kıble pusulası: ivme + manyetik sensörden telefonun yönünü hesaplar.
/// Sensörler sadece kart açıkken dinlenir (pil dostu).
class QiblaCard extends StatefulWidget {
  const QiblaCard({super.key});

  @override
  State<QiblaCard> createState() => _QiblaCardState();
}

class _QiblaCardState extends State<QiblaCard> {
  bool _open = false;
  double? _heading; // telefonun baktığı yön (derece)
  StreamSubscription<AccelerometerEvent>? _accSub;
  StreamSubscription<MagnetometerEvent>? _magSub;
  List<double> _g = [0, 0, 9.8];
  List<double>? _m;

  @override
  void dispose() {
    _stop();
    super.dispose();
  }

  void _start() {
    _accSub = accelerometerEventStream(samplingPeriod: SensorInterval.uiInterval).listen((e) {
      // alçak geçiren filtre
      _g = [_g[0] * 0.8 + e.x * 0.2, _g[1] * 0.8 + e.y * 0.2, _g[2] * 0.8 + e.z * 0.2];
    });
    _magSub = magnetometerEventStream(samplingPeriod: SensorInterval.uiInterval).listen((e) {
      final m = _m;
      _m = m == null ? [e.x, e.y, e.z] : [m[0] * 0.8 + e.x * 0.2, m[1] * 0.8 + e.y * 0.2, m[2] * 0.8 + e.z * 0.2];
      final h = _azimuth(_g, _m!);
      if (h != null && mounted) setState(() => _heading = (h + _declination(AppSettings.lat, AppSettings.lng) + 360) % 360);
    });
  }

  void _stop() {
    _accSub?.cancel();
    _magSub?.cancel();
    _accSub = null;
    _magSub = null;
  }

  /// Manyetik kuzey ile gerçek kuzey farkı (derece, doğuya pozitif). Sensör manyetik kuzeyi gösterir, kıble açısı
  /// gerçek kuzeye göre hesaplanır; fark düzeltilmezse ok Türkiye'de ~6° kayar. Türkiye için yaklaşık değer
  /// (2026, ±1,5°): İstanbul ≈ 6,0°, doğuya gittikçe artar. Türkiye dışında düzeltme yapılmaz.
  static double _declination(double lat, double lng) {
    if (lat < 34 || lat > 43 || lng < 25 || lng > 46) return 0;
    return 5.9 + 0.2 * (lng - 29.2);
  }

  /// Android SensorManager.getRotationMatrix + getOrientation ile aynı hesap.
  double? _azimuth(List<double> g, List<double> m) {
    var ax = g[0], ay = g[1], az = g[2];
    final ex = m[0], ey = m[1], ez = m[2];
    var hx = ey * az - ez * ay;
    var hy = ez * ax - ex * az;
    var hz = ex * ay - ey * ax;
    final normH = math.sqrt(hx * hx + hy * hy + hz * hz);
    if (normH < 0.1) return null;
    hx /= normH;
    hy /= normH;
    hz /= normH;
    final invA = 1 / math.sqrt(ax * ax + ay * ay + az * az);
    ax *= invA;
    ay *= invA;
    az *= invA;
    final my = az * hx - ax * hz;
    final deg = math.atan2(hy, my) * 180 / math.pi;
    return (deg + 360) % 360;
  }

  @override
  Widget build(BuildContext context) {
    final qibla = PrayerTimesService.qiblaBearing(AppSettings.lat, AppSettings.lng);
    final heading = _heading;
    final diff = heading == null ? null : ((qibla - heading + 540) % 360) - 180;
    final aligned = diff != null && diff.abs() < 5;

    // Material: ListTile dokunma efekti kartın üstünde görünsün (Container arka planı onu gizliyordu)
    return Material(
      textStyle: const TextStyle(color: AppColors.onCard),
      color: AppColors.card,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: Colors.transparent),
      ),
      child: Column(
        children: [
          ListTile(
            iconColor: AppColors.onCard,
            textColor: AppColors.onCard,
            leading: const Icon(Icons.explore, color: AppColors.onCard),
            title: Text(t('Kıble Pusulası', 'Qibla Compass'), style: const TextStyle(fontWeight: FontWeight.bold, color: AppColors.onCard)),
            subtitle: Text(t('${AppSettings.cityName} için kıble: ${qibla.toStringAsFixed(0)}°', 'Qibla for ${AppSettings.cityName}: ${qibla.toStringAsFixed(0)}°'), style: const TextStyle(color: AppColors.onCardMuted)),
            trailing: Icon(_open ? Icons.expand_less : Icons.expand_more, color: AppColors.onCard),
            onTap: () {
              setState(() => _open = !_open);
              _open ? _start() : _stop();
            },
          ),
          if (_open)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              child: Column(
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
                    decoration: BoxDecoration(color: AppColors.emeraldContainer, borderRadius: BorderRadius.circular(12)),
                    child: Text(t('🕋 Kıble ${qibla.toStringAsFixed(1)}°', '🕋 Qibla ${qibla.toStringAsFixed(1)}°'),
                        style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: AppColors.emerald)),
                  ),
                  const SizedBox(height: 12),
                  SizedBox(
                    width: 280,
                    height: 280,
                    child: heading == null
                        ? Center(child: Text(t('Sensör bekleniyor…', 'Waiting for sensor…')))
                        : Transform.rotate(
                            angle: -heading * math.pi / 180,
                            child: CustomPaint(painter: _CompassPainter(qibla, aligned, t('K', 'N'), t('D', 'E'), t('G', 'S'), t('B', 'W'))),
                          ),
                  ),
                  const SizedBox(height: 12),
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: aligned ? AppColors.prayedMint.withValues(alpha: 0.25) : Colors.transparent,
                      border: Border.all(color: aligned ? AppColors.prayedMint : AppColors.onCardFaint),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      heading == null
                          ? t('Telefonu düz tut.', 'Hold the phone flat.')
                          : aligned
                              ? t('Kıble yönündesin ✓', 'You are facing the qibla ✓')
                              : (diff! > 0 ? t('Sağa dön (${diff.abs().toStringAsFixed(0)}°)', 'Turn right (${diff.abs().toStringAsFixed(0)}°)') : t('Sola dön (${diff.abs().toStringAsFixed(0)}°)', 'Turn left (${diff.abs().toStringAsFixed(0)}°)')),
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                        color: aligned ? AppColors.prayedMint : AppColors.onCard,
                      ),
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    t('Telefonu yere paralel tut, metal ve mıknatıslardan uzak dur.', 'Hold the phone parallel to the ground, away from metal and magnets.'),
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 11, color: AppColors.onCardFaint),
                  ),
                  TextButton.icon(
                    style: TextButton.styleFrom(foregroundColor: AppColors.onCard),
                    icon: const Icon(Icons.map),
                    label: Text(t('Haritada göster', 'Show on map')),
                    onPressed: () => Navigator.of(context).push(MaterialPageRoute(builder: (_) => const QiblaMapScreen())),
                  ),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _CompassPainter extends CustomPainter {
  final double qibla;
  final bool aligned;
  final String n, e, so, w;
  _CompassPainter(this.qibla, this.aligned, this.n, this.e, this.so, this.w);

  @override
  void paint(Canvas canvas, Size size) {
    final c = size.center(Offset.zero);
    final r = size.width / 2 - 4;
    canvas.drawCircle(c, r, Paint()..color = AppColors.white);
    canvas.drawCircle(c, r, Paint()..color = AppColors.emeraldContainer..style = PaintingStyle.stroke..strokeWidth = 4);
    // Derece çizgileri: 10°'de kısa, 30°'de uzun, ana yönlerde yeşil
    for (var d = 0; d < 360; d += 10) {
      final a = d * math.pi / 180;
      final major = d % 30 == 0;
      final len = d % 90 == 0 ? 18.0 : (major ? 14.0 : 7.0);
      final dir = Offset(math.sin(a), -math.cos(a));
      canvas.drawLine(
          c + dir * (r - 6),
          c + dir * (r - 6 - len),
          Paint()
            ..color = d % 90 == 0 ? AppColors.emeraldLight : (major ? AppColors.textPrimary : AppColors.textMuted)
            ..strokeWidth = major ? 2.5 : 1.2);
    }
    // Ana yön harfleri (kadranla birlikte döner)
    final labels = {0: n, 90: e, 180: so, 270: w};
    labels.forEach((d, s) {
      final a = d * math.pi / 180;
      final tp = TextPainter(
        text: TextSpan(text: s, style: TextStyle(color: d == 0 ? AppColors.missed : AppColors.textPrimary, fontSize: 18, fontWeight: FontWeight.bold)),
        textDirection: TextDirection.ltr,
      )..layout();
      final p = c + Offset(math.sin(a), -math.cos(a)) * (r - 40);
      canvas.save();
      canvas.translate(p.dx, p.dy);
      canvas.rotate(a);
      tp.paint(canvas, Offset(-tp.width / 2, -tp.height / 2));
      canvas.restore();
    });
    // Kıble oku ve Kabe
    final a = qibla * math.pi / 180;
    final dir = Offset(math.sin(a), -math.cos(a));
    final color = aligned ? AppColors.emeraldLight : AppColors.gold;
    final tip = c + dir * (r - 62);
    canvas.drawLine(c, tip, Paint()..color = color..strokeWidth = 5..strokeCap = StrokeCap.round);
    final side = Offset(-dir.dy, dir.dx);
    canvas.drawPath(
        Path()
          ..moveTo((tip + dir * 12).dx, (tip + dir * 12).dy)
          ..lineTo((tip + side * 8).dx, (tip + side * 8).dy)
          ..lineTo((tip - side * 8).dx, (tip - side * 8).dy)
          ..close(),
        Paint()..color = color);
    final kc = c + dir * (r - 36);
    canvas.drawCircle(kc, 20, Paint()..color = AppColors.white);
    canvas.drawCircle(kc, 20, Paint()..color = color..style = PaintingStyle.stroke..strokeWidth = 3);
    final kaaba = TextPainter(text: const TextSpan(text: '🕋', style: TextStyle(fontSize: 22)), textDirection: TextDirection.ltr)..layout();
    kaaba.paint(canvas, kc - Offset(kaaba.width / 2, kaaba.height / 2));
    canvas.drawCircle(c, 7, Paint()..color = color);
  }

  @override
  bool shouldRepaint(covariant _CompassPainter old) => old.qibla != qibla || old.aligned != aligned || old.n != n;
}
