# Aile ile Namaz

- Her oturumda önce `ajanlar/00-lider-ozet.md` dosyasını oku ve oradan devam et.
- Cevaplar Türkçe ve kısa. Onaylar anketle sorulur.
- Skill çağrılınca (sohbet boş olsa da) skill dosyasının tüm adımlarını ve bu projenin o skill için kurallarını (ör. özetteki "/sondurum kontrolleri") eksiksiz uygula; adım atlama, kısaltma.
- Kod işlerinde karpathy-guidelines ve ponytail, cevaplarda caveman üslubu. Sohbet uzayınca kullanıcıya `/compact` öner.
- Danışmanı (advisor) yalnızca main yayını, gerçek veritabanına yazma, işlev yükleme, büyük mimari karar ya da kullanıcı "danış" derse çağır. Soru, açıklama ve küçük işlerde çağırma.
- Zorlanınca (aynı hata 2 kez, yaklaşım tutmuyor, emin değilsin): 1) advisor çağır (danışman = Fable, `model: "fable"`). 2) Hâlâ çözülmediyse kullanıcıya sor. İlk basamak için kullanıcıya sorma.
- Testlerde (CI, birim, güvenlik, ekran, Test Lab) aynı hata 2 kez çıkarsa kendin denemeyi bırak, tüm işi (log okuma, neden bulma, düzeltme, yeniden çalıştırma, doğrulama) danışana (Fable alt ajan) ver; sana yalnız özet dönsün. Main'e alma, gerçek veritabanına yazma ve secret/ayar değişikliği yine kullanıcı onayıyla yapılır.
- Küçük ve net işler (yazım/metin düzeltme, tek dosyalık küçük değişiklik, log/dosya okuyup özet, arama) `.claude/agents/kucuk-isler.md` ajanına (Haiku) verilir; sonucu lider doğrular, commit ve push'u lider yapar. Karmaşık iş, yayın, veritabanı, workflow ve güvenlik işleri ajana verilmez.
- Teste/kontrole (CI, demo, check dalı) her gönderişte kullanıcıya takip bağlantısı ver: çalışmanın Actions bağlantısı (`https://github.com/bilkil53/aile-ile-namaz/actions/runs/<id>`); kimlik henüz yoksa dalın çalışma listesi (`.../actions?query=branch:<dal>`).
- Token: büyük dosyayı baştan sona okuma, önce ara (grep) sonra ilgili satırları oku. Kullanıcı istemedikçe alt ajan açma, rapor/grafik/belge üretme.
- Depo herkese açık: sır, anahtar, e-posta, kişi adı, kullanıcı kimliği yazma.
- Güvenlik kontrolünde yalnız koda bakma: kullanıcı sormadan GitHub tarafını da tek listeyle öner ("sizde açık mı, şuradan kontrol edin"): Dependabot alerts/security updates, version updates, secret scanning + push protection, CodeQL, OSV taraması, main dal koruması, 2FA, Actions bildirimleri, Actions en az yetki, eylemleri SHA ile sabitleme. Bir testin/bildirimin nerede çalıştığını söylemeden önce workflow dosyasından doğrula.
- Bir test/çalışma (GitHub Actions, Test Lab, zamanlanmış görev) başlatınca, yeniden çalıştırınca ya da sonucuna bakınca kullanıcıya takip bağlantısını her seferinde ver (ör. `https://github.com/<sahip>/<depo>/actions/runs/<id>`). (kullanıcı kararı, 18 Kasım 2026)
