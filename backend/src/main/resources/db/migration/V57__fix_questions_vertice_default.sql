-- V57: Corregir campo vertice NULL en preguntas sembradas (V56)
-- El campo vertice es int primitivo en Java → no puede ser NULL
UPDATE questions SET vertice = 0 WHERE vertice IS NULL;
ALTER TABLE questions MODIFY COLUMN vertice INT NOT NULL DEFAULT 0;
