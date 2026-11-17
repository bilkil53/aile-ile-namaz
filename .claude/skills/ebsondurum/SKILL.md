---
name: ebsondurum
description: "Herhangi bir sohbette o ana kadarki işin son durumunu Türkçe maddeler hâlinde verir: yapılanlar, yapılmayanlar, onay bekleyenler dahil bekleyenler. /ebsondurum yazıldığında kullan."
---

# /ebsondurum — sohbetin son durumu

Kullanıcı `/ebsondurum` yazdığında, **yalnızca şu an içinde bulunulan sohbette** konuşulan ve yürütülen işin güncel durumunu çıkar. Başka sohbetlerin, hafızadaki diğer projelerin ya da bu sohbette hiç konuşulmamış işlerin durumunu rapora katma. Hangi konu ya da proje olursa olsun geçerlidir (yazılım projesi, iş belgesi, araştırma, plan vb.). Süren hiçbir işi durdurma, iptal etme ya da yeniden başlatma; bu yalnızca okuma yapan bir durum raporudur.

## 1. Güncel bilgiyi topla (yalnızca okuma)
Tahmin yürütme; her maddeyi doğrula:
- **Sohbet geçmişi:** istenenler, yapılanlar, yarım kalanlar, ertelenenler, kullanıcının verdiği kararlar ve cevap bekleyen sorular. Bağlam özetlenmişse özeti de kullan.
- **Sohbette kullanılan araçlar:** hangileri kullanıldıysa onlardan güncel durumu oku. Örnekler:
  - Kod deposu: son değişiklikler, dallar, derleme/test çalışmaları, yayınlar (`git`, `gh`).
  - Sunucu/veritabanı: yalnızca okuma sorgularıyla durum (değişiklik listesi, sayılar; kişi adı yazmadan).
  - Belge / yapıt (artifact): son sürüm.
- **Zamanlanmış görevler:** `list_triggers` ile oku, ama yalnızca bu sohbette oluşturulan ya da bu sohbetin konusuyla ilgili hatırlatmaları yaz; başka sohbetlere veya genel amaçlı (ör. saat başı /compact) hatırlatmaları atla.

### Proje kontrolleri
Projenin tek asıl özeti bağlı GitHub deposundaki `ajanlar/00-lider-ozet.md` dosyasıdır. Kod oturumunda proje klasöründen, sohbette (chat) GitHub aracıyla bağlı depodan (ana dal, main) oku; claude.ai projesindeki eski kopyaya bakma. Dosya varsa:
- Oradaki "/ebsondurum kontrolleri" bölümüne uy (depo: bağlı depo; sunucu: bağlı proje, yalnız okuma).
- "Durum", "Kullanıcıdan beklenen" ve "Sıradaki işler" bölümlerini de rapora ekle (yarım kalan işler, bekleyenler). Bu bölümler "yalnızca bu sohbet" kapsamının istisnasıdır; sohbette iş yapılmamış olsa bile atlama.
Dosyaya ulaşamazsan bunu tek cümleyle söyle ve devam et.

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