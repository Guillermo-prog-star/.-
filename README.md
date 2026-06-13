# Integrity Family — MVP Completo

Sistema de bienestar familiar con IA (Claude), Spring Boot, Angular y MySQL.

## Stack tecnológico

| Capa | Tecnología |
|---|---|
| Backend | Spring Boot 3.4.3 + Java 21 |
| Frontend | Angular 17 (Standalone components) |
| Base de datos | MySQL 8.4 |
| Mensajería | RabbitMQ 3 |
| IA | Claude (Anthropic API) |
| Contenedores | Docker + Docker Compose |

---

## Inicio rápido

### Prerrequisitos

- Docker Desktop instalado y corriendo
- Clave API de Anthropic: [console.anthropic.com](https://console.anthropic.com)

### Levantar todo con Docker Compose

```bash
# 1. Entra a la carpeta infra
cd infra

# 2. Configura tu clave de Claude
export ANTHROPIC_API_KEY=sk-ant-tu-clave-aqui

# 3. Levanta todo
docker compose up --build

# 4. Espera ~90 segundos a que todo esté listo
```

### Acceso

| Servicio | URL |
|---|---|
| Frontend Angular | http://localhost:4200 |
| Backend Swagger | http://localhost:8080/swagger-ui.html |
| RabbitMQ Panel | http://localhost:15672 (guest/guest) |

### Credenciales demo

```
Email:    admin@integrityfamily.com
Password: Admin123*
```

---

## Desarrollo local (sin Docker)

### Backend

```bash
cd backend

# Configurar API key
export ANTHROPIC_API_KEY=sk-ant-tu-clave

# Levantar solo infraestructura
cd ../infra && docker compose up mysql rabbitmq -d && cd ../backend

# Correr backend
mvn spring-boot:run
```

### Frontend

```bash
cd if-frontend
npm install
ng serve
```

Abre http://localhost:4200

---

## Flujo principal del sistema

```
1. Login / Registro
2. Crear o seleccionar familia  →  código IF-CO-QUI-2026-XXXX
3. Registrar miembros           →  autonomía + responsabilidad
4. Iniciar evaluación           →  preguntas por hito/bloque
5. Responder evaluación         →  escala 1–5 por dimensión
6. Ver resultado                →  análisis real generado por Claude
7. Ver plan generado            →  automático vía RabbitMQ
8. Gestionar checklist          →  derivado del plan o manual
9. Consultar IA                 →  chat libre con Claude
10. Dashboard                   →  resumen ejecutivo del estado familiar
```

---

## Módulos del sistema

### Backend (Spring Boot)

| Módulo | Endpoints principales |
|---|---|
| Auth | POST /api/auth/login · POST /api/auth/register |
| Families | CRUD /api/families |
| Members | CRUD /api/members · GET /api/members/family/{id} |
| Milestones | GET /api/milestones · GET /api/milestones/family/{id}/current |
| Evaluations | POST /api/evaluations/start · POST /api/evaluations/{id}/finalize |
| Risk | GET /api/risk/family/{id}/history |
| Plans | GET /api/plans/family/{id} · PUT /api/plans/tasks/{id}/complete |
| Checklist | GET/POST /api/checklist · POST /api/checklist/generate-from-plan |
| Chat IA | POST /api/chat |
| Analytics | GET /api/analytics/dashboard/family/{id} |

### Frontend (Angular)

| Ruta | Componente |
|---|---|
| /login | LoginPageComponent |
| /register | RegisterPageComponent |
| /dashboard | DashboardPageComponent |
| /families | FamilyListPageComponent |
| /families/create | FamilyCreatePageComponent |
| /members | MemberListPageComponent |
| /evaluations/start | EvaluationStartPageComponent |
| /evaluations/:id/form | EvaluationFormPageComponent |
| /evaluations/:id/result | EvaluationResultPageComponent |
| /plans | PlanListPageComponent |
| /checklist | ChecklistPageComponent |
| /chat | ChatPageComponent |

---

## Arquitectura del sistema

```
Angular Frontend (4200)
       │ HTTP + JWT
       ▼
Spring Boot Backend (8080)
  ├── Auth (JWT)
  ├── Evaluation → ScoringService → RiskService
  │                       └── AiService (Claude API)
  │                               └── EventPublisher → RabbitMQ
  │                                           └── EvaluationCompletedConsumer
  │                                                   └── PlanGenerationService
  └── Analytics ← todos los módulos
       │
MySQL 8.4 (3306)    RabbitMQ (5672)
```

---

## Filosofía del sistema

> Cada miembro debe tener autonomía e independencia, pero también compromiso y responsabilidad frente a su propia vida y la de los demás, para avanzar y progresar siempre juntos.

Esta filosofía se refleja directamente en la arquitectura:

- **Autonomía**: cada miembro responde su propia evaluación
- **Responsabilidad**: las respuestas impactan el estado real del sistema
- **Compromiso**: el plan genera tareas concretas con responsables
- **Progreso conjunto**: el dashboard muestra el avance colectivo

---

## Hitos del sistema (36 meses)

| Hito | Meses | Fase | Bloque |
|---|---|---|---|
| inicio | 0 | Diagnóstico Base | reconocimiento |
| m03 | 3 | Primeros Cambios | reconocimiento |
| m06 | 6 | Consolidación Inicial | amor |
| m12 | 12 | Primera Transformación | amor |
| m18 | 18 | Profundización | entrega |
| m24 | 24 | Madurez del Sistema | entrega |
| m30 | 30 | Cierre y Sostenimiento | entrega |
| m36 | 36 | Transformación Completa | entrega |

---

## Variables de entorno

| Variable | Descripción | Default |
|---|---|---|
| ANTHROPIC_API_KEY | Clave API de Claude | requerida |
| DB_HOST | Host de MySQL | localhost |
| DB_USER | Usuario MySQL | root |
| DB_PASS | Contraseña MySQL | root123 |
| RABBIT_HOST | Host de RabbitMQ | localhost |

## Actualizaciones recientes

- Se añadió la dependencia **H2** en `pom.xml` para que los tests usen una base de datos en‑memory.
- Se introdujo la constante `SIMULATION_MESSAGE` en `WhisperSttService` para unificar el mensaje de simulación y evitar fallos de pruebas.

---

## Solución de problemas

**Puerto 3306 ocupado:**
```bash
# Cambia el puerto en docker-compose.yml
ports: ["3307:3306"]
# Y actualiza DB_HOST URL en backend
```

**Flyway error en migraciones:**
```bash
docker compose exec mysql mysql -uroot -proot123 \
  -e "DROP DATABASE integrity_family; CREATE DATABASE integrity_family;"
```

**Claude devuelve texto de respaldo:**
```bash
# Verifica que ANTHROPIC_API_KEY esté configurada
echo $ANTHROPIC_API_KEY
# Debe comenzar con: sk-ant-api03-...
```

**Frontend no conecta con backend:**
```
# Verifica environment.ts:
apiBaseUrl: 'http://localhost:8080/api'
```
