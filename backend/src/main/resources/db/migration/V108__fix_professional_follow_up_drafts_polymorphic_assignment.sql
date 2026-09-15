-- V108: Corrige professional_follow_up_drafts.assignment_id (V107) -- no
-- debia tener FK a family_support_assignments.
--
-- SupportNetworkService.getDataView() (que ProfessionalFollowUpDraftService
-- reutiliza deliberadamente para autorizacion/datos, ver ADR-006) resuelve
-- assignmentId por DOS caminos distintos con espacios de ID separados:
-- FamilySupportAssignment (asignacion tradicional) o FamilyEcosystemLink
-- (profesional conectado via Ecosistema de Apoyo). La FK de V107 solo
-- aceptaba el primer camino -- cualquier profesional con acceso real via
-- Ecosistema (nivel de acceso 5/5, mismos datos visibles) no podia generar
-- un borrador: la fila fallaba con violacion de FK al guardar.
--
-- El propio modulo support ya resolvio este mismo problema antes, en
-- support_access_log (V77/V82): assignment_id ahi es deliberadamente
-- polimorfico, sin FK, indexado. Se replica ese patron aqui en vez de
-- restringir la generacion de borradores solo al camino de asignacion
-- tradicional (lo cual excluiria, sin justificacion real, a profesionales
-- del Ecosistema que ya tienen acceso legitimo a los mismos datos).
ALTER TABLE professional_follow_up_drafts
    DROP FOREIGN KEY fk_pfud_assignment;
