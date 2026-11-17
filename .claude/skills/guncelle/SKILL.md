---
name: guncelle
description: Proje özet dosyasını (ajanlar/00-lider-ozet.md) o anki gerçek durumla günceller ve main dalına push eder. Kullanıcı /guncelle yazdığında veya "özeti güncelle" dediğinde kullan.
---

# /guncelle — proje özetini güncelle ve main'e gönder

Amaç: yeni oturumun tek kaynağı olan `ajanlar/00-lider-ozet.md` dosyasını, bu oturumda doğrulanmış durumla güncellemek. Kullanıcı bu komutu verdiğinde onay sormadan yap; push, proje kuralı gereği doğrudan main'edir.

## Adımlar

1. **Güncel dosyayı al:** `git fetch origin main` ve `git show origin/main:ajanlar/00-lider-ozet.md` ile oku. Dosya küçüktür, tamamını okumak sorun değil.

2. **Yalnızca doğrulanmış bilgiyi yaz.** Şu bölümleri güncelle:
   - `## Durum`: main ve demo sürümleri, son yayınlar, çalışan testlerin sonucu.
   - `## Kullanıcıdan beklenen`: kullanıcının yapması gereken işler.
   - `## Sıradaki işler`: bekleyen işler ve öncelik sırası.
   - `## Çalışma düzeni` ve diğer bölümler: yalnızca değişiklik gerekiyorsa değiştir.
   Durumu doğrulamak için GitHub Actions kayıtlarına ve `git ls-remote --heads --tags origin` çıktısına bak. Doğrulayamadığın bir şeyi "yapıldı" diye yazma; "doğrulanamadı" yaz.

3. **Gizlilik:** Dosyaya e-posta, kişi adı, hesap kimliği, anahtar veya gizli bilgi yazma. Depo herkese açıktır.

4. **Ayrı bir çalışma ağacında commit et:** Mevcut dalı bozmamak için `git worktree add --detach <geçici-klasör> origin/main` ile geçici bir ağaç aç, dosyayı orada düzenle, commit et ve `git push origin HEAD:refs/heads/main` ile gönder. İşi bitirince ağacı `git worktree remove --force` ile kaldır.

5. **Commit mesajı** kısa Türkçe olsun, örnek: `Özet: <tek satır durum>`. Sonuna şu satırları ekle:
   ```
   Co-Authored-By: Claude Fable 5.2 <noreply@anthropic.com>
   Claude-Session: https://claude.ai/code/session_01UZxX5PasadE7e3fBUzb2jz
   ```

6. **Push başarısızsa** `git fetch origin main` ile son hâli al, değişikliğini yeniden uygula ve tekrar dene. Force push yapma.

7. **Raporla** (kısa, Türkçe): güncellenen bölümler, commit kısa hash'i, push sonucu. Dosyanın içeriğini tekrar yazma.

## Kurallar
- Yalnızca `ajanlar/00-lider-ozet.md` dosyasını değiştir. Başka dosyaya dokunma.
- Demo veya main testi hâlâ çalışıyorsa, sonucunu "devam ediyor" olarak yaz ve kullanıcıya bildir.
- Ayrı bir iş dalına (ör. `claude/...`) push etme; özet tek kaynağı olarak main'dir.
