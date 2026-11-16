# Lider özeti — Code sekmesi buradan devam eder

> Depo herkese açık. Burada sır, kimlik numarası, hesap adı ve güvenlik açığı ayrıntısı yazılmaz.
> Tam özet claude.ai'deki "Aile ile Namaz" projesinin belgesindedir (ajanlar/00-lider-ozet.md).

## Çalışma düzeni
- Tek ajan (lider) çalışır. Bağımsız inceleme gerekirse ayrı ajan kullanılır.
- Onaylar anketle sorulur. Cevaplar Türkçe ve kısa. Her raporda Play listesi ve token grafiği (explain-usage).
- Akış: iş dalı (check/...) → analiz + test → bağımsız inceleme → demo → kullanıcı telefonda bakar → onayla main. Demo ve main aynı anda yayınlanmaz.
- Gerçek veritabanına yazma, işlev yükleme, silme: ayrı onay ve önce geri alınan deneme.
- Sohbet uzayınca: kullanıcı `/compact` yazar (aynı sohbette devam). Yeni sohbet gerekirse önce "özeti güncelle", sonra yeni oturumda `/yenisohbet`.

## Durum (16 Kasım 2026)
- Demo v11.3 yayında (çam yeşili zemin), tüm testler geçti, tasarım onaylandı.
- main v1.0 henüz çıkmadı; demodaki sunucu değişiklikleri gerçek ortama taşınacak.

## Kullanıcıdan beklenen
1. İkinci telefonla demoya girip "girdim" yazmak.
2. Telefonlardaki eski uygulama sürümünü söylemek.
3. Web istemci sırrının yenilenip yenilenmediğini söylemek.

## Sıradaki işler
1. Demoda iki telefonla bildirim denemesi.
2. main v1.0 (onaylı, adım adım, üç telefon aynı gün).
3. Küçük işler ve temizlik (ayrıntı proje belgesinde).
4. Code sekmesinde sabit token göstergesi (mod) kur.

## Play'e çıkarken yapılacaklar
AAB ve Play testleri; Play App Signing; Google giriş ekranını yayınla; Pro test işlevlerini kaldır; KVKK ve avukat; Play Integrity; Play Billing; AdMob; Supabase ücretli plan; Diyanet resmî vakitleri; Test Lab yetkisini daralt; Developer Device Platform'a geçiş (en geç Nisan 2027).
