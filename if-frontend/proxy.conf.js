const PROXY_CONFIG = {
  "/api": {
    "target": `http://${process.env.BACKEND_URL || '127.0.0.1'}:8080`,
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  }
};

module.exports = PROXY_CONFIG;
