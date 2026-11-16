# Lider özeti — Code sekmesi buradan devam eder

> Depo herkese açık. Burada sır, kimlik numarası, hesap adı ve güvenlik açığı ayrıntısı yazılmaz.
> Tam özet claude.ai'deki "Aile ile Namaz" projesinin belgesindedir (ajanlar/00-lider-ozet.md).

## Çalışma düzeni
- Tek ajan (lider) çalışır. Bağımsız inceleme gerekirse ayrı ajan kullanılır.
- Onaylar anketle sorulur. Cevaplar Türkçe ve kısa. Play listesi ve token grafiği yalnız istenince.
- Akış (16 Kasım 2026'da güncellendi): iş dalı (check/...) → önce demo yayını → testler (analiz, CI, bağımsız inceleme) ile kullanıcının telefonda denemesi paralel yürür → onayla main. Demo ve main aynı anda yayınlanmaz.
- Testler kırmızı çıkarsa demo düzeltilip yeniden yayınlanır; main'e yalnız testler yeşil + kullanıcı onayıyla geçilir.
- Gerçek veritabanına yazma, işlev yükleme, silme: ayrı onay ve önce geri alınan deneme.
- Sohbet uzayınca: kullanıcı `/compact` yazar (aynı sohbette devam). Yeni oturum özeti CLAUDE.md sayesinde kendiliğinden okur. "Özeti güncelle" denince bu dosya güncellenip push edilir.

## Durum (16 Kasım 2026)
- Demo v11.3 yayında, iki telefon v11.3. Bildirim denemesi geçti (iki telefona 2 sn).
- Zemin çam yeşili çok koyu bulundu: `check/zemin-acik` dalında bir ton açık (#1A7561), demoya çıkmadı, derleme/test yapılmadı.
- main v1.0 henüz çıkmadı; demodaki sunucu değişiklikleri gerçek ortama taşınacak.

## Kullanıcıdan beklenen
1. Web istemci sırrının yenilenip yenilenmediğini söylemek.
2. Açık zemin rengini demoda telefonda görüp onaylamak.

## Sıradaki işler
1. `check/zemin-acik` demoya (v11.4) + paralel testler.
2. main v1.0 (onaylı, adım adım, üç telefon aynı gün).
3. Küçük işler ve temizlik (ayrıntı proje belgesinde).
4. Code sekmesinde sabit token göstergesi (mod) kur.

## Play'e çıkarken yapılacaklar
AAB ve Play testleri; Play App Signing; Google giriş ekranını yayınla; Pro test işlevlerini kaldır; KVKK ve avukat; Play Integrity; Play Billing; AdMob; Supabase ücretli plan; Diyanet resmî vakitleri; Test Lab yetkisini daralt; Developer Device Platform'a geçiş (en geç Nisan 2027).

## /sondurum kontrolleri (bu proje)
- Rapora bu özetteki "Durum", "Kullanıcıdan beklenen" ve "Sıradaki işler" bölümlerini de ekle (yarım kalan işler, bekleyenler). Sohbette iş yapılmamış olsa bile atlama.
- Depo: bu oturuma bağlı depo (`git remote`); dallar ve etiketler `git ls-remote --heads --tags origin`, çalışmalar ve yayınlar GitHub araçlarıyla.
- Sunucu: bağlı Supabase projesi, yalnız okuma (değişiklik listesi, hesap sayıları). Kimlik numarası yazma.
- Play listesi bu özetteki "Play'e çıkarken" bölümüdür, rapora yalnız istenince eklenir.
