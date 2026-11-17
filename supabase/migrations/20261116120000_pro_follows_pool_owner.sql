-- Havuzdaki üyenin Pro durumu = reisin durumu (deneme veya satın alma); katılma tarihi önemsiz.
-- Reis ücretsize düşerse üye de düşer. Havuzsuz kişi kendi durumunu kullanır.
create or replace function private.pro_status(p_uid uuid)
returns table (eff_until timestamptz, source text, trial_until timestamptz, paid_until timestamptz, family_until timestamptz)
language sql stable security definer set search_path to '' as $$
  with me as (select pro_until, pro_paid_until from public.profiles where id = p_uid),
  own as (
    select o.pro_until as t, o.pro_paid_until as pd
      from public.pool_members m
      join public.pools p on p.id = m.pool_id and p.owner_id <> p_uid
      join public.profiles o on o.id = p.owner_id
     where m.user_id = p_uid
     limit 1),
  x as (select (select count(*) from own) as inpool)
  select
    case when x.inpool > 0 then greatest((select t from own), (select pd from own))
         else greatest(me.pro_until, me.pro_paid_until) end,
    case
      when x.inpool > 0 then
        case when greatest((select t from own), (select pd from own)) > now() then 'family' else null end
      when coalesce(me.pro_paid_until, '-infinity') > now() then 'paid'
      when coalesce(me.pro_until, '-infinity') > now() then 'trial'
      else null end,
    me.pro_until, me.pro_paid_until,
    case when x.inpool > 0 then greatest((select t from own), (select pd from own)) end
  from me, x
$$;
