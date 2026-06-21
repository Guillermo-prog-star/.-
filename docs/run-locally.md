# Integrity Family — Cómo ejecutar localmente

**Última actualización:** 2026-06-20

---

## Prerrequisitos

- Docker Desktop corriendo
- Java 17 (Eclipse Adoptium en `C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot`)
- Node.js + npm
- Git Bash (para scripts .sh)

---

## Levantar el sistema completo

### 1. Base de datos y RabbitMQ
```powershell
# Desde C:\Proyectos\if-full
docker compose up -d db rabbitmq
```
Esperar que ambos contenedores digan `healthy`.

### 2. Backend
```powershell
cd C:\Proyectos\if-full\backend
mvn clean package -DskipTests
& "C:\Program Files\Eclipse Adoptium\jdk-17.0.18.8-hotspot\bin\java.exe" "-Dfile.encoding=UTF-8" -jar target\integrity-family-backend-1.0.0.jar
```
Listo cuando aparece: `Started IntegrityFamilyApplication in XX seconds`

### 3. Frontend
```powershell
cd C:\Proyectos\if-full\if-frontend
npx ng serve
```
Abrir: http://localhost:4200

---

## Notas importantes

- **`-Dfile.encoding=UTF-8`** es obligatorio en Windows. Sin él Spring Boot falla inmediatamente.
- El proxy Angular (`proxy.conf.js`) está configurado en `angular.json` — se activa automáticamente con `npx ng serve`.
- Si el dashboard queda en spinner: el backend no está corriendo o el token JWT expiró (cerrar sesión y volver a entrar).
- La BD local corre en puerto **3307** (no 3306) para no conflictuar con MySQL local.

---

## Contenedores Docker

| Contenedor | Puerto local | Propósito |
|-----------|-------------|-----------|
| integrity-db | 3307 | MySQL 8.4 |
| integrity-rabbitmq | 5672, 15672 | RabbitMQ (admin: localhost:15672) |
| integrity-backend | 8080 | Backend (solo docker compose up completo) |

---

## Credenciales locales

- **Admin:** william@integrity.family
- **BD:** root / root123
- **RabbitMQ admin:** http://localhost:15672 (guest/guest)
