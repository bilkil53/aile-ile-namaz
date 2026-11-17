-- Hata raporları yalnızca giriş yapmış kullanıcıdan alınır; misafir (anon) yazamaz.
-- Gerekçe: anon INSERT açıkken tek bir kişi saatlik sınırı doldurup gerçek raporları engelleyebiliyordu.
-- Saatlik sınır 300 → 60 (uygulama oturum başına zaten en fazla 5 gönderir).
-- Kayıt kimliksiz kalır (user_id sütunu yok, değişiklik 33); yalnızca kim yazabileceği daralır.
revoke insert on public.crash_reports from anon;
drop policy if exists crash_reports_insert on public.crash_reports;
create policy crash_reports_insert on public.crash_reports for insert to authenticated with check (true);

create or replace function private.crash_reports_limit()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
  -- Kötüye kullanıma karşı: saatte en fazla 60 rapor (uygulama oturum başına zaten en fazla 5 gönderir)
  if (select count(*) from public.crash_reports where created_at > now() - interval '1 hour') >= 60 then
    raise exception 'rate_limited' using errcode = 'P0001';
  end if;
  new.created_at := now();
  return new;
end;
$$;
revoke all on function private.crash_reports_limit() from public, anon, authenticated;
