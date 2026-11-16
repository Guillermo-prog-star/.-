-- V109: Corrige support_professional_notes.assignment_id (V77) -- mismo
-- problema que V108 corrigio en professional_follow_up_drafts.
--
-- addNote() (SupportNetworkService) se extiende para aceptar tambien
-- profesionales conectados via FamilyEcosystemLink (antes solo aceptaba
-- FamilySupportAssignment) -- verificado en vivo el 2026-07-18: un
-- profesional conectado por Ecosistema podia generar un borrador
-- (V108) pero no podia guardarlo como nota clinica, porque la FK de
-- V77 solo aceptaba IDs de family_support_assignments.
--
-- Mismo precedente que V108: support_access_log (V77/V82) ya trata
-- assignment_id como polimorfico, sin FK, solo indexado, porque tambien
-- se escribe desde ambos caminos de getDataView().
ALTER TABLE support_professional_notes
    DROP FOREIGN KEY fk_spn_assignment;
