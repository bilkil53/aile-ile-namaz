# Teknik notlar

Bu sayfa README'den taşındı. Mimari, veritabanı, derleme dalları ve geliştirici komutları.

## 📦 Teknik Mimari

| Katman | Kullanılan |
|---|---|
| Uygulama | Flutter (Dart 3), Material 3 · görünen sürüm `1.N`, `versionCode` = derleme no · tek pencere (`launchMode=singleTask`) · yazı ölçeği en fazla 1.0 |
| Arka uç | Supabase — PostgreSQL, Row Level Security, Auth, Realtime, Edge Functions, pg_cron, pg_net (Frankfurt / eu-central-1) |
| Giriş | Google Sign-In (telefondaki hesap seçici → `signInWithIdToken`); yedek: OAuth tarayıcı akışı, `io.supabase.aileilenamaz://login-callback/` (demo: `io.supabase.aileilenamaz.demo://login-callback/`) |
| Vakitler | aladhan.com, method 13 (Diyanet): her açılışta **30 gün ileri** aylık takvimle önbelleğe alınır → önbellek → (hiç yoksa) çevrimdışı yaklaşık hesap, ekranda "≈ Yaklaşık vakit" notuyla; 81 il koordinatı |
| Konum | geolocator (son bilinen → anlık konum), geocoding (İlçe, İl); her açılışta otomatik, hata olursa 10 sn arayla 3 deneme; arka plandan dönüşte 1 saatten eskiyse yenilenir |
| Bildirim | flutter_local_notifications (zamanlanmış, 10 gün ileri, kesin alarm; mübarek günler 60 gün ileri) · aile olayları: ön planda Realtime, arka planda **FCM** (`push-family` Edge Function) |
| Dini günler | aladhan `calendarMethod=DIYANET` (hToG / gToH): bu hicri yıl + sonraki yıl önbellekte, ayda bir kontrol, 120 günden az kalınca yenilenir |
| Çevrimdışı | shared_preferences önbelleği + bekleme kuyruğu (aynı vakit için en son kayıt) |
| Güncelleme | GitHub Releases API → işlemci türüne uygun APK (arm64-v8a / armeabi-v7a / x86_64) uygulama içinde indirilir → kendi FileProvider'ı ile ayrı görevde kurulum, eski pencere `finishAndRemoveTask` ile kapanır · Play sürümünde Google Play uygulama içi güncelleme |
| Derleme | GitHub Actions: `flutter create` → manifest/Gradle yaması → R8 koruma kuralları (`android-config/proguard-rules.pro`, APK içinde otomatik doğrulanır) → imzalı, karıştırılmış APK'lar + AAB → GitHub Releases · **Kapılar:** `flutter analyze` hata veya uyarı bulursa ya da birim testlerinden (`test/`) biri başarısız olursa derleme durur; GitHub'da `SUPABASE_DB_URL` ile sunucu güvenlik testi (`supabase/tests/security_test.sql`, geri alınarak) da her derlemede zorunlu çalışır; ayar yoksa ya da test geçmezse derleme ve yayın durur; `main` ve `demo` dallarında kalıcı imza anahtarı (veya parmak izi) yoksa yayın çıkmaz (`dev` geçici anahtarla devam eder) |
| Hata takibi | Dart: `FlutterError.onError` + `PlatformDispatcher.onError` · Android: `ApplicationExitInfo` (kapanma sebebi) + `App.kt` çökme izi · sebep penceresi; yalnızca kişi "Raporu gönder" derse anonim `crash_reports` (30 gün) ya da "Mail ile" · otomatik gönderim yok · gönderilmeden önce e-posta, oturum anahtarı, kimlik (uuid), telefon, gizli parametre ve dosya adları maskelenir (`lib/privacy.dart`) |
| Zamanlanmış görevler | pg_cron: süresi dolan istekleri her dakika düşürür; **`aile-push-retry`** her dakika gönderilemeyen aile bildirimlerini tekrar dener; her gece 7 günden eski aile hareketlerini ve 30 günden eski hata kayıtlarını temizler |
| Bildirim kuyruğu | `pool_events.push_status`: bekliyor → gönderiliyor → gönderildi / hata · `claim_push_event` ile atomik sahiplenme (aynı bildirim iki kez gitmez) · 10 dk içinde en fazla 3 deneme · geçici hatada (429/5xx) o cihaza hemen bir tekrar · ulaşamayan bildirim 1 saat sonra düşer (TTL) |
| Testler | `test/`: CI'da her derlemede çalışan birim testleri (gönderim hatası ayrımı, kuyruk, kaza gönderimi, gezinti modu, hata metni maskeleme, sürüm numarası, durum kodları) · `supabase/tests/security_test.sql`: sunucu güvenlik ve kural testleri (29 kontrol); Supabase SQL Editor'de çalıştırılır, sonunda her şey geri alınır, "SONUC: OK" görünmeli |
| Hata kaydı | `lib/log.dart`: sessiz hatalar konsola ve telefona (konu başına son hata) yazılır; Bildirim izinleri ekranında görünür |

### Paketler
`supabase_flutter ^2.8` · `firebase_core ^3.6` · `firebase_messaging ^15.1` · `in_app_update ^4.2` · `google_sign_in ^6.2` · `geolocator ^13` · `geocoding ^4` · `sensors_plus ^6.1` · `flutter_local_notifications ^18` · `timezone ^0.10` · `crypto ^3.0` · `shared_preferences ^2.3` · `package_info_plus ^8` · `url_launcher ^6.3` · `share_plus ^10.1` · `open_filex ^4.6` · `path_provider ^2.1` · `intl ^0.19`

### Kod yapısı
```
lib/
  main.dart            AuthGate: oturum → HomeShell, yoksa gezinti + Hoş Geldiniz penceresi
  config.dart          Supabase URL, publishable anahtar, Google web client ID
  data.dart            Repo: tüm Supabase sorguları ve RPC'ler, çevrimdışı kuyruk
  local_store.dart     Önbellek, bekleme kuyruğu
  privacy.dart         Hata metinlerinden kişisel/gizli bilgiyi maskeleme
  sync_logic.dart      Senkron kararları (hata ayrımı, kuyruk eşleştirme)
  models.dart          PrayerStatus, Pool, Member, JoinRequest, PoolEvent, ünvanlar
  prayer_times.dart    Diyanet vakitleri, 81 il, kıble açısı
  location.dart        GPS → ilçe/il
  notifications.dart   Vakit hatırlatmaları, günün içeriği, mübarek gün bildirimleri, aile bildirimleri
  dini_gunler.dart     Mübarek günler (Diyanet hicri takvimi), anlamları, yıllık otomatik güncelleme
  crash_report.dart    Hata yakalama, sebep penceresi, mail, anonim rapor, Android kapanma sebebi
  member_icon.dart     Üye simgesi (kız çocuğu simgelerinde fiyonk rozeti)
  push.dart            Firebase anlık bildirim: cihaz kaydı (devices)
  update_checker.dart  GitHub Releases'tan uygulama içi güncelleme (SHA-256 doğrulamalı)
  cuma_reminder.dart   Erkeklere peş peşe 3 Cuma uyarısı
  log.dart             Sessiz hataların kaydı (son hata)
  integrity.dart       APK imza kontrolü (tutmazsa uygulama açılmaz; Play sürümünde yok; yetki kontrolü sunucuda)
  kvkk.dart            KVKK aydınlatma (okudum beyanı)
  screens/             today, family, kaza, karne, settings, login (Hoş Geldiniz), home_shell, notify_health (Bildirim izinleri)
  widgets/             prayer_icon, qibla_card (pusula), dini_gun_card (bayram / kandil kartı)
android-config/
  kotlin/MainActivity.kt  Kapanma sebebi okuma, güncelleme kurulumu (tek pencere), imza parmak izi, pil tasarrufu durumu ve ayar sayfaları
  kotlin/App.kt           Uygulama genelinde çökme kaydedici
  proguard-rules.pro      Zamanlanmış bildirim alıcısı için R8 koruma kuralları
  content/             günlük ayet, hadis, dua, özlü söz, tebrikler
```

### Veritabanı
Tablolar: `profiles`, `pools` (owner = Aile Reisi, `invite_code` EMB…), `pool_members` (ünvan + simge), `join_requests` (5 dk), `daily_prayers`, `kaza_prayers` (sadece sahibi), `kaza_summary` (üyeler toplamı görür), `pool_events` (aile olayları + bildirim; `target_id` ile kişiye özel dua), `devices` (bildirim tercihleri), `crash_reports` (anonim hata kayıtları).

Sunucu fonksiyonları (`security definer`, sadece `authenticated`): `ensure_my_pool`, `create_pool`, `request_join`, `approve_join_with_roles`, `reject_join`, `cancel_join_request`, `my_outgoing_requests`, `leave_pool`, `remove_member`, `transfer_ownership`, `set_member_role`, `regenerate_invite_code`, `delete_my_account`. Yardımcı fonksiyonlar `private` şemasında.

Realtime yayını: `pool_events`, `daily_prayers`, `join_requests`, `pool_members`, `kaza_summary`.

Anlık bildirim: `pool_events` ekleme tetikleyicisi (`pg_net`) → Edge Function `supabase/functions/push-family` → FCM HTTP v1. Gizli ayar `FCM_SERVICE_ACCOUNT` (Supabase Edge Function secret, Firebase hizmet hesabı JSON'u); repoda yoktur.

Migration'lar: `01_core_tables` → `02_helpers_rls_grants` → `03_rpc_triggers_realtime` → `04_family_flow_v118` → `05_drop_join_by_email` → `06_hide_profile_emails` → `07_titles_relative_to_reis` → `08_auto_pool` → `09_emb_codes_5min_titles_delete` → `10_reis_icon_house` → `11_fcm_push_trigger` → `12_rate_limit_cleanup_retention` → `13_cleanup_fix` → `14_device_notification_prefs` → `15_member_gender_by_reis` → `16_crash_reports` → `17–19_targeted_dua` → `20_kvkk_consent` → `21_pro_subscription` → `22_one_pool_max_10` (havuz sınırı o gün 10; 23'te 5'e indi) → `23_family_pro_pool_max_5` → `24_pro_only_reis_requests` → `25_pro_trial_started_at` → `26_expire_test_now` → `27_pro_request_decline` → `28_test_pro_controls` (yalnızca test) → `29_push_queue_excuse_rule_grants` → `30_merge_guest_kaza` (misafir kaza sayıları tek işlemde, tekrar korumalı aktarılır) → `31_harden_function_grants` (anon yetkileri, search_path, pool_events sütun yetkisi) → `32_drop_guest_merge` (misafir aktarımı kaldırıldı) → `33_audit_hardening` (bildirim fonksiyonu anahtarı, telefon başına tek hesap, anonim hata raporu, kullanılmayan sütunlar) → `34_device_lang` → `35_pro_testers` (test Pro işlemleri yalnızca listedeki hesaplara) → `36_acik_riza` → `37_all_prayed` ("5 vaktin hepsini kıldı" aile bildirimi) → `38_hardening` (beyan tarihi sunucu saatiyle, görev günlüğü 7 gün, eksik indeks) → `39_delete_account_kaza_fix` → `40_family_sees_today_yesterday` → `41_push_deliveries_consent_status` → `42_consent_status_dates` → `43_hide_names_before_approval` (onaydan önce adlar gizli) → `44_hide_unconsented_from_family` (rıza vermeyenin bilgisi aileye görünmez) → `45_pro_lock_flag` (sunucuda Pro kilidi) → `46_riza_version_on_resend` (rıza, okunan metnin sürümüyle yazılır).

Tüm migration SQL dosyaları `supabase/migrations/` klasöründedir (sunucudaki kayıtla birebir). Tablo yetkileri, RLS politikaları, RPC'ler ve zamanlanmış işlerin özeti: [`docs/guvenlik-ozeti.md`](docs/guvenlik-ozeti.md).

### Derleme ve dallar
* `dev` → test derlemesi (yayınlanmaz) · `main` → resmi yayın (etiket `v1.N`: v1.0, v1.1 …; derleme no = N + 1) · `demo` → **Namaz Demo** ön sürümü (etiket `v11.N`: v11.0, v11.1 …; ayrı paket `com.bilkil53.aile_ile_namaz.demo`, ayrı test sunucusu, `aile-ile-namaz-demo*.apk`, `--dart-define=CHANNEL=demo`; stabil "son sürüm"ü etkilemez, demo uygulaması yalnızca demo ön sürümlerinden güncellenir). 15 Kasım 2026'da yeni imza anahtarına geçildi; eski `v1.0.N` yayınları arşivdir, yeni uygulamaya kurulamaz.
* Derlemeler sırayla çalışır (aynı sürüm numarası iki kez verilmez); numara yayınların ve etiketlerin en büyüğünden hesaplanır. Eski `r<N>` yayınları sayılmaz. Yalnızca belge (`docs/`, `*.md`) değişince sürüm çıkmaz.
* Sürüm açıklaması `RELEASE_NOTES.md` dosyasından + derleme bilgisiyle otomatik oluşur; ilk satır uygulama içi güncelleme penceresinde görünür.
* İmza: `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD` (GitHub Secrets). SHA-1 sabit kaldığı için Google girişi her sürümde çalışır.

### Geliştiriciler için
```bash
flutter create . --org com.bilkil53 --project-name aile_ile_namaz --platforms android
flutter pub get
flutter run
```
`lib/config.dart` içindeki anahtar **publishable** anahtardır (istemcide olması normaldir). `service_role` anahtarını asla repoya koyma.
