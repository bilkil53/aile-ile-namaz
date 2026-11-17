-- Dua metni yalnızca hazır listeden olabilir: uygulama zaten yalnızca hazır duaları gönderir
-- (boş metin = genel dua; diğerleri lib/content/family_message.dart içindeki Türkçe tebrikler).
-- Sunucu da aynısını zorlar: değiştirilmiş bir istemci aileye serbest metin gönderemez (fail-closed).
-- Liste uygulamayla aynı olmalıdır (test/family_message_test.dart denetler); yeni tebrik eklenince
-- bu işlev yeni bir geçiş dosyasıyla güncellenir.
create or replace function private.is_dua_preset(p_message text) returns boolean
language sql immutable set search_path = '' as $fn$
  select coalesce(p_message, '') = '' or p_message = any (array[
    $m$Allah kabul etsin, kıldığın namazlar kalbine nur, ömrüne huzur ve bereket olsun kardeşim.$m$,
    $m$Rabbim kıldığın namazları, ettiğin duaları dergâh-ı izzetinde en güzel şekilde makbul eylesin.$m$,
    $m$Namaz dinin direğidir. Birlikte hayırda yarışarak istikrarımızı koruduğumuz için Elhamdülillah!$m$,
    $m$Rabbim sana ve ailene afiyet, kalbine inşirah, hanene bereket ihsan eylesin.$m$,
    $m$Kaza namazlarını eritmeye devam; Rabbim azmini, gayretini ve sevabını kat kat artırsın.$m$,
    $m$Günün farzlarını tamamlayarak huzurla geceye kavuşturan Yüce Allah'a hamdolsun.$m$,
    $m$🎉 Ramazan Bayramınız mübarek olsun! Allah nice bayramlara sağlık, huzur ve birlik içinde ulaştırsın. 🤲$m$,
    $m$🐑 Kurban Bayramınız mübarek olsun! Allah nice bayramlara sağlık, huzur ve birlik içinde ulaştırsın. 🤲$m$,
    $m$🌙 Hicri yeni yılınız mübarek olsun! Rabbim yeni yılı hayırlara vesile kılsın. 🤲$m$,
    $m$🥣 Aşure Gününüz mübarek olsun! Rabbim rahmetini ve bereketini üzerimizden eksik etmesin. 🤲$m$,
    $m$🕯️ Mevlid Kandiliniz mübarek olsun! Rabbim dualarımızı kabul eylesin. 🤲$m$,
    $m$🌿 Üç Aylarınız mübarek olsun! Rabbim bu ayları hayırla idrak etmeyi nasip eylesin. 🤲$m$,
    $m$🕯️ Regaib Kandiliniz mübarek olsun! Rabbim dualarımızı kabul eylesin. 🤲$m$,
    $m$🕯️ Miraç Kandiliniz mübarek olsun! Rabbim dualarımızı kabul eylesin. 🤲$m$,
    $m$🕯️ Berat Kandiliniz mübarek olsun! Rabbim dualarımızı kabul eylesin. 🤲$m$,
    $m$🌙 Ramazan-ı Şerifiniz mübarek olsun! Rabbim oruçlarımızı ve ibadetlerimizi kabul eylesin. 🤲$m$,
    $m$✨ Kadir Geceniz mübarek olsun! Rabbim dualarımızı kabul eylesin. 🤲$m$,
    $m$🤲 Ramazan Bayramı Arefeniz mübarek olsun! Rabbim dualarımızı kabul eylesin.$m$,
    $m$🤲 Arefe gününüz mübarek olsun! Rabbim dualarımızı kabul eylesin.$m$
  ]::text[])
$fn$;
revoke all on function private.is_dua_preset(text) from public, anon, authenticated;

create or replace function private.pool_events_dua_preset() returns trigger
language plpgsql security definer set search_path = '' as $fn$
begin
  if new.event_type = 'DUA' and not private.is_dua_preset(new.message) then
    raise exception 'dua_not_preset' using errcode = 'P0001';
  end if;
  return new;
end $fn$;
revoke all on function private.pool_events_dua_preset() from public, anon, authenticated;

-- Ad sırası: rıza (0_) ve hız sınırı tetikleyicilerinden önce çalışır
drop trigger if exists pool_events_00_dua_preset on public.pool_events;
create trigger pool_events_00_dua_preset before insert on public.pool_events
  for each row execute function private.pool_events_dua_preset();
