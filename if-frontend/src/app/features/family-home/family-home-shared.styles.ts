/**
 * Estilos compartidos "dark-glass" para el Hogar Digital (Hito 7 IFRM-D).
 * Reutiliza el lenguaje visual ya establecido en profile-page.component.ts
 * (glass-card, skeleton shimmer) para que esta ruta nueva no introduzca un
 * sistema visual paralelo.
 */
export const FAMILY_HOME_SHARED_STYLES = `
  :host { display: block; }

  .glass-card {
    background: rgba(255,255,255,0.03);
    border: 1px solid rgba(255,255,255,0.08);
    border-radius: 24px;
    padding: 28px;
    backdrop-filter: blur(20px);
  }

  .section-title {
    font-size: 12px;
    font-weight: 700;
    color: rgba(255,255,255,0.3);
    text-transform: uppercase;
    letter-spacing: 0.08em;
    margin: 0 0 16px;
  }

  .fh-progress-wrap {
    display: flex;
    align-items: center;
    gap: 10px;
    margin: 16px 0;
  }
  .fh-progress-bar {
    flex: 1;
    height: 6px;
    background: rgba(255,255,255,0.07);
    border-radius: 3px;
    overflow: hidden;
  }
  .fh-progress-fill {
    height: 100%;
    background: linear-gradient(90deg, #6366f1, #818cf8);
    border-radius: 3px;
    transition: width .4s;
  }
  .fh-progress-pct {
    font-size: 12px;
    font-weight: 700;
    color: #818cf8;
    flex-shrink: 0;
  }

  .fh-cta {
    padding: 12px 24px;
    background: rgba(99,102,241,0.15);
    border: 1px solid rgba(99,102,241,0.3);
    border-radius: 12px;
    color: #a5b4fc;
    font-size: 14px;
    font-weight: 700;
    cursor: pointer;
    transition: all 0.2s;
  }
  .fh-cta:hover:not(:disabled) {
    background: rgba(99,102,241,0.28);
    border-color: rgba(99,102,241,0.5);
    transform: translateY(-1px);
  }
  .fh-cta:disabled {
    opacity: 0.5;
    cursor: not-allowed;
  }

  .fh-narrative {
    font-size: 15px;
    line-height: 1.6;
    color: rgba(255,255,255,0.85);
    margin: 0 0 20px;
  }

  .fh-dimensions-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
  }
  .fh-dimension-chip {
    display: flex;
    align-items: center;
    gap: 10px;
    padding: 12px 14px;
    background: rgba(255,255,255,0.03);
    border: 1px solid rgba(255,255,255,0.06);
    border-radius: 12px;
    font-size: 12px;
  }
  .fh-dimension-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    flex-shrink: 0;
  }
  .fh-dimension-dot.improving { background: #34d399; }
  .fh-dimension-dot.stable { background: #818cf8; }
  .fh-dimension-dot.declining { background: #f87171; }
  .fh-dimension-dot.insufficient_data { background: rgba(255,255,255,0.2); }
  .fh-dimension-label { color: rgba(255,255,255,0.6); font-weight: 600; }

  .fh-sprint-block {
    display: flex;
    flex-direction: column;
    gap: 10px;
    padding: 16px 20px;
    background: rgba(99,102,241,0.06);
    border: 1px solid rgba(99,102,241,0.15);
    border-radius: 16px;
    margin-bottom: 20px;
  }
  .fh-sprint-title { font-size: 14px; font-weight: 700; color: #fff; }

  .fh-safety-banner {
    display: flex;
    align-items: center;
    gap: 14px;
    padding: 14px 20px;
    border-radius: 16px;
    border-left: 3px solid;
    margin-bottom: 20px;
  }
  .fh-safety-banner.support_available { background: rgba(251,191,36,0.06); border-color: #fbbf24; }
  .fh-safety-banner.contact_professional,
  .fh-safety-banner.urgent_guidance { background: rgba(239,68,68,0.06); border-color: #f87171; }
  .fh-safety-icon { font-size: 24px; flex-shrink: 0; }
  .fh-safety-text strong { display: block; font-size: 14px; font-weight: 700; color: #fff; margin-bottom: 2px; }
  .fh-safety-text span { font-size: 12px; color: rgba(255,255,255,0.5); }

  .fh-toast {
    padding: 10px 16px;
    background: rgba(255,255,255,0.06);
    border: 1px solid rgba(255,255,255,0.12);
    border-radius: 10px;
    font-size: 12px;
    color: rgba(255,255,255,0.6);
    margin-bottom: 16px;
  }

  .skeleton {
    background: linear-gradient(90deg,
      rgba(255,255,255,0.04) 25%,
      rgba(255,255,255,0.08) 50%,
      rgba(255,255,255,0.04) 75%);
    background-size: 200% 100%;
    animation: shimmer 1.5s infinite;
    border-radius: 8px;
  }
  @keyframes shimmer {
    0%   { background-position: 200% 0; }
    100% { background-position: -200% 0; }
  }
  .skeleton-line { height: 14px; margin-bottom: 8px; }
  .skeleton-line.lg { height: 20px; width: 60%; }
`;
