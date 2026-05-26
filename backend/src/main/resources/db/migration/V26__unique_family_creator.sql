-- V26: Garantizar que cada usuario sólo pueda crear UN núcleo familiar.
-- La restricción UNIQUE en created_by_id impide duplicados incluso bajo concurrencia.
ALTER TABLE families
    ADD CONSTRAINT uq_family_creator UNIQUE (created_by_id);
