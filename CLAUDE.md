# Aile ile Namaz

- Her oturumda önce `ajanlar/00-lider-ozet.md` dosyasını oku ve oradan devam et.
- Cevaplar Türkçe ve kısa. Onaylar anketle sorulur.
- Kod işlerinde karpathy-guidelines ve ponytail, cevaplarda caveman üslubu. Sohbet uzayınca kullanıcıya `/compact` öner.
- Danışmanı (advisor) yalnızca main yayını, gerçek veritabanına yazma, işlev yükleme, büyük mimari karar ya da kullanıcı "danış" derse çağır. Soru, açıklama ve küçük işlerde çağırma.
- Zorlanınca (aynı hata 2 kez, yaklaşım tutmuyor, emin değilsin): 1) advisor çağır (Opus). 2) Hâlâ çözülmediyse `model: "fable"` ile tek bir alt ajan aç, sorunu ve denenenleri kısaca yaz, cevabını uygula. Bu iki basamak için kullanıcıya sorma.
- Küçük ve net işler (yazım/metin düzeltme, tek dosyalık küçük değişiklik, log/dosya okuyup özet, arama) `.claude/agents/kucuk-isler.md` ajanına (Haiku) verilir; sonucu lider doğrular, commit ve push'u lider yapar. Karmaşık iş, yayın, veritabanı, workflow ve güvenlik işleri ajana verilmez.
- Token: büyük dosyayı baştan sona okuma, önce ara (grep) sonra ilgili satırları oku. Kullanıcı istemedikçe alt ajan açma, rapor/grafik/belge üretme.
- Depo herkese açık: sır, anahtar, e-posta, kişi adı, kullanıcı kimliği yazma.
- Güvenlik kontrolünde yalnız koda bakma: kullanıcı sormadan GitHub tarafını da tek listeyle öner ("sizde açık mı, şuradan kontrol edin"): Dependabot alerts/security updates, version updates, secret scanning + push protection, CodeQL, OSV taraması, main dal koruması, 2FA, Actions bildirimleri, Actions en az yetki, eylemleri SHA ile sabitleme. Bir testin/bildirimin nerede çalıştığını söylemeden önce workflow dosyasından doğrula.
