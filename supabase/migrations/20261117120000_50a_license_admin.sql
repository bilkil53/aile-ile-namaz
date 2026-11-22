-- 50a: Pro plan kolonları ve yönetim uygulaması işlevleri. Gerçek sunucuya 18 Kasım 2026da uygulandı (adı 50a_license_admin_only).
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
  if p_plan is null or p_plan not in ('3_ay', '12_ay') then raise exception 'gecersiz_plan'; end if;
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
