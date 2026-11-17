-- 50: Pro plan (3 aylık / yıllık), yönetim uygulaması işlevleri, bitişten 7 gün önce yenileme hatırlatması.
-- Fiyat notu (Play Billing ürünleri açılınca bağlanır; burada saklanmaz): 3 aylık 153 TL, yıllık 1453 TL.

alter table public.profiles add column if not exists pro_plan text check (pro_plan in ('3_ay', '12_ay'));
alter table public.profiles add column if not exists pro_renewal_notified_until timestamptz;

-- Yönetici listesi: kimse API'den okuyamaz/yazamaz; kayıt SQL ile eklenir.
create table if not exists private.admins (user_id uuid primary key references auth.users(id) on delete cascade);
alter table private.admins enable row level security;
revoke all on private.admins from public, anon, authenticated;

create or replace function private.is_admin() returns boolean
language sql stable security definer set search_path = '' as $$
  select exists (select 1 from private.admins where user_id = auth.uid());
$$;
revoke all on function private.is_admin() from public, anon, authenticated;

-- Lisans listesi (yalnız yönetici): kimde hangi lisans var.
create or replace function public.admin_list_licenses()
returns table (user_id uuid, email text, display_name text, created_at timestamptz,
               plan text, trial_until timestamptz, paid_until timestamptz, status text)
language plpgsql stable security definer set search_path = '' as $$
begin
  if not private.is_admin() then raise exception 'yetkisiz' using errcode = '42501'; end if;
  return query
    select p.id, u.email::text, p.display_name, p.created_at, p.pro_plan, p.pro_until, p.pro_paid_until,
           case when coalesce(p.pro_paid_until, '-infinity') > now() then 'pro'
                when coalesce(p.pro_until, '-infinity') > now() then 'deneme'
                else 'ucretsiz' end
      from public.profiles p join auth.users u on u.id = p.id
     order by p.pro_paid_until desc nulls last, p.created_at desc;
end $$;

-- Lisans ver / uzat (yalnız yönetici). Mevcut bitiş ileride ise ondan sonrası eklenir.
create or replace function public.admin_set_license(p_uid uuid, p_plan text)
returns timestamptz language plpgsql security definer set search_path = '' as $$
declare v timestamptz;
begin
  if not private.is_admin() then raise exception 'yetkisiz' using errcode = '42501'; end if;
  if p_plan not in ('3_ay', '12_ay') then raise exception 'gecersiz_plan'; end if;
  update public.profiles
     set pro_plan = p_plan,
         pro_paid_until = greatest(coalesce(pro_paid_until, now()), now())
                          + case p_plan when '3_ay' then interval '3 months' else interval '12 months' end
   where id = p_uid returning pro_paid_until into v;
  if v is null then raise exception 'kullanici_yok'; end if;
  return v;
end $$;

-- Lisansı kaldır (yalnız yönetici)
create or replace function public.admin_revoke_license(p_uid uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
  if not private.is_admin() then raise exception 'yetkisiz' using errcode = '42501'; end if;
  update public.profiles set pro_paid_until = null, pro_plan = null, pro_renewal_notified_until = null where id = p_uid;
end $$;

revoke all on function public.admin_list_licenses() from public, anon;
revoke all on function public.admin_set_license(uuid, text) from public, anon;
revoke all on function public.admin_revoke_license(uuid) from public, anon;
grant execute on function public.admin_list_licenses() to authenticated;
grant execute on function public.admin_set_license(uuid, text) to authenticated;
grant execute on function public.admin_revoke_license(uuid) to authenticated;

-- Yenileme hatırlatması: bitişine 7 gün ya da daha az kalan ücretli Pro'lar için bildirim işlevi çağrılır.
-- İşaret (pro_renewal_notified_until) bildirim ulaşınca işlev tarafından yazılır; ulaşmazsa ertesi gün yeniden denenir.
-- Yenilenince pro_paid_until değişir, işaret eşleşmez ve sonraki dönem için yeniden hatırlatılır.
create or replace function private.queue_pro_renewals()
returns void language plpgsql security definer set search_path = '' as $$
declare r record; v_url text := private.push_url();
  h jsonb := jsonb_build_object('Content-Type', 'application/json', 'x-push-secret', private.push_hook_secret());
begin
  if v_url is null then return; end if;
  for r in select id, pro_paid_until from public.profiles
            where pro_paid_until > now() and pro_paid_until <= now() + interval '7 days'
              and pro_renewal_notified_until is distinct from pro_paid_until
            limit 200 loop
    perform net.http_post(url := v_url,
      body := jsonb_build_object('renewal_user', r.id, 'until', r.pro_paid_until),
      headers := h, timeout_milliseconds := 5000);
  end loop;
end $$;
revoke all on function private.queue_pro_renewals() from public, anon, authenticated;

select cron.unschedule(jobid) from cron.job where jobname = 'aile-pro-renewal';
select cron.schedule('aile-pro-renewal', '0 6 * * *', $job$ select private.queue_pro_renewals(); $job$);
