# 👨‍👩‍👧‍👦 Aile ile Namaz

<p align="center">
  <b>Aile Havuzu & İbadet Halkası · Diyanet Vakitleri · Kaza Takibi · İbadet Karnesi · Kıble Pusulası</b>
</p>

<p align="center">
  <a href="https://github.com/bilkil53/aile-ile-namaz/releases/latest/download/aile-ile-namaz.apk">
    <img src="https://img.shields.io/badge/İndir-Aile%20ile%20Namaz%20(APK)-0E5A4A?style=for-the-badge&logo=android&logoColor=white" alt="APK İndir" />
  </a>
  <a href="https://github.com/bilkil53/aile-ile-namaz/releases/latest">
    <img src="https://img.shields.io/badge/Son%20S%C3%BCr%C3%BCm-Notlar%C4%B1%20G%C3%B6r-0E5A4A?style=for-the-badge&logo=github&logoColor=white" alt="Son Sürüm" />
  </a>
  <img src="https://img.shields.io/badge/Flutter-Android-02569B?style=for-the-badge&logo=flutter&logoColor=white" alt="Flutter" />
  <img src="https://img.shields.io/badge/Supabase-PostgreSQL%20%2B%20RLS-3ECF8E?style=for-the-badge&logo=supabase&logoColor=white" alt="Supabase" />
  <img src="https://img.shields.io/badge/Vakitler-T.C.%20Diyanet%20Yöntemi-green?style=for-the-badge" alt="Diyanet" />
</p>

> *"Ailene namazı emret ve kendin de onda devamlı ol."* (Tâhâ Suresi, 132)  
> *"Hep birlikte Allah'ın ipine sımsıkı sarılın, fırkalara bölünüp parçalanmayın."* (Âl-i İmrân Suresi, 103)

---

## 📱 İndirme ve Kurulum

👉 **[📥 Aile ile Namaz — En Güncel APK'yı İndir](https://github.com/bilkil53/aile-ile-namaz/releases/latest/download/aile-ile-namaz.apk)**

🧪 **Test için:** [Namaz Demo sürümleri](https://github.com/bilkil53/aile-ile-namaz/releases?q=demo&expanded=true) — en üstteki `aile-ile-namaz-demo.apk`. Demo ayrı bir test sunucusuna bağlanır; gerçek aile verisi orada görünmez.

1. Telefonda linke dokun, APK insin.
2. İnen dosyayı aç; Android "bilinmeyen kaynaklardan yükleme" izni isterse ver.
3. Açılan **Hoş Geldiniz** penceresinde **Google ile Hızlı Giriş Yap**'a dokun ve telefondaki Gmail hesabını seç. İstersen **Giriş Yapmadan Devam Et** ile önce gezinebilirsin.

**Güncellemeler otomatik:** Uygulama her açılışta GitHub'daki son sürümü kontrol eder. "Güncelle"ye dokununca APK uygulamanın içinde iner ve Android'in kurulum ekranı açılır. Verilerin ve girişin korunur.

**Sürüm:** **v1.0.0, v1.0.1, v1.0.2 …** — son hane her yayında bir artar (yayın başlığında tarih ve saat de yazar). Büyük değişiklikte **v1.1.x / v2.x**'e geçilir. Android'in güncellemeyi tanıdığı derleme numarası = son hane + 1. 10 Kasım 2026'da numaralandırma sıfırlandı: v1.0.0'ı kurmak için eski uygulama telefondan kaldırılır.

---

## 🌟 Özellikler

### 1. 🕌 Bugün — Vakitler ve Günlük İbadet
* **Selamün aleyküm** karşılaması ve tarih. Konum her açılışta **otomatik** alınır (alınamazsa 10 sn arayla en fazla 3 deneme); konum bilgisi ve yenileme **Ayarlar**'da.
* **SIRADAKİ VAKİT** kartı: geri sayım ve günün 6 vakti (İmsak, Güneş, Öğle/Cuma, İkindi, Akşam, Yatsı).
* **5 vakit kartı** (ince çizgili simgeler): dokundukça **Kıldı → Kılmadı → Muaf → Bekliyor**. Muaf yalnızca kadın ve kız simgeli kişilerde görünür; sunucu da reisin erkek olarak belirlediği kişinin Muaf kaydını kabul etmez (boş olarak yazılır). İnternet yokken telefonda saklı son bilinen cinsiyete bakılır, bilinmiyorsa Muaf gizlenir. Aileye bildirim 15 sn, buluta kayıt 20 sn sonra gider; internet yoksa telefonda bekler, bağlantı gelince kendiliğinden gönderilir. Vakti girmemiş namaz ve vakitler yüklenmeden kartlar 🔒 kilitlidir.
* **← Dün / Bugün** geçişi: unutulan dünkü vakitler işaretlenebilir.
* **Günün Ayeti / Hadisi** (Arapça metin + Diyanet meali).
* **🎉 Bayram kartı:** Ramazan ve Kurban bayramı günlerinde ekranın en üstünde: "Bayramınız mübarek olsun", bayramın kaçıncı günü olduğu, bayramın anlamı, **Bayramlaş** (paylaşım penceresi) ve **Aileye gönder**.
* **🕯️ Kandil / mübarek gün kartı:** O gün sade bir kartla ("Bu akşam Miraç Kandili") ve kısa açıklamayla görünür; **Tebrik et** (WhatsApp vb. paylaşım) ve **Aileye gönder** (onay sorulur). Başka günlerde bu kartlar görünmez.
* **🧭 Kıble Pusulası:** İnternet gerektirmez; ivme + manyetik sensörle Kâbe yönü.

### 2. 👨‍👩‍👧‍👦 Aile Havuzu • İbadet Halkası
* Uygulamayı kuran herkes **Aile Ferdi** olarak başlar (**Bireysel Mod**). Giriş yapınca **EMB-123456** biçiminde davet kodu otomatik hazırdır.
* **Davet kodunu paylaş:** Telefonun paylaşım penceresiyle WhatsApp, Gmail, Mesajlar… (mesajda kod ve indirme linki hazır).
* **Katılma:** Aile ferdi sadece kodu girer. Aynı anda **tek istek**; istek **5 dakika** içinde onaylanmazsa sunucuda kendiliğinden düşer. İki tarafta canlı geri sayım.
* **Reis onayı:** Reis kendi ünvanını (🏡 Aile Reisi / 🧔 Baba / 🧕 Anne) ve katılanın ünvanını + simgesini seçer (Eşi, Oğlu, Kızı, Annesi, Babası, Kardeşi, Torunu, Gelini, Damadı, Dedesi, Ninesi).
* **Simgeler:** Kadın / Anne / Eş · Erkek / Baba / Eş · Erkek / Kız / Çocuk · Diğer. Kız çocuğu simgeleri anneden ayrılsın diye küçük çizilir ve fiyonk taşır (başörtülü 🧕🎀, başörtüsüz 👧🎀). Kadın veya kız simgesi seçilince **Muaf** kendiliğinden açılır; ayrıca cinsiyet sorulmaz.
* **Herkese göre hitap:** Ünvanlar reise göre saklanır, her kullanıcı diğerlerini kendine göre görür: Reis "Kızın, Oğlun, Eşin"; eşi "Eşin (Aile Reisi), Kızın"; kızı "Baban (Aile Reisi), Annen, Kardeşin". Çocuklarda (Oğlu, Kızı, Torunu) ve aynı ünvanlı birden fazla kişide bildirime ilk ad eklenir ("Kızın Ayşe namazını kıldı"); anne, baba ve eş adsız kalır.
* **1 aile = 1 havuz, en fazla 5 kişi:** Herkes yalnızca bir havuzda bulunur, reisin tek havuzu olur (sunucu kuralı). Listede "👥 X/5 kişi" yazar; havuz dolunca davet kodu yerine "Havuz dolu" görünür.
* **Liste sırası:** Önce kişinin kendisi, sonra Aile Reisi, sonra diğer fertler.
* **Bağlantıyı Teyit Et 🔄**, **BEKLEMEDE / AKTİF** rozeti, Bireysel / Aile Modu profil kartı.
* **Ortak Matris:** Ailenin 5 vakit durumu (vakit × kişi), **"Bugün Aile Başarısı: x / y Vakit (%)"**, **◄ Dünü Gör**. Yalnızca gerçekten işaretlenen vakitler görünür; boş vakitler "—".
* **🤲 Dua:** **Aileye Dua Gönder** tüm aileye gider. Bir kişinin kartındaki **Tebrik Et & Dua Gönder** yalnızca o kişiye gider ("Baban sana dua gönderdi 🤲"); kişiye özel duayı sunucu kuralıyla sadece gönderen ve alan görür. Yalnızca hazır dualar ve tebrikler gönderilir (elle metin yazılmaz).
* **Reis yetkileri:** Ünvan/simge değiştirme, reisliği devretme, üyeyi çıkarma, kodu yenileme, **Havuzu Kapat & Sıfırla**. Havuzu kapatma ve havuzdan ayrılma tek pencerede **1. onay + 2. onay** ister.
* **Aile bildirimleri (uygulama kapalıyken de):** Namaz, kaza, dua ve katılım bildirimleri Firebase Cloud Messaging ile gelir; bildirimde kişinin simgesi ve hitabı yazar. Kilit ekranında içeriğin görünüp görünmeyeceği seçilebilir.
* **Çevrimdışı:** İnternet yokken ailenin son bilinen durumu "İnternet yok" uyarısıyla gösterilir, bağlantı gelince kendiliğinden yenilenir.

### 3. 📿 Kaza Takibi (Mahremiyet Korumalı)
* **Kalan Kaza Borcu / Kılınan Kaza** kutuları ve ilerleme çubuğu.
* 6 vakit (vitir dahil): "Farz: 4 Rekat | Kılınan: X", **− / Borç / +**, **Kaza Kıldım** veya **Kaza Borcu Yok (Elhamdülillah)**.
* **Toplu Ekle:** **Yıl** (hızlı: 1, 2, 3, 5, 10, 15, 20 yıl; 1 yıl = 365 gün) veya **gün** olarak.
* **Sıfırla:** Tüm kaza kayıtlarını sıfırlar; iki kez onay ister (ikincisinde "SIFIRLA" yazılır).
* **Aile Fertleri Kaza Durumu (Detaysız):** Aile yalnızca toplamı görür; vakit ayrıntısı sadece sahibine açıktır.

### 4. 🏆 İbadet Karnesi (sadece kendi verin)
* Okul karnesi gibi: **Bu hafta / 30 gün / 3 ay** seçimi, başarı oranı ve not (**Pekiyi, İyi, Orta, Gayret et**). Muaf vakitler hesaba katılmaz.
* **Vakit vakit karne** ve **en çok aksatılan vakit** uyarısı.
* **Seri:** Şu anki ve en uzun 5 vakit serisi.
* **Aylık takvim:** Ayın her günü renkli (5 vakit / kısmen / kılınmadı / işaretsiz).
* **Kaza ilerlemesi.**
* **Manevi rozetler:** Sabah Namazı Muhafızı, 5'te 5 Sadakati, Namaz Serisi (7 / 30 / 100 / 365 gün), Sabah Serisi (7 / 21 / 40 gün), Yatsı Nöbetçisi, Cuma Sadığı, İstikrar Yıldızı, Yeniden Başlangıç, Kaza Avcısı (10 / 100 / 500 / 1000), Borçsuz Kul.
* **Mübarek gün rozetleri** (Diyanet takvimine göre, yalnızca o dönem gelince görünür): Ramazan Rozeti, Kandil Gecesi, Bayram Sabahı, Üç Aylar Yolcusu.
* Seviyeli rozetlerde **Bronz / Gümüş / Altın / Elmas** etiketi; kazanılmayanlar soluk, altında ilerleme çubuğu. Yeni rozet kazanınca tebrik bildirimi. Rozetler son 400 günün kişisel kayıtlarından hesaplanır (`lib/karne_badges.dart`).

### 5. 🔔 Bildirimler
* Her vakitten **11 dakika önce** hatırlatma; vakte özel hadis / ayet / dua ile. Hatırlatmalar **10 gün ileri** kurulur (uygulama 10 gün açılmasa da gelir); internet gerektirmez.
* **Cuma:** "Cuma Namazı 🕌" kartı ve "Hayırlı Cumalar!" hatırlatması.
* **Cuma uyarısı (erkeklere özel):** Peş peşe 3 Cuma, Cuma (Öğle) namazı "Kılmadı" işaretlenirse kişinin kendisine hadisli bir hatırlatma gelir; zincir sürdükçe her Cuma tekrar eder, metin sırayla değişir (Ebû Dâvûd/Tirmizî, Müslim). Aileye gitmez; boş, muaf ya da kılındı işaretli bir Cuma zinciri bozar (`lib/cuma_reminder.dart`).
* Her gün **12:00**'de günün ayeti / hadisi.
* **Mübarek günler:** Regaib, Miraç, Berat ve Mevlid kandilleri, Kadir Gecesi, Üç Ayların ve Ramazan'ın başlangıcı, arefeler, Ramazan ve Kurban bayramları, Hicri Yılbaşı ve Aşure için **1 gün kala akşam 19:00**'da, o günün anlamını anlatan açıklamalı bildirim. Tarihler **Diyanet hicri takviminden** alınır ve her yıl kendiliğinden ilerler.
* **Tam zamanında:** "Alarm ve hatırlatıcılar" izniyle dakikası dakikasına (izin yoksa Ayarlar'da uyarı ve İzin ver düğmesi).
* **Bildirim izinleri ekranı** (Ayarlar → Bildirimler → "Bildirim izinlerini kontrol et"; girişten sonra eksik varsa bir kez kendiliğinden açılır): bildirim izni, kesin alarm ve **pil tasarrufu muafiyeti** ✅/❌ ve tek dokunuşla ilgili Android ayarı; Samsung "Uyku moduna alınan uygulamalar" notu; **Test bildirimi gönder**; son bildirim / aile gönderim hatası.
* Vakit hatırlatması ve 12:00 bildirimi her zaman açıktır. Aile bildirimleri (namaz / kaza / dua / kilit ekranı) Ayarlar'dan ayrı ayrı açılıp kapatılır.

### 6. ⚙️ Ayarlar
* **Hesap:** Google adı ve e-postası, üyelik ve son giriş tarihi, tek satır bulut durumu ("Bulut senkronize ☁️"; bekleyen kayıt varsa sayısı, dokununca gönderir), Çıkış Yap.
* **Uygulama:** Konum (GPS ile otomatik, Yenile), sürüm (dokununca sürüm notları uygulama içinde), Güncel / Güncelleme var rozeti ve güncelleme kontrolü, **Önbelleği temizle** (vakitler ve aile özeti yeniden alınır, kayıtlar silinmez).
* **Diğer:** Uygulamayı paylaş, Öneri yaz (mail uygulaması), GitHub, Gizlilik politikası.
* **Bildirimler:** Aile bildirimleri ve alt seçenekleri; anlık bildirimde sorun varsa ayrıntı + tekrar dene.
* En altta çerçeveli **Hesabımı ve verilerimi sil** (iki aşamalı onay).

### Pro / Ücretsiz sürüm
* **7 günlük deneme:** Herkes (mevcut aileler dahil) yeni sürümü ilk açtığında başlar, kişiye özeldir; Ayarlar'da "🕒 7 günlük deneme: başlangıç – bitiş · N gün kaldı". Her şey açık.
* **Ücretsiz sürüm (8. gün):** yalnızca **Bugün** sekmesinde işaretleme yapılır. Kaza ve Karne sekmeleri kilitli (dokununca Pro penceresi), Aile Havuzu soluk görünür (yalnızca aileye katılma açık), **tüm bildirimler kapalı**, rapor ve teşvik mesajları gitmez.
* **Sunucuda Pro kilidi (`private.flags` → `pro_enforced`, açık):** Pro ya da denemede olmayan kişinin aile olayları (kıldı, kılamadı, kaza, dua, hepsi kılındı) aileye iletilmez; bu kişilere aile bildirimi gönderilmez (`push_allowed_recipients`). Eski ya da değiştirilmiş uygulamayla aşılamaz.
* **Aile Pro'su:** Pro'yu yalnızca **Aile Reisi** alır; reis Pro olunca havuzdaki herkes (en fazla 5 kişi) otomatik Pro olur. Ayrılan / çıkarılan üyenin aile Pro'su biter.
* **Aile Reisine ilet:** Reis olmayanlarda "Pro'ya geç" yerine "📩 Aile Reisine ilet" (günde 1). Reis bildirim ve istek kartı görür; **Reddet** ya da **Pro'ya geç**.
* **Ödeme:** kod girme yok. Pro yalnızca ödemeyle alınır: öncelik **hat faturasına yansıtma** (Google Play operatör faturalandırması), sonra **kart**. Ödeme uygulamanın Google Play sürümüyle açılacak. Fiyat (plan): aylık 153 ₺ · yıllık 1.553 ₺.
* **Pro'ya geçişte** eski bildirimler topluca gelmez (FCM TTL 1 saat).
* **🧪 Test Paneli** (yalnızca demo derlemesi ve test listesindeki hesaplar): başlıkta durum etiketi; Ayarlar'da denemeyi baştan başlat / bitir, Pro yap / kaldır. Sunucuda `test_restart_trial`, `test_remove_pro`, `expire_pro_test`, `redeem_pro_code` — Play'e çıkmadan önce kaldırılacak.

### 7. 🛟 Hata bildirimi
* **Hiçbir şey otomatik gönderilmez.** Uygulama kullanılırken beklenmedik bir hata olursa sebebiyle (kod hatası / bellek) bir pencere çıkar; kişi isterse **Raporu gönder** (anonim kayıt) ya da **Mail ile** der, **Kapat** derse hiçbir şey gitmez. İnternet kesintisinde pencere çıkmaz.
* Uygulama ekrandayken tamamen kapanırsa (bellek doldu, çöktü, dondu, aşırı işlemci kullanımı) bir sonraki açılışta sebebi aynı pencereyle gösterilir. Arka planda olan kapanmalar için pencere de rapor da yoktur.
* Gönderilen rapor anonimdir (sürüm, Android sürümü, bellek kullanımı, hata ve kod satırları); e-posta, kimlik, telefon, veritabanı satır değerleri (ad, dua metni) ve dosya yolları gönderilmeden önce silinir. 30 gün saklanır.

### 8. 👀 Gezinti Modu (giriş yapmadan)
* Gezinti modu **yalnızca bakmak içindir**: bütün ekranlar, Aile Havuzu dahil, görülebilir ama işaretleme ve kayıt yapılmaz.
* Vakit işaretleme, kaza değiştirme, dua gönderme gibi kayıt gerektiren işlemlerde "Google ile giriş" penceresi çıkar.
* Giriş yapınca gezinti sırasında yapılan hiçbir şey hesaba aktarılmaz (aktarılacak kayıt zaten yoktur).

### 9. 📲 Güncelleme ve görünüm
* **Uygulama simgesi:** yeşil zeminde altın minare, hilal ve E harfi (Android uyarlanabilir simge: `android-config/res`).
* Güncelleme telefonun işlemci türüne uygun küçük APK'yı uygulama içinde indirir; kurulum ekranı ayrı açılır ve uygulama tek pencerede kalır.
* İndirilen APK, GitHub'ın yayınladığı **SHA-256** değeriyle karşılaştırılır. Değer okunamazsa veya uymazsa dosya silinir ve **kurulmaz** (tarayıcıdan indirmeye de geçilmez). Android de güncellemeyi yalnızca aynı imza anahtarıyla imzalanmışsa kurar.
* Telefonun büyük yazı ayarı uygulamayı büyütmez; ekranlar ve pencereler taşmaz.

---

## 🔒 Güvenlik ve Gizlilik

| Konu | Nasıl korunuyor |
|---|---|
| Aile verisi | Supabase + satır düzeyi güvenlik (RLS): sadece havuz üyeleri görür |
| Kişiye özel dua | Sunucu kuralıyla yalnızca gönderen ve alan görür; bildirim sadece alana gider |
| Havuz kuralı | 1 aile = 1 havuz: her kişi yalnızca bir havuzda bulunur, reisin tek havuzu olur; havuz en fazla **5 kişi** (sunucuda benzersiz indeks + katılma/onay kontrolü) |
| Davet kodu | `EMB-` + 6 rastgele rakam, deneme sınırı, tek bekleyen istek, 5 dk süre (sunucu her dakika süresi dolanı düşürür), reis onayı |
| Bildirim teslimi | Kuyruk: bekliyor → gönderiliyor → gönderildi / hata. Google'a ulaşılamazsa her dakika çalışan görev 10 dk içinde en fazla 3 kez tekrar dener; aynı bildirim iki kez gitmez (atomik sahiplenme). Ulaşamayan bildirim 1 saat sonra düşer |
| Bildirim taşması | Kişi başı sınır: dua dakikada 1 / saatte 3 / günde 10; aynı vakit günde 2 |
| Hizmet sürekliliği | İnternet gerektiren özellikler (aile havuzu, bildirimler, bulut, Pro) sunucu altyapısı çalıştığı sürece kullanılabilir; namaz vakitleri ve işaretler internetsiz de çalışır (gizlilik sayfası + Play açıklaması) |
| Kod | Release derlemesi karıştırılmış (obfuscate) |
| E-posta | `profiles.email` sütununa istemci erişimi kapalı; hiçbir kullanıcı diğerinin e-postasını göremez |
| Kaza detayı | Veritabanı kuralıyla sadece sahibine açık; aile `kaza_summary` toplamını görür |
| Ünvan değiştirme | Sadece Aile Reisi (sunucu fonksiyonunda kontrol) |
| Konum | Sunucumuza gönderilmez. Vakitler için yaklaşık 1 km'ye yuvarlanmış koordinat kimliksiz olarak api.aladhan.com'a gider |
| Misafir (gezinti) | Kayıt yapılmaz; ekranlar yalnızca görüntülenir |
| Telefon yedeği | Android otomatik yedeklemesi kapalı; oturum bilgisi başka cihaza taşınamaz. Kişisel kayıtlar hesaba bağlı bulutta |
| Giriş yöntemi | Sadece Google; e-posta/şifre ile kayıt kapalı |
| Hesap silme | Uygulamadan; dolu havuzların reisliği en eski üyeye geçer |
| KVKK | Girişte iki ayrı kutu (Kurul İlke Kararı 2026/347): Aydınlatma Metni (TR/EN, `docs/privacy.html` · `privacy-en.html`) için "okudum ve anladım" beyanı (`profiles.kvkk_consent_at`) ve namaz/kaza kayıtları ile dua mesajları (din bilgisi, md. 6) için ayrı Açık Rıza Metni (`#acik-riza`; tarih `profiles.acik_riza_at`, yalnızca `record_acik_riza()` ile). Yurt dışı aktarım için standart sözleşme Play öncesi yapılacak (`docs/PLAY_STORE.md`) |
| İmza anahtarı | GitHub Secrets'ta; repoda değil |
| İmza kontrolü | Uygulama açılışta kendi sertifikasının SHA-256 parmak izini derlemede gömülen değerle (`--dart-define=EXPECTED_SIG`) karşılaştırır; uymazsa uyarı (şimdilik engellemez). Play imzası eklendiğinde iki değer olur |
| Hata kayıtları | Anonim; uygulama okuyamaz, sadece yazabilir; 30 gün sonra silinir |

📄 [Gizlilik Politikası](https://bilkil53.github.io/aile-ile-namaz/privacy.html) · 🗑 [Hesap silme](https://bilkil53.github.io/aile-ile-namaz/delete-account.html) · 🏪 [Play Store rehberi](docs/PLAY_STORE.md)

---

## 🛠️ Teknik bilgi

Mimari, veritabanı, derleme ve geliştirici notları: [`docs/TEKNIK.md`](docs/TEKNIK.md).
