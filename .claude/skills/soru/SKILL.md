---
name: "soru"
description: "Kullanıcı '/soru' ile soru sorduğunda: süren işi durdurmadan, iptal etmeden soruyu yanıtla ve işe kaldığın yerden devam et."
---

# /soru — işi durdurmadan soruya cevap

Kullanıcı bir mesajı `/soru` ile başlattığında bu bir **soru**dur, yeni bir talimat değildir.

1. **Süren hiçbir işi durdurma.** Başlamış yayınları, GitHub Actions çalışmalarını, testleri, arka plandaki komutları ve alt ajanları iptal etme, yeniden başlatma, geri alma. Soru bir sorun bildirse bile önce cevapla; durdurmak gerekiyorsa bunu öner ve kullanıcının açık kararını bekle.
2. **Soruyu kısa ve net yanıtla** (Türkçe). Gerekiyorsa sadece okuma yapan kontroller yap (dosya okuma, durum sorgulama); değişiklik yapma.
3. **Yanıtı `SendUserMessage` ile gönder**, böylece kullanıcı cevabı hemen görür.
4. **Kaldığın işe devam et** ve iş bitince her zamanki gibi sonucu bildir. Sorunun cevabı iş planını değiştiriyorsa bunu öneri olarak söyle, kendiliğinden uygulama.