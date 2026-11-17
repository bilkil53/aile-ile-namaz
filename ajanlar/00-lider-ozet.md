# Lider özeti — Code sekmesi buradan devam eder

> Depo herkese açık. Burada sır, kimlik numarası, hesap adı ve güvenlik açığı ayrıntısı yazılmaz.
> Tam özet claude.ai'deki "Aile ile Namaz" projesinin belgesindedir (ajanlar/00-lider-ozet.md).

## Çalışma düzeni
- Tek ajan (lider) çalışır. Bağımsız inceleme gerekirse ayrı ajan kullanılır.
- Onaylar anketle sorulur. Cevaplar Türkçe ve kısa. Play listesi ve token grafiği yalnız istenince.
- Akış (16 Kasım 2026'da güncellendi): iş dalı (check/...) → önce demo yayını → testler (analiz, CI, bağımsız inceleme) ile kullanıcının telefonda denemesi paralel yürür → onayla main. Demo ve main aynı anda yayınlanmaz.
- Testler kırmızı çıkarsa demo düzeltilip yeniden yayınlanır; main'e yalnız testler yeşil + kullanıcı onayıyla geçilir.
- Gerçek veritabanına yazma, işlev yükleme, silme: ayrı onay ve önce geri alınan deneme.
- Sohbet uzayınca: kullanıcı `/compact` yazar (aynı sohbette devam). Yeni oturum özeti CLAUDE.md sayesinde kendiliğinden okur. "Özeti güncelle" denince bu dosya güncellenip doğrudan main'e push edilir (tek güncel kaynak main; iş dalına push edilmez).

## Durum (17 Kasım 2026)
- main v1.3 yayında (cba6d35, etiket v1.3): içerik v1.2 ile aynı; main testinin Yayınla adımı ekran testinden önce çalıştığı için stabil sürüm test bitmeden çıktı.
- Demo v11.13 (ön sürüm, ce56ca8): tam test yeşil (run 44035540134): analiz, birim testleri, sunucu güvenlik, APK, demo ön sürüm yayını, ekran testi, Test Lab. Testsiz hak sayacı 0.
- Main tam testi (run 44039187900, cba6d35): analiz, birim, sunucu güvenlik ve APK adımları geçti; emülatör ekran testi kırmızı (giriş yapılmış test: "Invalid login credentials"). Test Lab adımına geçilmedi. Hata test hesabı girişiyle ilgili, v1.2'deki kırmızılıkla aynı neden (kayıtla doğrulandı).
- Gerçek sunucuya demodaki değişiklikler uygulandı (47, 48, Pro havuz sahibi, 49 hata raporu yalnız girişli); güvenlik testi geçti. Bildirim işlevi demo ile aynı kod.
- Bildirim ve Google girişi yeni Firebase projesine geçti; eski Google istemci kimliğinin gerçek sunucudan silinmesine karar verildi. Kodda eski projeye iz yok.
- Test hesabı gerçek sunucuda girişe izin vermiyor (hesap yok ya da parola farklı). Yayın, bu kırmızılığa rağmen çıktı; etkilenen kullanıcı yok (uygulama kullanıcıları etkilenmez, yalnız test otomasyonu).
- Token göstergesi işi iptal edildi.
- `check/destek-log` dalı (hızlı kontrol yeşil, demo bekliyor): destek adresi kişisel adresten ayrı uygulama adresine taşındı (uygulama + gizlilik/hesap silme sayfaları); kör noktadaki 7 boş `catch`'e `Log.warn`; Ayarlar → Hata kaydı (telefonda son 50 hata, kişi isterse kimliksiz `crash_reports`'a gönderir, otomatik gönderim yok).

## Kullanıcıdan beklenen
1. Demo v11.13'ü telefonda denemek; bittiğinde main'de tam testi başlatmak için onay vermek.
2. Üç telefona aynı gün v1.2 kurmak; Google girişi ve bildirimi denemek (v1.1'de kalan bildirim almaz, Google girişi yapamaz).
3. Ekran testi için test hesabını gerçek sunucuda yeniden açmak ya da GitHub'daki test hesabı ayarlarını güncellemek.
4. Eski Firebase projesini (Firebase konsolunda) silmek isteyip istemediğine karar vermek.
5. Play Console'daki geliştirici e-postasını yeni destek adresine çevirmek; hesap silme talepleri artık o kutuya gelir.

## Sıradaki işler
1. Test hesabı düzeltilince main tam testini yeniden çalıştırmak; yeşilse demo → main için onay (main'e push yeni stabil sürüm çıkarır).
2. `check/destek-log` → demo → telefonda deneme → onayla main (v11.13 main'e geçtikten sonra).
3. Telefon denemesi sonrası eski FCM kayıtlarını ve eski dalları temizlemek (onaylı).
4. Küçük işler ve temizlik (ayrıntı proje belgesinde).
5. Ertelenen bulgular (düşük): family_screen (1628 satır) ve settings_screen (1092 satır) bölünmesi, family_screen testi; kalan 69 `catch (_)` hatayı kaydetmiyor (çoğu bilerek).

## Sonra kontrol edilecek: mobil uygulama yayın öncesi 20 madde (kullanıcı notu, 16 Kasım 2026)
1 Kullanıcı izinleri · 2 Gizlilik politikası · 3 Hesap silme · 4 Veri güvenliği · 5 Kayıt ve giriş · 6 E-posta doğrulama · 7 Şifre sıfırlama · 8 Onboarding · 9 Ana kullanıcı akışı · 10 Verilerin kaydı · 11 Ödeme ve abonelik · 12 Satın alımları yükleme · 13 Yükleniyor ekranları · 14 Boş ekranlar · 15 Hata ekranları · 16 İnternet bağlantısı · 17 Bildirimler · 18 Analytics ve çökme raporu · 19 Farklı cihazlar · 20 Gerçek kullanıcı testi.
Kullanıcı isteyince uygulama bu listeye göre tek tek kontrol edilir.

## Play'e çıkarken yapılacaklar
AAB ve Play testleri; Play App Signing; Google giriş ekranını yayınla; Pro test işlevlerini kaldır; KVKK ve avukat; Play Integrity; Play Billing; AdMob; Supabase ücretli plan; Diyanet resmî vakitleri; Test Lab yetkisini daralt; Developer Device Platform'a geçiş (en geç Nisan 2027).

## /sondurum kontrolleri (bu proje)
- Rapora bu özetteki "Durum", "Kullanıcıdan beklenen" ve "Sıradaki işler" bölümlerini de ekle (yarım kalan işler, bekleyenler). Sohbette iş yapılmamış olsa bile atlama.
- Depo: bu oturuma bağlı depo (`git remote`); dallar ve etiketler `git ls-remote --heads --tags origin`, çalışmalar ve yayınlar GitHub araçlarıyla.
- Sunucu: bağlı Supabase projesi, yalnız okuma (değişiklik listesi, hesap sayıları). Kimlik numarası yazma.
- Play listesi bu özetteki "Play'e çıkarken" bölümüdür, rapora yalnız istenince eklenir.
