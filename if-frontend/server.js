const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 4200;
const BACKEND_URL = process.env.BACKEND_URL || 'if-backend';
const BACKEND_PORT = 8080;

const findDistPath = (dir) => {
    const files = fs.readdirSync(dir);
    if (files.includes('index.html')) return dir;
    for (const file of files) {
        const fullPath = path.join(dir, file);
        if (fs.statSync(fullPath).isDirectory()) {
            const found = findDistPath(fullPath);
            if (found) return found;
        }
    }
    return null;
};

const BASE_DIST = path.join(__dirname, 'dist');
const STATIC_PATH = findDistPath(BASE_DIST) || BASE_DIST;

const MIME_TYPES = {
    '.html': 'text/html', '.js': 'text/javascript', '.css': 'text/css',
    '.json': 'application/json', '.png': 'image/png', '.jpg': 'image/jpg',
    '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.woff2': 'font/woff2'
};

const server = http.createServer((req, res) => {
    if (req.url.startsWith('/api')) {
        // [SDD] Limpieza de headers para evitar conflictos de Host
        const headers = { ...req.headers };
        delete headers.host;
        delete headers.connection;

        const proxyReq = http.request({
            host: BACKEND_URL,
            port: BACKEND_PORT,
            path: req.url,
            method: req.method,
            headers: headers
        }, (proxyRes) => {
            res.writeHead(proxyRes.statusCode, proxyRes.headers);
            proxyRes.pipe(res);
        });

        req.pipe(proxyReq);
        proxyReq.on('error', (err) => {
            console.error('❌ Proxy error:', err.message);
            res.writeHead(502);
            res.end('Backend no alcanzable');
        });
        return;
    }

    // Servir el index.html estándar de Angular
    let filePath = path.join(STATIC_PATH, req.url === '/' ? 'index.html' : req.url);
    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        filePath = path.join(STATIC_PATH, 'index.html');
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, { 
        'Content-Type': MIME_TYPES[ext] || 'application/octet-stream',
        'Cache-Control': 'no-store, no-cache, must-revalidate, proxy-revalidate',
        'Pragma': 'no-cache',
        'Expires': '0',
        'Surrogate-Control': 'no-store'
    });
    fs.createReadStream(filePath).pipe(res);
});

server.listen(PORT, '0.0.0.0', () => {
    console.log(`✅ Frontend disponible en: http://localhost:${PORT}`);
    console.log(`📡 Proxy configurado hacia: http://${BACKEND_URL}:${BACKEND_PORT}`);
});