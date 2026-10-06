import { useEffect, useMemo, useState } from 'react'
import {
  API_BASE_URL,
  addDsaNews,
  categorizeDsaNews,
  checkDsaDuplicate,
  getDsaNews,
  getRelatedDsaNews,
  getTrendingKeywords,
  moderateDsaNews,
  searchFactChecks,
  searchNews,
  testBackend,
  verifyNews,
} from './api'

const NAV_ITEMS = [
  { id: 'overview', label: 'Overview', icon: '⌂' },
  { id: 'verify', label: 'Verify News', icon: '✓' },
  { id: 'search', label: 'Search News', icon: '⌕' },
  { id: 'dsa', label: 'DSA Workspace', icon: '⌘' },
  { id: 'factcheck', label: 'Fact Checks', icon: '◈' },
]

const DEMO_CLAIMS = [
  'India defeated Australia in cricket',
  'drinking hot water cures COVID-19',
  'Garlic can prevent coronavirus',
]

const TABS = [
  ['add', 'Add / Manage'],
  ['duplicate', 'Duplicate'],
  ['moderate', 'Moderation'],
  ['categorize', 'Categorize'],
  ['trending', 'Trending'],
  ['related', 'Related News'],
]

function App() {
  const [activePage, setActivePage] = useState('overview')
  const [backendStatus, setBackendStatus] = useState('checking')
  const [lastResult, setLastResult] = useState(null)
  const [history, setHistory] = useState(() => {
    try {
      return JSON.parse(localStorage.getItem('advanced-news-history')) || []
    } catch {
      return []
    }
  })

  useEffect(() => {
    testBackend()
      .then(() => setBackendStatus('online'))
      .catch(() => setBackendStatus('offline'))
  }, [])

  useEffect(() => {
    localStorage.setItem('advanced-news-history', JSON.stringify(history.slice(0, 8)))
  }, [history])

  const handleVerification = (result) => {
    setLastResult(result)
    setHistory((current) => {
      const entry = {
        claim: result.claim,
        verdict: result.verdict,
        confidence: result.confidence,
        timestamp: new Date().toISOString(),
      }
      return [entry, ...current.filter((item) => item.claim !== result.claim)].slice(0, 8)
    })
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark">AN</div>
          <div>
            <div className="brand-title">Advanced News</div>
            <div className="brand-subtitle">Intelligence System</div>
          </div>
        </div>
        <div className="side-label">WORKSPACE</div>
        <nav className="nav-list">
          {NAV_ITEMS.map((item) => (
            <button key={item.id} className={`nav-item ${activePage === item.id ? 'active' : ''}`} onClick={() => setActivePage(item.id)}>
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </button>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <div className="system-card">
          <div className="system-row">
            <span className={`status-dot ${backendStatus}`} />
            <span>{backendStatus === 'online' ? 'Backend online' : backendStatus === 'offline' ? 'Backend offline' : 'Checking backend'}</span>
          </div>
          <div className="system-endpoint">{API_BASE_URL || '/api via Vite proxy'}</div>
        </div>
        <div className="sidebar-foot">DSA + Online Evidence</div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div>
            <div className="eyebrow">TRUST &amp; EVIDENCE</div>
            <h1>{pageTitle(activePage)}</h1>
          </div>
          <div className="topbar-status">
            <span className={`status-dot ${backendStatus}`} />
            <span>{backendStatus === 'online' ? 'Live API connected' : backendStatus === 'offline' ? 'API unavailable' : 'Connecting...'}</span>
          </div>
        </header>

        <div className="page-body">
          {activePage === 'overview' && <OverviewPage backendStatus={backendStatus} history={history} lastResult={lastResult} onNavigate={setActivePage} />}
          {activePage === 'verify' && <VerifyPage onResult={handleVerification} history={history} />}
          {activePage === 'search' && <SearchPage />}
          {activePage === 'dsa' && <DsaWorkspace />}
          {activePage === 'factcheck' && <FactCheckPage />}
        </div>
      </main>
    </div>
  )
}

function pageTitle(page) {
  return {
    overview: 'Dashboard',
    verify: 'Verify a News Claim',
    search: 'Search Live News',
    dsa: 'DSA Workspace',
    factcheck: 'Fact Check Evidence',
  }[page]
}

function OverviewPage({ backendStatus, history, lastResult, onNavigate }) {
  const stats = useMemo(() => {
    const verified = history.length
    const supported = history.filter((item) => /TRUE|SUPPORTED|RELIABLE/i.test(item.verdict)).length
    const flagged = history.filter((item) => /FALSE|MISLEADING/i.test(item.verdict)).length
    const average = verified ? Math.round(history.reduce((sum, item) => sum + Number(item.confidence || 0), 0) / verified) : 0
    return { verified, supported, flagged, average }
  }, [history])

  return (
    <>
      <section className="hero-card">
        <div className="hero-copy">
          <span className="pill">ONLINE VERIFICATION ENGINE</span>
          <h2>Check a claim before you share it.</h2>
          <p>Compare online coverage with existing fact-check evidence and DSA-based text processing.</p>
          <button className="primary-button" onClick={() => onNavigate('verify')}>Verify News <span>→</span></button>
        </div>
        <div className="hero-visual">
          <div className="orbit orbit-a" />
          <div className="orbit orbit-b" />
          <div className="hero-core">✓</div>
          <span className="node node-a">NEWS</span>
          <span className="node node-b">FACT CHECK</span>
          <span className="node node-c">DSA</span>
        </div>
      </section>

      <div className="stats-grid">
        <MetricCard label="Claims checked" value={stats.verified} hint="this browser session" icon="↗" />
        <MetricCard label="Likely supported" value={stats.supported} hint="recorded verdicts" icon="✓" />
        <MetricCard label="False / misleading" value={stats.flagged} hint="fact-check signals" icon="!" />
        <MetricCard label="Avg. evidence" value={`${stats.average}%`} hint="project heuristic" icon="◒" />
      </div>

      <section className="content-grid two-col">
        <div className="panel">
          <PanelTitle title="Verification architecture" subtitle="Online evidence + DSA processing" />
          <div className="flow-list">
            <FlowStep number="01" title="Online coverage" text="NewsAPI retrieves matching article metadata." />
            <FlowStep number="02" title="Fact-check evidence" text="Google searches existing claim reviews." />
            <FlowStep number="03" title="DSA processing" text="Rabin-Karp, KMP, Edit Distance, HashMap, PriorityQueue and BFS power analysis." />
            <FlowStep number="04" title="Transparent result" text="The UI separates direct evidence from related evidence." />
          </div>
        </div>
        <div className="panel">
          <PanelTitle title="DSA modules now connected" subtitle="Available from the web app" />
          <div className="evidence-grid">
            <EvidenceCard title="Duplicate detection" value="Rabin-Karp + DP" text="Compare reworded titles and content." />
            <EvidenceCard title="Moderation" value="KMP" text="Find prohibited patterns efficiently." />
            <EvidenceCard title="Categorization" value="HashMap" text="Map keywords to news categories." />
            <EvidenceCard title="Trending" value="HashMap + PQ" text="Rank frequent keywords." />
            <EvidenceCard title="Related news" value="Graph + BFS" text="Traverse connected stories." />
            <EvidenceCard title="Persistence" value="File handling" text="Web-managed articles survive restarts." />
          </div>
        </div>
      </section>

      <section className="panel">
        <PanelTitle title="Recent verification history" subtitle="Stored locally in this browser" />
        {history.length === 0 ? (
          <EmptyState title="No claims checked yet" text="Run your first verification to build a local history." action="Verify a claim" onAction={() => onNavigate('verify')} />
        ) : (
          <div className="history-list">
            {history.map((item) => (
              <div className="history-row" key={`${item.claim}-${item.timestamp}`}>
                <div className="history-main"><strong>{item.claim}</strong><span>{formatDate(item.timestamp)}</span></div>
                <div className="history-meta"><Badge tone={toneForVerdict(item.verdict)}>{item.verdict}</Badge><span className="confidence-mini">{item.confidence}%</span></div>
              </div>
            ))}
          </div>
        )}
      </section>
      {lastResult && <section className="panel compact-panel"><PanelTitle title="Last verification" subtitle="Latest result from the backend" /><ResultSummary result={lastResult} /></section>}
    </>
  )
}

function VerifyPage({ onResult, history }) {
  const [claim, setClaim] = useState('')
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    const value = claim.trim()
    if (!value) return
    setLoading(true)
    setError('')
    try {
      const data = await verifyNews(value)
      setResult(data)
      onResult(data)
    } catch (err) {
      setError(err.message || 'Unable to verify this claim.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <section className="verify-layout">
      <div className="verify-left">
        <div className="panel verify-input-panel">
          <div className="panel-kicker">STEP 01</div>
          <h2>Enter a factual claim</h2>
          <p className="muted">Use a complete sentence rather than only a topic.</p>
          <form onSubmit={submit}>
            <textarea value={claim} onChange={(event) => setClaim(event.target.value)} placeholder="Example: India defeated Australia in cricket" rows="6" />
            <div className="quick-row">
              {DEMO_CLAIMS.map((item) => <button type="button" key={item} className="quick-chip" onClick={() => setClaim(item)}>{item}</button>)}
            </div>
            <button className="primary-button wide" disabled={loading || !claim.trim()}>{loading ? 'Checking evidence…' : 'Verify Claim'} <span>→</span></button>
          </form>
          {error && <div className="error-box">{error}</div>}
        </div>
        <div className="panel">
          <PanelTitle title="Evidence layers" subtitle="The result is evidence-based, not a promise of absolute truth." />
          <div className="evidence-grid">
            <EvidenceCard title="News coverage" value="NewsAPI" text="Find matching articles and source names." />
            <EvidenceCard title="Fact-check database" value="Google" text="Inspect claim reviews and ratings." />
            <EvidenceCard title="Pattern matching" value="Rabin-Karp" text="Search claim and keyword overlap." />
            <EvidenceCard title="Similarity" value="Edit Distance" text="Compare reworded claims with DP." />
          </div>
        </div>
      </div>
      <div className="verify-right">
        {result ? <VerificationResultCard result={result} /> : <div className="panel empty-result"><div className="result-icon">◌</div><h3>Verification result</h3><p>Submit a claim to see source agreement, fact-check evidence and the evidence heuristic.</p><div className="empty-checklist"><span>✓ Live news coverage</span><span>✓ Fact-check reviews</span><span>✓ DSA matching</span><span>✓ Evidence transparency</span></div></div>}
        {history.length > 0 && <div className="panel"><PanelTitle title="Previous checks" subtitle="Copy a recent claim into the workspace" /><div className="mini-history">{history.slice(0, 5).map((item) => <button key={`${item.claim}-${item.timestamp}`} onClick={() => setClaim(item.claim)}><span>{item.claim}</span><strong>{item.confidence}%</strong></button>)}</div></div>}
      </div>
    </section>
  )
}

function VerificationResultCard({ result }) {
  const tone = toneForVerdict(result.verdict)
  const confidence = Math.max(0, Math.min(100, Number(result.confidence || 0)))
  const evidenceStrength = result.relevantFactChecks > 0 ? 'STRONG' : result.relatedFactChecks > 0 ? 'RELATED' : result.uniqueSources >= 2 ? 'MODERATE' : confidence >= 50 ? 'WEAK' : 'INSUFFICIENT'

  return (
    <div className={`panel result-card result-${tone}`}>
      <div className="result-header">
        <div><div className="panel-kicker">STEP 02 · RESULT</div><div className="claim-label">CLAIM</div><h2>{result.claim}</h2></div>
        <div className="score-ring" style={{ background: `conic-gradient(#66d5ff 0 ${confidence * 3.6}deg, #183048 ${confidence * 3.6}deg 360deg)` }}><div><strong>{confidence}%</strong><span>evidence</span></div></div>
      </div>
      <div className="verdict-banner"><div><span className="verdict-label">VERDICT</span><strong>{result.verdict}</strong><span className="verdict-subtext">Evidence strength: {evidenceStrength}</span></div><Badge tone={tone}>{evidenceStrength} EVIDENCE</Badge></div>
      <div className="result-stats"><ResultStat label="Matching articles" value={result.matchingArticles} /><ResultStat label="Unique sources" value={result.uniqueSources} /><ResultStat label="Fact checks returned" value={result.factChecksReturned} /><ResultStat label="Direct fact checks" value={result.relevantFactChecks} /><ResultStat label="Related fact checks" value={result.relatedFactChecks} /></div>
      <section className="result-section"><div className="section-heading"><h3>News source agreement</h3><span>{result.uniqueSources || 0} unique source{result.uniqueSources === 1 ? '' : 's'}</span></div>{result.sources?.length ? <div className="source-list">{result.sources.map((source) => <div className="source-chip" key={source}>● {source}</div>)}</div> : <div className="muted-box">No matching online sources passed the similarity rules.</div>}</section>
      <section className="result-section"><div className="section-heading"><h3>Fact-check evidence</h3><span>{result.factChecks?.length || 0} surfaced result{result.factChecks?.length === 1 ? '' : 's'}</span></div>{result.factChecks?.length ? <div className="fact-list">{result.factChecks.map((item, index) => <FactCard key={`${item.url}-${index}`} item={item} />)}</div> : <div className="muted-box">No fact-check evidence passed the relevance threshold.</div>}</section>
      <div className="result-note"><span>ⓘ</span><p>Evidence confidence is a project-defined heuristic. It is not a statistical probability that the claim is true.</p></div>
    </div>
  )
}

function DsaWorkspace() {
  const [tab, setTab] = useState('add')
  return (
    <>
      <section className="panel search-hero">
        <div><div className="panel-kicker">DSA MODULES</div><h2>Use the project's core algorithms from the web UI.</h2><p className="muted">All operations below call Spring Boot endpoints. Managed articles are saved to a file by the backend.</p></div>
      </section>
      <div className="tab-strip">{TABS.map(([id, label]) => <button key={id} className={tab === id ? 'tab active' : 'tab'} onClick={() => setTab(id)}>{label}</button>)}</div>
      {tab === 'add' && <AddManagePanel />}
      {tab === 'duplicate' && <DuplicatePanel />}
      {tab === 'moderate' && <ModerationPanel />}
      {tab === 'categorize' && <CategorizePanel />}
      {tab === 'trending' && <TrendingPanel />}
      {tab === 'related' && <RelatedPanel />}
    </>
  )
}

function AddManagePanel() {
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [articles, setArticles] = useState([])
  const [message, setMessage] = useState('')
  const [error, setError] = useState('')

  const load = async () => {
    try { setArticles(await getDsaNews()) } catch (err) { setError(err.message) }
  }
  useEffect(() => { load() }, [])

  const submit = async (event) => {
    event.preventDefault()
    setMessage(''); setError('')
    try {
      const data = await addDsaNews(title, content)
      setMessage(`${data.message}. ID ${data.article.id} • ${data.article.category} • ${data.article.moderationStatus}${data.duplicateDetected ? ' • possible duplicate' : ''}`)
      setTitle(''); setContent(''); await load()
    } catch (err) { setError(err.message) }
  }

  return <section className="content-grid two-col"><div className="panel"><PanelTitle title="Add news article" subtitle="ArrayList + categorization + KMP moderation + persistence" /><form onSubmit={submit}><input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Article title" /><textarea value={content} onChange={(e) => setContent(e.target.value)} rows="7" placeholder="Article content" /><button className="primary-button wide" disabled={!title.trim() || !content.trim()}>Add Article <span>→</span></button></form>{message && <div className="success-box">{message}</div>}{error && <div className="error-box">{error}</div>}</div><div className="panel"><PanelTitle title="Managed news" subtitle="Loaded from the backend file" />{articles.length ? <div className="managed-list">{articles.map((a) => <div className="managed-card" key={a.id}><div className="managed-head"><strong>#{a.id} {a.title}</strong><div className="badge-row"><Badge tone="info">{a.category}</Badge><Badge tone={a.moderationStatus === 'FLAGGED' ? 'bad' : 'good'}>{a.moderationStatus}</Badge></div></div><p>{a.content}</p></div>)}</div> : <div className="muted-box">No managed articles yet.</div>}</div></section>
}

function DuplicatePanel() {
  const [title, setTitle] = useState('India defeated Australia in cricket')
  const [content, setContent] = useState('India defeated Australia in an exciting cricket sports match.')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const submit = async (e) => { e.preventDefault(); setError(''); try { setResult(await checkDsaDuplicate(title, content)) } catch (err) { setError(err.message) } }
  return <section className="content-grid two-col"><div className="panel"><PanelTitle title="Duplicate detection" subtitle="Rabin-Karp + Edit Distance / DP" /><form onSubmit={submit}><input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="New article title" /><textarea value={content} onChange={(e) => setContent(e.target.value)} rows="7" placeholder="New article content" /><button className="primary-button wide">Check Duplicate <span>→</span></button></form>{error && <div className="error-box">{error}</div>}</div><div className="panel"><PanelTitle title="Comparison result" subtitle="Possible duplicates are compared to stored articles" />{result ? <><div className="verdict-banner"><div><span className="verdict-label">RESULT</span><strong>{result.duplicateDetected ? 'POSSIBLE DUPLICATE' : 'NO DUPLICATE DETECTED'}</strong></div><Badge tone={result.duplicateDetected ? 'warn' : 'good'}>{result.matches?.length || 0} match{result.matches?.length === 1 ? '' : 'es'}</Badge></div>{result.matches?.length ? <div className="fact-list">{result.matches.map((m) => <div className="fact-card" key={m.existingId}><div className="fact-topline"><span>Article #{m.existingId}</span><span>{m.overallSimilarity}% overall</span></div><h4>{m.existingTitle}</h4><p>Title similarity: {m.titleSimilarity}% • Content similarity: {m.contentSimilarity}% • Common keywords: {m.commonKeywords}</p><p>Rabin-Karp full-title match: {m.rabinKarpMatch ? 'Yes' : 'No'}</p></div>)}</div> : <div className="muted-box">The stored articles did not meet any duplicate threshold.</div>}</> : <EmptyState title="No comparison yet" text="Enter a title and content, then run duplicate detection." />}</div></section>
}

function ModerationPanel() {
  return <ArticleActionPanel title="Content moderation" subtitle="KMP pattern matching against prohibited words" actionLabel="Run KMP Moderation" action={moderateDsaNews} mode="moderate" />
}

function CategorizePanel() {
  return <ArticleActionPanel title="News categorization" subtitle="HashMap keyword-to-category mapping" actionLabel="Categorize Article" action={categorizeDsaNews} mode="categorize" />
}

function ArticleActionPanel({ title, subtitle, actionLabel, action, mode }) {
  const [articles, setArticles] = useState([])
  const [id, setId] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const load = async () => { try { setArticles(await getDsaNews()) } catch (err) { setError(err.message) } }
  useEffect(() => { load() }, [])
  const submit = async (e) => { e.preventDefault(); setError(''); try { const data = await action(Number(id)); setResult(data); await load() } catch (err) { setError(err.message) } }
  return <section className="content-grid two-col"><div className="panel"><PanelTitle title={title} subtitle={subtitle} /><form onSubmit={submit}><select value={id} onChange={(e) => setId(e.target.value)}><option value="">Select article</option>{articles.map((a) => <option key={a.id} value={a.id}>#{a.id} {a.title}</option>)}</select><button className="primary-button wide" disabled={!id}>{actionLabel} <span>→</span></button></form>{error && <div className="error-box">{error}</div>}</div><div className="panel"><PanelTitle title="Action result" subtitle="Backend response" />{result ? <div className="result-stat-grid"><ResultStat label="Article" value={`#${result.article?.id ?? id}`} /><ResultStat label={mode === 'moderate' ? 'Status' : 'Category'} value={mode === 'moderate' ? (result.status || '-') : (result.category || '-')} />{mode === 'moderate' && <ResultStat label="Pattern" value={result.patternDetected || 'None'} />}</div> : <EmptyState title="No action run" text="Select an article and run the module." />}</div></section>
}

function TrendingPanel() {
  const [data, setData] = useState([])
  const [error, setError] = useState('')
  const load = async () => { try { setData(await getTrendingKeywords()) } catch (err) { setError(err.message) } }
  useEffect(() => { load() }, [])
  return <section className="panel"><div className="section-heading"><div><div className="panel-kicker">HASHMAP + PRIORITYQUEUE</div><h3>Trending keywords</h3></div><button className="secondary-button" onClick={load}>Refresh</button></div>{error && <div className="error-box">{error}</div>}{data.length ? <div className="trend-list">{data.map((item) => <div className="trend-row" key={item.keyword}><span className="rank-badge">{item.rank}</span><div><strong>{item.keyword}</strong><span>{item.mentions} mentions</span></div><div className="trend-bar"><i style={{ width: `${Math.min(100, item.mentions * 15)}%` }} /></div></div>)}</div> : <EmptyState title="No trend data" text="Add some managed news articles first." />}</section>
}

function RelatedPanel() {
  const [articles, setArticles] = useState([])
  const [id, setId] = useState('')
  const [result, setResult] = useState(null)
  const [error, setError] = useState('')
  const load = async () => { try { setArticles(await getDsaNews()) } catch (err) { setError(err.message) } }
  useEffect(() => { load() }, [])
  const submit = async (e) => { e.preventDefault(); setError(''); try { setResult(await getRelatedDsaNews(Number(id))) } catch (err) { setError(err.message) } }
  return <section className="content-grid two-col"><div className="panel"><PanelTitle title="Related news" subtitle="Graph construction + BFS traversal" /><form onSubmit={submit}><select value={id} onChange={(e) => setId(e.target.value)}><option value="">Select starting article</option>{articles.map((a) => <option key={a.id} value={a.id}>#{a.id} {a.title}</option>)}</select><button className="primary-button wide" disabled={!id}>Find Related <span>→</span></button></form>{error && <div className="error-box">{error}</div>}</div><div className="panel"><PanelTitle title="BFS result" subtitle={result?.startArticle ? `Starting article #${result.startArticle.id}` : 'Connected stories'} />{result ? result.relatedArticles?.length ? <div className="managed-list">{result.relatedArticles.map((a) => <div className="managed-card" key={a.id}><div className="managed-head"><strong>#{a.id} {a.title}</strong><Badge tone="info">{a.category}</Badge></div><p>{a.content}</p></div>)}</div> : <div className="muted-box">No related articles were found.</div> : <EmptyState title="No BFS traversal yet" text="Select a starting article to traverse the related-news graph." />}</div></section>
}

function SearchPage() {
  const [query, setQuery] = useState('India cricket'); const [data, setData] = useState(null); const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  const submit = async (event) => { event.preventDefault(); if (!query.trim()) return; setLoading(true); setError(''); try { setData(await searchNews(query.trim())) } catch (err) { setError(err.message) } finally { setLoading(false) } }
  return <><section className="panel search-hero"><div><div className="panel-kicker">NEWS DISCOVERY</div><h2>Search online coverage.</h2><p className="muted">Live article metadata from the Spring Boot backend.</p></div><form className="search-form" onSubmit={submit}><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search headlines or topics" /><button className="primary-button">{loading ? 'Searching…' : 'Search'}</button></form></section>{error && <div className="error-box">{error}</div>}{data && <section className="panel"><div className="section-heading"><div><div className="panel-kicker">SEARCH RESULTS</div><h3>{data.totalResults ?? 0} result{data.totalResults === 1 ? '' : 's'}</h3></div><Badge tone="info">{data.status || 'ok'}</Badge></div><div className="article-grid">{(data.articles || []).map((article, index) => <ArticleCard key={`${article.url}-${index}`} article={article} />)}</div></section>}</>
}

function FactCheckPage() {
  const [query, setQuery] = useState('drinking hot water cures COVID-19'); const [data, setData] = useState(null); const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  const submit = async (event) => { event.preventDefault(); if (!query.trim()) return; setLoading(true); setError(''); try { setData(await searchFactChecks(query.trim())) } catch (err) { setError(err.message) } finally { setLoading(false) } }
  const claims = data?.claims || []
  return <><section className="panel search-hero"><div><div className="panel-kicker">FACT CHECK API</div><h2>Inspect the underlying claim reviews.</h2><p className="muted">See raw claim evidence before relevance filtering.</p></div><form className="search-form" onSubmit={submit}><input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Enter a claim" /><button className="primary-button">{loading ? 'Searching…' : 'Find Fact Checks'}</button></form></section>{error && <div className="error-box">{error}</div>}{data && <section className="panel"><div className="section-heading"><div><div className="panel-kicker">RAW CLAIMS</div><h3>{claims.length} surfaced claim{claims.length === 1 ? '' : 's'}</h3></div><Badge tone="info">Google</Badge></div><div className="fact-list">{claims.map((claim, index) => <div className="fact-card" key={`${claim.text}-${index}`}><div className="fact-topline"><span>{claim.claimant || 'Claim record'}</span><span>{claim.claimDate ? formatDate(claim.claimDate) : ''}</span></div><h4>{claim.text}</h4>{(claim.claimReview || []).map((review, reviewIndex) => <div className="review-item" key={reviewIndex}><div><strong>{review.publisher?.name || 'Publisher'}</strong><span>{review.textualRating || 'No rating'}</span></div><a href={review.url} target="_blank" rel="noreferrer">Open review ↗</a></div>)}</div>)}</div></section>}</>
}

function ArticleCard({ article }) {
  return <article className="article-card"><div className="article-source">{article.source?.name || 'Unknown source'}</div><h4>{article.title || 'Untitled article'}</h4><p>{article.description || 'No description provided.'}</p><div className="article-meta"><span>{article.publishedAt ? formatDate(article.publishedAt) : ''}</span>{article.url ? <a href={article.url} target="_blank" rel="noreferrer">Read article ↗</a> : null}</div></article>
}

function FactCard({ item }) {
  const related = item.matchType !== 'STRONG'
  const ratingTone = /false|mislead|wrong|incorrect/i.test(item.rating || '') ? 'bad' : /true|accurate|correct/i.test(item.rating || '') ? 'good' : 'neutral'
  return <div className={`fact-card ${related ? 'related-evidence-card' : 'strong-evidence-card'}`}>
    <div className="fact-topline">
      <div className="fact-badges">
        <Badge tone={item.matchType === 'STRONG' ? 'good' : 'info'}>{item.matchType || 'EVIDENCE'}</Badge>
        <Badge tone={ratingTone}>{item.rating || 'Unrated'}</Badge>
      </div>
      <span>{Number(item.matchScore || 0).toFixed(1)} match</span>
    </div>
    <div className="fact-label">{related ? 'Related fact-check evidence' : 'Direct fact-check evidence'}</div>
    <h4>{item.claim}</h4>
    <p><strong>Fact-checker rating:</strong> {item.rating || 'No rating available'}</p>
    <p><strong>Publisher:</strong> {item.publisher || 'Unknown publisher'}{item.title ? ` · ${item.title}` : ''}</p>
    {related && <div className="related-note">This is related evidence, not a direct verdict on the exact claim entered.</div>}
    {item.url && <a href={item.url} target="_blank" rel="noreferrer">Read original fact check ↗</a>}
  </div>
}

function MetricCard({ label, value, hint, icon }) { return <div className="metric-card"><div className="metric-icon">{icon}</div><span>{label}</span><strong>{value}</strong><small>{hint}</small></div> }
function EvidenceCard({ title, value, text }) { return <div className="evidence-card"><span>{title}</span><strong>{value}</strong><p>{text}</p></div> }
function FlowStep({ number, title, text }) { return <div className="flow-step"><span>{number}</span><div><strong>{title}</strong><p>{text}</p></div></div> }
function PanelTitle({ title, subtitle }) { return <div className="panel-title"><div><h3>{title}</h3><span>{subtitle}</span></div></div> }
function ResultStat({ label, value }) { return <div className="result-stat"><span>{label}</span><strong>{value}</strong></div> }
function ResultSummary({ result }) { return <div className="summary-row"><div><strong>{result.claim}</strong><span>{result.verdict}</span></div><div className="summary-right"><Badge tone={toneForVerdict(result.verdict)}>{result.verdict}</Badge><span className="confidence-mini">{result.confidence}%</span></div></div> }
function Badge({ tone = 'neutral', children }) { return <span className={`badge ${tone}`}>{children}</span> }
function EmptyState({ title, text, action, onAction }) { return <div className="empty-state"><div className="empty-state-icon">◌</div><div><strong>{title}</strong><p>{text}</p></div>{action && <button className="secondary-button" onClick={onAction}>{action}</button>}</div> }
function toneForVerdict(verdict = '') { if (/FALSE|MISLEADING/i.test(verdict)) return 'bad'; if (/TRUE|SUPPORTED|RELIABLE/i.test(verdict)) return 'good'; return 'warn' }
function confidenceBand(score = 0) { if (score >= 80) return 'STRONG'; if (score >= 60) return 'MODERATE'; if (score >= 35) return 'WEAK'; return 'LOW' }
function formatDate(value) { const date = new Date(value); return Number.isNaN(date.getTime()) ? value : date.toLocaleString() }

export default App
