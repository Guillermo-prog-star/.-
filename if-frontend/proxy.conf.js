const backendHost = process.env.BACKEND_URL || 'localhost';

const PROXY_CONFIG = {
  "/api": {
    "target": `http://${backendHost}:8080`,
    "secure": false,
    "changeOrigin": true
  },
  "/ws": {
    "target": `ws://${backendHost}:8080`,
    "secure": false,
    "ws": true
  }
};

module.exports = PROXY_CONFIG;

