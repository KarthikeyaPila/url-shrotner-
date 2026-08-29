import { useEffect, useState } from 'react'

const API_BASE = 'http://localhost:8080'

async function readResponse(response) {
  if (response.status === 204) return null
  const data = await response.json().catch(() => ({}))
  if (!response.ok) throw new Error(data.error || 'Something went wrong.')
  return data
}

export default function App() {
  const [csrfToken, setCsrfToken] = useState('')
  const [user, setUser] = useState(null)
  const [authReady, setAuthReady] = useState(false)
  const [showAuthOverlay, setShowAuthOverlay] = useState(false)
  const [authMode, setAuthMode] = useState('login')
  const [passwordVisible, setPasswordVisible] = useState(false)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [authError, setAuthError] = useState('')
  const [authLoading, setAuthLoading] = useState(false)
  const [longUrl, setLongUrl] = useState('')
  const [alias, setAlias] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const [copied, setCopied] = useState(false)

  useEffect(() => {
    async function restoreSession() {
      try {
        const csrfResponse = await fetch(`${API_BASE}/api/auth/csrf`, { credentials: 'include' })
        const token = await csrfResponse.text()
        setCsrfToken(token)

        const sessionResponse = await fetch(`${API_BASE}/api/auth/me`, { credentials: 'include' })
        if (sessionResponse.ok) {
          setUser(await sessionResponse.json())
          setShowAuthOverlay(false)
        } else {
          setShowAuthOverlay(true)
        }
      } catch {
        // Show the optional login prompt even when the API is offline.
        setShowAuthOverlay(true)
      } finally {
        setAuthReady(true)
      }
    }

    restoreSession()
  }, [])

  async function apiRequest(path, options = {}) {
    const headers = new Headers(options.headers)
    if (options.body) headers.set('Content-Type', 'application/json')
    if (csrfToken) headers.set('X-XSRF-TOKEN', csrfToken)

    const response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
      credentials: 'include',
    })
    return readResponse(response)
  }

  async function authenticate(event) {
    event.preventDefault()
    setAuthError('')
    setAuthLoading(true)

    try {
      if (authMode === 'register') {
        await apiRequest('/api/auth/register', {
          method: 'POST',
          body: JSON.stringify({ email, password, displayName }),
        })
      }

      const loggedInUser = await apiRequest('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      setUser(loggedInUser)
      setPassword('')
      setShowAuthOverlay(false)
    } catch (requestError) {
      setAuthError(requestError.message === 'Failed to fetch'
        ? 'The API is not running. Start the Spring Boot backend and try again.'
        : requestError.message)
    } finally {
      setAuthLoading(false)
    }
  }

  async function logout() {
    try {
      await apiRequest('/api/auth/logout', { method: 'POST' })
    } finally {
      setUser(null)
      setShowAuthOverlay(true)
    }
  }

  async function shorten(event) {
    event.preventDefault()
    setError('')
    setResult(null)
    setCopied(false)
    setLoading(true)

    try {
      const data = await apiRequest('/api/urls', {
        method: 'POST',
        body: JSON.stringify({ longUrl, alias: alias || null }),
      })
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

  function switchAuthMode(mode) {
    setAuthMode(mode)
    setAuthError('')
  }

  function openAuthOverlay(mode = 'login') {
    setAuthMode(mode)
    setAuthError('')
    setShowAuthOverlay(true)
  }

  return (
    <main className="page-shell">
      <nav className="nav">
        <span className="brand"><span className="brand-mark">↗</span> shortly</span>
        {authReady && (user
          ? <span className="account-area">Hi, {user.displayName} <button className="text-button" onClick={logout}>Log out</button></span>
          : <button className="text-button" onClick={() => openAuthOverlay()}>Log in</button>)}
      </nav>

      <section className="hero">
        <div className="eyebrow">URL SHORTENER · MVP</div>
        <h1>Make the web<br /><em>a little shorter.</em></h1>
        <p className="intro">Turn long, unwieldy links into clean little shortcuts. No accounts, no expiry dates—just a link that works.</p>

        <form onSubmit={shorten} className="shorten-card">
          <label htmlFor="long-url">Paste your long URL</label>
          <div className="input-row"><input id="long-url" type="url" required value={longUrl} onChange={(event) => setLongUrl(event.target.value)} placeholder="https://a-very-long-link.com/..." /><button disabled={loading || !authReady}>{loading ? 'Shortening…' : 'Shorten link'} <span>→</span></button></div>
          <label htmlFor="alias" className="alias-label">Custom alias <small>optional</small></label>
          <div className="alias-row"><span>short.ly/</span><input id="alias" pattern="[A-Za-z0-9_-]+" value={alias} onChange={(event) => setAlias(event.target.value)} placeholder="my-link" /></div>
          {error && <p className="message error">{error}</p>}
        </form>

        {result && <section className="result-card"><div><span className="result-label">YOUR SHORT LINK</span><a href={result.shortUrl}>{result.shortUrl}</a></div><button className="copy-button" onClick={copyUrl}>{copied ? 'Copied!' : 'Copy link'}</button></section>}

      </section>

      {authReady && showAuthOverlay && !user && <div className="auth-overlay" role="presentation">
        <section className="auth-card auth-modal" role="dialog" aria-modal="true" aria-labelledby="auth-title">
          <button className="modal-close" type="button" aria-label="Continue without an account" onClick={() => setShowAuthOverlay(false)}>×</button>
          <div className="auth-heading"><span className="eyebrow">OPTIONAL ACCOUNT</span><h2 id="auth-title">{authMode === 'login' ? 'Keep track of your links.' : 'Create your account.'}</h2><p>{authMode === 'login' ? 'Log in to prepare for link history and other personal features.' : 'Sign up now and your future links can belong to you.'}</p></div>
          <div className="auth-tabs"><button className={authMode === 'login' ? 'active' : ''} onClick={() => switchAuthMode('login')} type="button">Log in</button><button className={authMode === 'register' ? 'active' : ''} onClick={() => switchAuthMode('register')} type="button">Register</button></div>
          <form onSubmit={authenticate}>
            {authMode === 'register' && <label htmlFor="display-name">Display name<input id="display-name" required value={displayName} onChange={(event) => setDisplayName(event.target.value)} placeholder="Your name" /></label>}
            <label htmlFor="email">Email address<input id="email" type="email" required value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></label>
            <label htmlFor="password">Password
              <span className="password-input"><input id="password" type={passwordVisible ? 'text' : 'password'} minLength="8" required value={password} onChange={(event) => setPassword(event.target.value)} placeholder="At least 8 characters" /><button type="button" onClick={() => setPasswordVisible(!passwordVisible)}>{passwordVisible ? 'Hide' : 'Show'}</button></span>
            </label>
            {authError && <p className="message error">{authError}</p>}
            <button className="auth-submit" disabled={authLoading || !csrfToken}>{authLoading ? 'Please wait…' : authMode === 'login' ? 'Log in' : 'Create account'}</button>
          </form>
          <button className="no-thanks" type="button" onClick={() => setShowAuthOverlay(false)}>No thanks, continue without an account</button>
        </section>
      </div>}

      <footer><span>Built while learning Java, HTTP & databases.</span><span>More features coming soon ✦</span></footer>
    </main>
  )
}
