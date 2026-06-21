# Integrity Family — Arquitectura del Sistema

**Última actualización:** 2026-06-20  
**Versión:** v1.1.9

---

## Stack tecnológico activo

| Capa | Tecnología | Versión | Dónde corre |
|------|-----------|---------|-------------|
| Backend API | Spring Boot | 3.4.3 | Railway (Docker) |
| Frontend SPA | Angular | 18 | Vercel |
| Base de datos | MySQL | 8.4 | Railway (volumen persistente) |
| Mensajería | RabbitMQ | 3 | CloudAMQP (prod) / Docker (local) |
| IA principal | Claude API | claude-sonnet-4-6 | Anthropic cloud |
| Auth | JWT (jjwt) | 0.12.6 | Embebido en backend |
| Migraciones | Flyway | - | Automático al iniciar |
| Imagen Docker | william195/if-backend | v1.1.9 | Docker Hub |

---

## Topología de despliegue

```
[Usuario]
    │
    ├── Browser → Vercel (Angular SPA)
    │               │
    │               └── HTTPS → Railway (Spring Boot :8080)
    │                               │
    │                               ├── MySQL :3306 (Railway volume)
    │                               ├── CloudAMQP (RabbitMQ)
    │                               └── Anthropic API (Claude)
    │
    └── Alexa Echo Show → Alexa Skill → Railway (Spring Boot /alexa)
```

---

## Estructura del backend (44 módulos)

### Módulos críticos (no tocar sin migración)
- `domain/` — Entidades JPA + Repositorios
- `config/` — SecurityConfig, RabbitMQConfig, WebSocketConfig
- `common/` — SecurityValidator, EventPublisher, excepciones

### Flujo de una evaluación ICF
```
POST /api/evaluations
  → EvaluationService.create()
  → EvaluationScoringService.calculate()  (4 dimensiones)
  → FamilyIcfRecalculatedEvent (RabbitMQ)
  → PlanGenerationService.generate()      (Claude API)
  → ImprovementPlan guardado en BD
```

### Flujo de autenticación
```
POST /api/auth/login
  → CustomUserDetailsService.loadByEmail()
  → JwtService.generateToken()
  → Response: { accessToken, refreshToken }

Cada request protegido:
  → JwtAuthenticationFilter.doFilterInternal()
  → SecurityValidator.validateFamilyOwnership()
```

---

## Independencia de IA

El sistema usa `AiProviderSelector` para abstraer el proveedor:
```java
AiProvider (interface)
  ├── ClaudeAiService    ← activo en producción
  ├── GeminiAiService    ← disponible
  └── OpenAiService      ← disponible
```
Cambiar de Claude a otro proveedor: modificar solo `AiProviderSelector`.

---

## Alexa Skill

- **Skill ID:** amzn1.ask.skill.25d146d1-4c9b-4be1-84df-bc9f073684f2
- **Invocación:** "Alexa, abre integridad familiar"
- **26 intents** activos (4 Amazon + 22 custom)
- **APL** habilitado para Echo Show 8
- Endpoint: `POST /alexa` en el backend

---

## Variables de entorno en Railway

| Variable | Propósito |
|----------|-----------|
| `SPRING_DATASOURCE_URL` | URL de MySQL |
| `JWT_SECRET` | Firma de tokens |
| `CLAUDE_API_KEY` | API de Anthropic |
| `SPRING_RABBITMQ_*` | CloudAMQP |
| `SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=false` | Evita spam de logs RabbitMQ |

---

## Migraciones Flyway

- Estado actual: **V71** (family_chapter_progress)
- Próximo disponible: **V72**
- Regla: NUNCA modificar una migración ya ejecutada
- Script de reparación automática: `FlywayAutoRepairConfig`

---

## Decisiones de arquitectura importantes

| Decisión | Por qué | Fecha |
|----------|---------|-------|
| `task_evidences.task_id` nullable (V65) | Evidencias espontáneas sin tarea asociada | 2026 |
| `SPRING_RABBITMQ_LISTENER_SIMPLE_AUTO_STARTUP=false` | Railway no tiene RabbitMQ local, evita retry spam | 2026-06-20 |
| Sistema de 75 capítulos en JSON | Contenido narrativo separado del código, fácil de editar | 2026-06-20 |
| `file.encoding=UTF-8` obligatorio en Windows | Spring Boot 3.4+ exige UTF-8 explícito en JVM Windows | 2026-06-20 |
