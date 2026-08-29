import { useState } from 'react'

const API_URL = 'http://localhost:8080/api/urls'

export default function App() {
  const [longUrl, setLongUrl] = useState('')
  const [alias, setAlias] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)

  async function shorten(event) {
    event.preventDefault()
    setError('')
    setResult(null)
    setCopied(false)
    setLoading(true)

    try {
      const response = await fetch(API_URL, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ longUrl, alias: alias || null }),
      })
      const data = await response.json()
      if (!response.ok) throw new Error(data.error || 'Could not shorten that URL.')
      setResult(data)
    } catch (requestError) {
      setError(requestError.message === 'Failed to fetch'
        ? 'The API is not running. Start the Spring Boot backend and try again.'
        : requestError.message)
    } finally {
      setLoading(false)
    }
  }

  async function copyUrl() {
    if (!result) return
    await navigator.clipboard.writeText(result.shortUrl)
    setCopied(true)
  }

  return (
    <main className="page-shell">
      <nav className="nav"><span className="brand"><span className="brand-mark">↗</span> shortly</span><span className="nav-note">your tiny corner of the internet</span></nav>
      <section className="hero">
        <div className="eyebrow">URL SHORTENER · MVP</div>
        <h1>Make the web<br /><em>a little shorter.</em></h1>
        <p className="intro">Turn long, unwieldy links into clean little shortcuts. No accounts, no expiry dates—just a link that works.</p>
        <form onSubmit={shorten} className="shorten-card">
          <label htmlFor="long-url">Paste your long URL</label>
          <div className="input-row"><input id="long-url" type="url" required value={longUrl} onChange={(event) => setLongUrl(event.target.value)} placeholder="https://a-very-long-link.com/..." /><button disabled={loading}>{loading ? 'Shortening…' : 'Shorten link'} <span>→</span></button></div>
          <label htmlFor="alias" className="alias-label">Custom alias <small>optional</small></label>
          <div className="alias-row"><span>short.ly/</span><input id="alias" pattern="[A-Za-z0-9_-]+" value={alias} onChange={(event) => setAlias(event.target.value)} placeholder="my-link" /></div>
          {error && <p className="message error">{error}</p>}
        </form>
        {result && <section className="result-card"><div><span className="result-label">YOUR SHORT LINK</span><a href={result.shortUrl}>{result.shortUrl}</a></div><button className="copy-button" onClick={copyUrl}>{copied ? 'Copied!' : 'Copy link'}</button></section>}
      </section>
      <footer><span>Built while learning Java, HTTP & databases.</span><span>More features coming soon ✦</span></footer>
    </main>
  )
}
