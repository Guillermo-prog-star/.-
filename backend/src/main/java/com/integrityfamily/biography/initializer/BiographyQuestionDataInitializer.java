package com.integrityfamily.biography.initializer;

import com.integrityfamily.biography.domain.BiographyQuestion;
import com.integrityfamily.biography.repository.BiographyQuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Siembra el catálogo global y fijo del diario guiado biográfico: 12
 * capítulos de vida (igual para cualquier familia de la plataforma),
 * transcritos del documento "Papá, quiero escuchar tu historia".
 *
 * Es IDEMPOTENTE: si ya hay preguntas sembradas, no hace nada.
 *
 * Se siembra por Java y no por SQL crudo en la migración V113 — ver
 * ADR-017. V75 sembró el Banco de Trayectorias directo por SQL y eso
 * produjo el defecto corregido en V100-V101 (valores de `category` que no
 * coincidían con el enum Java real, silenciado meses por un try/catch
 * amplio). Un inicializador Java es mucho más seguro para ~75 filas de
 * texto largo en español con acentos y comillas, y se puede corregir sin
 * una nueva migración mientras el contenido se sigue afinando.
 */
@Component
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
@Order(11)
public class BiographyQuestionDataInitializer implements CommandLineRunner {

    private final BiographyQuestionRepository questionRepository;

    private record Seed(int chapter, String chapterTitle, String key, String prompt, String deepen) {}

    @Override
    @Transactional
    public void run(String... args) {
        if (questionRepository.count() > 0) {
            log.info(">>>> [BIOGRAPHY] Catálogo ya sembrado. Omitiendo para evitar duplicados.");
            return;
        }

        log.info(">>>> [BIOGRAPHY] Sembrando catálogo del diario guiado biográfico ({} preguntas, 12 capítulos).", SEEDS.size());

        int order = 0;
        int chapter = -1;
        for (Seed s : SEEDS) {
            if (s.chapter() != chapter) {
                chapter = s.chapter();
                order = 0;
            }
            order++;
            questionRepository.save(BiographyQuestion.builder()
                    .chapterNumber(s.chapter())
                    .chapterTitle(s.chapterTitle())
                    .questionKey(s.key())
                    .promptText(s.prompt())
                    .deepenText(s.deepen())
                    .sortOrder(order)
                    .build());
        }

        log.info(">>>> [BIOGRAPHY] ✓ Catálogo sembrado — {} preguntas en 12 capítulos.", SEEDS.size());
    }

    private static final String T1 = "Los comienzos";
    private static final String T2 = "Infancia";
    private static final String T3 = "Adolescencia";
    private static final String T4 = "Trabajo y trayectoria";
    private static final String T5 = "Vida adulta y hogar";
    private static final String T6 = "Familia y legado";
    private static final String T7 = "Mi árbol familiar";
    private static final String T8 = "Mis padres y mis abuelos";
    private static final String T9 = "Mis hermanos";
    private static final String T10 = "Convertirme en papá y serlo";
    private static final String T11 = "Amor, compromiso y celebraciones";
    private static final String T12 = "Favoritos, creencias y consejos";

    private static final String CIERRE_CAPITULO =
            "Conversación al cerrar el capítulo. ¿Qué entendió quien me escuchó? ¿Qué corregiría o matizaría yo?";

    private static final List<Seed> SEEDS = List.of(

        // ── 1. Los comienzos ────────────────────────────────────────────
        new Seed(1, T1, "CH01_Q01_NACIMIENTO",
            "El día en que nací. ¿Qué sabes de tu nacimiento: fecha, lugar, personas presentes y circunstancias de la familia?",
            "¿Qué dato está documentado y qué parte te la contaron? ¿Quién la contó y cuándo?"),
        new Seed(1, T1, "CH01_Q02_HISTORIA_RECIBIDA",
            "La historia que recibí. ¿Qué anécdota repetían tus padres o familiares sobre mis primeros días?",
            "Cuéntala como te la narraban. ¿Había otras versiones? ¿Qué emoción atribuyes ahora a quienes la contaban?"),
        new Seed(1, T1, "CH01_Q03_MUNDO_DE_ENTONCES",
            "El mundo de entonces. ¿Cómo era el barrio, el trabajo y la economía del hogar cuando llegaste?",
            "Nombra una compra, una casa o una rutina concreta. Si recuerdas precios o canciones, indica la fuente y evita adivinar cifras."),
        new Seed(1, T1, "CH01_Q04_SIGNIFICADO_ORIGEN",
            "Lo que el origen llegó a significar. ¿Qué te hizo creer esa historia sobre tu lugar en la familia?",
            "Distingue lo que alguien dijo explícitamente de lo que tú interpretaste después."),
        new Seed(1, T1, "CH01_Q05_CIERRE", CIERRE_CAPITULO, null),

        // ── 2. Infancia ──────────────────────────────────────────────────
        new Seed(2, T2, "CH02_Q01_DIA_CUALQUIERA",
            "Un día cualquiera. Llévame a una tarde de tu infancia que todavía recuerdes. ¿Dónde estabas y qué hiciste?",
            "¿Quién estaba contigo? ¿Qué edad aproximada tenías? ¿Qué detalle podrías dibujar?"),
        new Seed(2, T2, "CH02_Q02_JUEGOS_CURIOSIDAD",
            "Juegos y curiosidad. ¿Qué te gustaba explorar, fabricar, desarmar o preguntar? ¿Qué querías ser de grande?",
            "Cuenta una ocasión concreta y quién alentó o desalentó esa curiosidad."),
        new Seed(2, T2, "CH02_Q03_MONEDAS_TESOROS",
            "Monedas y tesoros. Cuando recibías unas monedas o guardabas algo valioso para ti, ¿qué hacías?",
            "¿Cuánto recuerdas con certeza? ¿Por qué ese objeto importaba entonces?"),
        new Seed(2, T2, "CH02_Q04_MESA_REGLAS",
            "Mesa y reglas. ¿Qué plato te gustaba y cuál te hacía poner mala cara? ¿Qué ocurría si no querías comer?",
            "Relata un almuerzo específico. ¿Qué regla era explícita y cómo la entiendes hoy?"),
        new Seed(2, T2, "CH02_Q05_FIESTA_ESPERADA",
            "Fiesta esperada. ¿Qué celebración esperabas más y cómo transcurría desde la mañana hasta la noche?",
            "Recupera un olor, un sonido y una persona. ¿Qué parte puede confirmar otro familiar?"),
        new Seed(2, T2, "CH02_Q06_RECUERDO_DIFICIL",
            "Un recuerdo difícil o incompleto. ¿Hay una escena infantil que hoy recuerdes de otra manera o sobre la que tengas dudas?",
            "Puedes dejarla en blanco. Si la cuentas, separa recuerdo propio, relato ajeno y significado actual."),
        new Seed(2, T2, "CH02_Q07_CIERRE", CIERRE_CAPITULO, null),

        // ── 3. Adolescencia ─────────────────────────────────────────────
        new Seed(3, T3, "CH03_Q01_QUIEN_CREIA_SER",
            "Quién creía ser. ¿Con qué tres palabras te habrías descrito entonces y qué ejemplo mostraría cada una?",
            "¿Qué esperaba tu familia o tu colegio de ti? ¿En qué coincidías y en qué no?"),
        new Seed(3, T3, "CH03_Q02_COLEGIO_APRENDIZAJE",
            "Colegio y aprendizaje. ¿Qué materia te atrapaba y cuál te costaba? Recuerda una clase, examen o docente.",
            "¿Qué ocurrió exactamente? ¿Cuál era tu interpretación de tus capacidades en ese momento?"),
        new Seed(3, T3, "CH03_Q03_MUSICA_AMISTADES",
            "Música, amistades y pertenencia. ¿Qué música sonaba entre tus amigos y qué canción te lleva a una escena precisa?",
            "¿Dónde la escuchabas? ¿Qué sentías entonces y qué crees que expresaba hoy?"),
        new Seed(3, T3, "CH03_Q04_PRIMER_AFECTO",
            "Primer afecto y decisiones. Si deseas contarlo, ¿cómo fue una cita, un baile o un vínculo importante?",
            "Describe lo que hiciste y dijiste sin atribuir al otro pensamientos que no conocías."),
        new Seed(3, T3, "CH03_Q05_ALGO_CAMBIARIA",
            "Algo que cambiaría. ¿Qué momento adolescente querrías repetir o reparar?",
            "¿Qué información te faltaba entonces? ¿Qué consejo darías a ese joven sin juzgarlo desde el futuro?"),
        new Seed(3, T3, "CH03_Q06_CIERRE", CIERRE_CAPITULO, null),

        // ── 4. Trabajo y trayectoria ────────────────────────────────────
        new Seed(4, T4, "CH04_Q01_PRIMER_EMPLEO",
            "Primer empleo. ¿Cómo conseguiste tu primer trabajo y qué pasó durante el primer día?",
            "¿Quién te recibió? ¿Cuál fue la primera tarea y el primer error o aprendizaje?"),
        new Seed(4, T4, "CH04_Q02_PRIMER_PAGO",
            "Primer pago. ¿Qué sentiste al recibir tu primer pago y qué hiciste con él?",
            "Si no recuerdas la cifra, conserva la decisión y el contexto sin inventar montos."),
        new Seed(4, T4, "CH04_Q03_ELEGIR_CAMINO",
            "Elegir un camino. ¿Qué querías hacer, qué opciones tenías y qué expectativas familiares influyeron?",
            "¿Qué dijeron realmente otras personas? ¿Qué presión interpretaste tú?"),
        new Seed(4, T4, "CH04_Q04_OFICIO_CRISIS",
            "Oficio y crisis. Cuéntame una jornada difícil y una ocasión en que te sentiste competente.",
            "¿Cuál era el problema, qué hiciste, qué resultado observaste y qué atribuyes a otros?"),
        new Seed(4, T4, "CH04_Q05_COSTO_ESFUERZO",
            "El costo del esfuerzo. ¿Qué ganó tu familia gracias a tu trabajo y qué tiempo o posibilidad se perdió?",
            "Evita un balance obligado: registra una escena de presencia y otra de ausencia si ambas existieron."),
        new Seed(4, T4, "CH04_Q06_CIERRE", CIERRE_CAPITULO, null),

        // ── 5. Vida adulta y hogar ──────────────────────────────────────
        new Seed(5, T5, "CH05_Q01_DESPUES_COLEGIO",
            "Después del colegio. ¿Qué hiciste al terminar el colegio y por qué elegiste ese camino?",
            "¿Qué alternativas estaban realmente disponibles? ¿Qué motivo puedes documentar y cuál reconstruyes hoy?"),
        new Seed(5, T5, "CH05_Q02_MIS_VEINTE",
            "Mis veinte años. ¿Cómo era un día típico en tus veinte y qué metas ocupaban tus pensamientos?",
            "Cuenta una decisión que reveló tus prioridades, aunque luego cambiaran."),
        new Seed(5, T5, "CH05_Q03_PRIMERA_VIVIENDA",
            "Primera vivienda propia o independiente. Describe la primera vivienda que sentiste tuya: espacios, objetos, personas y rutina.",
            "¿Cuántas habitaciones recuerdas? ¿Cuál es una escena feliz allí y qué sacrificio la hizo posible?"),
        new Seed(5, T5, "CH05_Q04_CAMBIOS_RUMBO",
            "Cambios de rumbo. ¿Cuándo descubriste que debías ajustar un plan importante?",
            "¿Qué señal te hizo cambiar? ¿Qué alternativa descartaste y qué efecto tuvo?"),
        new Seed(5, T5, "CH05_Q05_CIERRE", CIERRE_CAPITULO, null),

        // ── 6. Familia y legado ─────────────────────────────────────────
        new Seed(6, T6, "CH06_Q01_LO_QUE_RECIBI",
            "Lo que recibí. ¿Qué valor, regla o gesto de tus padres conservaste y cuál decidiste cambiar?",
            "Compara una escena de tu infancia con una escena de tu propia crianza."),
        new Seed(6, T6, "CH06_Q02_LLEGADA_HIJOS",
            "La llegada de mis hijos. ¿Qué recuerdas de un momento en que entendiste que eras padre?",
            "¿Qué pasó, quién estaba allí y qué imaginabas que sería distinto a partir de entonces?"),
        new Seed(6, T6, "CH06_Q03_LO_QUE_VEO_EN_USTEDES",
            "Lo que veo en ustedes. ¿En qué gesto o manera de pensar reconoces algo tuyo en tus hijos y dónde te sorprenden?",
            "Cuenta una situación concreta y pregunta después si ellos la recuerdan igual."),
        new Seed(6, T6, "CH06_Q04_MEMORIA_COMPARTIDA",
            "Una memoria compartida. ¿Cuál es un recuerdo favorito de tiempo con la familia ya siendo adulto?",
            "Reconstruye un día específico: lugar, diálogo, imprevisto y lo que significó entonces."),
        new Seed(6, T6, "CH06_Q05_LO_QUE_QUIERO_DEJAR",
            "Lo que quiero dejar. ¿Qué historia, habilidad, pregunta o forma de tratar a otros quisieras transmitir?",
            "¿Qué ejemplo de tu vida la respalda? ¿Qué parte todavía estás aprendiendo?"),
        new Seed(6, T6, "CH06_Q06_LO_QUE_QUISIERA_PREGUNTAR",
            "Lo que quisiera preguntarte. ¿Qué pregunta te gustaría que tus hijos te hicieran y cuál quisieras hacerles tú?",
            "Escribe la pregunta sin forzar una respuesta inmediata."),
        new Seed(6, T6, "CH06_Q07_CIERRE", CIERRE_CAPITULO, null),

        // ── 7. Mi árbol familiar ─────────────────────────────────────────
        new Seed(7, T7, "CH07_Q01_RAMA_PATERNA_PADRE",
            "Rama paterna de mi padre (abuelo paterno). ¿Quiénes fueron el bisabuelo y la bisabuela de esta rama, y el abuelo o abuela que desciende de ellos?",
            "Indica las fuentes del parentesco y las dudas pendientes."),
        new Seed(7, T7, "CH07_Q02_RAMA_MATERNA_PADRE",
            "Rama materna de mi padre (abuela paterna). ¿Quiénes fueron el bisabuelo y la bisabuela de esta rama, y el abuelo o abuela que desciende de ellos?",
            "Indica las fuentes del parentesco y las dudas pendientes."),
        new Seed(7, T7, "CH07_Q03_RAMA_PATERNA_MADRE",
            "Rama paterna de mi madre (abuelo materno). ¿Quiénes fueron el bisabuelo y la bisabuela de esta rama, y el abuelo o abuela que desciende de ellos?",
            "Indica las fuentes del parentesco y las dudas pendientes."),
        new Seed(7, T7, "CH07_Q04_RAMA_MATERNA_MADRE",
            "Rama materna de mi madre (abuela materna). ¿Quiénes fueron el bisabuelo y la bisabuela de esta rama, y el abuelo o abuela que desciende de ellos?",
            "Indica las fuentes del parentesco y las dudas pendientes."),
        new Seed(7, T7, "CH07_Q05_RAMAS_QUE_CONOZCO",
            "Las ramas que conozco. ¿Quién me explicó por primera vez el árbol familiar y qué nombres recuerdo de su relato?",
            "Ubica una conversación concreta. ¿Había nombres distintos, apodos o parentescos inciertos?"),
        new Seed(7, T7, "CH07_Q06_MAS_QUE_NOMBRES",
            "Más que nombres. ¿Qué historia conserva la familia de un bisabuelo o una bisabuela?",
            "¿Quién la contó, qué ocurrió según esa persona y qué parte no puede comprobarse?"),

        // ── 8. Mis padres y mis abuelos ──────────────────────────────────
        new Seed(8, T8, "CH08_Q01_MI_MADRE",
            "Mi madre. ¿Cuál era su nombre completo, dónde nació y dónde creció? ¿Cómo era ella en una escena que recuerdes?",
            "¿Qué dato proviene de ella o de un documento? Describe un momento favorito: lugar, palabras, gesto y edad aproximada tuya."),
        new Seed(8, T8, "CH08_Q02_MI_PADRE",
            "Mi padre. ¿Cuál era su nombre completo, dónde nació y dónde creció? ¿Cómo era él en una escena que recuerdes?",
            "Distingue lo que viste de lo que te contaron. Describe un momento favorito y lo que significó entonces y ahora."),
        new Seed(8, T8, "CH08_Q03_HISTORIA_PADRES",
            "La historia de mis padres. ¿Sabes cuándo y dónde se conocieron, sus edades aproximadas y si se casaron, cuándo y dónde?",
            "¿Quién te contó la boda o su relación? ¿Qué edad tenía cada uno según esa fuente? ¿Hay una foto, acta u otra versión? Si no hubo boda, cuenta cómo formaron familia."),
        new Seed(8, T8, "CH08_Q04_ABUELOS_PATERNOS",
            "Abuelos paternos. ¿Cuáles eran sus nombres completos, cómo los llamabas, dónde nacieron y crecieron?",
            "Si aplica, registra el apellido de nacimiento de tu abuela. ¿Qué sabes de sus estudios y oficios, y de qué fuente proviene?"),
        new Seed(8, T8, "CH08_Q05_ABUELOS_MATERNOS",
            "Abuelos maternos. ¿Qué sabes de sus nombres, lugares de origen, estudios y trabajos?",
            "¿Los conociste personalmente? Si no, ¿quién te habló de ellos y qué quedó sin saber?"),
        new Seed(8, T8, "CH08_Q06_ESCENA_ABUELOS",
            "Una escena con mis abuelos. ¿Cuál es un recuerdo que muestre cómo era uno de mis abuelos?",
            "Nombra una acción observable y evita convertir una sola escena en una descripción definitiva de su carácter."),
        new Seed(8, T8, "CH08_Q07_OTRAS_PERSONAS_CRIANZA",
            "Otras personas que me criaron. ¿Qué familiares, vecinos, docentes u otras personas participaron de mi crianza?",
            "¿Qué hicieron concretamente por ti? ¿Qué aprendiste y qué atribución es tu interpretación posterior?"),

        // ── 9. Mis hermanos ──────────────────────────────────────────────
        new Seed(9, T9, "CH09_Q01_LUGAR_ENTRE_HERMANOS",
            "Lugar entre hermanos. ¿Eres hijo único? Si tienes hermanos, ¿cuántos son y qué lugar ocupas por edad?",
            "Escribe sus nombres de mayor a menor, incluyéndote. Señala si una edad o el orden es incierto."),
        new Seed(9, T9, "CH09_Q02_FACIL_DIFICIL",
            "Lo fácil y lo difícil. ¿Crees que tu lugar entre hermanos influyó en alguna oportunidad o carga durante la infancia?",
            "Da una escena y considera otra explicación posible antes de atribuirlo al orden de nacimiento."),
        new Seed(9, T9, "CH09_Q03_VINCULO_HERMANOS",
            "Vínculo entre hermanos. ¿Qué recuerdo de la infancia muestra cómo se acompañaban o discutían?",
            "¿Qué pasó, quién estaba allí y cómo lo recuerda el otro hermano?"),

        // ── 10. Convertirme en papá y serlo ──────────────────────────────
        new Seed(10, T10, "CH10_Q01_LLEGADA_CADA_HIJO",
            "La llegada de cada hijo. ¿Cómo recibiste la noticia y qué recuerdas del nacimiento de cada hijo?",
            "Repite por cada hijo que desees incluir. ¿Qué viviste directamente y qué te contaron después?"),
        new Seed(10, T10, "CH10_Q02_ELEGIR_NOMBRES",
            "Elegir sus nombres. ¿Cómo eligieron el nombre de cada hijo y quién propuso cada opción?",
            "¿Hubo desacuerdos? Reconstruye una conversación concreta sin atribuir motivos que la otra persona no expresó."),
        new Seed(10, T10, "CH10_Q03_PRIMEROS_REGISTROS",
            "Primeros registros. ¿Sabes cuánto midió y pesó cada hijo al nacer, cuándo caminó y cuáles fueron sus primeras palabras?",
            "Consulta registros si los hay. Si no, escribe \"no lo recuerdo\" o una edad aproximada con la fuente."),
        new Seed(10, T10, "CH10_Q04_CANCIONES_RUTINAS",
            "Canciones y rutinas. ¿Qué canción, juego o rutina compartías con ellos cuando eran pequeños?",
            "Describe una noche o una mañana precisa y qué reacción observaste."),
        new Seed(10, T10, "CH10_Q05_SORPRESA_PATERNIDAD",
            "La sorpresa de ser padre. ¿Qué cambió en tu idea de la paternidad después de nacer tu primer hijo?",
            "Compara una expectativa anterior con una situación que la contradijo o la confirmó."),
        new Seed(10, T10, "CH10_Q06_DESCUBRIRME_PADRE",
            "Descubrirme como padre. ¿Qué descubriste de ti mismo que no esperabas?",
            "Narra una decisión, un error reparado o un gesto repetido. ¿Cómo crees que lo vivió tu hijo? Pregúntaselo si desea hablar."),

        // ── 11. Amor, compromiso y celebraciones ─────────────────────────
        new Seed(11, T11, "CH11_Q01_COMIENZO_RELACION",
            "El comienzo de una relación. ¿Qué primer encuentro o decisión recuerdas de una relación amorosa importante?",
            "Describe lo ocurrido y después lo que crees que significó. ¿La otra persona lo recuerda igual?"),
        new Seed(11, T11, "CH11_Q02_COMPROMISO",
            "Compromiso. Si hubo compromiso, ¿a quién se lo contaste primero y cómo reaccionó?",
            "¿Cuánto tiempo pasó hasta la boda, si la hubo? ¿Qué fechas sabes y cuáles estimas?"),
        new Seed(11, T11, "CH11_Q03_PREPARATIVOS",
            "Preparativos. Si hubo boda, ¿cómo la planificaron, quién hizo qué y qué dificultad surgió?",
            "Cuenta una decisión concreta y deja lugar a la perspectiva de la otra persona."),
        new Seed(11, T11, "CH11_Q04_DIA_BODA",
            "El día de la boda. Si hubo boda, ¿dónde fue, quiénes estuvieron y qué anécdota conserva la memoria?",
            "¿Qué muestran las fotografías y qué recuerdas independientemente de ellas?"),
        new Seed(11, T11, "CH11_Q05_AMOR_A_TRAVES_TIEMPO",
            "Amor a través del tiempo. ¿Qué momento de cuidado, desacuerdo o reparación describe mejor una relación importante?",
            "Puedes incluir otras bodas o relaciones si deseas. No fuerces un balance definitivo."),

        // ── 12. Favoritos, creencias y consejos ──────────────────────────
        new Seed(12, T12, "CH12_Q01_TITULO_PALABRAS",
            "Título y palabras que me acompañan. Si escribieras una autobiografía, ¿qué título elegirías? ¿Qué cita, pasaje u oración te acompaña?",
            "Explica una escena que dé sentido al título. Atribuye el texto citado si sabes su origen; puedes parafrasearlo."),
        new Seed(12, T12, "CH12_Q02_MIEDO_FANTASIA",
            "Miedo y fantasía. ¿Qué superpoder escogerías y cuál es un temor que te gustaría explicar?",
            "¿Qué necesidad revela esa elección? Puedes omitir cualquier temor que prefieras reservar."),
        new Seed(12, T12, "CH12_Q03_VIAJES",
            "Viajes. ¿Cuál fue un viaje memorable y qué diez lugares te gustaría conocer si pudieras?",
            "Prioriza un episodio observado y explica por qué cada destino te atrae, sin obligación de completar diez."),
        new Seed(12, T12, "CH12_Q04_DEPORTE_CINE",
            "Deporte y cine. ¿Qué derrota deportiva te dolió más? ¿Cuál es tu película deportiva favorita y qué películas juveniles vuelves a ver?",
            "¿Dónde estabas cuando ocurrió la derrota? ¿Qué recuerdas del resultado y qué sentiste?"),
        new Seed(12, T12, "CH12_Q05_PELICULAS_TV",
            "Diez películas y televisión. ¿Qué diez películas elegirías para volver a ver? ¿Qué programas o películas suelen compartir en las fiestas?",
            "¿Qué serie antigua querrías que continuara y qué escena o compañía asocias a ella?"),
        new Seed(12, T12, "CH12_Q06_LIBROS_LECTURA",
            "Libros y lectura. ¿Cuántos libros calculas que lees en un año habitual y qué géneros prefieres?",
            "Es una estimación, no un examen. Menciona un libro que haya cambiado una opinión y cómo ocurrió."),
        new Seed(12, T12, "CH12_Q07_RELIGION_INFANCIA",
            "Religión en mi infancia. ¿Qué papel tuvo la religión en tu hogar y qué ritos o celebraciones recuerdas?",
            "Describe una práctica observada. Diferencia lo que te enseñaron de lo que tú crees hoy."),
        new Seed(12, T12, "CH12_Q08_CONSEJOS_AMBAS_DIRECCIONES",
            "Consejos en ambas direcciones. ¿Qué consejo darías a alguien de 20 a 39 años y qué podrías aprender de alguien de esa edad?",
            "Relaciona cada consejo con una experiencia propia y pregunta si hay excepciones a tu regla."),
        new Seed(12, T12, "CH12_Q09_ALGO_SIN_PREGUNTAR",
            "Algo que quiero contar aunque nadie lo haya preguntado.", null),
        new Seed(12, T12, "CH12_Q10_PREGUNTA_PARA_FAMILIA",
            "Una pregunta para mis hijos o para otra persona de la familia.", null),
        new Seed(12, T12, "CH12_Q11_HISTORIA_SEGUNDA_CONVERSACION",
            "¿Qué historia merece una segunda conversación, con fotos, documentos o la voz de otra persona?", null),
        new Seed(12, T12, "CH12_Q12_SORPRESA_PROPIA_HISTORIA",
            "¿Qué te sorprendió de tu propia historia al contarla?", null)
    );
}
