// PATH: scripts/test-api.js
const fetch = require('node-fetch'); // Opcional en Node 18+
const { apiUrl, token } = require('../env.config.js');

async function checkConnection() {
    console.log(`🔍 Probando conexión a: ${apiUrl}/status`);
    try {
        const response = await fetch(`${apiUrl}/status`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error(`HTTP Error: ${response.status}`);
        }

        const data = await response.json();
        console.log('✅ Estado de Integrity Family:', data);
    } catch (error) {
        console.error('❌ Error de conexión:', error.message);
        console.log('💡 Tip: Asegúrate de que el backend de Spring Boot esté corriendo en el puerto 8080.');
    }
}

checkConnection();