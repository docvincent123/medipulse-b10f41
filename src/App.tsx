import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AppWindow,
  BadgeCheck,
  Boxes,
  BrainCircuit,
  Calculator,
  CheckCircle2,
  ChevronRight,
  CircleUserRound,
  CloudDownload,
  Code2,
  Cpu,
  Download,
  ExternalLink,
  FileUp,
  FlaskConical,
  Glasses,
  HeartPulse,
  Home,
  Library,
  Microscope,
  PackageCheck,
  RefreshCw,
  Search,
  Settings,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Stethoscope,
  UploadCloud,
  WandSparkles,
  Zap,
} from 'lucide-react';
import { catalog, Product } from './catalog';

type NavId = 'home' | 'library' | 'updates' | 'store' | 'developer' | 'settings';
type Registry = Record<string, string>;
type Tasks = Record<string, number>;

const REGISTRY_KEY = 'quremed-studio-registry-v1';

const defaultRegistry: Registry = {
  rehaflow: '2.3.0',
  'medical-calculator': '0.8.0',
  caselab: '0.4.9',
};

const navItems: Array<{ id: NavId; label: string; icon: typeof Home }> = [
  { id: 'home', label: 'Home', icon: Home },
  { id: 'library', label: 'Library', icon: Library },
  { id: 'updates', label: 'Updates', icon: RefreshCw },
  { id: 'store', label: 'Store', icon: ShoppingBag },
  { id: 'developer', label: 'Developer', icon: Code2 },
  { id: 'settings', label: 'Settings', icon: Settings },
];

function productIcon(product: Product) {
  const common = { size: 24, strokeWidth: 1.8 };
  switch (product.icon) {
    case 'rehab': return <HeartPulse {...common} />;
    case 'calc': return <Calculator {...common} />;
    case 'case': return <BrainCircuit {...common} />;
    case 'ecg': return <Activity {...common} />;
    case 'glasses': return <Glasses {...common} />;
  }
}

function statusFor(product: Product, registry: Registry) {
  const installed = registry[product.id];
  if (!installed) return { kind: 'install' as const, label: 'Install' };
  if (installed !== product.latestVersion) return { kind: 'update' as const, label: 'Update' };
  return { kind: 'open' as const, label: 'Open' };
}

function ProductCard({
  product,
  registry,
  tasks,
  onAction,
}: {
  product: Product;
  registry: Registry;
  tasks: Tasks;
  onAction: (product: Product) => void;
}) {
  const status = statusFor(product, registry);
  const progress = tasks[product.id];
  const installed = registry[product.id];

  return (
    <article className="product-card" style={{ '--product-accent': product.accent } as React.CSSProperties}>
      <div className="product-card__glow" />
      <div className="product-card__head">
        <div className="product-icon">{productIcon(product)}</div>
        <div className="product-card__meta">
          <span className={`channel channel--${product.channel.toLowerCase()}`}>{product.channel}</span>
          <span>{product.category}</span>
        </div>
      </div>

      <div className="product-card__body">
        <div>
          <h3>{product.name}</h3>
          <p>{product.description}</p>
        </div>
        <div className="version-line">
          <span>{installed ? `Installed ${installed}` : 'Not installed'}</span>
          <span>Latest {product.latestVersion}</span>
        </div>
      </div>

      {progress !== undefined && (
        <div className="download-progress">
          <div className="download-progress__label">
            <span>Downloading package</span><strong>{progress}%</strong>
          </div>
          <div className="download-progress__track"><i style={{ width: `${progress}%` }} /></div>
        </div>
      )}

      <div className="product-card__footer">
        <span className="price-label">{product.priceLabel}</span>
        <button
          className={`action-button action-button--${status.kind}`}
          onClick={() => onAction(product)}
          disabled={progress !== undefined}
        >
          {progress !== undefined ? <RefreshCw className="spin" size={16} /> : status.kind === 'install' ? <Download size={16} /> : status.kind === 'update' ? <CloudDownload size={16} /> : <AppWindow size={16} />}
          {progress !== undefined ? 'Working…' : status.label}
        </button>
      </div>
    </article>
  );
}

function App() {
  const [active, setActive] = useState<NavId>('home');
  const [registry, setRegistry] = useState<Registry>(() => {
    try {
      return JSON.parse(localStorage.getItem(REGISTRY_KEY) || '') || defaultRegistry;
    } catch {
      return defaultRegistry;
    }
  });
  const [tasks, setTasks] = useState<Tasks>({});
  const [query, setQuery] = useState('');
  const [notice, setNotice] = useState('QureMed Studio is ready');
  const [appVersion, setAppVersion] = useState('0.1.0');
  const [draftName, setDraftName] = useState('Medical Calculator Pro');
  const [draftVersion, setDraftVersion] = useState('0.9.0');
  const [draftNotes, setDraftNotes] = useState('New clinical calculators\nUI refinements\nPerformance improvements');

  useEffect(() => {
    localStorage.setItem(REGISTRY_KEY, JSON.stringify(registry));
  }, [registry]);

  useEffect(() => {
    window.quremed?.getAppMeta().then((meta) => setAppVersion(meta.version)).catch(() => undefined);
  }, []);

  const updates = useMemo(
    () => catalog.filter((product) => registry[product.id] && registry[product.id] !== product.latestVersion),
    [registry],
  );

  const installedCount = Object.keys(registry).length;
  const filteredCatalog = catalog.filter((product) => {
    const haystack = `${product.name} ${product.description} ${product.category}`.toLowerCase();
    return haystack.includes(query.toLowerCase());
  });

  const runAction = (product: Product) => {
    const status = statusFor(product, registry);
    if (status.kind === 'open') {
      setNotice(`${product.name} launched · executable binding is the next backend step`);
      return;
    }

    setNotice(`${status.kind === 'install' ? 'Installing' : 'Updating'} ${product.name}…`);
    setTasks((current) => ({ ...current, [product.id]: 4 }));
    let value = 4;
    const timer = window.setInterval(() => {
      value = Math.min(100, value + Math.floor(Math.random() * 13) + 6);
      setTasks((current) => ({ ...current, [product.id]: value }));
      if (value >= 100) {
        window.clearInterval(timer);
        setRegistry((current) => ({ ...current, [product.id]: product.latestVersion }));
        setTasks((current) => {
          const next = { ...current };
          delete next[product.id];
          return next;
        });
        setNotice(`${product.name} ${product.latestVersion} is ready`);
      }
    }, 180);
  };

  const renderCatalog = (items: Product[]) => (
    <div className="product-grid">
      {items.map((product) => (
        <ProductCard key={product.id} product={product} registry={registry} tasks={tasks} onAction={runAction} />
      ))}
    </div>
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><Stethoscope size={23} /></div>
          <div><strong>QureMed</strong><span>STUDIO</span></div>
        </div>

        <nav className="nav-list">
          {navItems.map((item) => {
            const Icon = item.icon;
            return (
              <button key={item.id} onClick={() => setActive(item.id)} className={active === item.id ? 'nav-item nav-item--active' : 'nav-item'}>
                <Icon size={18} /><span>{item.label}</span>
                {item.id === 'updates' && updates.length > 0 && <b>{updates.length}</b>}
              </button>
            );
          })}
        </nav>

        <div className="sidebar-spacer" />
        <div className="system-card">
          <div className="system-card__icon"><ShieldCheck size={18} /></div>
          <div><strong>System healthy</strong><span>Studio runtime online</span></div>
          <i />
        </div>
        <div className="sidebar-version">QureMed Studio v{appVersion}</div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div className="search-box">
            <Search size={17} />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search QureMed apps…" />
            <kbd>Ctrl K</kbd>
          </div>
          <div className="topbar-actions">
            <div className="cloud-state"><span /> QureMed Cloud</div>
            <button className="icon-button"><Zap size={18} /></button>
            <button className="account-button"><div>MV</div><span><strong>QureMed ID</strong><small>Developer</small></span><ChevronRight size={15} /></button>
          </div>
        </header>

        <section className="content">
          {active === 'home' && (
            <>
              <div className="hero-panel">
                <div className="hero-orbit hero-orbit--one" />
                <div className="hero-orbit hero-orbit--two" />
                <div className="hero-copy">
                  <div className="eyebrow"><Sparkles size={14} /> QureMed Industries ecosystem</div>
                  <h1>One place for every<br /><span>QureMed product.</span></h1>
                  <p>Install, update, launch and manage the complete QureMed software ecosystem from one Windows workspace.</p>
                  <div className="hero-actions">
                    <button className="primary-button" onClick={() => setActive('library')}><Boxes size={17} /> Browse library</button>
                    <button className="secondary-button" onClick={() => setActive('developer')}><Code2 size={17} /> Developer Console</button>
                  </div>
                </div>
                <div className="hero-status-stack">
                  <div className="floating-card floating-card--main">
                    <div className="floating-card__top"><div className="mini-logo"><HeartPulse size={20} /></div><span>RehaFlow</span><BadgeCheck size={17} /></div>
                    <strong>Update available</strong>
                    <p>2.3.0 <ChevronRight size={14} /> 2.4.1</p>
                    <div className="mini-progress"><i style={{ width: '68%' }} /></div>
                  </div>
                  <div className="floating-card floating-card--side"><RefreshCw size={17} /><span><strong>{updates.length} updates</strong><small>Ready to install</small></span></div>
                </div>
              </div>

              <div className="metrics-row">
                <div className="metric-card"><span className="metric-icon"><PackageCheck size={18} /></span><div><small>Installed products</small><strong>{installedCount}</strong></div><em>of {catalog.length}</em></div>
                <div className="metric-card"><span className="metric-icon"><CloudDownload size={18} /></span><div><small>Available updates</small><strong>{updates.length}</strong></div><em>{updates.length ? 'Action needed' : 'Up to date'}</em></div>
                <div className="metric-card"><span className="metric-icon"><Cpu size={18} /></span><div><small>Release channel</small><strong>Stable</strong></div><em>Protected</em></div>
                <div className="metric-card"><span className="metric-icon"><ShieldCheck size={18} /></span><div><small>Integrity</small><strong>Verified</strong></div><em>SHA ready</em></div>
              </div>

              <div className="section-heading"><div><span>YOUR ECOSYSTEM</span><h2>Products</h2></div><button onClick={() => setActive('library')}>View all <ChevronRight size={15} /></button></div>
              {renderCatalog(filteredCatalog.slice(0, 4))}
            </>
          )}

          {active === 'library' && (
            <>
              <div className="page-title"><div><span>QUREMED LIBRARY</span><h1>Your products</h1><p>Everything installed or available for this QureMed ID.</p></div><div className="title-badge"><Library size={17} /> {installedCount} installed</div></div>
              {renderCatalog(filteredCatalog)}
            </>
          )}

          {active === 'updates' && (
            <>
              <div className="page-title"><div><span>UPDATE CENTER</span><h1>Keep everything current.</h1><p>Review versions, release notes and deploy updates from one screen.</p></div><button className="primary-button" disabled={!updates.length} onClick={() => updates.forEach(runAction)}><RefreshCw size={17} /> Update all</button></div>
              <div className="update-list">
                {updates.length === 0 ? (
                  <div className="empty-state"><CheckCircle2 size={36} /><h3>Everything is up to date</h3><p>No QureMed product needs an update right now.</p></div>
                ) : updates.map((product) => (
                  <div className="update-row" key={product.id}>
                    <div className="product-icon" style={{ '--product-accent': product.accent } as React.CSSProperties}>{productIcon(product)}</div>
                    <div className="update-row__main"><div><h3>{product.name}</h3><span>{registry[product.id]} → {product.latestVersion} · {product.size}</span></div><ul>{product.releaseNotes.map((note) => <li key={note}>{note}</li>)}</ul></div>
                    <button className="action-button action-button--update" onClick={() => runAction(product)}><CloudDownload size={16} /> Update</button>
                  </div>
                ))}
              </div>
            </>
          )}

          {active === 'store' && (
            <>
              <div className="page-title"><div><span>QUREMED STORE</span><h1>Expand your workspace.</h1><p>Clinical, educational and experimental tools from QureMed Industries.</p></div><div className="title-badge"><ShoppingBag size={17} /> Demo catalog</div></div>
              <div className="store-banner"><div><span><WandSparkles size={15} /> FEATURED</span><h2>Medical Calculator Pro</h2><p>Clinical scores, infusion tools and chart-ready calculations — inside the QureMed ecosystem.</p><button onClick={() => runAction(catalog[1])}>Get product <ChevronRight size={15} /></button></div><Calculator size={116} strokeWidth={0.8} /></div>
              {renderCatalog(filteredCatalog)}
            </>
          )}

          {active === 'developer' && (
            <>
              <div className="page-title"><div><span>DEVELOPER CONSOLE</span><h1>Release Studio</h1><p>Prepare new QureMed product builds and publish them into Studio.</p></div><div className="title-badge title-badge--dev"><Code2 size={17} /> Internal tools</div></div>
              <div className="developer-grid">
                <div className="panel release-form">
                  <div className="panel-heading"><div className="panel-icon"><UploadCloud size={18} /></div><div><h3>Create release</h3><p>Draft metadata for the next package.</p></div></div>
                  <label>Product<input value={draftName} onChange={(e) => setDraftName(e.target.value)} /></label>
                  <div className="form-row"><label>Version<input value={draftVersion} onChange={(e) => setDraftVersion(e.target.value)} /></label><label>Channel<select defaultValue="Stable"><option>Stable</option><option>Beta</option><option>Internal</option></select></label></div>
                  <label>Release notes<textarea rows={6} value={draftNotes} onChange={(e) => setDraftNotes(e.target.value)} /></label>
                  <div className="drop-zone"><FileUp size={26} /><strong>Drop Windows package here</strong><span>.exe, .msi or .zip · signing hook ready</span><button>Select package</button></div>
                  <button className="primary-button wide" onClick={() => setNotice(`Draft ${draftName} ${draftVersion} saved locally`)}><UploadCloud size={17} /> Save release draft</button>
                </div>
                <div className="developer-side">
                  <div className="panel pipeline-panel"><div className="panel-heading"><div className="panel-icon"><FlaskConical size={18} /></div><div><h3>Release pipeline</h3><p>Planned publishing flow</p></div></div><div className="pipeline-step pipeline-step--done"><i><CheckCircle2 size={16} /></i><span><strong>Build package</strong><small>GitHub Actions / Windows runner</small></span></div><div className="pipeline-step"><i>2</i><span><strong>Verify SHA-256</strong><small>Integrity manifest</small></span></div><div className="pipeline-step"><i>3</i><span><strong>Publish release</strong><small>Stable / Beta channel</small></span></div><div className="pipeline-step"><i>4</i><span><strong>Studio distribution</strong><small>Users receive update</small></span></div></div>
                  <div className="panel api-panel"><Microscope size={24} /><div><strong>Manifest API</strong><p>The UI is ready for a remote catalog endpoint. Next phase connects release files, signatures and real downloads.</p></div><ExternalLink size={17} /></div>
                </div>
              </div>
            </>
          )}

          {active === 'settings' && (
            <>
              <div className="page-title"><div><span>STUDIO SETTINGS</span><h1>Environment</h1><p>Configure release channels and local client behavior.</p></div></div>
              <div className="settings-grid">
                <div className="panel setting-card"><div className="panel-icon"><RefreshCw size={18} /></div><div><h3>Automatic updates</h3><p>Check QureMed products on Studio launch.</p></div><button className="toggle toggle--on"><i /></button></div>
                <div className="panel setting-card"><div className="panel-icon"><FlaskConical size={18} /></div><div><h3>Beta channel</h3><p>Allow preview versions where available.</p></div><button className="toggle"><i /></button></div>
                <div className="panel setting-card"><div className="panel-icon"><ShieldCheck size={18} /></div><div><h3>Verify packages</h3><p>Validate package hash before install.</p></div><button className="toggle toggle--on"><i /></button></div>
                <div className="panel setting-card"><div className="panel-icon"><CircleUserRound size={18} /></div><div><h3>QureMed ID</h3><p>Developer workspace · local demo entitlement.</p></div><button className="secondary-button compact">Manage</button></div>
              </div>
            </>
          )}
        </section>

        <footer className="statusbar"><div><span className="status-dot" /> {notice}</div><div><span>Windows channel</span><b>Stable</b><span>Runtime</span><b>Electron</b></div></footer>
      </main>
    </div>
  );
}

export default App;
