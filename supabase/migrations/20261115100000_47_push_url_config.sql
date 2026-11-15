-- Bildirim işlevi adresi koda sabit yazılmaz: her sunucu kendi adresini private.config'te tutar.
-- (Demo ve gerçek sunucu ayrı; aynı geçiş dosyası ikisine de uygulanır.)
-- Adres ilk kurulumda mevcut tetikleyicinin içindeki adresten alınır; böylece her sunucu kendi adresini korur.
-- Adres yoksa bildirim gönderilmez (fail-closed), olay kaydı yine de yazılır.
create table if not exists private.config (key text primary key, value text not null);
alter table private.config enable row level security;
revoke all on private.config from public, anon, authenticated;

insert into private.config(key, value)
select 'push_url', u
  from (select substring(pg_get_functiondef('private.notify_push_family()'::regprocedure)
                         from 'https://[a-z0-9]+\.supabase\.co/functions/v1/push-family') u) s
 where u is not null
on conflict (key) do nothing;

create or replace function private.push_url() returns text
language sql stable security definer set search_path = '' as $$
  select value from private.config where key = 'push_url'
$$;
revoke all on function private.push_url() from public, anon, authenticated;

create or replace function private.notify_push_family()
returns trigger language plpgsql security definer set search_path = '' as $$
declare v_url text := private.push_url();
begin
  if v_url is null then
    raise warning 'push_url ayarı yok; bildirim gönderilmedi';
    return new;
  end if;
  perform net.http_post(
    url := v_url,
    body := jsonb_build_object('event_id', new.id),
    headers := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', private.push_hook_secret()),
    timeout_milliseconds := 5000
  );
  return new;
exception when others then
  -- Bildirim gönderilemese bile olay kaydı başarısız olmasın
  return new;
end;
$$;
revoke all on function private.notify_push_family() from public, anon, authenticated;

create or replace function private.retry_push_events()
returns void language plpgsql security definer set search_path = '' as $$
declare r record; v_url text := private.push_url();
  h jsonb := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', private.push_hook_secret());
begin
  if v_url is null then return; end if;
  update public.pool_events set push_status = 'failed'
   where push_status = 'processing' and push_claimed_at < now() - interval '2 minutes';
  for r in select id from public.pool_events
            where push_status in ('pending', 'failed') and push_attempts < 3
              and created_at between now() - interval '10 minutes' and now() - interval '30 seconds'
            order by id limit 50 loop
    perform net.http_post(url := v_url, body := jsonb_build_object('event_id', r.id),
                          headers := h, timeout_milliseconds := 5000);
  end loop;
end
$$;
revoke all on function private.retry_push_events() from public, anon, authenticated;
