// C:\Proyectos\if-full\if-frontend\proxy.conf.js

const PROXY_CONFIG = {
  "/api": {
    "target": "http://127.0.0.1:8080",
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug",
    "onError": function (err, req, res) {
      console.error(`[Proxy Error] Falla en ${req.url}:`, err.message);
    }
  }
};

module.exports = PROXY_CONFIG;