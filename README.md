# QureMed Studio

QureMed Studio is the Windows launcher, app library, store and update center for QureMed Industries products.

## Current v0.1

- Premium React desktop UI
- Electron Windows shell
- Home / Library / Updates / Store / Developer / Settings
- Local product registry
- Install / Open / Update states
- Animated package progress
- Stable and Beta channels
- Release notes
- Developer Release Studio draft UI
- Windows `.exe` packaging workflow

Included catalog:
- RehaFlow
- Medical Calculator Pro
- CaseLab
- ECG Studio
- QureMed Vision Lab

## Stack

- React + TypeScript + Vite
- Electron
- electron-builder
- GitHub Actions

## Run locally

```bash
npm install
npm run dev
```

## Build frontend

```bash
npm run build
```

## Build Windows EXE

```bash
npm install
npm run dist:win
```

Windows artifacts are written to `release/` as an NSIS installer and a portable `.exe`.

## Update architecture

The first version stores installed product versions locally and simulates install/update progress. The next integration phase will replace that demo transport with real signed release packages.

Planned production flow:

1. QureMed developer uploads a product package.
2. Release metadata contains version, channel, notes, package URL and SHA-256.
3. QureMed Studio compares the remote manifest with the local registry.
4. Studio downloads the package to a temporary directory.
5. SHA-256 and signature are verified.
6. The old app is closed, the new package is installed, and the local registry is updated.
7. Previous versions remain available for rollback.

## Security direction

- Electron context isolation enabled
- Node integration disabled in renderer
- Sandboxed renderer
- Only HTTPS external URLs allowed through the preload bridge
- Package hash verification planned before real installers are enabled

## Next phase

- Real GitHub Releases or QureMed API-backed manifests
- Real package downloads and installation
- SHA-256 verification
- Rollback
- QureMed ID / licensing
- Product icons
- Windows code signing
- Developer Console publishing backend
