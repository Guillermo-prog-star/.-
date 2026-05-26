-- =========================================================
-- V28: Guardián Familiar + Misiones Familiares
-- =========================================================

-- 1. Campos del Guardián en la tabla families
ALTER TABLE families
  ADD COLUMN guardian_member_id   BIGINT       NULL,
  ADD COLUMN guardian_since       DATETIME     NULL,
  ADD COLUMN rotation_enabled     BOOLEAN      NOT NULL DEFAULT FALSE,
  ADD COLUMN participation_score  INT          NOT NULL DEFAULT 0,
  ADD CONSTRAINT fk_families_guardian
      FOREIGN KEY (guardian_member_id)
      REFERENCES family_members(id)
      ON DELETE SET NULL;

-- 2. Tabla de votos para elegir Guardián
CREATE TABLE guardian_votes (
  id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
  family_id            BIGINT      NOT NULL,
  voter_member_id      BIGINT      NOT NULL,
  nominated_member_id  BIGINT      NOT NULL,
  voted_at             DATETIME    NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_gv_family    FOREIGN KEY (family_id)           REFERENCES families(id)       ON DELETE CASCADE,
  CONSTRAINT fk_gv_voter     FOREIGN KEY (voter_member_id)     REFERENCES family_members(id) ON DELETE CASCADE,
  CONSTRAINT fk_gv_nominated FOREIGN KEY (nominated_member_id) REFERENCES family_members(id) ON DELETE CASCADE,
  UNIQUE KEY uk_one_vote_per_member (family_id, voter_member_id)
);

-- 3. Tabla de Misiones Familiares
CREATE TABLE family_missions (
  id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
  family_id             BIGINT       NOT NULL,
  title                 VARCHAR(255) NOT NULL,
  description           TEXT,
  category              VARCHAR(100) NOT NULL DEFAULT 'CONEXION',
  duration_minutes      INT          NOT NULL DEFAULT 60,
  status                VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
  created_by_member_id  BIGINT       NULL,
  activated_at          DATETIME     NULL,
  completed_at          DATETIME     NULL,
  created_at            DATETIME     NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_fm_family  FOREIGN KEY (family_id)            REFERENCES families(id)       ON DELETE CASCADE,
  CONSTRAINT fk_fm_creator FOREIGN KEY (created_by_member_id) REFERENCES family_members(id) ON DELETE SET NULL
);

-- 4. Misiones predefinidas (catálogo semilla - se insertan como plantilla global, family_id=NULL)
--    Se usan como sugerencias; el guardián las activa copiándolas a su familia.
CREATE TABLE mission_templates (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  title            VARCHAR(255) NOT NULL,
  description      TEXT,
  category         VARCHAR(100) NOT NULL DEFAULT 'CONEXION',
  duration_minutes INT          NOT NULL DEFAULT 60,
  difficulty       VARCHAR(50)  NOT NULL DEFAULT 'FACIL',
  emoji            VARCHAR(10)  NULL
);

INSERT INTO mission_templates (title, description, category, duration_minutes, difficulty, emoji) VALUES
('Cena sin pantallas',       'Una comida juntos donde nadie usa el celular. Solo conversación.', 'CONEXION',    60,  'FACIL',  '🍽️'),
('15 minutos de escucha',    'Cada miembro habla 2 minutos sin ser interrumpido. Los demás escuchan.', 'COMUNICACION', 20, 'FACIL', '👂'),
('Día de gratitud',          'Cada miembro escribe o dice algo por lo que agradece a otro integrante.', 'GRATITUD',    30,  'FACIL',  '🙏'),
('Caminar juntos',           'Una caminata familiar de 30 minutos sin destino fijo.', 'HABITOS',     30,  'FACIL',  '🚶'),
('Historia familiar',        'Alguien cuenta un recuerdo familiar especial. Los demás preguntan.', 'MEMORIA',     30,  'FACIL',  '📖'),
('Abrazo colectivo',         'Todos se abrazan durante 10 segundos. Simple pero poderoso.', 'CONEXION',    5,   'FACIL',  '🤗'),
('Cocinamos juntos',         'Preparar una receta juntos. Cada uno tiene una tarea.', 'HABITOS',     90,  'MEDIO',  '👨‍🍳'),
('Carta para el futuro',     'Cada miembro escribe una carta para abrirla en 1 año.', 'REFLEXION',   45,  'MEDIO',  '✉️'),
('Apagón digital 2h',        'Dos horas sin internet ni pantallas. Juegos de mesa, conversación.', 'HABITOS',     120, 'MEDIO',  '📵'),
('Acuerdo familiar',         'Definir una norma de convivencia que todos proponen y votan.', 'COMUNICACION', 60, 'MEDIO', '🤝'),
('Celebrar un logro pequeño','Reconocer algo que alguien de la familia logró esta semana.', 'GRATITUD',    20,  'FACIL',  '🎉'),
('Meditar juntos 10 min',    'Meditación guiada corta en silencio. Todos en el mismo espacio.', 'BIENESTAR',   10,  'MEDIO',  '🧘');
