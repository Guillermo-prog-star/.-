# Integrity Family — Guía de Despliegue en la Nube

## Arquitectura de producción

```
Browser ──► Vercel (Angular SPA)
               │  /api/* proxy
               ▼
           Railway (Spring Boot + MySQL)
```

---

## 1. Frontend → Vercel ✅ (ya desplegado)

**URL:** https://if-frontend-nine.vercel.app

### GitHub Secrets necesarios
| Secret | Valor |
|--------|-------|
| `VERCEL_TOKEN` | https://vercel.com/account/tokens |
| `VERCEL_ORG_ID` | `team_qw3OrINJ6Lw0RiwfkdOeTWUp` |
| `VERCEL_PROJECT_ID` | `prj_yQ8vimSRCRIMdhJxG35uBQuXRce4` |

---

## 2. Backend → Railway

### Paso 1 — Crear proyecto en Railway
1. Ve a https://railway.com/new
2. Elige **"Empty Project"**
3. Nombre: `integrity-family-backend`

### Paso 2 — Añadir MySQL
1. En el proyecto → **"+ New Service"** → **"Database"** → **MySQL**
2. Railway crea la BD y expone estas variables automáticamente:
   - `MYSQLHOST`, `MYSQLPORT`, `MYSQLDATABASE`, `MYSQLUSER`, `MYSQLPASSWORD`
   - También la variable compuesta: `MYSQL_URL`

### Paso 3 — Añadir el servicio Backend
1. **"+ New Service"** → **"GitHub Repo"** → selecciona `Guillermo-prog-star/if-full`
2. **Root Directory:** `backend`
3. Railway detectará el `Dockerfile` automáticamente

### Paso 4 — Variables de entorno del servicio backend
En el servicio backend → **"Variables"** → añade:

| Variable | Valor |
|----------|-------|
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8` |
| `SPRING_DATASOURCE_USERNAME` | `${{MySQL.MYSQLUSER}}` |
| `SPRING_DATASOURCE_PASSWORD` | `${{MySQL.MYSQLPASSWORD}}` |
| `JWT_SECRET` | *(genera uno seguro: `openssl rand -base64 64`)* |
| `CLAUDE_API_KEY` | *(tu clave de Anthropic — opcional)* |
| `SPRING_PROFILES_ACTIVE` | `railway` |

> **Nota:** La sintaxis `${{MySQL.VARIABLE}}` es la referencia cruzada de Railway entre servicios.

### Paso 5 — Obtener el RAILWAY_TOKEN para GitHub Actions
1. Ve a https://railway.com/account/tokens
2. **"+ New Token"** → nombre: `github-actions`
3. Añade a GitHub Secrets del repo:

| Secret | Valor |
|--------|-------|
| `RAILWAY_TOKEN` | *(el token generado)* |

### Paso 6 — Conectar el proxy en Vercel
Una vez Railway te dé la URL pública del backend (ej: `https://if-backend.railway.app`),
edita `if-frontend/vercel.json`:

```json
"rewrites": [
  { "source": "/api/:path*", "destination": "https://TU-BACKEND.railway.app/api/:path*" },
  { "source": "/(.*)",       "destination": "/index.html" }
]
```

Luego: `git commit` + `git push` → Vercel redeploya automáticamente.

---

## CI/CD — Flujo completo post-configuración

```
git push origin main
    │
    ├─► if-frontend/** cambiado?
    │       └─► GitHub Actions → Vercel production
    │
    └─► backend/** cambiado?
            └─► GitHub Actions → Railway production
```
