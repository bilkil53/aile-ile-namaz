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

## Durum (17 Kasım 2026, öğleden sonra)
- Demo `5fba53f` tam yeşil (run 44075033754): analiz, birim, sunucu güvenlik, APK, demo ön sürüm, giriş yapılmış ekran testi (TR+EN) ve Google Test Lab geçti.
- Demo → main birleşimi `4665259` main'e alındı (hata kaydı, destek adresi uygulama adresine taşındı; main'de kişisel e-posta kalmadı, kod ve sayfalar üzerinden doğrulandı, telefonda henüz bakılmadı). Ana iş akışı çalışması 44092955273 başladı; sonucu henüz doğrulanmadı. Yayın ekran testinden önce çıkar.
- Demo'daki eski test hesabı silinemedi (veritabanı kısıtı; ayrıntı özel nottadır). Kullanıcı yeni test hesabını GitHub secret'larına ekledi; giriş testinin buna göre geçtiği doğrulanmadı.
- Eski repo için yerel klon silindi. GitHub'daki eski reponun arşivlenmesi ya da silinmesi kullanıcı kararı.
- Madde 00 sadeleştirildi; ayrıntı yerel özel nottadır ve commit edilmez. Eski ayrıntı herkese açık geçmişte duruyor, geçmiş temizliği için onay bekleniyor.
- main v1.3 yayında (cba6d35, etiket v1.3): içerik v1.2 ile aynı; main testinin Yayınla adımı ekran testinden önce çalıştığı için stabil sürüm test bitmeden çıktı.
- Demo v11.13 (ön sürüm, ce56ca8): tam test yeşil (run 44035540134): analiz, birim testleri, sunucu güvenlik, APK, demo ön sürüm yayını, ekran testi, Test Lab. Testsiz hak sayacı 0.
- Main tam testi (run 44039187900, cba6d35): analiz, birim, sunucu güvenlik ve APK adımları geçti; emülatör ekran testi kırmızı (giriş yapılmış test: "Invalid login credentials"). Test Lab adımına geçilmedi. Hata test hesabı girişiyle ilgili, v1.2'deki kırmızılıkla aynı neden (kayıtla doğrulandı).
- Gerçek sunucuya demodaki değişiklikler uygulandı (47, 48, Pro havuz sahibi, 49 hata raporu yalnız girişli); güvenlik testi geçti. Bildirim işlevi demo ile aynı kod.
- Bildirim ve Google girişi yeni Firebase projesine geçti; eski Google istemci kimliğinin gerçek sunucudan silinmesine karar verildi. Kodda eski projeye iz yok.
- Test hesabı gerçek sunucuda girişe izin vermiyor (hesap yok ya da parola farklı). Yayın, bu kırmızılığa rağmen çıktı; kullanıcıya etkisi doğrulanmadı.
- Token göstergesi işi iptal edildi.
- Karne düzeltmesi (kaydı olmayana "en çok aksattığın vakit" ve %0 not çıkıyordu; artık ilk kayıt gününden sayılır, bugünün işaretsiz vakitleri sayılmaz, misafirde "Henüz kayıt yok", Aile Başarısı misafirde "–", Bugün ipucu misafire giriş gerektiğini söyler; test/karne_score_test.dart): ilk üç çalışma (demo 44044402429, main 44044406248, demo 44044883798) gereksiz `!` analiz uyarısı yüzünden kırmızı çıktı, yayın çıkmadı. Düzeltmeyle demo 3a832f2 TAM YEŞİL (run 44047751755: analiz, birim, demo sunucu güvenlik, APK, demo ön sürüm v11.14, GitHub emülatör ekran testi giriş yapılmış akış dahil, Test Lab). main 73d6366'ya aynı değişiklikler uygulandı (main ve demo kodu/test/workflow birebir aynı); main çalışması 44051122792: analiz, birim, gerçek sunucu güvenlik, APK, R8, test kodu kontrolü geçti, v1.4 yayınlandı; GitHub emülatörde misafir akışı TR+EN geçti, giriş yapılmış ekran testi "Invalid login credentials" ile kırmızı (test hesabı sorunu, Karne ile ilgisiz); bu yüzden Test Lab adımı atlandı (main'de Test Lab hiç koşmadı).
- Küçük işler ajanı eklendi: `.claude/agents/kucuk-isler.md` (Haiku; okuma/arama/düzenleme/kabuk; commit, push, workflow, supabase, yayın ve gerçek veritabanına dokunmaz). CLAUDE.md'ye devir kuralı yazıldı; ajan listesi oturum açılırken yüklendiği için yeni oturumda görünür.
- Testsiz demo sayacı kuralı tamamen silindi (ci/demo-testsiz.txt ve workflow koşulları; demo fd8d62a, main 7cc2253). Yeni akış: işler önce demoya, tüm testler yeşilse main.
- `check/destek-log` işi (hızlı kontrol yeşil) güncel demonun üstüne 3 commit olarak alındı (çakışmasız) ve demoya gönderildi (5fba53f; main'e dokunulmadı): destek adresi kişisel adresten ayrı uygulama adresine taşındı (uygulama + gizlilik/hesap silme sayfaları); kör noktadaki 7 boş `catch`'e `Log.warn`; Ayarlar → Hata kaydı (telefonda son 50 hata, kişi isterse kimliksiz `crash_reports`'a gönderir, otomatik gönderim yok). Demo tam testi hâlâ çalışıyor (run 44064903939, 17 Kasım 10:26'da başladı; sonuç henüz yok, doğrulanmadı); yerelde Flutter yok, test yalnız CI'da.
- Kullanıcı eklenti/bağlayıcı temizliği için `/plugin` ile kapatma yapacak; `.claude/settings.json`'da 5 eklenti zaten kapalı.

## Kullanıcıdan beklenen
1. Demo v11.14'ü telefonda denemek (uygulama içi güncelleme; misafirde Karne'de "Henüz kayıt yok" görünmeli, kayıtlıda not ve ipucu doğru olmalı). Main'e geçiş onayı alındı ve yapıldı.
2. Üç telefona aynı gün v1.2 kurmak; Google girişi ve bildirimi denemek (v1.1'de kalan bildirim almaz, Google girişi yapamaz).
3. Ekran testi için test hesabını gerçek sunucuda yeniden açmak ya da GitHub'daki test hesabı ayarlarını güncellemek.
4. Eski Firebase projesini Firebase konsolunda silmek (silme kararı verildi; önce yeni projeyle bildirim ve Google girişi telefonda doğrulanmalı). Ben konsola erişemem.
5. (Kullanıcı kararı: Play Console'a şimdilik dokunulmuyor, geliştirici e-postası eski kalır.) Play Console'daki geliştirici e-postasını yeni destek adresine çevirmek; hesap silme talepleri artık o kutuya gelir.
6. Telefonda misafir olarak Ayarlar → Öneri yaz'ı denemek (main'de uygulama adresine gitmeli).
7. GitHub'daki eski repo için arşivleme ya da silme kararı (yerel klon silindi).
8. Demo'daki eski test hesabı için Supabase'de oturumları kapatmak (önceki sıfırlama linkinde oturum bilgisi paylaşıldı, token 1 saatte geçersiz olur) ve hesabı demo'da bırakmak ya da silmek.

## Sıradaki işler
0. Main çalışması 44092955273'ün sonucunu kontrol etmek. Giriş testi kırmızıysa GitHub secret'larındaki yeni hesap bilgilerini doğrulamak; geçerse Test Lab'ı çalıştırmak (main'de Test Lab henüz hiç koşmadı).
1. Main'de sonuç yeşil ve telefon denemesi tamamlanınca bekleyen işleri kapatmak.
2. Demo (destek-log) testi yeşilse telefonda deneme → onayla main. Eski `check/destek-log` dalı iş demoya alındığı için silinebilir (onaylı); remote silme 403 ile reddedildi, GitHub'dan elle silinmesi ya da silme izni gerekiyor.
3. Telefon denemesi sonrası eski FCM kayıtlarını ve eski dalları temizlemek (onaylı).
4. Küçük işler ve temizlik (ayrıntı proje belgesinde).
5. Ertelenen bulgular (düşük): family_screen (1628 satır) ve settings_screen (1092 satır) bölünmesi, family_screen testi; kalan 69 `catch (_)` hatayı kaydetmiyor (çoğu bilerek).

## Sonra kontrol edilecek: mobil uygulama yayın öncesi 20 madde (kullanıcı notu, 16 Kasım 2026)
1 Kullanıcı izinleri · 2 Gizlilik politikası · 3 Hesap silme · 4 Veri güvenliği · 5 Kayıt ve giriş · 6 E-posta doğrulama · 7 Şifre sıfırlama · 8 Onboarding · 9 Ana kullanıcı akışı · 10 Verilerin kaydı · 11 Ödeme ve abonelik · 12 Satın alımları yükleme · 13 Yükleniyor ekranları · 14 Boş ekranlar · 15 Hata ekranları · 16 İnternet bağlantısı · 17 Bildirimler · 18 Analytics ve çökme raporu · 19 Farklı cihazlar · 20 Gerçek kullanıcı testi.
Kullanıcı isteyince uygulama bu listeye göre tek tek kontrol edilir.

## Play'e çıkarken yapılacaklar
Pro planları ve fiyat (kullanıcı notu, 17 Kasım 2026): 3 aylık 153 ₺, yıllık 1453 ₺ → Play Billing ürünleri bu fiyatlarla açılır; yönetim uygulaması (`admin_app/`) ve yenileme hatırlatması (geçiş 50) bunlara göre çalışır. AAB ve Play testleri; Play App Signing; Google giriş ekranını yayınla; Pro test işlevlerini kaldır; KVKK ve avukat; Play Integrity; Play Billing; AdMob; Supabase ücretli plan; Diyanet resmî vakitleri; Test Lab yetkisini daralt; Developer Device Platform'a geçiş (en geç Nisan 2027).

## /sondurum kontrolleri (bu proje)
- Rapora bu özetteki "Durum", "Kullanıcıdan beklenen" ve "Sıradaki işler" bölümlerini de ekle (yarım kalan işler, bekleyenler). Sohbette iş yapılmamış olsa bile atlama.
- Depo: bu oturuma bağlı depo (`git remote`); dallar ve etiketler `git ls-remote --heads --tags origin`, çalışmalar ve yayınlar GitHub araçlarıyla.
- Sunucu: bağlı Supabase projesi, yalnız okuma (değişiklik listesi, hesap sayıları). Kimlik numarası yazma.
- Play listesi bu özetteki "Play'e çıkarken" bölümüdür, rapora yalnız istenince eklenir.
