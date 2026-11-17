---
name: ebguncelle
description: Lider özetini (ajanlar/00-lider-ozet.md) bu oturumda yapılanlarla günceller ve doğrudan main'e gönderir. Kullanıcı /ebguncelle ya da "özeti güncelle" yazdığında kullan.
---

# Özeti güncelle

1. `git fetch origin main` yap. Geçici bir worktree'de en güncel `origin/main` üzerinden çalış. Çalışma dalını kirletme.
2. `ajanlar/00-lider-ozet.md` dosyasını oku. Başka oturumların eklediği satırları koru, hiçbirini silme.
3. Bu oturumda olanları ilgili bölümlere kısa maddelerle işle:
   - **Durum:** hangi dal, ne yapıldı, testler (yeşil/kırmızı), hangi aşamada (dal / demo / main).
   - **Kullanıcıdan beklenen:** onay, telefon denemesi, konsol işleri.
   - **Sıradaki işler:** sıra numarasıyla; ertelenenleri de yaz.
   - Biten işleri listeden çıkar ya da "bitti" diye işaretle.
4. Depo herkese açık: sır, anahtar, e-posta, kişi adı, kullanıcı kimliği yazma.
5. Yalnız bu dosyayı commit'le (`docs: lider özeti güncellendi`) ve `git push origin HEAD:main` yap.
   - Reddedilirse: yeniden fetch et, en yeni main üzerine tekrar uygula, iki tarafı birleştir, tekrar gönder. Force push yapma.
6. Worktree'yi sil. Kullanıcıya Türkçe, kısa yaz: commit kısaltması, eklenen maddeler, başka oturumla birleştirme olduysa onu da söyle.

Not: Sadece `.md` değiştiği için main'e gönderim yeni sürüm (APK) çıkarmaz.
