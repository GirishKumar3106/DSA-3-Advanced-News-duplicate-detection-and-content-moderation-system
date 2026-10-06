const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '').replace(/\/$/, '')

async function request(path, options = {}) {
  let response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...(options.headers || {}),
      },
    })
  } catch {
    throw new Error('Cannot reach the backend. Make sure Spring Boot is running on http://localhost:8080.')
  }

  const text = await response.text()

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`
    try {
      const parsed = JSON.parse(text)
      message = parsed.message || parsed.error || message
    } catch {
      if (text) message = text
    }
    throw new Error(message)
  }

  try {
    return JSON.parse(text)
  } catch {
    return text
  }
}

export function verifyNews(claim) {
  return request(`/api/news/verify?q=${encodeURIComponent(claim)}`)
}

export function searchNews(query) {
  return request(`/api/news/search?q=${encodeURIComponent(query)}`)
}

export function searchFactChecks(query) {
  return request(`/api/news/factcheck?q=${encodeURIComponent(query)}`)
}

export function testBackend() {
  return request('/api/news/test')
}

export function addDsaNews(title, content) {
  return request('/api/dsa/news/add', {
    method: 'POST',
    body: JSON.stringify({ title, content }),
  })
}

export function getDsaNews() {
  return request('/api/dsa/news')
}

export function checkDsaDuplicate(title, content) {
  return request('/api/dsa/duplicate', {
    method: 'POST',
    body: JSON.stringify({ title, content }),
  })
}

export function moderateDsaNews(id) {
  return request(`/api/dsa/moderate/${encodeURIComponent(id)}`, {
    method: 'POST',
  })
}

export function categorizeDsaNews(id) {
  return request(`/api/dsa/categorize/${encodeURIComponent(id)}`, {
    method: 'POST',
  })
}

export function getTrendingKeywords() {
  return request('/api/dsa/trending')
}

export function getRelatedDsaNews(id) {
  return request(`/api/dsa/related/${encodeURIComponent(id)}`)
}

export { API_BASE_URL }
