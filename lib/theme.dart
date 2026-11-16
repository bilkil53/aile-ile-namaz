import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Uygulama renkleri (çam yeşili zemin + beyaz kartlar).
class AppColors {
  static const emerald = Color(0xFF0E5A4A);
  static const emeraldDark = Color(0xFF074336);
  static const emeraldLight = Color(0xFF166B58);
  static const emeraldContainer = Color(0xFFE2EFEA);
  static const background = Color(0xFFF7FAF8); // krem zemin
  /// Yeşil zemine doğrudan (kart dışında) yazılan yazı ve ikonlar.
  static const onBackground = Color(0xFF142421);
  static const onBackgroundMuted = Color(0xFF5B6E67);
  /// Beyaz yüzey içindeki krem kutular (eski zemin rengi).
  static const cream = Color(0xFFF7FAF8);
  /// Çam yeşili kartlar ve üzerindeki beyaz yazı.
  static const card = emerald;
  static const onCard = Color(0xFFFFFFFF);
  static const onCardMuted = Color(0xCCFFFFFF);
  static const onCardFaint = Color(0xA6FFFFFF);
  static const white = Color(0xFFFFFFFF);
  static const cardBorder = Color(0xFFE2EBE6);
  static const segmentInactive = Color(0xFFD6E3DE);
  static const textPrimary = Color(0xFF142421);
  static const textSecondary = Color(0xFF5B6E67);
  static const textMuted = Color(0xFF7D918A);
  static const missed = Color(0xFFDC2626);
  static const prayedMint = Color(0xFF86EFAC);
  static const missedCoral = Color(0xFFFCA5A5);
  static const gold = Color(0xFFD97706);
  static const goldContainer = Color(0xFFFEF3C7);
}

/// Yeşil kart içindeki yazı ve ikonlar varsayılan olarak beyaz.
Widget onCardTheme(Widget child) => DefaultTextStyle.merge(
      style: const TextStyle(color: AppColors.onCard),
      child: IconTheme.merge(data: const IconThemeData(color: AppColors.onCard), child: child),
    );

ThemeData buildAppTheme() {
  final scheme = ColorScheme.fromSeed(seedColor: AppColors.emerald).copyWith(
    primary: AppColors.emerald,
    onPrimary: AppColors.white,
    primaryContainer: AppColors.emeraldContainer,
    onPrimaryContainer: AppColors.emerald,
    secondary: AppColors.gold,
    onSecondary: AppColors.white,
    secondaryContainer: AppColors.goldContainer,
    surface: AppColors.white,
    onSurface: AppColors.textPrimary,
    onSurfaceVariant: AppColors.textSecondary,
    outline: AppColors.cardBorder,
  );
  return ThemeData(
    useMaterial3: true,
    colorScheme: scheme,
    scaffoldBackgroundColor: AppColors.background,
    appBarTheme: const AppBarTheme(
      backgroundColor: AppColors.emeraldDark, // yeşil zeminden ayrışsın
      systemOverlayStyle: SystemUiOverlayStyle.light, // durum çubuğu ikonları açık renk
      foregroundColor: AppColors.white,
      elevation: 3,
      surfaceTintColor: Colors.transparent,
    ),
    // Açılır pencereler: başlık ve metin daha küçük ve dengeli
    dialogTheme: const DialogThemeData(
      titleTextStyle: TextStyle(fontSize: 17, fontWeight: FontWeight.bold, color: AppColors.textPrimary),
      contentTextStyle: TextStyle(fontSize: 14, height: 1.4, color: AppColors.textPrimary),
    ),
    listTileTheme: const ListTileThemeData(
      titleTextStyle: TextStyle(fontSize: 14, color: AppColors.textPrimary),
      subtitleTextStyle: TextStyle(fontSize: 12, color: AppColors.textSecondary),
    ),
    snackBarTheme: const SnackBarThemeData(contentTextStyle: TextStyle(fontSize: 13, color: Colors.white)),
    bottomSheetTheme: const BottomSheetThemeData(surfaceTintColor: Colors.transparent),
  );
}
