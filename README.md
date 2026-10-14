# SourceVault — Chrome Source Collector & ZIP Downloader

[![Deploy to Render](https://render.com/images/deploy-to-render-button.svg)](https://render.com/deploy?repo=https://github.com/SanthoshBussa/SourceVault)

> **Enterprise website resource collector, source-map reconstructor, and ZIP packaging engine built with Java 21, Spring Boot 3 microservices, and React.**

---

## 1. Project Overview & Primary Workflow

SourceVault allows any user to enter only a website URL (such as `https://topbrains.com/`), analyze discoverable public resources or active browser session resources, reconstruct original source files from source maps, and download a validated ZIP backup archive.

```
+-----------------------------------------------------------------------------------+
|                              SourceVault User Workflow                             |
+-----------------------------------------------------------------------------------+
|  1. Enter URL  ==>  2. Analyze Site  ==>  3. Review Assets  ==> 4. Collect &     |
|   (e.g. https://      (SSRF Guard,         (Origin & Type        Reconstruct      |
|    topbrains.com/)     Discovery)           Breakdown)           Source Maps)     |
|                                                                        |          |
|                                                                        V          |
|  6. Download ZIP <== 5. CRC32 Integrity <== 4. Package Archive <=======+          |
|     (Valid Archive)     Verification        (manifest, README,                    |
|                                              reconstructed_sources)               |
+-----------------------------------------------------------------------------------+
```

---

## 2. Architecture & Microservices

SourceVault is built as a **Maven multi-module architecture** where each service can be run independently as an isolated microservice, or orchestrated together:

| Module / Service | Port | Description & Responsibilities |
|---|---|---|
| **`sourcevault-common`** | — | Shared DTOs, Enums, `SsrfValidator`, and `PathSanitizer`. |
| **`website-analysis-service`** | 8081 | Validates URLs, enforces SSRF rules, parses HTML, discovers scripts, styles, images, fonts, manifests, and detects source-map references. |
| **`resource-collection-service`**| 8082 | Asynchronous streaming downloader with rate limits, size limits, SHA-256 computation, and raw byte storage. |
| **`archive-service`** | 8083 | Reconstructs original source files from `sourcesContent`, separates compiled bundles, generates `manifest.csv`, `README.md`, `summary.json`, packages ZIP, and validates CRC32 integrity. |
| **`job-service`** | 8084 | Manages job lifecycle, persists job history in file-backed H2 (`./data/sourcevault_db`), exposes pagination, filtering, and retries. |
| **`api-gateway`** | 8080 | Single entry point, CORS configuration, request routing, rate limits, and Chrome extension session registry. |
| **`sourcevault-server`** | 8088 / `$PORT` | Unified production runner that embeds all services and serves the bundled React frontend for single-container deployment (e.g. Render / Docker). |
| **`chrome-extension/`** | — | Manifest V3 Chrome Extension for capturing authenticated active tabs and virtual source files. |
| **`frontend/`** | 5173 | React 18, TypeScript, Vite, Tailwind CSS, Lucide icons, TanStack Query, and Recharts. |

---

## 3. Browser Integration vs. URL-Only Mode

SourceVault supports two operational collection modes:

1. **URL-Only Mode (Default)**:
   - Requires zero installation from the user.
   - Crawls public HTML, CSS `@import`, and script tags safely using server-side clients.
   - Protects against SSRF by validating all resolved IP addresses against private networks.
   - *Limitation*: Cannot access pages requiring authenticated user login.

2. **Browser-Integrated Mode (Chrome Extension Manifest V3)**:
   - Located in the `chrome-extension/` directory.
   - Inspects the active tab DOM and network requests (`window.performance.getEntriesByType('resource')`).
   - Allows syncing resources and session state directly with SourceVault.
   - **One-time Setup**:
     1. Open Google Chrome and navigate to `chrome://extensions/`.
     2. Enable **Developer mode** (toggle in upper right).
     3. Click **Load unpacked** and select the `chrome-extension` directory.
     4. Open the extension popup, enter your SourceVault URL (e.g. `http://localhost:8088` or your Render deployment URL), and click **Sync to SourceVault**.

---

## 4. Source-Map Recovery & Archive Layout

When `.map` files are discovered and contain `sourcesContent`, SourceVault reconstructs the original, unminified source code into a clean file tree:

```
topbrains_source_backup/
├── topbrains.com/
│   ├── index.html
│   └── assets/
│       ├── index-Bh2Cssy7.js
│       └── index-C7WfsAY_.css
├── reconstructed_sources/
│   ├── src/
│   │   ├── main.tsx
│   │   ├── App.tsx
│   │   └── components/
│   │       ├── Header.tsx
│   │       └── Card.tsx
├── manifest.csv
├── README.md
├── failed_resources.txt
└── summary.json
```

### Archive Integrity Validation
Before marking any archive as ready:
- Reads every entry in the generated ZIP.
- Validates entry counts and CRC32 checksums against `manifest.csv`.
- Ensures binary assets retain their byte fidelity.

---

## 5. Security & SSRF Protection

- **Protocol Restriction**: Only `http` and `https` schemes are accepted.
- **SSRF Guard (`SsrfValidator`)**:
  - Rejects loopback (`127.0.0.0/8`, `::1`).
  - Rejects private IP ranges (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`).
  - Rejects link-local & cloud metadata (`169.254.169.254`, `metadata.google.internal`).
  - Rejects carrier-grade NAT (`100.64.0.0/10`).
  - Validates DNS resolution before connecting and inspects every redirect hop.
- **Path Traversal Protection (`PathSanitizer`)**: Strips illegal path characters and prevents `..` directory traversal attempts.
- **Resource Limits**: Max 20MB per file, max 100MB per collection job by default.

---

## 6. Local Quickstart & Commands

### Prerequisites
- Java 21 LTS
- Maven 3.9+
- Node.js 20+ & npm

### Running the Multi-Module Build & Tests
```bash
# Compile and run all backend and integration tests
mvn clean test

# Build frontend production bundle
cd frontend
npm install
npm run build
cd ..

# Package all-in-one production server jar
mvn package -DskipTests -pl sourcevault-server
```

### Running the Production Server Locally
```bash
java -jar sourcevault-server/target/sourcevault-server-1.0.0.jar --server.port=8088
```
Then visit **`http://localhost:8088`** in your browser.

---

## 7. Render 24/7 Global Deployment Guide

Deploy SourceVault to [Render](https://render.com) so it is online 24/7 and available worldwide:

### Option A: Automatic 1-Click Deployment via GitHub + Blueprint (`render.yaml`)

1. **Push your repository to GitHub**:
   ```bash
   gh repo create SourceVault --public --source=. --remote=origin --push
   ```
2. **Log into Render** ([dashboard.render.com](https://dashboard.render.com)).
3. Click **New +** -> **Blueprint**.
4. Connect your `SourceVault` GitHub repository.
5. Render reads `render.yaml` automatically:
   - Configures Web Service with Docker runtime.
   - Sets dynamic port `$PORT`.
   - Attaches a 1GB persistent disk at `/app/data` for the H2 database and archive storage.
6. Click **Apply**.
7. Once deployed, Render generates a global HTTPS URL:
   `https://sourcevault-xxxx.onrender.com`

### Option B: Manual Web Service on Render
- **Environment**: Docker
- **Build Command**: Uses root `Dockerfile`
- **Port**: `10000` (Render's internal port)
- **Disk**: Mount `/app/data` (1GB)

---

## 8. REST API Documentation

| Endpoint | Method | Description |
|---|---|---|
| `/api/v1/stats` | `GET` | Dashboard aggregate metrics and active browser status |
| `/api/v1/analysis` | `POST` | Inspect website URL, discover assets and source maps |
| `/api/v1/jobs` | `POST` | Launch an asynchronous resource collection job |
| `/api/v1/jobs` | `GET` | List paginated collection jobs |
| `/api/v1/jobs/{jobId}` | `GET` | Get job details and metrics |
| `/api/v1/jobs/{jobId}/progress` | `GET` | Get real-time progress, percentage, and events |
| `/api/v1/jobs/{jobId}/resources` | `GET` | Filter and paginate collected assets |
| `/api/v1/jobs/{jobId}/cancel` | `POST` | Cancel an in-progress collection job |
| `/api/v1/jobs/{jobId}/archive` | `GET` | Download the finalized ZIP archive |
| `/api/v1/jobs/{jobId}/manifest` | `GET` | Download `manifest.csv` |
| `/api/v1/jobs/{jobId}/failed-resources` | `GET` | Download `failed_resources.txt` |
| `/api/v1/browser/sessions` | `POST` | Register Chrome extension session |
| `/api/v1/browser/resources` | `POST` | Sync captured active tab resources |
| `/api/v1/browser/status` | `GET` | Check extension connectivity status |
| `/actuator/health` | `GET` | Service health check |

---

## 9. Verification & Automated Test Suite

All tests can be executed with `mvn test`:
- `SecurityUtilsTest`: SSRF loopback blocking, metadata address rejection, path sanitization.
- `ResourceDiscoveryParserTest`: HTML resource discovery, stylesheets, images, fonts, and source-map comments.
- `SourceMapReconstructorTest`: Extraction of `sources` and `sourcesContent` into file hierarchy.
- `ZipPackageBuilderAndValidatorTest`: Deflate ZIP generation, CRC32 reading, and manifest verification.
- `H2PersistenceAndJobLifecycleTest`: JPA entity mapping, status updates, and repository filtering.
- `FullPipelineIntegrationTest`: Embedded HTTP server serving HTML, CSS, JS, source map, images, fonts, JSON, and deliberately missing 404 resource; verifies entire pipeline end-to-end.
- `frontend.test.ts`: Vitest frontend test verifying URL validation, byte formatting, and breakdown aggregation.
