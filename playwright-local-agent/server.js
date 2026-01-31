#!/usr/bin/env node
/**
 * Playwright Local Agent - Run UI test scripts and record from GMN Test Platform locally.
 * Start with: npx playwright-local-agent  or  node server.js
 * Listens on http://localhost:9933
 * - GET /health -> { ok: true }
 * - POST /run  -> body { script: "..." } -> runs script, returns { success, stdout, stderr, error? }
 * - POST /start-recording -> body { targetUrl: "..." } -> runs codegen locally (browser opens on this machine)
 * - POST /stop-recording -> stops codegen
 * - GET /recording-code -> returns recorded code so far
 */

const http = require('http');
const fs = require('fs');
const path = require('path');
const { spawn } = require('child_process');
const os = require('os');

const PORT = parseInt(process.env.PLAYWRIGHT_AGENT_PORT || '9933', 10);

let recordingProcess = null;
let recordedCode = '';

function corsHeaders() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Content-Type': 'application/json; charset=utf-8'
  };
}

function send(res, status, obj) {
  res.writeHead(status, corsHeaders());
  res.end(JSON.stringify(obj));
}

function runScript(scriptContent) {
  return new Promise((resolve) => {
    const agentDir = path.resolve(__dirname);
    const runDir = path.join(agentDir, '.run-tmp');
    try {
      if (!fs.existsSync(runDir)) {
        fs.mkdirSync(runDir, { recursive: true });
      }
    } catch (_) {}
    const tmpFile = path.join(runDir, `run-${Date.now()}-${Math.random().toString(36).slice(2)}.js`);
    let stdout = '';
    let stderr = '';

    try {
      fs.writeFileSync(tmpFile, scriptContent, 'utf8');
    } catch (e) {
      resolve({ success: false, stdout: '', stderr: '', error: 'Failed to write temp file: ' + e.message });
      return;
    }

    const nodePath = path.join(agentDir, 'node_modules');
    const child = spawn(process.execPath, [tmpFile], {
      cwd: agentDir,
      stdio: ['ignore', 'pipe', 'pipe'],
      env: {
        ...process.env,
        ELECTRON_SKIP_BINARY_DOWNLOAD: '1',
        NODE_PATH: nodePath
      }
    });

    child.stdout.on('data', (d) => { stdout += d.toString(); });
    child.stderr.on('data', (d) => { stderr += d.toString(); });

    child.on('close', (code) => {
      try { fs.unlinkSync(tmpFile); } catch (_) {}
      resolve({
        success: code === 0,
        stdout: stdout.trim(),
        stderr: stderr.trim(),
        exitCode: code,
        error: code !== 0 ? (stderr || `Process exited with code ${code}`) : null
      });
    });

    child.on('error', (err) => {
      try { fs.unlinkSync(tmpFile); } catch (_) {}
      resolve({ success: false, stdout, stderr, error: err.message });
    });
  });
}

const server = http.createServer(async (req, res) => {
  if (req.method === 'OPTIONS') {
    res.writeHead(204, corsHeaders());
    res.end();
    return;
  }

  if (req.method === 'GET' && (req.url === '/' || req.url === '/health')) {
    send(res, 200, { ok: true, service: 'playwright-local-agent', port: PORT });
    return;
  }

  if (req.method === 'POST' && req.url === '/run') {
    let body = '';
    for await (const chunk of req) {
      body += chunk.toString();
    }
    let script;
    try {
      const json = JSON.parse(body || '{}');
      script = json.script;
    } catch (e) {
      send(res, 400, { success: false, error: 'Invalid JSON or missing script' });
      return;
    }
    if (typeof script !== 'string') {
      send(res, 400, { success: false, error: 'Missing or invalid "script" field' });
      return;
    }

    const result = await runScript(script);
    send(res, 200, result);
    return;
  }

  if (req.method === 'POST' && req.url === '/start-recording') {
    let body = '';
    for await (const chunk of req) {
      body += chunk.toString();
    }
    let targetUrl;
    try {
      const json = JSON.parse(body || '{}');
      targetUrl = json.targetUrl;
    } catch (e) {
      send(res, 400, { success: false, error: 'Invalid JSON or missing targetUrl' });
      return;
    }
    if (typeof targetUrl !== 'string' || !targetUrl.trim()) {
      send(res, 400, { success: false, error: 'targetUrl is required' });
      return;
    }
    if (recordingProcess) {
      send(res, 409, { success: false, error: 'Recording already in progress' });
      return;
    }
    recordedCode = '';
    const agentDir = path.resolve(__dirname);
    let codegenExitCode = null;
    let responseSent = false;
    function sendOnce(status, obj) {
      if (responseSent) return;
      responseSent = true;
      send(res, status, obj);
    }
    try {
      recordingProcess = spawn('npx', ['playwright', 'codegen', targetUrl.trim(), '--target', 'javascript', '--output', '-', '--headed'], {
        cwd: agentDir,
        stdio: ['ignore', 'pipe', 'pipe'],
        env: { ...process.env },
        shell: true,
        detached: true
      });
      recordingProcess.stdout.on('data', (d) => { recordedCode += d.toString(); });
      recordingProcess.stderr.on('data', (d) => { recordedCode += d.toString(); });
      recordingProcess.on('close', (code) => {
        codegenExitCode = code;
        recordingProcess = null;
      });
      recordingProcess.on('error', () => { recordingProcess = null; });
      if (recordingProcess.unref) {
        recordingProcess.unref();
      }
      // Wait 2.5s: if codegen exits with error (e.g. browsers not installed), return error to user
      setTimeout(() => {
        if (recordingProcess === null && codegenExitCode !== null && codegenExitCode !== 0) {
          const errText = (recordedCode && recordedCode.trim()) ? recordedCode.trim() : `Playwright codegen exited with code ${codegenExitCode}. Run "npx playwright install" in playwright-local-agent folder.`;
          sendOnce(500, { success: false, error: errText });
        } else {
          sendOnce(200, { success: true, message: 'Recording started. Browser should open on this machine.' });
        }
      }, 2500);
    } catch (e) {
      recordingProcess = null;
      sendOnce(500, { success: false, error: e.message });
    }
    return;
  }

  if (req.method === 'POST' && req.url === '/stop-recording') {
    if (recordingProcess) {
      recordingProcess.kill('SIGTERM');
      recordingProcess = null;
    }
    send(res, 200, { success: true, message: 'Recording stopped' });
    return;
  }

  if (req.method === 'GET' && req.url === '/recording-code') {
    send(res, 200, { success: true, code: recordedCode || '' });
    return;
  }

  send(res, 404, { error: 'Not found' });
});

server.listen(PORT, '127.0.0.1', () => {
  console.log(`Playwright Local Agent running at http://127.0.0.1:${PORT}`);
  console.log('Use GET /health to check, POST /run with {"script":"..."} to run a test.');
});

server.on('error', (err) => {
  if (err.code === 'EADDRINUSE') {
    console.error(`Port ${PORT} already in use. Set PLAYWRIGHT_AGENT_PORT to use another port.`);
  } else {
    console.error(err);
  }
  process.exit(1);
});
