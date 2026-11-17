# Yönetim uygulaması

Yalnız yönetici: kimde hangi lisans var (e-posta, plan, bitiş), lisans ver/uzat/kaldır.

Çalıştırma (adres ve anahtar koda yazılmaz):

    flutter create --platforms=android,web .   # ilk kez
    flutter run --dart-define=SUPABASE_URL=... --dart-define=SUPABASE_ANON_KEY=...

Yönetici olmak için: giriş yapılan hesabın kimliği SQL ile `private.admins` tablosuna eklenir.
Yönetici değilse liste "yetkisiz" hatası verir; sunucu işlevleri (50 numaralı geçiş) yetkiyi kendisi denetler.
Planlar: 3 aylık, yıllık (fiyat notu: 153 TL / 1453 TL, Play Billing bağlanınca).
