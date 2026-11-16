-- V101: DOC-TRAY-001/002 (V75) usaban status='PUBLISHED', pero el resto de los
-- documentos sembrados usa 'ACTIVE' (default de ProjectDocument), que es el
-- único valor que filtra DocumentationService.listAll()/listByCategory().
-- Sin este fix, los documentos son alcanzables por código directo pero nunca
-- aparecen en el listado general ni al navegar por categoría.

UPDATE project_documents SET status = 'ACTIVE' WHERE code IN ('DOC-TRAY-001', 'DOC-TRAY-002');
