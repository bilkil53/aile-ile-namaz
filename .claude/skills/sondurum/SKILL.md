---
name: "sondurum"
description: "Herhangi bir sohbette o ana kadarki işin son durumunu Türkçe maddeler hâlinde verir: yapılanlar, yapılmayanlar, onay bekleyenler dahil bekleyenler. /sondurum yazıldığında kullan."
---

# /sondurum — sohbetin son durumu

Kullanıcı `/sondurum` yazdığında, **yalnızca şu an içinde bulunulan sohbette** konuşulan ve yürütülen işin güncel durumunu çıkar. Başka sohbetlerin, hafızadaki diğer projelerin ya da bu sohbette hiç konuşulmamış işlerin durumunu rapora katma. Hangi konu ya da proje olursa olsun geçerlidir (yazılım projesi, iş belgesi, araştırma, plan vb.). Süren hiçbir işi durdurma, iptal etme ya da yeniden başlatma; bu yalnızca okuma yapan bir durum raporudur.

## 1. Güncel bilgiyi topla (yalnızca okuma)
Tahmin yürütme; her maddeyi doğrula:
- **Sohbet geçmişi:** istenenler, yapılanlar, yarım kalanlar, ertelenenler, kullanıcının verdiği kararlar ve cevap bekleyen sorular. Bağlam özetlenmişse özeti de kullan.
- **Sohbette kullanılan araçlar:** hangileri kullanıldıysa onlardan güncel durumu oku. Örnekler:
  - Kod deposu: son değişiklikler, dallar, derleme/test çalışmaları, yayınlar (`git`, `gh`).
  - Sunucu/veritabanı: yalnızca okuma sorgularıyla durum (değişiklik listesi, sayılar; kişi adı yazmadan).
  - Belge / yapıt (artifact): son sürüm.
- **Zamanlanmış görevler:** `list_triggers` ile oku, ama yalnızca bu sohbette oluşturulan ya da bu sohbetin konusuyla ilgili hatırlatmaları yaz; başka sohbetlere veya genel amaçlı (ör. saat başı /compact) hatırlatmaları atla.

### Aile ile Namaz projesi için ek kontroller
Sohbet bu projeyle ilgiliyse (GitHub: bilkil53/aile-ile-namaz-, Supabase projesi tgshrnodnhmnofjhzpqp) ayrıca:
- Son çalışmalar: `gh api repos/bilkil53/aile-ile-namaz-/actions/runs?per_page=10` (dal, iş adı, durum, sonuç).
- Yayınlar: `gh api repos/bilkil53/aile-ile-namaz-/releases?per_page=6` (etiket, ön sürüm mü).
- Dallar ve etiketler: `git ls-remote --heads --tags origin`.
- Sunucu (yalnızca `select`): `list_migrations`; `profiles` tablosunda `acik_riza_at` dolu olan / toplam hesap sayısı.
- Yayındaki son kararlı ve demo sürümün numarası ve indirme bağlantısı; doğrulama biçimi (canlı geri alınan test, bağımsız inceleme, emülatör + Test Lab).
- Raporun sonuna her seferinde ayrı bir **"🏪 Google Play'e çıkarken yapılacaklar (ertelendi)"** bölümü ekle: Play paketi (AAB) derlemesinin yeniden açılması (`PLAY_BUNDLE`), Play'e özel testler, sunucudaki Pro test işlevlerinin kaldırılması, KVKK sözleşmesi ve avukat, Play Integrity, ödeme (Play Billing), Supabase ücretli plan (yedek), Diyanet resmî vakitleri, güvenlik raporu madde 07. Bunlardan biri tamamlandıysa listeden çıkar.

## 2. Raporu şu düzende yaz (Türkçe, kısa maddeler)
Boş kalan bölümü atla.

**✅ Yapılanlar**
Bitmiş işler; her madde için ne yapıldı, nasıl doğrulandı ve varsa ilgili bağlantı (dosya, sürüm, belge).

**⏳ Devam edenler**
Şu an çalışan işler ve izleme bağlantıları.

**🙋 Onayını / kararını bekleyenler**
Kullanıcının cevap vermesi gereken her şey: açık sorular, onay gerektiren adımlar, seçenek kararları. Her madde tek cümlelik soru biçiminde ve önerilen seçenekle birlikte.

**👤 Senin yapman gerekenler**
Yalnızca kullanıcının yapabileceği işler (ayar, giriş, gizli bilgi ekleme, kurulum, başkasına iletme vb.). Her biri için adım ve bağlantı.

**🔜 Sıradaki işler (bekleyen)**
Planlanmış ama başlanmamış işler; öncelik sırasıyla ve neye bağlı olduklarıyla.

**🗓️ Hatırlatmalar**
Yaklaşan zamanlanmış görevler, tarih ve kullanıcının saat dilimindeki saatleriyle.

**❌ Yapılmayacaklar / ertelenenler**
Kullanıcının şimdilik istemediğini söylediği işler ve nedeni.

## Kurallar
- Kapsam yalnızca bu sohbet: başka sohbetten, geçmiş sohbet aramasından ya da hafızadan gelen ve bu sohbette konuşulmamış hiçbir işi yazma.
- Tüm yanıt Türkçe; İngilizce terim kullanırsan yanına Türkçe karşılığını yaz.
- Doğrulayamadığın bir şeyi "yapıldı" diye yazma; "doğrulanamadı" diye belirt.
- Gizli bilgi (şifre, anahtar, e-posta, kişi adı, kullanıcı kimliği) yazma.
- Sonunda tek satırlık öneri ekle: kullanıcının şimdi yapabileceği en faydalı sonraki adım.