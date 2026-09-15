-- V100: Corrige categorías de project_documents que no existen en el enum Java
-- DocumentCategory (PROJECT, RESEARCH, FAMILY, AI, DEVELOPMENT). V75 insertó
-- DOC-TRAY-001/002 con category='TECHNICAL'/'GUIDE', que no son constantes
-- válidas. Como el campo usa @Enumerated(EnumType.STRING), cualquier consulta
-- JPA que traiga estas filas (ej. ProjectDocumentRepository.findAll(), usado
-- por DocumentationDataInitializerPart2 en cada arranque) lanza
-- IllegalArgumentException, capturada como WARN y silenciada, pero bloquea
-- la carga de TODOS los documentos complementarios, no solo estos dos.
--
-- Se remapean a PROJECT (igual que PT-MTC-01/PT-API-01/PT-ARQ-01: son
-- documentación técnica/operativa del sistema, no de la identidad de la
-- familia como FAM-CON-01/FAM-VAL-01).

UPDATE project_documents SET category = 'PROJECT' WHERE code IN ('DOC-TRAY-001', 'DOC-TRAY-002');
