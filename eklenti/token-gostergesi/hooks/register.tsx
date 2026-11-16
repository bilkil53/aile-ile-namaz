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

function note(u: SessionUsage): string {
  const left = k(u.context.window - (u.context.tokens ?? 0))
  const cats = (u.context.breakdown?.categories ?? [])
    .filter(c => c.tokens > 0 && c.name !== 'Free space')
    .sort((a, b) => b.tokens - a.tokens)
    .map(c => `${c.name}${c.isDeferred ? ' (ertelenmiş)' : ''} ${k(c.tokens)}`)
    .join(', ')
  return `\n\n[token-gostergesi eklentisi: kalan ${left}. Dağılım: ${cats}. Cevabının sonuna şu iki satırı aynen yaz: "Dağılım: ${cats}" ve "Kalan token: ${left}"]`
}

export const register: Register = on => {
  let text = ''

  on('session.start', async ($, e, next) => {
    text = line(await $.session.usage())
    $.ui.status(text)
    return next(e)
  })
  on('prompt.submit', async ($, e, next) => {
    const u = await $.session.usage({ breakdown: 'summary' })
    text = line(u)
    $.ui.status(text)
    return next({ ...e, text: e.text + note(u) })
  })
  on('turn.complete', async ($, e, next) => {
    text = line(await $.session.usage())
    $.ui.status(text)
    return next(e)
  })

  on('ui.render', { component: 'AbovePrompt' }, async ($, e, next) => {
    if (text === '') {
      text = line(await $.session.usage())
    }
    const { Box, Text } = $.ui.resolve(e)
    return (
      <Box>
        <Text dimColor>{text}</Text>
      </Box>
    )
  })
}
