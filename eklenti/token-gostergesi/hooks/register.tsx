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

function note(u: SessionUsage, isFull: boolean, deltas: number[]): string {
  const total = u.context.window - (u.context.tokens ?? 0)
  const left = k(total)
  const last = deltas.length > 0 ? deltas[deltas.length - 1] : 0
  const avg = deltas.length > 0 ? Math.round(deltas.reduce((a, b) => a + b, 0) / deltas.length) : 0
  const forecast =
    avg > 0 ? ` | son tur +${k(last)}, ort. +${k(avg)}/tur, tahmini kalan: bu tur sonunda ~${k(total - avg)}, ~${Math.floor(total / avg)} tur daha` : ''
  const lastLine = `Kalan token: ${left}${forecast}`
  if (!isFull) {
    return `\n\n[token-gostergesi eklentisi. Cevabının son satırına aynen yaz: "${lastLine}"]`
  }
  const cats = (u.context.breakdown?.categories ?? [])
    .filter(c => c.tokens > 0 && c.name !== 'Free space')
    .sort((a, b) => b.tokens - a.tokens)
    .map(c => `${c.name}${c.isDeferred ? ' (ertelenmiş)' : ''} ${k(c.tokens)}`)
    .join(', ')
  return `\n\n[token-gostergesi eklentisi, 5. mesaj raporu. Cevabının sonuna şu iki satırı aynen yaz: "Dağılım: ${cats}" ve "${lastLine}"]`
}

export const register: Register = on => {
  let text = ''
  let count = 0
  let prev = -1
  const deltas: number[] = []

  on('session.start', async ($, e, next) => {
    text = line(await $.session.usage())
    $.ui.status(text)
    return next(e)
  })
  on('prompt.submit', async ($, e, next) => {
    count += 1
    const isFull = count % 5 === 0
    const u = await $.session.usage(isFull ? { breakdown: 'summary' } : undefined)
    const used = u.context.tokens ?? 0
    if (prev >= 0 && used > prev) {
      deltas.push(used - prev)
      if (deltas.length > 10) deltas.shift()
    }
    prev = used
    text = line(u)
    $.ui.status(text)
    return next({ ...e, text: e.text + note(u, isFull, deltas) })
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
