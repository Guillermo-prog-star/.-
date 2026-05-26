const PROXY_CONFIG = {
  "/api": {
    "target": `http://${process.env.BACKEND_URL || '127.0.0.1'}:8080`,
    "secure": false,
    "changeOrigin": true,
    "logLevel": "debug"
  },
  "/ws": {
    "target": `ws://${process.env.BACKEND_URL || '127.0.0.1'}:8080`,
    "secure": false,
    "ws": true
  }
};

module.exports = PROXY_CONFIG;
