import http from 'http';
import https from 'https';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = Number(process.env.PORT || 3000);
const HOST = process.env.IP || '0.0.0.0';

const BACKEND_HOST = process.env.BACKEND_HOST || 'services-tanyouwei.alwaysdata.net';
const BACKEND_PORT = Number(process.env.BACKEND_PORT || 8380);
const BACKEND_ORIGIN = `http://${BACKEND_HOST}:${BACKEND_PORT}`;

const DIST_DIR = path.join(__dirname, 'dist');

const MIME_TYPES = {
  '.html': 'text/html; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.mjs': 'application/javascript; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.gif': 'image/gif',
  '.ico': 'image/x-icon',
  '.woff': 'font/woff',
  '.woff2': 'font/woff2',
  '.ttf': 'font/ttf',
  '.map': 'application/json; charset=utf-8',
};

function send(res, status, body, contentType = 'text/plain; charset=utf-8') {
  res.writeHead(status, {
    'Content-Type': contentType,
    'Content-Length': Buffer.byteLength(body),
  });
  res.end(body);
}

function sendStream(res, status, stream, headers = {}) {
  res.writeHead(status, headers);
  stream.pipe(res);
}

function proxyApi(req, res) {
  const options = {
    hostname: BACKEND_HOST,
    port: BACKEND_PORT,
    path: req.url,
    method: req.method,
    headers: {
      ...req.headers,
      host: `${BACKEND_HOST}:${BACKEND_PORT}`,
      origin: BACKEND_ORIGIN,
      referer: BACKEND_ORIGIN,
      connection: 'close',
    },
  };

  const proxyReq = http.request(options, (proxyRes) => {
    const responseHeaders = { ...proxyRes.headers };
    delete responseHeaders['content-length'];
    delete responseHeaders['transfer-encoding'];
    delete responseHeaders['connection'];
    res.writeHead(proxyRes.statusCode, responseHeaders);
    proxyRes.pipe(res);
  });

  proxyReq.on('error', (err) => {
    const body = JSON.stringify({ error: 'Backend unavailable', detail: err.message });
    send(res, 502, body, 'application/json; charset=utf-8');
  });

  proxyReq.on('timeout', () => proxyReq.destroy());
  proxyReq.setTimeout(60000);

  req.pipe(proxyReq);
}

function serveStatic(req, res) {
  const urlPath = decodeURIComponent(req.url.split('?')[0]);

  let filePath;
  if (urlPath === '/') {
    filePath = path.join(DIST_DIR, 'index.html');
  } else {
    filePath = path.normalize(path.join(DIST_DIR, urlPath));
    if (!filePath.startsWith(DIST_DIR)) {
      return send(res, 403, 'Forbidden');
    }
  }

  fs.stat(filePath, (err, stat) => {
    if (!err && stat.isFile()) {
      const ext = path.extname(filePath).toLowerCase();
      const contentType = MIME_TYPES[ext] || 'application/octet-stream';
      const stream = fs.createReadStream(filePath);
      stream.on('error', () => send(res, 500, 'Read Error'));
      return sendStream(res, 200, stream, {
        'Content-Type': contentType,
        'Cache-Control': ext === '.html' ? 'no-store' : 'public, max-age=31536000, immutable',
      });
    }

    // SPA fallback：文件不存在就返回 index.html
    const fallbackPath = path.join(DIST_DIR, 'index.html');
    fs.stat(fallbackPath, (ferr, fstat) => {
      if (ferr || !fstat.isFile()) {
        return send(res, 404, 'Not Found');
      }
      const stream = fs.createReadStream(fallbackPath);
      stream.on('error', () => send(res, 500, 'Read Error'));
      sendStream(res, 200, stream, {
        'Content-Type': MIME_TYPES['.html'],
        'Cache-Control': 'no-store',
      });
    });
  });
}

const server = http.createServer((req, res) => {
  if (req.url.startsWith('/api/') || req.url === '/api') {
    return proxyApi(req, res);
  }
  serveStatic(req, res);
});

server.listen(PORT, HOST, () => {
  console.log(`Frontend server running on ${HOST}:${PORT}`);
});
