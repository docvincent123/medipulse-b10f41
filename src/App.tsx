import { useEffect, useMemo, useState } from 'react';
import {
  Activity,
  AppWindow,
  Bell,
  Boxes,
  BrainCircuit,
  Calculator,
  CheckCircle2,
  ChevronRight,
  CloudDownload,
  Code2,
  Download,
  ExternalLink,
  FileJson,
  FileUp,
  Glasses,
  HeartPulse,
  Home,
  KeyRound,
  Library,
  LifeBuoy,
  Link2,
  LockKeyhole,
  Newspaper,
  PackageCheck,
  RefreshCw,
  Search,
  Settings,
  ShieldCheck,
  ShoppingBag,
  Sparkles,
  Stethoscope,
  UploadCloud,
  UserRound,
  WandSparkles,
  Zap,
} from 'lucide-react';
import { catalog, LicenseType, Product } from './catalog';
import { news } from './news';

type NavId = 'home' | 'library' | 'store' | 'updates' | 'news' | 'support' | 'developer' | 'settings';
type Registry = Record<string, InstalledProduct>;
type Tasks = Record<string, number>;
type Notice = { text: string; tone?: 'normal' | 'success' | 'error' };

const WEB_REGISTRY_KEY = 'quremed-studio-web-registry-v2';
const webPreviewRegistry: Registry = {
  rehaflow: { version: '2.3.0', installedAt: '2026-09-10' },
  'medical-calculator': { version: '0.8.0', installedAt: '2026-09-11' },
};

const navItems: Array<{ id: NavId; label: string; icon: typeof Home }> = [
  { id: 'home', label: 'Home', icon: Home },
  { id: 'library', label: 'Library', icon: Library },
  { id: 'store', label: 'Store', icon: ShoppingBag },
  { id: 'updates', label: 'Updates', icon: RefreshCw },
  { id: 'news', label: 'News', icon: Newspaper },
  { id: 'support', label: 'Support', icon: LifeBuoy },
  { id: 'developer', label: 'Developer', icon: Code2 },
  { id: 'settings', label: 'Settings', icon: Settings },
];

const licenseCopy: Record<LicenseType, string> = {
  free: 'Free',
  pro: 'Free + Pro',
  clinic: 'Clinic',
  preview: 'Preview',
};

function productIcon(product: Product, size = 24) {
  const props = { size, strokeWidth: 1.8 };
  if (product.icon === 'rehab') return <HeartPulse {...props} />;
  if (product.icon === 'calc') return <Calculator {...props} />;
  if (product.icon === 'case') return <BrainCircuit {...props} />;
  if (product.icon === 'ecg') return <Activity {...props} />;
  return <Glasses {...props} />;
}

function statusFor(product: Product, registry: Registry) {
  if (product.status === 'coming-soon') return { kind: 'soon' as const, label: 'Coming soon' };
  const installed = registry[product.id];
  if (!installed) return { kind: 'install' as const, label: product.licenseType === 'free' ? 'Install' : 'Get' };
  if (installed.version !== product.latestVersion) return { kind: 'update' as const, label: 'Update' };
  return { kind: 'open' as const, label: 'Open' };
}

function ProductCard({ product, registry, tasks, onAction }: {
  product: Product;
  registry: Registry;
  tasks: Tasks;
  onAction: (product: Product) => void;
}) {
  const status = statusFor(product, registry);
  const installed = registry[product.id];
  const progress = tasks[product.id];
  return (
    <article className="product-card" style={{ '--product-accent': product.accent } as React.CSSProperties}>
      <div className="product-glow" />
      <div className="product-head">
        <div className="product-icon">{productIcon(product)}</div>
        <div className="chips">
          <span className={`chip chip--${product.channel.toLowerCase()}`}>{product.channel}</span>
          <span className={`chip license license--${product.licenseType}`}>{licenseCopy[product.licenseType]}</span>
        </div>
      </div>
      <div className="product-copy">
        <small>{product.category}</small>
        <h3>{product.name}</h3>
        <p>{product.description}</p>
      </div>
      <div className="version-row">
        <span>{installed ? `Installed ${installed.version}` : 'Not installed'}</span>
        <span>Latest {product.latestVersion}</span>
      </div>
      {progress !== undefined && (
        <div className="progress-wrap">
          <div><span>Preparing product</span><strong>{progress}%</strong></div>
          <div className="progress-track"><i style={{ width: `${progress}%` }} /></div>
        </div>
      )}
      <div className="product-footer">
        <span className="price">{product.priceLabel}</span>
        <button className={`product-action product-action--${status.kind}`} disabled={status.kind === 'soon' || progress !== undefined} onClick={() => onAction(product)}>
          {progress !== undefined ? <RefreshCw className="spin" size={16} /> : status.kind === 'open' ? <AppWindow size={16} /> : status.kind === 'update' ? <CloudDownload size={16} /> : <Download size={16} />}
          {progress !== undefined ? 'Working…' : status.label}
        </button>
      </div>
    </article>
  );
}

function App() {
  const [active, setActive] = useState<NavId>('home');
  const [registry, setRegistry] = useState<Registry>({});
  const [registryReady, setRegistryReady] = useState(false);
  const [tasks, setTasks] = useState<Tasks>({});
  const [query, setQuery] = useState('');
  const [notice, setNotice] = useState<Notice>({ text: 'QureMED Studio ready', tone: 'success' });
  const [appVersion, setAppVersion] = useState('0.2.0');
  const [autoUpdates, setAutoUpdates] = useState(true);
  const [betaChannel, setBetaChannel] = useState(false);
  const [draftProduct, setDraftProduct] = useState('RehaFlow');
  const [draftVersion, setDraftVersion] = useState('2.5.0');

  useEffect(() => {
    let alive = true;
    const load = async () => {
      try {
        if (window.quremed) {
          const [meta, stored] = await Promise.all([window.quremed.getAppMeta(), window.quremed.getRegistry()]);
          if (!alive) return;
          setAppVersion(meta.version);
          setRegistry(stored);
        } else {
          const raw = localStorage.getItem(WEB_REGISTRY_KEY);
          setRegistry(raw ? JSON.parse(raw) : webPreviewRegistry);
        }
      } catch {
        setRegistry(window.quremed ? {} : webPreviewRegistry);
        setNotice({ text: 'Registry could not be loaded', tone: 'error' });
      } finally {
        if (alive) setRegistryReady(true);
      }
    };
    void load();
    return () => { alive = false; };
  }, []);

  const persistRegistry = async (next: Registry) => {
    setRegistry(next);
    try {
      if (window.quremed) await window.quremed.setRegistry(next);
      else localStorage.setItem(WEB_REGISTRY_KEY, JSON.stringify(next));
    } catch {
      setNotice({ text: 'Could not save product registry', tone: 'error' });
    }
  };

  const updates = useMemo(
    () => catalog.filter((product) => registry[product.id] && registry[product.id].version !== product.latestVersion),
    [registry],
  );
  const installedProducts = useMemo(() => catalog.filter((product) => registry[product.id]), [registry]);
  const filteredCatalog = useMemo(() => {
    const needle = query.trim().toLowerCase();
    if (!needle) return catalog;
    return catalog.filter((product) => `${product.name} ${product.description} ${product.category} ${product.licenseLabel}`.toLowerCase().includes(needle));
  }, [query]);

  const simulateInstall = (product: Product) => {
    if (tasks[product.id] !== undefined || product.status === 'coming-soon') return;
    const old = registry[product.id];
    setNotice({ text: `${old ? 'Updating' : 'Adding'} ${product.name}…` });
    setTasks((current) => ({ ...current, [product.id]: 8 }));
    let progress = 8;
    const timer = window.setInterval(() => {
      progress = Math.min(100, progress + 13);
      setTasks((current) => ({ ...current, [product.id]: progress }));
      if (progress >= 100) {
        window.clearInterval(timer);
        setTasks((current) => {
          const next = { ...current };
          delete next[product.id];
          return next;
        });
        setRegistry((current) => {
          const next = {
            ...current,
            [product.id]: {
              version: product.latestVersion,
              installedAt: new Date().toISOString(),
              executablePath: current[product.id]?.executablePath,
            },
          };
          if (window.quremed) void window.quremed.setRegistry(next);
          else localStorage.setItem(WEB_REGISTRY_KEY, JSON.stringify(next));
          return next;
        });
        setNotice({ text: `${product.name} ${product.latestVersion} is in your library`, tone: 'success' });
      }
    }, 140);
  };

  const openProduct = async (product: Product) => {
    if (!window.quremed) {
      setNotice({ text: `${product.name}: real launch works in the Windows build` });
      return;
    }
    let executablePath = registry[product.id]?.executablePath;
    if (!executablePath) {
      const pick = await window.quremed.chooseExecutable();
      if (pick.canceled || !pick.filePath) return;
      executablePath = pick.filePath;
      await persistRegistry({
        ...registry,
        [product.id]: { ...registry[product.id], executablePath },
      });
    }
    const result = await window.quremed.launchExecutable(executablePath);
    setNotice(result.ok ? { text: `${product.name} launched`, tone: 'success' } : { text: result.error || 'Launch failed', tone: 'error' });
  };

  const runAction = (product: Product) => {
    const status = statusFor(product, registry);
    if (status.kind === 'open') void openProduct(product);
    else if (status.kind !== 'soon') simulateInstall(product);
  };

  const updateAll = () => updates.forEach((product, index) => window.setTimeout(() => simulateInstall(product), index * 180));

  const exportDiagnostics = async () => {
    if (!window.quremed) {
      setNotice({ text: 'Diagnostic export is available in the Windows build' });
      return;
    }
    const result = await window.quremed.exportDiagnostics({
      generatedAt: new Date().toISOString(),
      studioVersion: appVersion,
      installedProducts: registry,
      availableUpdates: updates.map((item) => ({ id: item.id, latestVersion: item.latestVersion })),
      preferences: { autoUpdates, betaChannel },
    });
    setNotice(result.saved ? { text: 'Diagnostics saved', tone: 'success' } : { text: 'Diagnostic export cancelled' });
  };

  const renderProducts = (items: Product[]) => (
    <div className="product-grid">
      {items.map((product) => <ProductCard key={product.id} product={product} registry={registry} tasks={tasks} onAction={runAction} />)}
    </div>
  );

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand"><div className="brand-mark"><Stethoscope size={23} /></div><div><strong>QureMED</strong><span>STUDIO</span></div></div>
        <nav className="nav-list">
          {navItems.map((item) => {
            const Icon = item.icon;
            return <button key={item.id} className={active === item.id ? 'nav-item active' : 'nav-item'} onClick={() => setActive(item.id)}><Icon size={18} /><span>{item.label}</span>{item.id === 'updates' && updates.length > 0 && <b>{updates.length}</b>}</button>;
          })}
        </nav>
        <div className="sidebar-bottom">
          <div className="health-card"><ShieldCheck size={18} /><div><strong>Studio protected</strong><span>Sandboxed desktop runtime</span></div><i /></div>
          <small>QureMED Studio v{appVersion}</small>
        </div>
      </aside>

      <main className="main-panel">
        <header className="topbar">
          <div className="search-box"><Search size={17} /><input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search QureMED products…" /><kbd>Ctrl K</kbd></div>
          <div className="top-actions"><span className="cloud-state"><i /> QureMED Cloud</span><button className="square-button"><Bell size={18} /></button><button className="profile"><span>QM</span><div><strong>QureMED ID</strong><small>Local profile</small></div><ChevronRight size={15} /></button></div>
        </header>

        <section className="content">
          {!registryReady && <div className="loading-bar"><i /></div>}

          {active === 'home' && <>
            <div className="hero">
              <div className="hero-grid" />
              <div className="hero-copy"><div className="eyebrow"><Sparkles size={14} /> QureMED Industries ecosystem</div><h1>Your medical software.<br /><span>One studio.</span></h1><p>Discover, install, update and launch every QureMED product from one Windows workspace.</p><div className="hero-actions"><button className="primary" onClick={() => setActive('store')}><ShoppingBag size={17} /> Explore Store</button><button className="secondary" onClick={() => setActive('library')}><Library size={17} /> My Library</button></div></div>
              <div className="hero-visual"><div className="orbit orbit-one" /><div className="orbit orbit-two" /><div className="core-logo"><Stethoscope size={38} /></div><div className="float-card fc-one"><RefreshCw size={17} /><span><strong>{updates.length} updates</strong><small>Ready in Update Center</small></span></div><div className="float-card fc-two"><PackageCheck size={17} /><span><strong>{installedProducts.length} products</strong><small>Linked to this PC</small></span></div></div>
            </div>
            <div className="metric-grid">
              <div className="metric"><PackageCheck /><span><small>In library</small><strong>{installedProducts.length}</strong></span></div>
              <div className="metric"><CloudDownload /><span><small>Updates</small><strong>{updates.length}</strong></span></div>
              <div className="metric"><ShoppingBag /><span><small>Catalog</small><strong>{catalog.length}</strong></span></div>
              <div className="metric"><ShieldCheck /><span><small>Integrity</small><strong>Protected</strong></span></div>
            </div>
            <SectionTitle eyebrow="QUREMED CATALOG" title="Featured products" action="View store" onAction={() => setActive('store')} />
            {renderProducts(filteredCatalog.filter((item) => item.featured || item.id === 'medical-calculator' || item.id === 'caselab'))}
            <SectionTitle eyebrow="LATEST" title="News & releases" action="All news" onAction={() => setActive('news')} />
            <div className="news-strip">{news.slice(0, 3).map((item) => <article key={item.id}><span>{item.tag} · {item.date}</span><h3>{item.title}</h3><p>{item.summary}</p></article>)}</div>
          </>}

          {active === 'library' && <>
            <PageTitle eyebrow="MY LIBRARY" title="Installed products" description="Applications registered on this Windows PC. Link an existing executable once and Studio can launch it directly." badge={`${installedProducts.length} products`} />
            {installedProducts.length ? renderProducts(installedProducts) : <Empty icon={<Library size={38} />} title="Your library is empty" text="Open the Store and add your first QureMED product." action="Open Store" onAction={() => setActive('store')} />}
          </>}

          {active === 'store' && <>
            <PageTitle eyebrow="QUREMED STORE" title="Built for medicine." description="Free tools, Pro software, clinic systems and preview products from QureMED Industries." badge="QureMED verified" />
            <div className="store-banner"><div><span><WandSparkles size={15} /> FEATURED CLINICAL PLATFORM</span><h2>RehaFlow</h2><p>A local-first operations platform for rehabilitation centers, patient workflows and coordinated staff work.</p><button onClick={() => runAction(catalog[0])}>View product <ChevronRight size={15} /></button></div><HeartPulse size={122} strokeWidth={0.75} /></div>
            <div className="store-filters"><button className="filter active">All</button><button className="filter">Free</button><button className="filter">Pro</button><button className="filter">Clinic</button><span>{filteredCatalog.length} products</span></div>
            {renderProducts(filteredCatalog)}
          </>}

          {active === 'updates' && <>
            <PageTitle eyebrow="UPDATE CENTER" title="Keep every tool current." description="Review available versions and release notes before updating products in your library." action={<button className="primary" disabled={!updates.length} onClick={updateAll}><RefreshCw size={17} /> Update all</button>} />
            {updates.length ? <div className="update-list">{updates.map((product) => <article className="update-row" key={product.id}><div className="product-icon" style={{ '--product-accent': product.accent } as React.CSSProperties}>{productIcon(product)}</div><div className="update-main"><div><h3>{product.name}</h3><span>{registry[product.id].version} → {product.latestVersion} · {product.size}</span></div><ul>{product.releaseNotes.map((note) => <li key={note}>{note}</li>)}</ul></div><button className="product-action product-action--update" onClick={() => runAction(product)}><CloudDownload size={16} /> Update</button></article>)}</div> : <Empty icon={<CheckCircle2 size={40} />} title="Everything is up to date" text="No installed QureMED product needs an update." />}
          </>}

          {active === 'news' && <>
            <PageTitle eyebrow="QUREMED NEWSROOM" title="Updates from the ecosystem." description="Product releases, Studio changes and research work from QureMED Industries." badge="Latest releases" />
            <div className="news-grid">{news.map((item, index) => <article className={index === 0 ? 'news-card featured' : 'news-card'} key={item.id}><div><span className="news-tag">{item.tag}</span><small>{item.date}</small></div><h2>{item.title}</h2><p>{item.summary}</p>{item.productId && <button onClick={() => setActive('store')}>Open product <ExternalLink size={14} /></button>}</article>)}</div>
          </>}

          {active === 'support' && <>
            <PageTitle eyebrow="QUREMED SUPPORT" title="Help when you need it." description="Troubleshoot Studio, collect safe diagnostics and reach QureMED support from one place." badge="Support Center" />
            <div className="support-grid">
              <div className="support-card highlight"><LifeBuoy size={28} /><div><span>QUICK HELP</span><h3>Studio diagnostics</h3><p>Export Studio version, product versions and update state into a JSON file. No patient records, passwords or API keys are included.</p></div><button className="primary" onClick={() => void exportDiagnostics()}><FileJson size={17} /> Export diagnostics</button></div>
              <div className="support-card"><Link2 size={24} /><div><h3>Product connection</h3><p>If Open cannot find an application, Studio lets you select its Windows `.exe` once and remembers the link.</p></div></div>
              <div className="support-card"><ShieldCheck size={24} /><div><h3>Security</h3><p>The renderer is sandboxed and receives only a limited desktop bridge. Remote package signatures are the next distribution milestone.</p></div></div>
              <div className="support-card"><LifeBuoy size={24} /><div><h3>Contact support</h3><p>Use the support channel for installation, licensing and account issues.</p></div><button className="secondary" onClick={() => void window.quremed?.openExternal('https://github.com/docvincent123/medipulse-b10f41/issues')}>Open support <ExternalLink size={15} /></button></div>
            </div>
          </>}

          {active === 'developer' && <>
            <PageTitle eyebrow="DEVELOPER CONSOLE" title="Release Studio" description="Prepare product metadata today; signed package publishing and the remote catalog API are the next backend phase." badge="Internal" />
            <div className="developer-grid">
              <div className="panel"><div className="panel-head"><UploadCloud size={20} /><div><h3>Create release draft</h3><p>Prepare the next package version.</p></div></div><label>Product<select value={draftProduct} onChange={(event) => setDraftProduct(event.target.value)}>{catalog.map((item) => <option key={item.id}>{item.name}</option>)}</select></label><label>Version<input value={draftVersion} onChange={(event) => setDraftVersion(event.target.value)} /></label><label>Windows package<div className="file-drop"><FileUp size={24} /><span>Signed installer upload will connect here</span><small>.exe / .msi</small></div></label><button className="primary" onClick={() => setNotice({ text: `Release draft ${draftProduct} ${draftVersion} prepared` })}><Boxes size={17} /> Prepare draft</button></div>
              <div className="panel roadmap"><div className="panel-head"><Code2 size={20} /><div><h3>Distribution pipeline</h3><p>From build to verified install.</p></div></div>{['Upload signed product build', 'Create version + release notes', 'Generate SHA-256 manifest', 'Publish catalog entitlement', 'Studio verifies & installs', 'Keep rollback metadata'].map((step, index) => <div className="roadmap-step" key={step}><b>{index + 1}</b><span>{step}</span>{index < 3 ? <CheckCircle2 size={16} /> : <LockKeyhole size={16} />}</div>)}</div>
            </div>
          </>}

          {active === 'settings' && <>
            <PageTitle eyebrow="STUDIO SETTINGS" title="Desktop preferences" description="Choose update behavior and product channels for this computer." badge={`v${appVersion}`} />
            <div className="settings-grid">
              <div className="setting-row"><div><RefreshCw size={20} /><span><strong>Automatic updates</strong><small>Prepare updates when a newer product version is available.</small></span></div><button className={autoUpdates ? 'switch on' : 'switch'} onClick={() => setAutoUpdates(!autoUpdates)}><i /></button></div>
              <div className="setting-row"><div><Zap size={20} /><span><strong>Beta channel</strong><small>Show beta releases alongside stable builds.</small></span></div><button className={betaChannel ? 'switch on' : 'switch'} onClick={() => setBetaChannel(!betaChannel)}><i /></button></div>
              <div className="setting-row"><div><KeyRound size={20} /><span><strong>QureMED ID</strong><small>Account sign-in and paid entitlements will connect to the licensing backend.</small></span></div><button className="secondary" onClick={() => setNotice({ text: 'QureMED ID backend is the next integration phase' })}>Account</button></div>
              <div className="setting-row"><div><ShieldCheck size={20} /><span><strong>Product integrity</strong><small>Signed manifests and SHA-256 verification are required before remote installers go live.</small></span></div><span className="status-pill">Protected design</span></div>
            </div>
          </>}
        </section>

        <footer className={`statusbar statusbar--${notice.tone || 'normal'}`}><span><i />{notice.text}</span><span>QureMED Industries · Windows x64</span></footer>
      </main>
    </div>
  );
}

function SectionTitle({ eyebrow, title, action, onAction }: { eyebrow: string; title: string; action: string; onAction: () => void }) {
  return <div className="section-title"><div><span>{eyebrow}</span><h2>{title}</h2></div><button onClick={onAction}>{action} <ChevronRight size={15} /></button></div>;
}

function PageTitle({ eyebrow, title, description, badge, action }: { eyebrow: string; title: string; description: string; badge?: string; action?: React.ReactNode }) {
  return <div className="page-title"><div><span>{eyebrow}</span><h1>{title}</h1><p>{description}</p></div>{action || (badge && <div className="title-badge"><ShieldCheck size={16} /> {badge}</div>)}</div>;
}

function Empty({ icon, title, text, action, onAction }: { icon: React.ReactNode; title: string; text: string; action?: string; onAction?: () => void }) {
  return <div className="empty-state">{icon}<h3>{title}</h3><p>{text}</p>{action && onAction && <button className="primary" onClick={onAction}>{action}</button>}</div>;
}

export default App;
