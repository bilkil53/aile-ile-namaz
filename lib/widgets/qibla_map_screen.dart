import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';

import '../lang.dart';
import '../prayer_times.dart';
import '../settings.dart';
import '../theme.dart';

/// Haritada kıble: konumdan Kabe'ye büyük daire boyunca kesikli çizgi. Yön sensörüne bağlı değil;
/// çizgiyi sokak ve binalara bakarak takip edersin.
class QiblaMapScreen extends StatefulWidget {
  const QiblaMapScreen({super.key});

  @override
  State<QiblaMapScreen> createState() => _QiblaMapScreenState();
}

class _QiblaMapScreenState extends State<QiblaMapScreen> {
  static const _kaaba = LatLng(21.4225, 39.8262);
  static const _zoom = 16.0;
  final _map = MapController();

  @override
  Widget build(BuildContext context) {
    final me = LatLng(AppSettings.lat, AppSettings.lng);
    final qibla = PrayerTimesService.qiblaBearing(me.latitude, me.longitude);
    const dist = Distance();
    final km = dist.as(LengthUnit.Kilometer, me, _kaaba);
    // Büyük daire: 50 km'lik adımlarla, her adımda yön yeniden hesaplanır
    final path = <LatLng>[me];
    var p = me;
    while (dist.as(LengthUnit.Kilometer, p, _kaaba) > 50) {
      p = dist.offset(p, 50000, dist.bearing(p, _kaaba));
      path.add(p);
    }
    path.add(_kaaba);

    Widget roundBtn(IconData icon, VoidCallback onTap) => Material(
          color: AppColors.white,
          elevation: 3,
          shape: const CircleBorder(),
          child: IconButton(icon: Icon(icon, color: AppColors.emerald), onPressed: onTap),
        );

    return Scaffold(
      appBar: AppBar(title: Text(t('Haritada Kıble', 'Qibla on Map'))),
      body: Stack(
        children: [
          FlutterMap(
            mapController: _map,
            options: MapOptions(initialCenter: me, initialZoom: _zoom),
            children: [
              TileLayer(
                urlTemplate: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
                userAgentPackageName: 'com.bilkil53.aile_ile_namaz',
              ),
              PolylineLayer(polylines: [
                Polyline(
                  points: path,
                  strokeWidth: 5,
                  color: AppColors.gold,
                  pattern: StrokePattern.dashed(segments: [14, 8]),
                ),
              ]),
              MarkerLayer(markers: [
                Marker(point: me, child: const Icon(Icons.my_location, color: Colors.blue, size: 28)),
                const Marker(point: _kaaba, child: Text('🕋', style: TextStyle(fontSize: 24))),
              ]),
              const SimpleAttributionWidget(source: Text('OpenStreetMap')),
            ],
          ),
          Positioned(
            right: 12,
            top: 12,
            child: roundBtn(Icons.my_location, () => _map.move(me, _zoom)),
          ),
          Positioned(
            right: 12,
            bottom: 110,
            child: Column(children: [
              roundBtn(Icons.add, () => _map.move(_map.camera.center, _map.camera.zoom + 1)),
              const SizedBox(height: 8),
              roundBtn(Icons.remove, () => _map.move(_map.camera.center, _map.camera.zoom - 1)),
            ]),
          ),
          Positioned(
            left: 12,
            right: 12,
            bottom: 36,
            child: Material(
              color: AppColors.white,
              elevation: 4,
              borderRadius: BorderRadius.circular(16),
              child: Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                  children: [
                    Text('🕋 ${qibla.toStringAsFixed(1)}°',
                        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold, color: AppColors.emerald)),
                    Container(width: 1, height: 28, color: AppColors.cardBorder),
                    Text('${km.toStringAsFixed(0)} km',
                        style: const TextStyle(fontSize: 18, color: AppColors.textSecondary)),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}
