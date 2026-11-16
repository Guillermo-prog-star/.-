# Product BackLog - Documentación

Lista de la documentación de proyecto tipo 1 para Integrity Family. La estimación en puntos no existe en el repositorio: la columna queda en NA para no inventar un planning póker. La prioridad alta es la que el centro suele exigir para sustentar.

| **Planteamiento del problema** | **Especificaciones de requisitos** | Especificacion de Casos de Uso | **Informe general del sistema** | **Prototipado del sistema** |
| --- | --- | --- | --- | --- |
| Introducción redactada | Introducción redactada | Introducción redactada | Introducción redactada | Introducción redactada |
| Referencias del repo y normas citadas por el código | Referencias de código | Actores | Objetivo tomado de la visión | Arquitectura de información desde las rutas |
| Descripción general | Características del producto | Diagramas Mermaid | Alcance según módulos existentes | Pantallas nombradas con su ruta |
| Situación actual con cita de archivo | Módulos M01 a M11 | Doce casos especificados | Situación actual | Capturas [Pendiente] |
| Situación esperada | 68 requisitos funcionales |  | Situación esperada | Enlace local del frontend |
| Justificación | Requisitos no funcionales |  | Justificación |  |
| Aspectos legales | Restricciones |  | Historias de usuario |  |

| ID | Título | Descripción | Prioridad | Estimación | Criterios de Aceptación |
| --- | --- | --- | --- | --- | --- |
| 1 | PT-PP-01 Planteamiento del problema | Problema, situación y normas apoyadas en el repo. | Alta | NA | Existe el archivo y cada afirmación de contexto cita un archivo. |
| 2 | PT-ERS-02 Requisitos | Requisitos funcionales por módulo y no funcionales. | Alta | NA | Identificadores RF y RNF estables, sin funciones inventadas. |
| 3 | PT-ECU-02 Casos de uso | Actores, diagramas y especificación de los casos principales. | Alta | NA | Cada caso cita requisitos RF. |
| 4 | PT-IGS-01 Informe general | Objetivo, alcance, roles e historias. | Alta | NA | Los datos institucionales que faltan están marcados pendiente. |
| 5 | PT-PS-01 Prototipado | Mapa de navegación y pantallas reales. | Alta | NA | Cada pantalla corresponde a una ruta de `app.routes.ts`. |
| 6 | PT-PP-03 Plan de pruebas | Casos ligados a clases de prueba existentes. | Alta | NA | Ningún caso nombra una clase que no esté en `src/test`. |
| 7 | PT-MCBD-01 Base de datos | MER, diccionario del núcleo y catálogo de entidades. | Alta | NA | Tablas coinciden con `@Table`. |
| 8 | PT-MTC-01 Manual técnico | Arranque local y despliegues declarados. | Alta | NA | Comandos salen de compose, Maven y `package.json`. |
| 9 | PT-MU-01 Manual de usuario | Navegación y mensajes literales. | Alta | NA | Cada mensaje citado aparece en el fuente. |
| 10 | Anexos y actas | Formato de acta y backlog. | Media | NA | Lo que no existe queda pendiente, sin minuta ficticia. |

### Subtareas para 1. planteamiento del problema:

| ID | Título | Descripción | Estimación | Criterios de Aceptación |
| --- | --- | --- | --- | --- |
| 1.1 | Lectura de visión y ADR | Usar `docs/vision.md` y ADR-019 como fuente del problema y de la regla de la IA. | NA | El PT-PP-01 cita esos archivos. |
| 1.2 | Introducción | Presentar el documento y el producto. | NA | Menciona ICF, plan y sprint sin copiar un ejemplo de otra plantilla. |
| 1.3 | Alcance de lo no verificable | Separar lo que el código no prueba. | NA | Cifrado en reposo, borrado a 30 días y datos del centro quedan pendientes. |
| 1.4 | Situación actual | Describir el problema según la visión y el estado del software en la rama. | NA | Incluye las contradicciones README frente a `pom.xml`. |
| 1.5 | Situación esperada | Listar lo que el sistema ya busca con las funciones existentes. | NA | Cada punto tiene respaldo en migración, ruta o ADR. |
| 1.6 | Justificación | Explicar aceptación del plan, protocolo manual y consentimiento. | NA | Cita V111, V98 y V104. |
| 1.7 | Normas | Incluir 1581 y 1098 solo donde el código trata esos datos. | NA | Hay campos de edad, nacimiento y documento, y la política cita la Ley 1581. |
