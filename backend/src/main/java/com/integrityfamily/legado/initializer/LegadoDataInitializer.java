package com.integrityfamily.legado.initializer;

import com.integrityfamily.legado.domain.FamilyLegacy;
import com.integrityfamily.legado.domain.FamilyValue;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.legado.repository.FamilyValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * LegadoDataInitializer — Constitución, Misión, Visión y Carta al Futuro
 * Familia: López Blanco (family_id = 1)
 * Patriarca: Jesús María López García
 *
 * IDEMPOTENTE: si ya existe un legado para la familia, no hace nada.
 * Se ejecuta después del LineageDataInitializer (@Order(10) → @Order(11)).
 * ════════════════════════════════════════════════════════════════════════════
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(11)
public class LegadoDataInitializer implements CommandLineRunner {

    private static final long FAMILY_ID = 1L;

    private final FamilyLegacyRepository legacyRepo;
    private final FamilyValueRepository  valueRepo;

    @Override
    @Transactional
    public void run(String... args) {

        // ── GUARDIA IDEMPOTENTE ────────────────────────────────────────────
        if (legacyRepo.findByFamilyId(FAMILY_ID).isPresent()) {
            log.info(">>>> [LEGADO] Legado López Blanco ya existe. Omitiendo sembrado.");
            return;
        }

        log.info(">>>> [LEGADO] Sembrando Constitución y Legado de la familia López Blanco...");

        // ── 1. LEGADO FAMILIAR ─────────────────────────────────────────────
        FamilyLegacy legacy = FamilyLegacy.builder()
                .familyId(FAMILY_ID)

                // Historia
                .historyLessons(
                    "Nuestros padres y abuelos nos enseñaron que la honestidad vale más que cualquier " +
                    "riqueza material. Aprendimos que el trabajo diario, la fe en Dios y el amor a la " +
                    "familia son los cimientos de una vida con sentido. El respeto a los mayores, la " +
                    "mesa compartida y la oración en familia fueron sus grandes legados.")
                .historyConserve(
                    "La tradición del rosario en familia, iniciada por la abuela paterna. " +
                    "Las reuniones dominicales con la mesa puesta y la cocina como espacio de encuentro. " +
                    "El valor de dar la palabra y cumplirla. El hábito del trabajo honrado como forma " +
                    "de dignidad personal y familiar.")
                .historyAvoidErrors(
                    "La improvisación en las decisiones importantes de la familia. " +
                    "Dejar pasar los conflictos sin resolverlos con claridad y amor. " +
                    "La falta de comunicación abierta entre padres e hijos sobre el futuro y el dinero. " +
                    "No documentar la historia familiar para las generaciones siguientes.")
                .historyToLeave(
                    "Un sistema de gobierno familiar consciente que trascienda generaciones. " +
                    "Una historia documentada para que nuestros nietos sepan de dónde vienen. " +
                    "Propiedades y ahorro como herramienta de libertad, no de lujo. " +
                    "Y sobre todo: el apellido López Blanco asociado a integridad, fe y servicio.")
                .historyRecognition(
                    "Honramos con profundo amor y gratitud a nuestros padres y abuelos. " +
                    "A Jesús María López García padre, quien trabajó incansablemente para que sus hijos " +
                    "tuvieran lo que él no tuvo. A Mariana Blanco Enríquez, corazón y columna del hogar, " +
                    "quien convirtió cada momento ordinario en un acto de amor extraordinario. " +
                    "Su misión fue cumplida con excelencia. Este árbol de evolución es su legado vivo.")

                // Constitución
                .constitutionFamilyName("Familia López Blanco")
                .constitutionYear(1979)
                .foundingPrinciple(
                    "Somos una familia fundada en la fe, la integridad y el amor consciente. " +
                    "Creemos que cada generación tiene la responsabilidad de dejar el mundo " +
                    "— y especialmente su familia — mejor de como lo encontró. " +
                    "Nuestro apellido es nuestro compromiso: López Blanco significa honrar " +
                    "el pasado, gobernar el presente y sembrar el futuro.")
                .commitments(
                    "1. Nos reunimos en familia al menos una vez al mes para revisar, celebrar y orar juntos.\n" +
                    "2. Cada miembro tiene voz en las decisiones que afectan a toda la familia.\n" +
                    "3. Invertimos en la educación y el crecimiento de cada integrante de la familia.\n" +
                    "4. Documentamos nuestra historia para que las generaciones futuras conozcan sus raíces.\n" +
                    "5. Resolvemos los conflictos con la palabra, nunca con el silencio o la violencia.\n" +
                    "6. Celebramos los logros de cada miembro como si fueran logros de todos.")
                .neverDo(
                    "1. Nunca permitiremos que el dinero o el éxito individual destruya la unidad familiar.\n" +
                    "2. Nunca dejaremos a un miembro de la familia solo en una crisis sin acompañamiento.\n" +
                    "3. Nunca hablaremos mal de un miembro de la familia fuera del hogar.\n" +
                    "4. Nunca permitiremos que la improvisación reemplace al gobierno familiar consciente.\n" +
                    "5. Nunca ignoraremos los valores y la historia que nos dieron origen.")
                .conflictResolution(
                    "Ante cualquier conflicto familiar, seguimos estos pasos:\n" +
                    "1. Pausa: tomamos 24 horas antes de hablar en caliente.\n" +
                    "2. Escucha: cada parte habla sin ser interrumpida.\n" +
                    "3. Reconocimiento: cada uno reconoce la parte de verdad del otro.\n" +
                    "4. Acuerdo: buscamos una solución que honre a ambas partes.\n" +
                    "5. Cierre: cerramos el conflicto con un abrazo o gesto de reconciliación.\n" +
                    "En conflictos mayores, convocamos al Consejo Familiar con Jesús María como mediador.")

                // Misión & Visión
                .familyMission(
                    "Somos la familia López Blanco: una comunidad de amor, fe y transformación. " +
                    "Nuestra misión es ser una familia íntegra que honra su historia, " +
                    "gobierna conscientemente su presente y siembra con sabiduría su futuro. " +
                    "Cada miembro es un guardián del legado y un sembrador de valores " +
                    "para las generaciones que vendrán.")
                .familyVision(
                    "En 2040, la familia López Blanco será reconocida por sus propios miembros " +
                    "como una familia de referencia en integridad, unidad y propósito. " +
                    "Nuestros hijos y nietos conocerán sus raíces, vivirán sus valores y " +
                    "continuarán el árbol de evolución generacional que hoy estamos construyendo " +
                    "con amor, consciencia y determinación.")
                .familyTagline("Raíces profundas · Frutos abundantes · Legado eterno")

                // Carta al Futuro
                .letterFrom("Jesús María López García, Patriarca · 2026")
                .letterTo("Mis queridos hijos, nietos y descendientes de la familia López Blanco")
                .letterOpenInYear(2046)
                .letterSealed(false)
                .letterContent(
                    "Querida familia,\n\n" +
                    "Si estás leyendo esta carta, significa que el tiempo ha hecho su obra y que " +
                    "las semillas que plantamos con tanto amor han dado fruto.\n\n" +
                    "Quiero que sepas que cada decisión que tomé, cada sacrificio, cada madrugada " +
                    "de trabajo y cada momento de oración, fue pensado en ustedes. No en los que " +
                    "estaban conmigo entonces, sino en los que vendrían después. En ti.\n\n" +
                    "Te pido tres cosas:\n\n" +
                    "Primera: Conoce tus raíces. Este árbol de evolución que construimos con " +
                    "Integrity Family no es un ejercicio académico — es la memoria viva de " +
                    "personas reales que amaron, sufrieron y triunfaron para que tú pudieras " +
                    "estar aquí leyendo esto.\n\n" +
                    "Segunda: Honra el apellido. López Blanco no es solo un nombre — es un " +
                    "compromiso de integridad que cada generación ha renovado. Que tu vida sea " +
                    "la prueba de que el apellido vale más que cualquier herencia material.\n\n" +
                    "Tercera: Sigue gobernando la familia con consciencia. Los sistemas cambian, " +
                    "las tecnologías evolucionan, pero el amor familiar bien gobernado es " +
                    "el único activo que no se deprecia.\n\n" +
                    "Les amé entonces. Los amo ahora desde donde esté. Y sé — con la certeza " +
                    "de quien plantó el árbol — que ustedes serán su fruto más hermoso.\n\n" +
                    "Con todo mi amor y mi legado,\n" +
                    "Jesús María López García\n" +
                    "Patriarca de la Familia López Blanco\n" +
                    "Integrity Family — Colombia, 2026")
                .build();

        legacyRepo.save(legacy);

        // ── 2. VALORES FAMILIARES ──────────────────────────────────────────
        List<FamilyValue> valores = List.of(
                FamilyValue.builder().familyId(FAMILY_ID).icon("🙏").name("Fe")
                        .description("La fe en Dios y en la familia es el fundamento de todo lo que hacemos. " +
                                     "Antes de cada decisión importante, oramos. " +
                                     "La fe nos une cuando todo lo demás tira hacia lados distintos.")
                        .sortOrder(1).build(),

                FamilyValue.builder().familyId(FAMILY_ID).icon("🤝").name("Integridad")
                        .description("Hacemos lo que decimos y decimos lo que hacemos. " +
                                     "Nuestro apellido es nuestra marca: López Blanco significa palabra cumplida, " +
                                     "trato justo y conducta honrada en público y en privado.")
                        .sortOrder(2).build(),

                FamilyValue.builder().familyId(FAMILY_ID).icon("❤️").name("Amor Consciente")
                        .description("No basta con amar — hay que amar con intención y con presencia. " +
                                     "El amor en nuestra familia se expresa en tiempo dedicado, " +
                                     "en escucha real, en celebración de los logros ajenos y en " +
                                     "acompañamiento en las crisis.")
                        .sortOrder(3).build(),

                FamilyValue.builder().familyId(FAMILY_ID).icon("📚").name("Aprendizaje")
                        .description("Cada generación debe ser más sabia que la anterior. " +
                                     "Aprendemos de los errores propios y ajenos. " +
                                     "Invertimos en educación como la mejor herencia que podemos dar. " +
                                     "El conocimiento que nadie puede quitarte.")
                        .sortOrder(4).build(),

                FamilyValue.builder().familyId(FAMILY_ID).icon("🌱").name("Responsabilidad Generacional")
                        .description("Cada decisión que tomamos hoy afecta a los que vendrán. " +
                                     "Somos mayordomos del legado que recibimos y sembradores " +
                                     "del legado que dejaremos. No vivimos solo para nosotros.")
                        .sortOrder(5).build(),

                FamilyValue.builder().familyId(FAMILY_ID).icon("💪").name("Resiliencia")
                        .description("La familia López Blanco ha superado adversidades en cada generación. " +
                                     "Caemos, pero nos levantamos juntos. " +
                                     "Las crisis nos fortalecen cuando las enfrentamos con unidad, " +
                                     "fe y el apoyo mutuo que solo da una familia íntegra.")
                        .sortOrder(6).build(),

                FamilyValue.builder().familyId(FAMILY_ID).icon("🏛️").name("Legado")
                        .description("Pensamos en el largo plazo. Plantamos árboles bajo cuya sombra " +
                                     "nunca nos sentaremos, sabiendo que nuestros hijos y nietos sí lo harán. " +
                                     "Todo lo que construimos tiene un destinatario: las generaciones futuras.")
                        .sortOrder(7).build()
        );

        valueRepo.saveAll(valores);

        log.info(">>>> [LEGADO] ✓ Legado sembrado — Constitución, Misión/Visión, Historia, " +
                 "Carta al Futuro y {} valores familiares.", valores.size());
    }
}
