# QureMED Studio

QureMED Studio is the Windows launcher, product library, store, update center, newsroom and support hub for QureMED Industries software.

## v0.2 scope

- React + TypeScript + Vite interface in a sandboxed Electron desktop shell
- Home dashboard and unified QureMED Store
- Separate Library for installed/linked applications
- Free / Pro / Clinic / Preview product tiers
- Product update center with release notes
- Newsroom for product and company updates
- Support center with safe diagnostic JSON export
- Installed-version registry persisted under the Windows user profile
- One-time executable linking so Studio can launch locally installed products
- Developer Release Studio draft screen for future package publishing
- GitHub Actions build for NSIS installer + portable Windows `.exe`

## Current product catalog

- RehaFlow
- Medical Calculator Pro
- CaseLab
- ECG Studio
- QureMED Vision Lab

## Run locally

```bash
npm install
npm run dev
```

## Build the frontend

```bash
npm run build
```

## Build Windows packages

```bash
npm install
npm run dist:win
```

Artifacts are written to `release/`.

## Desktop data

The Electron main process stores the installed-product registry in the app's Windows `userData` folder as `product-registry.json`. The renderer does not get Node.js access. It communicates through a narrow preload IPC bridge.

The support page can export a JSON diagnostic bundle containing the Studio version, installed product versions, update state and non-secret preferences. It intentionally excludes passwords, API keys and clinical/patient content.

## Package distribution roadmap

v0.2 establishes the desktop product model and local launcher behavior. The next integration phase is the production distribution layer:

1. Move catalog and news data to a QureMED API or signed remote manifest.
2. Add QureMED ID authentication and license entitlements.
3. Upload Windows installers per product/version/channel.
4. Generate SHA-256 hashes and signatures for each release.
5. Download packages to a temporary directory from Studio.
6. Verify hash/signature before install.
7. Run installer silently or interactively according to package policy.
8. Keep rollback metadata for the previous version.
9. Add code signing for QureMED Studio and product installers.

Until signed package delivery is connected, the UI's install/update progress represents catalog registration rather than an unattended remote installer. Already installed applications can be linked to their `.exe` and launched for real from Library.

## Security baseline

- `contextIsolation: true`
- `nodeIntegration: false`
- sandboxed renderer
- renderer receives only explicit IPC methods from `preload.cjs`
- external links restricted to HTTPS / mailto
- executable launcher accepts `.exe` paths only and checks that the file exists
- diagnostics export is user-initiated through a save dialog
