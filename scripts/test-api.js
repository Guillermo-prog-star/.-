// PATH: scripts/test-api.js

/**
 * ESPECIFICACIÓN: Validación de conectividad Integrity Family.
 * Requiere: node-fetch @v2 para compatibilidad con require.
 */
const fetch = require('node-fetch');
const { apiUrl, token } = require('../env.config.js');

async function checkConnection() {
    console.log(`🔍 Intentando conectar a: ${apiUrl}/status`);

    try {
        const response = await fetch(`${apiUrl}/status`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${token}`,
                'Content-Type': 'application/json'
            }
        });

        if (response.ok) {
            const data = await response.json();
            console.log('✅ CONEXIÓN EXITOSA:', data);
        } else {
            console.error(`⚠️ EL SERVIDOR RESPONDIÓ: ${response.status}`);
        }
    } catch (error) {
        console.error('❌ FALLO DE RED:', error.message);
        console.log('💡 TIP: Verifica que el Backend de Java esté corriendo.');
    }
}

checkConnection();