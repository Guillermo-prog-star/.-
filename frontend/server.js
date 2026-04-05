const http = require('http');
const fs = require('fs');
const path = require('path');

const PORT = 4200;
// Buscamos la carpeta donde Angular deja el index.html (suele ser dist/frontend/browser o dist)
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
    let filePath = path.join(STATIC_PATH, req.url === '/' ? 'index.html' : req.url);
    
    // Lógica SPA: Si el archivo no existe, servimos index.html para que el Router de Angular decida
    if (!fs.existsSync(filePath) || fs.statSync(filePath).isDirectory()) {
        filePath = path.join(STATIC_PATH, 'index.html');
    }

    const ext = path.extname(filePath).toLowerCase();
    res.writeHead(200, { 'Content-Type': MIME_TYPES[ext] || 'application/octet-stream' });
    fs.createReadStream(filePath).pipe(res);
});

// BINDING CRÍTICO A 0.0.0.0 PARA DOCKER
server.listen(PORT, '0.0.0.0', () => {
    console.log(`✅ Frontend disponible en: http://localhost:${PORT}`);
    console.log(`📂 Sirviendo desde: ${STATIC_PATH}`);
});