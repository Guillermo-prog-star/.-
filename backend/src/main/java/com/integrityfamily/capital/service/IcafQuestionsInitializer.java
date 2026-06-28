package com.integrityfamily.capital.service;

import com.integrityfamily.domain.Question;
import com.integrityfamily.domain.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Inicializador de preguntas ICaF — dominios confianza y bienestar_emocional.
 *
 * Se ejecuta al arranque solo si las preguntas no existen (idempotente).
 * Usa question_key como identificador único (UPSERT por existencia).
 *
 * Convención de claves:
 *   ICAF_CONF_001 … ICAF_CONF_007   → dominio confianza
 *   ICAF_BIEN_001 … ICAF_BIEN_007   → dominio bienestar_emocional
 *
 * Escala: 1-5 (totalmente en desacuerdo → totalmente de acuerdo)
 * Dirección NEGATIVE: la pregunta está formulada negativamente → se invierte al calcular.
 */
@Slf4j
@Component
@Order(20)
@RequiredArgsConstructor
public class IcafQuestionsInitializer implements CommandLineRunner {

    private static final String TYPE  = "ICAF";
    private static final String CONF  = "confianza";
    private static final String BIEN  = "bienestar_emocional";
    private static final String AUTO  = "autonomia";
    private static final String PROP  = "proposito";
    private static final String EMPR  = "emprendimiento";
    private static final String LEGA  = "legado";

    private final QuestionRepository questionRepo;

    @Override
    public void run(String... args) {
        int created = 0;
        for (Question q : buildQuestions()) {
            if (questionRepo.findByQuestionKey(q.getQuestionKey()).isEmpty()) {
                questionRepo.save(q);
                created++;
            }
        }
        if (created > 0) {
            log.info("[ICaF-Init] {} preguntas ICaF creadas (confianza + bienestar + autonomia + proposito + emprendimiento + legado)", created);
        }
    }

    private List<Question> buildQuestions() {
        return List.of(

            // ── DOMINIO: confianza (7 ítems) ──────────────────────────────────
            // Mide la percepción de apoyo, escucha y apertura emocional en la familia.

            q("ICAF_CONF_001",
              "Cuando tengo un problema, siento que puedo hablar con alguien de mi familia.",
              CONF, "POSITIVE", 1),

            q("ICAF_CONF_002",
              "En mi familia me escuchan cuando necesito expresar cómo me siento.",
              CONF, "POSITIVE", 2),

            q("ICAF_CONF_003",
              "Confío en que mi familia me apoya en los momentos difíciles.",
              CONF, "POSITIVE", 3),

            q("ICAF_CONF_004",
              "Puedo compartir mis miedos o inseguridades con alguien de mi familia sin sentirme juzgado/a.",
              CONF, "POSITIVE", 4),

            q("ICAF_CONF_005",
              "Siento que las conversaciones importantes en mi familia son honestas.",
              CONF, "POSITIVE", 5),

            q("ICAF_CONF_006",
              "Cuando alguien de mi familia promete algo, confío en que lo cumplirá.",
              CONF, "POSITIVE", 6),

            // Ítem inverso: detecta percepción de secretismo o distancia emocional
            q("ICAF_CONF_007",
              "En mi familia hay temas de los que no se puede hablar aunque sean importantes.",
              CONF, "NEGATIVE", 7),

            // ── DOMINIO: bienestar_emocional (7 ítems) ────────────────────────
            // Mide ansiedad, esperanza, sentido y estabilidad emocional individual.

            q("ICAF_BIEN_001",
              "Me siento emocionalmente estable en mi vida cotidiana.",
              BIEN, "POSITIVE", 1),

            q("ICAF_BIEN_002",
              "Tengo esperanza sobre el futuro de mi familia.",
              BIEN, "POSITIVE", 2),

            q("ICAF_BIEN_003",
              "Siento que lo que hago tiene sentido y propósito.",
              BIEN, "POSITIVE", 3),

            q("ICAF_BIEN_004",
              "Puedo manejar el estrés del día a día sin que me desborde.",
              BIEN, "POSITIVE", 4),

            // Ítem inverso: detecta ansiedad persistente
            q("ICAF_BIEN_005",
              "Me siento ansioso/a o preocupado/a con frecuencia.",
              BIEN, "NEGATIVE", 5),

            // Ítem inverso: detecta desesperanza
            q("ICAF_BIEN_006",
              "Siento que las cosas en mi familia no van a mejorar.",
              BIEN, "NEGATIVE", 6),

            q("ICAF_BIEN_007",
              "Me siento capaz de superar los desafíos que enfrenta mi familia.",
              BIEN, "POSITIVE", 7),

            // ── DOMINIO: autonomia (6 ítems) ──────────────────────────────────
            // Mide la capacidad de cada miembro de actuar con independencia
            // y ser respetado en sus decisiones dentro del núcleo familiar.

            q("ICAF_AUTO_001",
              "En mi familia, cada miembro puede tomar sus propias decisiones sin ser juzgado.",
              AUTO, "POSITIVE", 1),

            q("ICAF_AUTO_002",
              "Siento que tengo libertad para expresar mis opiniones dentro de mi familia.",
              AUTO, "POSITIVE", 2),

            q("ICAF_AUTO_003",
              "Mi familia respeta que cada persona tenga sus propios proyectos y metas.",
              AUTO, "POSITIVE", 3),

            q("ICAF_AUTO_004",
              "En mi familia hay miembros que controlan o deciden por los demás sin consultarles.",
              AUTO, "NEGATIVE", 4),

            q("ICAF_AUTO_005",
              "Puedo actuar de manera independiente en aspectos importantes de mi vida.",
              AUTO, "POSITIVE", 5),

            q("ICAF_AUTO_006",
              "Siento que debo pedir aprobación para hacer cosas que me corresponden decidir a mí.",
              AUTO, "NEGATIVE", 6),

            // ── DOMINIO: proposito (6 ítems) ──────────────────────────────────
            // Mide el sentido de dirección compartida, valores comunes y visión
            // de futuro que sostiene la unidad familiar.

            q("ICAF_PROP_001",
              "Mi familia tiene metas o sueños que nos unen y nos dan dirección.",
              PROP, "POSITIVE", 1),

            q("ICAF_PROP_002",
              "Siento que como familia sabemos para qué estamos juntos.",
              PROP, "POSITIVE", 2),

            q("ICAF_PROP_003",
              "En mi familia hablamos sobre el tipo de familia que queremos ser en el futuro.",
              PROP, "POSITIVE", 3),

            q("ICAF_PROP_004",
              "Los valores de mi familia me dan sentido y orientan mis decisiones.",
              PROP, "POSITIVE", 4),

            q("ICAF_PROP_005",
              "Siento que mi familia va sin rumbo; cada quien por su lado.",
              PROP, "NEGATIVE", 5),

            q("ICAF_PROP_006",
              "Mi familia celebra los logros colectivos, no solo los individuales.",
              PROP, "POSITIVE", 6),

            // ── DOMINIO: emprendimiento (5 ítems) ────────────────────────────
            // Mide la capacidad de la familia de adaptarse, innovar y tomar
            // iniciativa ante los retos y cambios.

            q("ICAF_EMPR_001",
              "Mi familia es capaz de adaptarse cuando las circunstancias cambian.",
              EMPR, "POSITIVE", 1),

            q("ICAF_EMPR_002",
              "En mi familia alentamos que cada miembro intente cosas nuevas aunque impliquen riesgo.",
              EMPR, "POSITIVE", 2),

            q("ICAF_EMPR_003",
              "Ante un problema, mi familia busca soluciones creativas en lugar de rendirse.",
              EMPR, "POSITIVE", 3),

            q("ICAF_EMPR_004",
              "En mi familia tendemos a quedarnos en la queja sin buscar alternativas.",
              EMPR, "NEGATIVE", 4),

            q("ICAF_EMPR_005",
              "Mi familia apoya los proyectos personales y el crecimiento de sus miembros.",
              EMPR, "POSITIVE", 5),

            // ── DOMINIO: legado (5 ítems) ─────────────────────────────────────
            // Mide la conciencia de la historia familiar, la transmisión de
            // valores y la conexión intergeneracional.

            q("ICAF_LEGA_001",
              "En mi familia valoramos y hablamos sobre nuestra historia y tradiciones.",
              LEGA, "POSITIVE", 1),

            q("ICAF_LEGA_002",
              "Siento que mi familia me ha transmitido valores que quiero pasar a las siguientes generaciones.",
              LEGA, "POSITIVE", 2),

            q("ICAF_LEGA_003",
              "Conocemos la historia de nuestra familia y eso nos da identidad.",
              LEGA, "POSITIVE", 3),

            q("ICAF_LEGA_004",
              "En mi familia no nos preocupamos por dejar algo positivo a las generaciones que vienen.",
              LEGA, "NEGATIVE", 4),

            q("ICAF_LEGA_005",
              "Mi familia trabaja para construir algo duradero, no solo para el presente.",
              LEGA, "POSITIVE", 5)
        );
    }

    private Question q(String key, String text, String icafDomain, String direction, int sortOrder) {
        return Question.builder()
                .questionKey(key)
                .text(text)
                .questionType(TYPE)
                .icafDomain(icafDomain)
                .dimension(icafDomain)
                .direction(direction)
                .active(true)
                .sortOrder(sortOrder)
                .severityWeight(1.0)
                .detectsRelapse(false)
                .requiresEvidence(false)
                .reverseQuestion("NEGATIVE".equals(direction))
                .version("1.0")
                .build();
    }
}
