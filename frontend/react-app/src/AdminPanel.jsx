import { useEffect, useState } from 'react'

function formatDate(value) {
  return new Intl.DateTimeFormat(undefined, { dateStyle: 'medium' }).format(new Date(value))
}

function statusFor(link) {
  return link.deleted ? 'DELETED' : link.active ? 'ACTIVE' : 'DISABLED'
}

export default function AdminPanel({ apiRequest, user, onBack }) {
  const [summary, setSummary] = useState(null)
  const [users, setUsers] = useState([])
  const [links, setLinks] = useState([])
  const [tab, setTab] = useState('overview')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    async function loadAdminData() {
      try {
        const [summaryData, usersData, linksData] = await Promise.all([
          apiRequest('/api/admin/summary'),
          apiRequest('/api/admin/users'),
          apiRequest('/api/admin/urls'),
        ])
        setSummary(summaryData)
        setUsers(usersData)
        setLinks(linksData)
      } catch (requestError) {
        setError(requestError.message)
      } finally {
        setLoading(false)
      }
    }

    loadAdminData()
  }, [apiRequest])

  if (loading) return <section className="admin-shell"><p className="admin-empty">Loading admin console…</p></section>
  if (error) return <section className="admin-shell"><p className="message error">{error}</p><button className="admin-back" onClick={onBack}>← Back to app</button></section>

  return (
    <section className="admin-shell">
      <div className="admin-header"><div><span className="eyebrow">ADMIN CONSOLE</span><h1>See the whole picture.</h1><p>Welcome back, {user.displayName}. This view is read-only for now.</p></div><button className="admin-back" onClick={onBack}>← Back to app</button></div>
      <nav className="admin-tabs"><button className={tab === 'overview' ? 'active' : ''} onClick={() => setTab('overview')}>Overview</button><button className={tab === 'users' ? 'active' : ''} onClick={() => setTab('users')}>Users</button><button className={tab === 'links' ? 'active' : ''} onClick={() => setTab('links')}>Links</button></nav>

      {tab === 'overview' && <>
        <div className="admin-stats"><div><span>Total users</span><strong>{summary.totalUsers}</strong></div><div><span>Total links</span><strong>{summary.totalLinks}</strong></div><div><span>Active links</span><strong>{summary.activeLinks}</strong></div><div><span>Disabled</span><strong>{summary.disabledLinks}</strong></div><div><span>Deleted</span><strong>{summary.deletedLinks}</strong></div></div>
        <div className="admin-columns"><div className="admin-section"><h2>Recent users</h2>{summary.recentUsers.map((entry) => <div className="admin-list-row" key={entry.id}><div><strong>{entry.displayName}</strong><small>{entry.email}</small></div><span>{entry.linkCount} links</span></div>)}</div><div className="admin-section"><h2>Recent links</h2>{summary.recentLinks.map((entry) => <div className="admin-list-row" key={entry.code}><div><strong>{entry.code}</strong><small>{entry.ownerEmail}</small></div><span className={`admin-status ${entry.deleted ? 'deleted' : entry.active ? 'active' : 'disabled'}`}>{statusFor(entry)}</span></div>)}</div></div>
      </>}

      {tab === 'users' && <div className="admin-section"><h2>All users</h2><div className="admin-table">{users.map((entry) => <div className="admin-table-row" key={entry.id}><strong>{entry.displayName}</strong><span>{entry.email}</span><span>{entry.role}</span><span>{entry.linkCount} links</span><small>{formatDate(entry.createdAt)}</small></div>)}</div></div>}

      {tab === 'links' && <div className="admin-section"><h2>All links</h2><div className="admin-table">{links.map((entry) => <div className="admin-table-row admin-link-row" key={entry.code}><strong>{entry.code}</strong><span title={entry.longUrl}>{entry.longUrl}</span><span>{entry.ownerEmail}</span><span className={`admin-status ${entry.deleted ? 'deleted' : entry.active ? 'active' : 'disabled'}`}>{statusFor(entry)}</span><small>{formatDate(entry.createdAt)}</small></div>)}</div></div>}
    </section>
  )
}
