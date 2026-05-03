USE integrity_family;
-- 1. Forzar activación del Sentinel
UPDATE families SET sentinel_active = 1 WHERE id = 2;
-- 2. Inyectar Snapshot de ruptura (ICF 85.0) para obligar al Watchdog a reaccionar
DELETE FROM risk_snapshots WHERE icf > 80;
INSERT INTO risk_snapshots (family_id, icf, risk_level, consciousness_level, has_crisis, created_at)
VALUES (2, 85.0, 'EMERGENCIA_TOTAL', 1, 1, NOW());