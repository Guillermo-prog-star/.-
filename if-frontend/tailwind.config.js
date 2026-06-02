/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    extend: {
      colors: {
        // Deep Night base
        'deep-night': '#020617',
        'glass-bg': 'rgba(255, 255, 255, 0.05)',

        // ── Integrity Family — Semántica de módulos ──────────────────
        // Sistema: Configuración, Dashboard, Administración
        'if-system': {
          DEFAULT: '#3B82F6',
          deep:    '#1E3A8A',
          soft:    'rgba(59, 130, 246, 0.12)',
        },
        // Familia: Transformación Diaria, Misiones, Guardián
        'if-family': {
          DEFAULT: '#F59E0B',
          soft:    'rgba(245, 158, 11, 0.12)',
        },
        // Evolución: Plan, Ruta 36 Meses, Logros
        'if-evolution': {
          DEFAULT: '#22C55E',
          soft:    'rgba(34, 197, 94, 0.12)',
        },
        // Crisis: SOLO alertas Sentinel y Crisis Familiar
        'if-crisis': {
          DEFAULT: '#EF4444',
          soft:    'rgba(239, 68, 68, 0.12)',
        },
        // Legado: Gratitud, Constitución, Historia
        'if-legacy': {
          DEFAULT: '#D4AF37',
          soft:    'rgba(212, 175, 55, 0.12)',
        },
        // Inteligencia: Consultor IA, Cognitivo
        'if-intel': {
          DEFAULT: '#8B5CF6',
          soft:    'rgba(139, 92, 246, 0.12)',
        },
        // Diagnóstico: Evaluaciones, Panel Clínico
        'if-diagnosis': {
          DEFAULT: '#06B6D4',
          soft:    'rgba(6, 182, 212, 0.12)',
        },
      },
      animation: {
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
      }
    },
  },
  plugins: [],
}
