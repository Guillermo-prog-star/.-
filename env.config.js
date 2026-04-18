// PATH: C:\Proyectos\if-full\env.config.js
require('dotenv').config();

const config = {
    apiUrl: process.env.API_URL || 'http://localhost:8080/api/v1',
    token: process.env.FAMILY_TOKEN,
    aiKey: process.env.ANTHROPIC_API_KEY
};

// Validación Crítica (Anti-Vibe Coding)
if (!config.aiKey) {
    console.error("❌ ERROR: ANTHROPIC_API_KEY no encontrada en .env");
    process.exit(1);
}

module.exports = config;