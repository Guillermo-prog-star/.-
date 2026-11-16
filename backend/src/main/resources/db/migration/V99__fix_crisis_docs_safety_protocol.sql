-- V99: Corrige documentación que describía un flujo aspiracional no implementado
-- ("Scanner -> AlertEngine -> ErrorProtocolService activa protocolo" en CU-10 de
-- PT-CU-01) para reflejar el mecanismo real construido en V98:
-- SafetyProtocolActivation, activado manualmente por un humano (Guardián/profesional),
-- distinto de FamilyErrorProtocol (ritual para misiones fallidas, no para crisis).

UPDATE project_documents
SET content = REPLACE(
    content,
    '## CU-10: Gestión de Crisis\n**Actor**: Sistema\n**Flujo**: Scanner detecta señales de riesgo → AlertEngine clasifica → ErrorProtocolService activa protocolo → notifica al Copilot y al creador.\n',
    '## CU-10: Gestión de Crisis\n**Actor**: Sistema / Guardián / Profesional\n**Flujo**: Scanner detecta señales de riesgo → AlertEngine clasifica → si la trayectoria tiene requires_safety_protocol=true, el sistema notifica al Guardián/creador → el Guardián o profesional confirma responsable, acción inicial y fecha de seguimiento → se activa un SafetyProtocolActivation (requiere confirmación humana, nunca automático) → el Mentor de IA recibe la trayectoria como contexto prioritario.\n\n## CU-11: Gestión de Error Familiar\n**Actor**: Creador / Miembro\n**Flujo**: Una misión falla → familia inicia un FamilyErrorProtocol → ciclo Detectar-Sentir-Comprender-Accionar-Acordar-Seguimiento-Aprender → se cierra registrando el aprendizaje. Distinto de CU-10: este protocolo procesa fallos cotidianos, no situaciones de riesgo vital o legal.\n'
)
WHERE code = 'PT-CU-01';

UPDATE project_documents
SET content = REPLACE(
    content,
    'Siempre documentar: fecha de detección, acción realizada, persona responsable del seguimiento.\nEl Mentor de IA incluirá estas trayectorias críticas como contexto prioritario en cada sesión.',
    'Mecanismo real: POST /api/trajectories/family/{id}/safety-protocol (SafetyProtocolActivation) exige responsible_id, initial_action y follow_up_date — no se puede activar sin los tres. Requiere confirmación humana del Guardián o profesional; nunca se dispara solo en segundo plano.\nEl Mentor de IA incluirá estas trayectorias críticas como contexto prioritario en cada sesión.'
),
    version = '1.2'
WHERE code = 'DOC-TRAY-002';
