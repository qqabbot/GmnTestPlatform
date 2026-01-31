# Playwright Local Agent

Run UI tests from GMN Test Platform **locally in your browser** (not only copy script).

## Where is this folder?

`playwright-local-agent` lives **inside the GMN Test Platform repo**, next to `backend/` and `frontend-app/`:

```
GmnTestPlatform/           ← repo root (run commands from here)
├── backend/
├── frontend-app/
├── playwright-local-agent/   ← this project
│   ├── package.json
│   ├── server.js
│   └── README.md
├── doc/
└── ...
```

Open a terminal in the **repo root** (the folder that contains `playwright-local-agent`), then run:

```bash
cd playwright-local-agent && npm install && npx playwright install && npm start
```

Or step by step:

```bash
cd playwright-local-agent
npm install              # installs this project's deps, including the Playwright npm package
npx playwright install   # downloads Chromium etc. — required before recording or run
npm start
```

- **`npm install`** installs this project’s dependencies, including the **Playwright** npm package.
- **`npx playwright install`** downloads Playwright browser binaries (Chromium, etc.). **Required** before recording or local run; otherwise the browser will not open.
- **`npm start`** starts the Local Agent server. **No browser opens at this step** — the agent just listens and waits for requests.

Agent listens on **http://127.0.0.1:9933**.

### When does the browser open?

The browser opens **only when you use the platform**:

1. Keep the agent running (the terminal where you ran `npm start`).
2. Open the GMN Test Platform in your browser (frontend URL, e.g. http://localhost:5173).
3. Go to a UI test case → click **Recording** → enter a **Target URL** (e.g. https://example.com) → click **Start Recording**.
4. The platform sends a request to the agent; the agent runs Playwright Codegen, and **a browser window opens on your computer** for recording.

- **Local Execution**: In the platform, choose **Local Execution** and click **Execute** — the test runs in a local browser when the agent is running.
- **Recording**: Click **Recording** → enter URL → **Start Recording** — the browser opens on this computer (the agent must be running first).

## Port

Default port: **9933**. Override with:

```bash
PLAYWRIGHT_AGENT_PORT=9944 npm start
```

## API

- **GET /health** — `{ ok: true }`
- **POST /run** — Body: `{ "script": "..." }` (Playwright Node script). Returns `{ success, stdout, stderr, error? }`

## Requirements

- Node.js 18+
- Playwright browsers: run **`npx playwright install`** in `playwright-local-agent` before first use.

### If the browser doesn't open when you click Start Recording

1. **Install browsers**: In a terminal, from the repo root run:
   ```bash
   cd playwright-local-agent && npx playwright install
   ```
2. **Restart the agent**: Stop the agent (Ctrl+C), then run `npm start` again.
3. **Start Recording again** in the platform. The browser should open on this computer.
4. Ensure the agent was started from a **terminal on this computer** (not over SSH).

### "Executable doesn't exist" / "chromium-xxxx/chrome-mac-arm64" error

Playwright’s Chromium is missing or incomplete. From the **playwright-local-agent** directory run:

```bash
cd playwright-local-agent
npx playwright install chromium
```

If it still fails, reinstall with dependencies (macOS may need system libs):

```bash
npx playwright install chromium --with-deps
```

Then restart the agent (`npm start`) and try Local Execution or Recording again.
