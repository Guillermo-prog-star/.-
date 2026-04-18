-- V1: Schema completo Integrity Family
CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS users (
    id         BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name  VARCHAR(120) NOT NULL,
    email      VARCHAR(120) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT NOT NULL, role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_u FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_ur_r FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE IF NOT EXISTS milestones (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    milestone_key VARCHAR(20) NOT NULL UNIQUE,
    label         VARCHAR(50) NOT NULL,
    months        INT NOT NULL,
    phase         VARCHAR(80) NOT NULL,
    bloque        VARCHAR(30) NOT NULL,
    sort_order    INT NOT NULL
);

CREATE TABLE IF NOT EXISTS families (
    id                BIGINT PRIMARY KEY AUTO_INCREMENT,
    name              VARCHAR(120) NOT NULL,
    description       VARCHAR(255),
    family_code       VARCHAR(30) UNIQUE,
    current_milestone VARCHAR(20) DEFAULT 'inicio',
    pin_hash          VARCHAR(255),
    whatsapp          VARCHAR(30),
    municipio         VARCHAR(80),
    created_by        BIGINT NOT NULL,
    created_at        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    next_evaluation_at TIMESTAMP NULL,
    CONSTRAINT fk_fam_u FOREIGN KEY (created_by) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS family_members (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id            BIGINT NOT NULL,
    full_name            VARCHAR(120) NOT NULL,
    role_type            VARCHAR(50) NOT NULL,
    age                  INT,
    autonomy_level       INT DEFAULT 50,
    responsibility_level INT DEFAULT 50,
    active               BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_mem_f FOREIGN KEY (family_id) REFERENCES families(id)
);

CREATE TABLE IF NOT EXISTS questions (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    dimension     VARCHAR(50)  NOT NULL,
    question_text VARCHAR(300) NOT NULL,
    bloque        VARCHAR(30)  NOT NULL DEFAULT 'reconocimiento',
    vertice       ENUM('RECONOCIMIENTO', 'AMOR', 'ENTREGA') NOT NULL DEFAULT 'RECONOCIMIENTO', -- <--- Agrégala aquí
    active        BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS evaluations (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id    BIGINT NOT NULL,
    member_id    BIGINT,
    status       VARCHAR(30) NOT NULL DEFAULT 'STARTED',
    started_at   TIMESTAMP NULL,
    finalized_at TIMESTAMP NULL,
    has_crisis   BOOLEAN DEFAULT FALSE,
    icf          DECIMAL(5,2),
    milestone_key VARCHAR(50),
    CONSTRAINT fk_ev_f FOREIGN KEY (family_id) REFERENCES families(id),
    CONSTRAINT fk_ev_m FOREIGN KEY (member_id) REFERENCES family_members(id)
);

CREATE TABLE IF NOT EXISTS evaluation_answers (
    id            BIGINT PRIMARY KEY AUTO_INCREMENT,
    evaluation_id BIGINT NOT NULL,
    question_id   BIGINT NOT NULL,
    answer_value  INT NOT NULL,
    comment       VARCHAR(500) NULL,
    CONSTRAINT fk_ea_e FOREIGN KEY (evaluation_id) REFERENCES evaluations(id),
    CONSTRAINT fk_ea_q FOREIGN KEY (question_id)   REFERENCES questions(id)
);

CREATE TABLE IF NOT EXISTS risk_snapshots (
    id                  BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id           BIGINT NOT NULL,
    evaluation_id       BIGINT NOT NULL,
    risk_level          VARCHAR(30)  NOT NULL,
    score_emotions      DECIMAL(5,2) NOT NULL,
    score_communication DECIMAL(5,2) NOT NULL,
    score_habits        DECIMAL(5,2) NOT NULL,
    score_times         DECIMAL(5,2) NOT NULL,
    global_score        DECIMAL(5,2) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rs_f FOREIGN KEY (family_id)    REFERENCES families(id),
    CONSTRAINT fk_rs_e FOREIGN KEY (evaluation_id) REFERENCES evaluations(id)
);

CREATE TABLE IF NOT EXISTS plans (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id       BIGINT NOT NULL,
    evaluation_id   BIGINT NOT NULL,
    title           VARCHAR(150) NOT NULL,
    description     TEXT,
    ai_report       TEXT,
    ai_generated_at TIMESTAMP NULL,
    status          VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pl_f FOREIGN KEY (family_id)    REFERENCES families(id),
    CONSTRAINT fk_pl_e FOREIGN KEY (evaluation_id) REFERENCES evaluations(id)
);

CREATE TABLE IF NOT EXISTS plan_tasks (
    id                 BIGINT PRIMARY KEY AUTO_INCREMENT,
    plan_id            BIGINT NOT NULL,
    title              VARCHAR(150) NOT NULL,
    description        VARCHAR(300),
    assigned_member_id BIGINT NULL,
    completed          BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_pt_p FOREIGN KEY (plan_id)            REFERENCES plans(id),
    CONSTRAINT fk_pt_m FOREIGN KEY (assigned_member_id) REFERENCES family_members(id)
);

CREATE TABLE IF NOT EXISTS checklist_items (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT,
    family_id    BIGINT NOT NULL,
    plan_id      BIGINT NULL,
    plan_task_id BIGINT NULL,
    title        VARCHAR(150) NOT NULL,
    completed    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ci_f FOREIGN KEY (family_id)    REFERENCES families(id),
    CONSTRAINT fk_ci_p FOREIGN KEY (plan_id)      REFERENCES plans(id),
    CONSTRAINT fk_ci_t FOREIGN KEY (plan_task_id) REFERENCES plan_tasks(id)
);

CREATE TABLE IF NOT EXISTS audit_events (
    id             BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_type     VARCHAR(100) NOT NULL,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id   BIGINT,
    payload_json   JSON,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- V2: Seed completo
INSERT INTO roles (name) VALUES ('ROLE_ADMIN'), ('ROLE_USER');

INSERT INTO milestones (milestone_key,label,months,phase,bloque,sort_order) VALUES
('inicio','Inicio',0,'Diagnóstico Base','reconocimiento',1),
('m03','3 Meses',3,'Primeros Cambios','reconocimiento',2),
('m06','6 Meses',6,'Consolidación Inicial','amor',3),
('m12','12 Meses',12,'Primera Transformación','amor',4),
('m18','18 Meses',18,'Profundización','entrega',5),
('m24','24 Meses',24,'Madurez del Sistema','entrega',6),
('m30','30 Meses',30,'Cierre y Sostenimiento','entrega',7),
('m36','36 Meses',36,'Transformación Completa','entrega',8);

-- Reemplaza el primer bloque de preguntas por este corregido:
INSERT INTO questions (dimension, question_text, bloque, vertice) VALUES
('EMOTIONS','¿Me he sentido emocionalmente escuchado en mi familia esta semana?','reconocimiento', 'RECONOCIMIENTO'),
('EMOTIONS','¿He logrado expresar mis emociones con respeto y claridad?','reconocimiento', 'RECONOCIMIENTO'),
('EMOTIONS','¿He reconocido las emociones de los demás sin juzgar?','reconocimiento', 'RECONOCIMIENTO'),
('COMMUNICATION','¿Nuestra comunicación fue clara y respetuosa esta semana?','reconocimiento', 'RECONOCIMIENTO'),
('COMMUNICATION','¿Escuchamos activamente antes de responder o reaccionar?','reconocimiento', 'RECONOCIMIENTO'),
('COMMUNICATION','¿Pudimos resolver desacuerdos sin conflictos mayores?','reconocimiento', 'RECONOCIMIENTO'),
('COMMUNICATION','¿Me expresé con honestidad sin herir a los demás?','reconocimiento', 'RECONOCIMIENTO'),
('HABITS','¿Cumplí con mis compromisos y hábitos familiares?','reconocimiento', 'RECONOCIMIENTO'),
('HABITS','¿Aporté responsablemente a la rutina del hogar?','reconocimiento', 'RECONOCIMIENTO'),
('TIMES','¿Compartimos tiempo de calidad en familia?','reconocimiento', 'RECONOCIMIENTO'),
('TIMES','¿Respeté el tiempo personal de los demás y el mío?','reconocimiento', 'RECONOCIMIENTO'),
('EMOTIONS','¿He expresado afecto y gratitud a mi familia?','amor', 'AMOR'),
('EMOTIONS','¿He gestionado el estrés sin descargarlo en mi familia?','amor', 'AMOR'),
('EMOTIONS','¿Me siento seguro emocionalmente en el núcleo familiar?','amor', 'AMOR'),
('COMMUNICATION','¿Hemos tenido conversaciones profundas y significativas?','amor', 'AMOR'),
('COMMUNICATION','¿Pusimos en práctica acuerdos comunicativos previos?','amor', 'AMOR'),
('COMMUNICATION','¿Celebramos los logros de cada miembro?','amor', 'AMOR'),
('COMMUNICATION','¿Pedimos ayuda cuando la necesitamos?','amor', 'AMOR'),
('HABITS','¿Mantuvimos hábitos de autocuidado personal y familiar?','amor', 'AMOR'),
('HABITS','¿Cumplimos los compromisos establecidos juntos?','amor', 'AMOR'),
('TIMES','¿Creamos un momentó especial compartido esta semana?','amor', 'AMOR'),
('TIMES','¿Equilibramos el tiempo individual y colectivo de forma sana?','amor', 'AMOR'),
('EMOTIONS','¿Sostenemos la calma en momentos de tensión familiar?','entrega', 'ENTREGA'),
('EMOTIONS','¿He crecido emocionalmente gracias a mi familia?','entrega', 'ENTREGA'),
('EMOTIONS','¿Nuestra familia tiene una base emocional sólida?','entrega', 'ENTREGA'),
('COMMUNICATION','¿Nuestra comunicación ha evolucionado hacia mayor profundidad?','entrega', 'ENTREGA'),
('COMMUNICATION','¿Tomamos decisiones importantes de forma participativa?','entrega', 'ENTREGA'),
('COMMUNICATION','¿Damos y recibimos retroalimentación sin defensividad?','entrega', 'ENTREGA'),
('COMMUNICATION','¿Conversamos sobre el futuro familiar con apertura?','entrega', 'ENTREGA'),
('HABITS','¿Nuestros hábitos familiares son sostenibles a largo plazo?','entrega', 'ENTREGA'),
('HABITS','¿Hemos incorporado nuevos hábitos positivos este mes?','entrega', 'ENTREGA'),
('TIMES','¿El tiempo juntos es consciente y de calidad genuina?','entrega', 'ENTREGA'),
('TIMES','¿Cada miembro tiene autonomía de tiempo y espacio respetado?','entrega', 'ENTREGA');
-- Password: Admin123*
INSERT INTO users (full_name,email,password,active) VALUES
('William López','admin@integrityfamily.com',
 '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhy',TRUE);

INSERT INTO user_roles (user_id,role_id) VALUES (1,1),(1,2);

INSERT INTO families (name,description,family_code,current_milestone,municipio,created_by) VALUES
('Familia López','Familia demo Integrity Family','IF-CO-QUI-2026-3847','inicio','Armenia',1);

INSERT INTO family_members (family_id,full_name,role_type,age,autonomy_level,responsibility_level) VALUES
(1,'William López','PADRE',40,88,92),
(1,'Ana López','MADRE',38,85,90),
(1,'Andrés Felipe','HIJO',14,60,65);

-- SEED DE PREGUNTAS COMPLEMENTARIAS (CORREGIDAS PARA TU ESQUEMA)

-- DIMENSIÓN: EMOTIONS
INSERT INTO questions (dimension, question_text, bloque, active) VALUES 
('EMOTIONS', '¿Con qué frecuencia los miembros de la familia expresan gratitud entre sí?', 'reconocimiento', true),
('EMOTIONS', '¿Siente que existe un ambiente de apoyo emocional cuando alguien está triste o estresado?', 'reconocimiento', true),
('EMOTIONS', '¿Qué tan capaces son los miembros de la familia de pedir perdón genuinamente?', 'reconocimiento', true),
('EMOTIONS', '¿Se celebran los logros individuales de cada miembro con alegría compartida?', 'reconocimiento', true),
('EMOTIONS', '¿Existe libertad para expresar opiniones diferentes sin temor a represalias emocionales?', 'reconocimiento', true);

-- DIMENSIÓN: COMMUNICATION
INSERT INTO questions (dimension, question_text, bloque, active) VALUES 
('COMMUNICATION', '¿Se realizan reuniones familiares para discutir temas importantes o resolver conflictos?', 'reconocimiento', true),
('COMMUNICATION', '¿Siente que es escuchado activamente cuando habla con los demás miembros?', 'reconocimiento', true),
('COMMUNICATION', '¿Qué tan claro es el flujo de información sobre planes o cambios en la rutina familiar?', 'reconocimiento', true),
('COMMUNICATION', '¿Se evitan los gritos o el lenguaje despectivo durante las discusiones?', 'reconocimiento', true),
('COMMUNICATION', '¿Existe un espacio digital saludable (uso moderado de celulares) durante las comidas?', 'reconocimiento', true);

-- DIMENSIÓN: HABITS
INSERT INTO questions (dimension, question_text, bloque, active) VALUES 
('HABITS', '¿Se mantienen horarios regulares para las comidas principales en familia?', 'reconocimiento', true),
('HABITS', '¿Existe una distribución equitativa de las tareas del hogar entre los miembros?', 'reconocimiento', true),
('HABITS', '¿Se promueven hábitos de vida saludable (ejercicio, sueño) dentro del hogar?', 'reconocimiento', true),
('HABITS', '¿Hay consistencia en el cumplimiento de las normas y límites establecidos?', 'reconocimiento', true),
('HABITS', '¿Se fomenta el hábito de la lectura o el aprendizaje continuo en casa?', 'reconocimiento', true);

-- DIMENSIÓN: TIMES
INSERT INTO questions (dimension, question_text, bloque, active) VALUES 
('TIMES', '¿Se dedica al menos una tarde a la semana exclusivamente a una actividad familiar?', 'reconocimiento', true),
('TIMES', '¿Tienen los padres espacios de tiempo a solas para fortalecer su relación?', 'reconocimiento', true),
('TIMES', '¿Se respeta el tiempo de descanso y privacidad de cada miembro de la familia?', 'reconocimiento', true),
('TIMES', '¿Qué tanto tiempo de calidad (sin pantallas) comparten diariamente?', 'reconocimiento', true),
('TIMES', '¿Se planifican vacaciones o salidas recreativas con anticipación y consenso?', 'reconocimiento', true);

-- SEED DE PREGUNTAS POR VÉRTICE (CORREGIDAS PARA TU ESQUEMA)

-- VÉRTICE: RECONOCIMIENTO
INSERT INTO questions (dimension, vertice, question_text, bloque, active) VALUES 
('EMOTIONS', 'RECONOCIMIENTO', '¿Eres consciente de que estás sintiendo algo en este momento?', 'reconocimiento', true),
('EMOTIONS', 'RECONOCIMIENTO', '¿Puedes reconocer la emoción presente sin juzgarla?', 'reconocimiento', true),
('EMOTIONS', 'RECONOCIMIENTO', '¿Notas tu respiración cuando estás enojado?', 'reconocimiento', true),
('COMMUNICATION', 'RECONOCIMIENTO', '¿Eres consciente de que estás escuchando ahora mismo?', 'reconocimiento', true),
('COMMUNICATION', 'RECONOCIMIENTO', '¿Te das cuenta de tus palabras mientras las pronuncias?', 'reconocimiento', true),
('HABITS', 'RECONOCIMIENTO', '¿Eres consciente de que repites ciertas acciones sin pensarlo?', 'reconocimiento', true),
('HABITS', 'RECONOCIMIENTO', '¿Reconoces cuando haces algo de forma automática?', 'reconocimiento', true),
('TIMES', 'RECONOCIMIENTO', '¿Eres consciente del tiempo que transcurre en este momento?', 'reconocimiento', true),
('TIMES', 'RECONOCIMIENTO', '¿Notas cómo vives cada instante sin medirlo?', 'reconocimiento', true);

-- VÉRTICE: AMOR (CONEXIÓN HISTÓRICA)
INSERT INTO questions (dimension, vertice, question_text, bloque, active) VALUES 
('EMOTIONS', 'AMOR', '¿Cómo conecta lo que sentiste ayer con lo que sientes ahora?', 'amor', true),
('COMMUNICATION', 'AMOR', '¿Cómo conecta lo que dijiste ayer con lo que expresas hoy?', 'amor', true),
('HABITS', 'AMOR', '¿Cómo conecta el hábito que practicaste ayer con lo que haces hoy?', 'amor', true),
('TIMES', 'AMOR', '¿Cómo conecta el tiempo que usaste ayer con lo que haces hoy?', 'amor', true);

-- VÉRTICE: ENTREGA (ACCIÓN/IMPACTO)
INSERT INTO questions (dimension, vertice, question_text, bloque, active) VALUES 
('EMOTIONS', 'ENTREGA', '¿Qué podría ocurrir si no reconoces esta emoción a tiempo?', 'entrega', true),
('COMMUNICATION', 'ENTREGA', '¿Qué ocurriría si no expresas lo que realmente piensas?', 'entrega', true),
('HABITS', 'ENTREGA', '¿Qué ocurriría si mantienes un hábito poco saludable por un año más?', 'entrega', true),
('TIMES', 'ENTREGA', '¿Qué pasaría si nunca revisas cómo usas tu tiempo cada día?', 'entrega', true);