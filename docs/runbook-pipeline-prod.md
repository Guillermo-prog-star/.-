# Runbook — Reconstrucción del pipeline de despliegue a producción

**Creado:** 2026-09-08 · **Contexto:** hallazgo 11 de [`auditoria-2026-09.md`](auditoria-2026-09.md)

## Problema que resuelve

Producción (`api.integrityfamily.online`) **no se despliega desde este repo**. El servicio de
Railway `if-backend` (proyecto `steadfast-nurturing`, entorno `production`) está configurado como
**Settings → Source → "Source Image": `william195/if-backend:v1.1.9`** — una imagen de Docker Hub
construida manualmente hace ~2 meses (~julio 2026). `deploy-backend.yml`, `railway.json` y
`backend/railway.toml` describen un build desde `backend/Dockerfile` que **nunca ocurre**.

Consecuencia: prod está congelada en julio (sin ADR-006→013, sin la auditoría 2026-09). Mergear
cualquier rama a `main` no cambia nada en prod.

## Orden crítico

> **La migración se ensaya en local ANTES de tocar Railway.** En cuanto el servicio construya
> desde el repo, arranca y **Flyway aplica el salto de migraciones contra la BD de producción
> sin vuelta atrás**. No hay "deshacer" para un `ALTER TABLE`.

## Rollback (tenerlo claro antes de empezar)

| Qué se rompió | Cómo se revierte |
|---|---|
| El deploy nuevo no arranca / falla el healthcheck | Railway → Deployments → History → deployment de `v1.1.9` → **Redeploy**. Vuelve en segundos. |
| Una migración V107→V112 corrompió datos | Restaurar el **dump de la Fase B** en la BD de prod. Las migraciones son forward-only; el dump es la única red. |
| Todo mal, hay que abortar | Settings → Source → **Disconnect** el repo → **Connect** de nuevo la imagen `william195/if-backend:v1.1.9`. |

---

## Fase A — Medir el estado real (solo lectura, sin riesgo)

- [ ] **A.1 — Migración aplicada en prod.** Railway → servicio **MySQL** → *Connect* (o
  `railway connect MySQL`). En el shell:
  ```sql
  SELECT version, description, success, installed_on
  FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 15;
  ```
  Anotar la última `version` con `success = 1` y si hay alguna `success = 0` (migración fallida
  a medias — habría que resolverla aparte antes de nada). El salto "V107→V112" es la hipótesis;
  esto lo confirma o lo corrige.

- [ ] **A.2 — Rama de producción.** Estado de ramas al 2026-09-08:
  - `main` local (`38bede4`) **diverge** de `origin/main` (`126673b`).
  - `principal` (`b502f4b`) sincronizada con su origin.
  - `feature/v1.2-micro-simulations` (`9fb48d7`) → **72 commits** por delante de `origin/principal`.

  Decidir qué rama es "producción" y consolidarla. Ruta recomendada:
  ```
  feature/v1.2-micro-simulations  →  principal  →  main
  ```
  Resolver primero la divergencia local de `main` (`git fetch`, revisar `126673b`, decidir
  merge/rebase). **Railway apuntará a `main`.**

- [ ] **A.3 — Migraciones que se aplicarían.** Con la rama consolidada:
  ```bash
  ls backend/src/main/resources/db/migration/ | sort -V | tail -20
  ```
  Comparar con A.1. El conjunto V(prod+1)…V112 es lo que Flyway ejecutará en el primer boot.

---

## Fase B — Red de seguridad: dump de producción

`scripts/backup-mysql.sh` está cableado al contenedor local `integrity-db` — **no sirve para
Railway**. Dump directo:

- [ ] **B.1** — Credenciales desde Railway → **MySQL → Variables** (`MYSQLHOST`, `MYSQLPORT`,
  `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE` — usar los de conexión pública / proxy).
- [ ] **B.2** —
  ```bash
  mysqldump -h <MYSQLHOST> -P <MYSQLPORT> -u <MYSQLUSER> -p<MYSQLPASSWORD> \
    --single-transaction --routines --triggers --set-gtid-purged=OFF \
    <MYSQLDATABASE> > prod_$(date +%Y%m%d_%H%M%S).sql
  ```
- [ ] **B.3** — Guardar el archivo fuera del repo (pesa MB, no versionarlo). Verificar que no
  esté truncado: `tail -5 prod_*.sql` debe terminar en `-- Dump completed`.

**Este dump es el único rollback real de datos. No continuar sin él.**

---

## Fase C — Ensayo de la migración en local

- [ ] **C.1** — `docker compose up -d db` (MySQL local en `localhost:3307`).
- [ ] **C.2** — Restaurar el dump en una BD desechable:
  ```bash
  mysql -h127.0.0.1 -P3307 -uroot -proot123 -e "DROP DATABASE IF EXISTS prod_rehearsal; CREATE DATABASE prod_rehearsal CHARACTER SET utf8mb4"
  mysql -h127.0.0.1 -P3307 -uroot -proot123 prod_rehearsal < prod_YYYYMMDD_HHMMSS.sql
  ```
- [ ] **C.3** — Arrancar el backend (rama consolidada de A.2) contra esa BD, **perfil `prod`**,
  Flyway on:
  ```bash
  cd backend
  SPRING_PROFILES_ACTIVE=prod \
  SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3307/prod_rehearsal?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota" \
  SPRING_DATASOURCE_USERNAME=root \
  SPRING_DATASOURCE_PASSWORD=root123 \
  JWT_SECRET=rehearsal_only_min_32_chars_0000000000 \
  FAMILY_HOME_ID_SECRET=rehearsal_only_min_32_chars_0000000000 \
  mvn spring-boot:run
  ```
- [ ] **C.4 — Observar el arranque.** Debe verse Flyway aplicar `V(prod+1)` … `V112`:
  - Sin `Validate failed: ... checksum mismatch`. Hoy `spring.flyway.validate-on-migrate: false`
    lo silencia, pero conviene mirar los WARN. Cuidado con **V69** (snapshot idempotente con
    `CREATE TABLE IF NOT EXISTS`) y **V68** (procedure sobre `information_schema`).
  - Sin `Migration ... failed` / errores de DDL (columna que ya existe, FK a tabla ausente…).
    **V112** está escrita para tolerar `critical_days.member_id` preexistente por `ddl-auto`;
    verificar que efectivamente no rompe.
  - Tras Flyway, el `ddl-auto: update` de `application-prod.yml` no debe intentar cambios de
    schema inesperados (revisar los `Hibernate: alter table ...` en el log).
- [ ] **C.5** — Con la app arriba contra `prod_rehearsal`: probar **login** + 3-4 endpoints
  clave (familia, evaluación, plan). Confirmar que responden.
- [ ] **C.6** — Si algo falla: arreglarlo aquí (una migración correctiva `V113__...`) y repetir
  C.2–C.5. **No pasar a Fase D hasta que el ensayo sea limpio.**

---

## Fase D — Cutover en Railway

- [ ] **D.1 — Resolver el conflicto de perfil (antes de conectar).** `backend/railway.toml` fuerza
  `-Dspring.profiles.active=railway` en su `startCommand`; `backend/Dockerfile` fija
  `ENV SPRING_PROFILES_ACTIVE=prod`. Difieren (`railway` → `ddl-auto: update`, sin ocultar
  errores, Swagger on; `prod` → `ddl-auto: update` vía `application-prod.yml` pero errores
  ocultos y Swagger off). **Elegir `prod`:**
  - Borrar el bloque `[deploy] startCommand = "..."` de `backend/railway.toml`, **o** borrar
    `backend/railway.toml` entero y quedarse solo con `railway.json` (raíz).
  - Commit + push a la rama consolidada.
- [ ] **D.2** — Railway → `if-backend` → **Settings → Source** → **Disconnect** la imagen
  `william195/if-backend:v1.1.9`.
- [ ] **D.3** — **Connect Repo** → autorizar GitHub → repo `Guillermo-prog-star/.-` → rama
  **`main`**.
- [ ] **D.4** — **Root Directory** = raíz del repo (Railway leerá `railway.json` →
  `builder: DOCKERFILE`, `dockerfilePath: backend/Dockerfile`). Confirmar en Settings que el
  builder es DOCKERFILE y el path es `backend/Dockerfile`.
- [ ] **D.5** — Verificar variables (Settings → Variables): `JWT_SECRET` ✓ (ya está),
  `FAMILY_HOME_ID_SECRET` ✓, `SPRING_DATASOURCE_*` ✓, `CLAUDE_API_KEY` (hoy ausente → IA en
  `MOCK_KEY`; añadir si se quiere IA real en prod).
- [ ] **D.6** — Disparar el deploy (Railway lo hace solo al conectar, o Deployments → Deploy).
  Railway construye desde `backend/Dockerfile`. **Primer boot → Flyway V(prod+1)…V112**
  (ya validado en Fase C).
- [ ] **D.7 — Vigilar.** Deployments → **View logs**:
  - Build OK (Maven dentro del Dockerfile).
  - Arranque: banner Spring Boot + `Started ... in Xs`, Flyway aplica el salto sin fallo,
    sin `WeakKeyException`.
  - `curl https://api.integrityfamily.online/actuator/health` → `{"status":"UP"}`.
  - Login end-to-end en la app.
- [ ] **D.8** — Si D.7 falla → **rollback** (tabla de arriba): Deployments → History → `v1.1.9`
  → Redeploy. Si además Flyway ya migró y hay daño de datos → restaurar el dump de Fase B.

---

## Fase E — Limpieza (con el deploy nuevo estable ≥ 2-3 días)

- [ ] **E.1** — `if-frontend/src/environments/environment.prod.ts` → `https://api.integrityfamily.online`
  (hoy apunta a `if-backend-v1-0-0.onrender.com`, Render abandonado). Commit + deploy frontend.
- [ ] **E.2** — Decomisionar el servicio de **Render** (`if-backend-v1-0-0.onrender.com`).
- [ ] **E.3** — Borrar el perfil `render` de `backend/src/main/resources/application.yml`
  (y el `railway` si D.1 lo dejó huérfano).
- [ ] **E.4** — Docker Hub → `william195/if-backend` → borrar el repositorio (opcional; ya no
  se usa). Si se conserva, documentar que **no** es la fuente de prod.
- [ ] **E.5** — `deploy-backend.yml`: con Railway auto-construyendo en push a `main`, el
  `railway up` del workflow es redundante. Opciones:
  - Borrarlo (Railway despliega solo; `quality.yml` sigue siendo el gate en PRs).
  - Dejarlo solo como gate de tests (quitar el job `deploy`).
- [ ] **E.6** — Actualizar [`auditoria-2026-09.md`](auditoria-2026-09.md) (hallazgos 5, 6, 11)
  y `CLAUDE.md` (sección CI/CD) con el pipeline real.
- [ ] **E.7** — Hallazgos 5 y 6 quedan desbloqueados: revisar que el perfil `prod` efectivamente
  oculta mensajes de excepción y desactiva Swagger en la URL pública; evaluar pasar
  `ddl-auto` a `validate` (requiere que el schema esté 100% alineado con Flyway — probablemente
  otra migración snapshot tipo V69 primero).

---

## Apéndice — ficheros de config relevantes

| Fichero | Rol | Acción en este runbook |
|---|---|---|
| `railway.json` (raíz) | `builder: DOCKERFILE`, `dockerfilePath: backend/Dockerfile` | Se conserva (fuente de verdad del build) |
| `backend/railway.toml` | `startCommand` con `-Dspring.profiles.active=railway` + healthcheck | D.1: borrar el `startCommand` o el fichero entero |
| `backend/Dockerfile` | build multi-stage, `ENV SPRING_PROFILES_ACTIVE=prod`, `eclipse-temurin:21` | Se conserva; es el perfil que gana |
| `.github/workflows/deploy-backend.yml` | `railway up` en push a `main` (hoy inerte) | E.5: borrar o reducir a gate de tests |
| `.github/workflows/quality.yml` | tests + JaCoCo + Sonar en PRs a `main`/`principal` | Se conserva |
