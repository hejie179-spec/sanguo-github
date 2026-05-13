import { searchGlobal } from '../api/search'

function safeStr(v) {
  return String(v ?? '').trim()
}

export async function resolveResourceTab(keyword) {
  const kw = safeStr(keyword)
  if (!kw) return null
  try {
    const results = await searchGlobal(kw, 10)
    if (!results || results.length === 0) return null
    const first = results[0]
    return first?.type || null
  } catch {
    return null
  }
}

