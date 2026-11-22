-- 50b: bitişten 7 gün önce yenileme hatırlatması. Şart: push-family yeni sürümü (renewal_user) yüklü olmalı. Gerçek sunucuda HENÜZ YOK.
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
