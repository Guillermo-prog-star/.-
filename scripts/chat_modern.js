import fs from "fs";
import fsPromises from 'fs/promises';
import path from "path";
import dotenv from "dotenv";
import readline from "readline";
import crypto from "crypto";
import fetch from "node-fetch";

// --- Módulos de Comunicación (Simulados o Reales) ---
// Nota: Requieren whatsapp.js y email.js en el mismo directorio
const wa = {
    iniciar: async () => console.log("🟢 WhatsApp Service: Inicializado"),
    enviarMensaje: async (num, msg) => console.log(`📩 Enviando WA a ${num}: ${msg.slice(0, 50)}...`),
    enviarBienvenida: async (num, perfil) => console.log(`👋 Bienvenida enviada a ${perfil.nombreRef}`)
};

dotenv.config();

// Configuración Global y Seguridad
const CONFIG = {
    API_KEY: process.env.ANTHROPIC_API_KEY,
    MODEL: "claude-3-5-sonnet-20241022",
    ADMIN_KEY: process.env.ADMIN_KEY || "IF-2026-SECURE",
    DATA_DIR: path.resolve("./data"),
    FAMILIAS_DIR: path.resolve("./data/familias"),
    GLOBAL_FILE: path.join(path.resolve("./data"), "familias.json")
};

if (!CONFIG.API_KEY) {
    console.error("❌ Error: ANTHROPIC_API_KEY no detectada. Abortando.");
    process.exit(1);
}

const rl = readline.createInterface({ input: process.stdin, output: process.stdout });

// ═══════════════════════════════════════════════════════════════
// ARQUITECTURA DE LOS 8 PILARES (SINCRO JAVA)
// ═══════════════════════════════════════════════════════════════
const HITOS = {
    MES_00_DIAGNOSTICO_BASE:    { label: "📍 Inicio", meses: 0, fase: "Diagnóstico Base", color: "🔵" },
    MES_03_PRIMEROS_CAMBIOS:    { label: "🌱 3 Meses", meses: 3, fase: "Primeros Cambios", color: "🟢" },
    MES_06_CONSOLIDACION_INICIAL: { label: "🌳 6 Meses", meses: 6, fase: "Consolidación Inicial", color: "🟡" },
    MES_12_PRIMERA_TRANSFORMACION: { label: "🛡️ 12 Meses", meses: 12, fase: "Primera Transformación", color: "🟠" },
    MES_18_PROFUNDIZACION:       { label: "🚀 18 Meses", meses: 18, fase: "Profundización", color: "🔴" },
    MES_24_MADUREZ_SISTEMA:      { label: "💎 24 Meses", meses: 24, fase: "Madurez del Sistema", color: "🟣" },
    MES_30_CIERRE_SOSTENIMIENTO: { label: "📊 30 Meses", meses: 30, fase: "Cierre y Sostenimiento", color: "⭐" },
    MES_36_TRANSFORMACION_COMPLETA: { label: "🏆 36 Meses", meses: 36, fase: "Transformación Completa", color: "🏆" }
};

const SECUENCIA_HITOS = Object.keys(HITOS);

// ═══════════════════════════════════════════════════════════════
// UTILIDADES DE SISTEMA
// ═══════════════════════════════════════════════════════════════
const utils = {
    ensureDir: (p) => { if (!fs.existsSync(p)) fs.mkdirSync(p, { recursive: true }); },
    safeRead: (p, fall) => { try { return JSON.parse(fs.readFileSync(p, "utf-8")); } catch { return fall; } },
    preguntar: (t) => new Promise(res => rl.question(t, res)),
    hash: (t) => crypto.createHash("sha256").update(t).digest("hex"),
    rutas: (codigo) => {
        const dir = path.join(CONFIG.FAMILIAS_DIR, codigo);
        return {
            dir,
            perfil: path.join(dir, "perfil.json"),
            respuestas: path.join(dir, "respuestas.json"),
            checklist: path.join(dir, "checklist.json"),
            audit: path.join(dir, "auditoria.txt")
        };
    }
};

// ═══════════════════════════════════════════════════════════════
// IA: MOTOR CLAUDE OPTIMIZADO
// ═══════════════════════════════════════════════════════════════
async function callClaude(prompt, perfil, context = "") {
    const system = `Eres un Consultor de Integrity Family. 
    FAMILIA: ${perfil.nombreRef} (${perfil.codigo})
    HITO ACTUAL: ${perfil.pilarActual}
    CONTEXTO: ${context}
    Responde con Pedagogía de la Tensión (Sepúlveda). No des soluciones fáciles, busca que la familia trabaje en sus acuerdos.`;

    try {
        const r = await fetch("https://api.anthropic.com/v1/messages", {
            method: "POST",
            headers: { 
                "Content-Type": "application/json", 
                "x-api-key": CONFIG.API_KEY, 
                "anthropic-version": "2023-06-01" 
            },
            body: JSON.stringify({ 
                model: CONFIG.MODEL, 
                max_tokens: 1500, 
                system, 
                messages: [{ role: "user", content: prompt }] 
            })
        });
        const data = await r.json();
        return data.content[0].text;
    } catch (e) {
        return `⚠️ Error IA: ${e.message}`;
    }
}

// ═══════════════════════════════════════════════════════════════
// MÓDULOS DE NEGOCIO
// ═══════════════════════════════════════════════════════════════
async function registrarFamilia() {
    console.log("\n[NUEVO REGISTRO: NODO ARMENIA]");
    const nombreRef = await utils.preguntar("Nombre referencia (alias): ");
    const whatsapp  = await utils.preguntar("WhatsApp (57...): ");
    
    const año       = new Date().getFullYear();
    const global    = utils.safeRead(CONFIG.GLOBAL_FILE, { contador: 0 });
    const codigo    = `IF-CO-QUI-${año}-${String(global.contador + 1).padStart(4, "0")}`;
    
    const pin       = await utils.preguntar("Especifique PIN de 4 dígitos: ");
    
    const perfil = {
        codigo, nombreRef, 
        contacto: { whatsapp },
        pinHash: utils.hash(pin.trim()),
        pilarActual: "MES_00_DIAGNOSTICO_BASE",
        fechaRegistro: new Date().toISOString()
    };

    const r = utils.rutas(codigo);
    utils.ensureDir(r.dir);
    
    await fsPromises.writeFile(r.perfil, JSON.stringify(perfil, null, 2));
    await fsPromises.writeFile(r.checklist, JSON.stringify({ actividades: [] }, null, 2));
    
    global.contador++;
    await fsPromises.writeFile(CONFIG.GLOBAL_FILE, JSON.stringify(global, null, 2));

    console.log(`\n✅ PROTOCOLO ACTIVADO: ${codigo}`);
    await wa.enviarBienvenida(whatsapp, perfil);
    return { perfil, rutas: r };
}

async function verChecklist(rutas) {
    const data = utils.safeRead(rutas.checklist, { actividades: [] });
    const pendientes = data.actividades.filter(a => !a.completada);
    
    console.log("\n--- CHECKLIST FAMILIAR ---");
    if (pendientes.length === 0) {
        console.log("🌟 ¡Felicidades! No hay tareas pendientes.");
    } else {
        pendientes.forEach((a, i) => console.log(`${i+1}. [ ] ${a.titulo}`));
    }
}

// ═══════════════════════════════════════════════════════════════
// INICIO DEL SISTEMA (LOOP)
// ═══════════════════════════════════════════════════════════════
async function start() {
    utils.ensureDir(CONFIG.DATA_DIR);
    utils.ensureDir(CONFIG.FAMILIAS_DIR);

    console.log("\nINTEGRITY FAMILY OMNI-CHANNEL v7.0");
    const clave = await utils.preguntar("🔐 Clave de Admin: ");
    if (clave.trim() !== CONFIG.ADMIN_KEY) process.exit(1);

    await wa.iniciar();

    while (true) {
        console.log("\n1. Registrar Nueva Familia\n2. Acceder a Núcleo Existente\n3. Salir");
        const op = await utils.preguntar("Opción: ");

        if (op === "3") break;

        let session = null;
        if (op === "1") {
            session = await registrarFamilia();
        } else {
            const cod = (await utils.preguntar("Código: ")).toUpperCase();
            const r = utils.rutas(cod);
            if (fs.existsSync(r.perfil)) {
                const p = utils.safeRead(r.perfil);
                const pin = await utils.preguntar("PIN: ");
                if (utils.hash(pin.trim()) === p.pinHash) {
                    session = { perfil: p, rutas: r };
                    console.log(`🎉 Acceso concedido a: ${p.nombreRef}`);
                }
            }
        }

        if (session) {
            let interactivo = true;
            while (interactivo) {
                const msg = await utils.preguntar(`\n[${session.perfil.codigo}] 👤: `);
                if (msg === "salir" || msg === "menu") {
                    interactivo = false;
                } else if (msg === "checklist") {
                    await verChecklist(session.rutas);
                } else {
                    const resp = await callClaude(msg, session.perfil);
                    console.log(`\n🤖: ${resp}`);
                }
            }
        }
    }
    process.exit(0);
}

start();
