# Anexos

Integrity Family. Versión documental 1.0. 16/11/2026.

Los anexos reúnen el seguimiento del proyecto formativo y los materiales que el código no puede suplir (encuestas, actas, canvas). Los textos de ejemplo de la plantilla (SIGEF, compras en línea, «dsagasfga») se reemplazaron por el estado real o por un pendiente.

# Lista de anexos

| Anexo | Archivo | Estado |
| --- | --- | --- |
| Informes escaneados | [anexos/10a_Informes_escaneados.md](anexos/10a_Informes_escaneados.md) | [Pendiente: digitalizar informes firmados] |
| Encuestas, entrevistas y pruebas de campo | [anexos/10b_Encuestas_Entrevistas.md](anexos/10b_Encuestas_Entrevistas.md) | [Pendiente: instrumentos y tabulados] |
| Product backlog de documentación | [anexos/10c_Product_BackLog_Documentacion.md](anexos/10c_Product_BackLog_Documentacion.md) | Borrador a partir de los documentos de `docs/sena/` |
| Product backlog de desarrollo | [anexos/10d_Product_BackLog_Desarrollo.md](anexos/10d_Product_BackLog_Desarrollo.md) | Funciones ya presentes en el código, más huecos reales |
| Canvas de modelo de negocio | NA | [Pendiente: archivo PT-MC-01. No está en el repositorio.] |
| Investigación | [anexos/10e_Anexos_Investigacion.md](anexos/10e_Anexos_Investigacion.md) | Remite a la visión y a los ADR. Falta el concepto del equipo. |
| Arquitectura tecnológica | [anexos/10f_Informe_Arquitectura_tecnologica.md](anexos/10f_Informe_Arquitectura_tecnologica.md) | Lenguajes y versiones tomadas de los archivos de construcción |
| Actas | [anexos/00b_Lista_Actas_Proyecto.md](anexos/00b_Lista_Actas_Proyecto.md) | Sin filas reales |

**Enlace JIRA:** [Pendiente: no hay proyecto Jira en el repositorio.]

**Enlaces GITHUB:** remoto `Guillermo-prog-star/.-`. Documentación de esta entrega: carpeta `docs/sena/`. [Pendiente: enlace público estable.]

**Presentación para la sustentación:** [Pendiente: diapositivas en el formato del centro.]

# Bitácora Seguimiento del proyecto.

El estado de abajo corresponde a la documentación generada el 16/11/2026 por lectura del código. No sustituye el seguimiento trimestral que firme el instructor.

## Trazabilidad artefactos.

| ARTEFACTO | OBSERVACIÓN (Indicar estado del documento) |
| --- | --- |
| PT-PP-01 - Planteamiento del problema | Borrador completo. Pendiente de datos institucionales y de aprobación. |
| PT-ERS-02 - Especificación de requisitos | Borrador con 68 requisitos funcionales y 22 no funcionales extraídos del código. |
| PT-ECU-02 - Especificación de casos de uso | Borrador con diagramas Mermaid y doce casos especificados. |
| Informe general del sistema | Borrador. Faltan centro, titulación, ciudad y el resto del equipo. |
| PT-PS-01 - Prototipado del sistema | Arquitectura de información y pantallas descritas. Capturas pendientes si el frontend no se ejecutó. |
| PT-PP-03 - Plan de pruebas | Casos mapeados a clases existentes. Falta la corrida y el resultado. |
| PT-MU-01 - Manual de usuario | Borrador con navegación y mensajes literales. Faltan capturas. |
| PT-MTC-01 - Manual técnico de configuración | Borrador alineado con Docker, Maven y workflows. |
| PT-MCBD-01 - Manual de configuración de base de datos | Borrador con MER, diccionario del núcleo y catálogo de 122 entidades. |
| Video promocional | [Pendiente: no está en el repositorio.] |
| Presentación en formato SENA | [Pendiente.] |
| ANEXOS | En proceso. Faltan evidencias de campo. |

## Trazabilidad Desarrollo.

El estado «Terminado» significa que hay código y pruebas en la rama analizada, no que un instructor haya aceptado el módulo. El número de sprint del equipo no está en el código. [Pendiente: cruzar con el sprint formativo real.]

| MODULO | DESCRIPCION AVANCE | SPRINT | ESTADO |
| --- | --- | --- | --- |
| 1. Acceso y sesión | Login, registro de familia, voucher, recuperación, bloqueo, JWT | [Pendiente] | Terminado en código |
| 2. Núcleo, miembros y guardián | Familias, miembros, documento de identidad, elección de guardián | [Pendiente] | Terminado en código |
| 3. Evaluación e ICF | Ruta de Conciencia, escenarios V1.2, ICaF | [Pendiente] | Terminado en código |
| 4. Riesgo y protocolo | Banco de trayectorias, activación manual | [Pendiente] | Terminado en código |
| 5. Plan y sprint | Aceptación del plan, sprint 7 o 15, daily, evidencias | [Pendiente] | Terminado en código |
| 6. Memoria | Biografía, diario, linaje, legado, documental | [Pendiente] | Terminado en código |
| 7. Interoperabilidad y consentimiento | Fases 0 a 5 descritas en `CLAUDE.md`. Mapa clínico vacío a propósito | [Pendiente] | En proceso |
| 8. Codificación clínica y consumidor FHIR | Sin códigos validados y sin IPS conectada | [Pendiente] | Detenido, a la espera de un tercero |
