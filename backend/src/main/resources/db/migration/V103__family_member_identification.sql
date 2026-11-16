-- V103: Ancla de identidad para interoperabilidad (Fase 0 del programa de
-- interoperabilidad con el ecosistema de salud). Sin numero de documento no
-- hay forma de mapear FamilyMember a FHIR Patient.identifier ni de hacer
-- matching contra un Master Patient Index. Nullable: la mayoria de miembros
-- historicos no lo tienen capturado.
ALTER TABLE family_members
    ADD COLUMN document_type   VARCHAR(20) NULL AFTER birth_date,
    ADD COLUMN document_number VARCHAR(30) NULL AFTER document_type;

-- Unico cuando ambos estan presentes (MySQL trata cada NULL como distinto,
-- asi que no bloquea a los miembros sin documento registrado).
CREATE UNIQUE INDEX uk_family_members_document ON family_members (document_type, document_number);
