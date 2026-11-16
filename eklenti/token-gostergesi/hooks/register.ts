import type { Register, SessionUsage } from 'claude-code'

function k(n: number): string {
  return n >= 1000 ? `${Math.round(n / 100) / 10}k` : `${n}`
}

function line(u: SessionUsage): string {
  const { context, cost, rateLimits } = u
  const parts = [
    `Token ${k(context.tokens ?? 0)}/${k(context.window)}` +
      (context.percent === undefined ? '' : ` (%${Math.round(context.percent)})`),
  ]
  if (cost) parts.push(`$${cost.usd.toFixed(2)}`)
  for (const r of rateLimits) parts.push(`${r.kind} %${Math.round(r.percentUsed)}`)
  return parts.join(' · ')
}

export const register: Register = on => {
  on('session.start', async ($, e, next) => {
    $.ui.status(line(await $.session.usage()))
    return next(e)
  })
  on('prompt.submit', async ($, e, next) => {
    $.ui.status(line(await $.session.usage()))
    return next(e)
  })
  on('turn.complete', async ($, e, next) => {
    $.ui.status(line(await $.session.usage()))
    return next(e)
  })
}
