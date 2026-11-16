# Product BackLog - Desarrollo

Ítems que el código ya implementa o que el propio código deja abiertos. La estimación en puntos no está en el repositorio y se marca NA. «Hecho» significa presente en la rama `feature/v1.2-micro-simulations`, no aceptado por el instructor.

### Product Backlog:

| ID | Título | Descripción | Prioridad | Estimación | Criterios de Aceptación |
| --- | --- | --- | --- | --- | --- |
| 001 | Registro de familia | Como visitante, creo un núcleo y quedo como administrador familiar. | Alta | NA | `POST /api/auth/register-family` y `ROLE_FAMILY_ADMIN`. Hecho. |
| 002 | Inicio de sesión | Como persona registrada, entro con correo y contraseña y recibo un JWT. | Alta | NA | BCrypt, bloqueo por intentos, redirección según profesional o familia. Hecho. |
| 003 | Miembros | Como administrador familiar, registro a las personas del núcleo. | Alta | NA | Nombre obligatorio; edad, nacimiento y documento opcionales. Hecho. |
| 004 | Evaluación ICF | Como miembro, respondo y obtengo el ICF. | Alta | NA | Ruta de Conciencia y cierre con puntaje. Hecho. |
| 005 | Micro-simulaciones | Como miembro, recorro las fases congeladas del escenario V1.2. | Alta | NA | Fases NOTICE, THINK, ACT, AFTERMATH y EFFECT en V89–V95. Hecho. |
| 006 | Aceptar el plan | Como familia, acepto el plan que la IA propuso. | Alta | NA | Columnas de `V111`. Completar la tarea no crea evidencia. Hecho. |
| 007 | Sprint y daily | Como familia, corro un ciclo de 7 o 15 días y registro el día. | Alta | NA | Estados de misión y `attempt_status` de `V114`. Hecho. |
| 008 | Protocolo de seguridad | Como responsable, confirmo una activación con fecha de seguimiento. | Alta | NA | No hay alta automática. `SafetyProtocolServiceTest`. Hecho. |
| 009 | Consentimiento | Como administrador familiar, otorgo o revoco el permiso de compartir. | Alta | NA | Tabla `consents` y auditoría. Hecho. |
| 010 | Panel profesional | Como profesional, veo las familias asignadas y dejo un borrador. | Alta | NA | `professional_follow_up_drafts` sin llave foránea polimórfica rota. Hecho. |
| 011 | Hogar Digital | Como familia, aterrizo en el hogar y no en un tablero genérico. | Alta | NA | Ruta por defecto `family-home`. Hecho. |
| 012 | Mapa clínico | Cargar SNOMED, LOINC o CIE-10 solo con validación de un codificador. | Alta | NA | `TerminologyService` sigue vacío hasta esa validación. No hecho, a propósito. |
| 013 | Supresión de cuenta | Cumplir o corregir la promesa de borrado a 30 días de la política. | Media | NA | Hoy no hay un proceso en el backend. No hecho. |
| 014 | Cifrado en reposo | Evidenciar AES-256 o ajustar la política si el host no lo aplica. | Media | NA | [Pendiente: evidencia del proveedor.] |
| 015 | Sacar secretos del fuente | Rotar la clave del administrador sembrado y quitarla de `MasterDataInitializer`. | Alta | NA | La clave no debe quedar en el repositorio. No hecho. |

Estas filas no son un comercio ni un carrito: reemplazan el ejemplo de la plantilla por el producto real.

| ID | Título | Descripción | Prioridad | Estimación | Criterios de Aceptación |
| --- | --- | --- | --- | --- | --- |
| 016 | Integración continua | El workflow de calidad corre pruebas y JaCoCo al 65 % de líneas. | Alta | NA | `.github/workflows/quality.yml` existe. Hecho como pipeline. [Pendiente: resultado de la última corrida.] |
| 017 | Despliegue | Railway, Render y Vercel están declarados. | Media | NA | Workflows presentes. [Pendiente: cuál URL está viva.] |
| 018 | Consumidor de salud | Conectar una IPS o SISPRO cuando exista. | Baja | NA | `CLAUDE.md` recomienda no construir el gateway OAuth antes de ese consumidor. No hecho. |
| 019 | Pruebas de campo | Aplicar el sistema con familias del piloto y guardar el resultado. | Alta | NA | [Pendiente: instrumentos. No están en el repo.] |
