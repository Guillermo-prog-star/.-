package com.integrityfamily.lineage.initializer;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.lineage.domain.*;
import com.integrityfamily.lineage.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ════════════════════════════════════════════════════════════════════════════
 * LineageDataInitializer — Árbol de Evolución y Legado Familiar
 * Familia: López Blanco (family_id = 1)
 * Patriarca: Jesús María López García (willibla1957@gmail.com)
 *
 * Es IDEMPOTENTE: si el linaje ya existe, no hace nada.
 * Sobrevive reinicios, drops y restauraciones de BD de prueba.
 *
 * ESTRUCTURA GENERACIONAL:
 *   Gen -2 → Bisabuelos (raíces por reconstruir)
 *   Gen -1 → Abuelos paternos y maternos de Jesús María
 *   Gen  0 → Jesús María López García ⚓ + Mariana Blanco Enríquez (ANCLA)
 *   Gen +1 → William, Luz Marina, Jesús María, Sandra Patricia, Martha Cecilia
 *   Gen +2 → Nietos (futura generación — por agregar)
 * ════════════════════════════════════════════════════════════════════════════
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class LineageDataInitializer implements CommandLineRunner {

    private static final long FAMILY_ID = 1L; // families.id = 1 → "Lopez Blanco" IF-2026-0001

    private final FamilyRepository               familyRepo;
    private final FamilyLineageRepository        lineageRepo;
    private final LineageMemberRepository        memberRepo;
    private final LineageRelationshipRepository  relRepo;
    private final LineageGenerationInfoRepository genInfoRepo;

    @Override
    @Transactional
    public void run(String... args) {

        // ── GUARDIA IDEMPOTENTE ────────────────────────────────────────────
        if (lineageRepo.existsByFamilyId(FAMILY_ID)) {
            log.info(">>>> [LINAJE] Linaje López Blanco ya existe. Omitiendo sembrado.");
            return;
        }

        Family family = familyRepo.findById(FAMILY_ID).orElse(null);
        if (family == null) {
            log.warn(">>>> [LINAJE] Familia ID {} no encontrada. Backend arrancó antes que Flyway?", FAMILY_ID);
            return;
        }

        log.info(">>>> [LINAJE] Sembrando árbol generacional: {} ({})", family.getName(), family.getFamilyCode());

        // ── 1. LINAJE ──────────────────────────────────────────────────────
        FamilyLineage lineage = lineageRepo.save(FamilyLineage.builder()
                .family(family)
                .lineageCode("IF-LIN-LOPBLA-0001")
                .title("Árbol de Evolución · Familia López Blanco")
                .description(
                    "Historia, valores y legado de las generaciones de la familia " +
                    "López Blanco. Patriarca: Jesús María López García. " +
                    "Construido sistemáticamente a través de Integrity Family.")
                .anchorGeneration(0)    // Jesús María = Generación Responsable (ancla)
                .maxPastGen(-2)         // Hasta bisabuelos
                .maxFutureGen(2)        // Hasta nietos
                .visionStatement(
                    "Que las decisiones que tomamos hoy sean el legado de integridad, " +
                    "fe y trabajo que nuestros hijos y nietos reciban mañana.")
                .foundingYear("~1880")
                .build());

        // ── 2. CONTEXTO POR GENERACIÓN ─────────────────────────────────────
        seedGenerationContext(lineage);

        // ── 3. MIEMBROS ────────────────────────────────────────────────────
        List<LineageMember> members = seedMembers(lineage);

        // ── 4. RELACIONES ──────────────────────────────────────────────────
        seedRelationships(lineage, members);

        log.info(">>>> [LINAJE] ✓ Árbol sembrado — {} miembros, {} relaciones.",
                members.size(), relRepo.findByLineageId(lineage.getId()).size());
    }

    // ══════════════════════════════════════════════════════════════════════
    // CONTEXTO POR GENERACIÓN
    // ══════════════════════════════════════════════════════════════════════

    private void seedGenerationContext(FamilyLineage lineage) {

        // Gen -2: Bisabuelos
        genInfoRepo.save(LineageGenerationInfo.builder()
                .lineage(lineage).generationLevel(-2).generationType("founding")
                .title("Los Ancestros Fundadores")
                .summary("Raíces de la familia López y de la familia Blanco/García. " +
                         "Generación que fundó el núcleo familiar en tierra colombiana.")
                .context("Primera mitad del siglo XX — Colombia rural, economía agrícola, " +
                         "grandes familias, valores profundamente religiosos y comunitarios.")
                .keyChallenge("Sobrevivir épocas de violencia, pobreza y escasez. " +
                              "Mantener la familia unida en condiciones adversas.")
                .keyAchievement("Transmitieron fe, trabajo y honradez como ADN familiar " +
                                "que persiste hasta hoy en todos sus descendientes.")
                .periodStart("1880").periodEnd("1940").build());

        // Gen -1: Abuelos
        genInfoRepo.save(LineageGenerationInfo.builder()
                .lineage(lineage).generationLevel(-1).generationType("builder")
                .title("La Generación Constructora — Abuelos de Jesús María")
                .summary("Generación que construyó el puente entre el campo y la ciudad, " +
                         "entre la escasez y la oportunidad. Forjó los valores que Jesús María " +
                         "heredó y transmitió a sus hijos.")
                .context("Mediados del siglo XX — Colombia en transformación. " +
                         "La Violencia (1948), urbanización acelerada, primeras oportunidades educativas.")
                .keyChallenge("Migrar, adaptarse, mantener la identidad familiar " +
                              "mientras todo cambiaba a su alrededor.")
                .keyAchievement("Educaron a sus hijos con valores sólidos y les abrieron " +
                                "puertas que ellos nunca tuvieron.")
                .periodStart("1920").periodEnd("1980").build());

        // Gen 0: Jesús María + Mariana (ANCLA)
        genInfoRepo.save(LineageGenerationInfo.builder()
                .lineage(lineage).generationLevel(0).generationType("responsible")
                .title("La Generación Responsable — Jesús María y Mariana")
                .summary("El patriarca Jesús María López García y la matrona Mariana Blanco Enríquez. " +
                         "Forjaron un hogar de 5 hijos con amor, sacrificio y fe. " +
                         "Jesús María lidera hoy el gobierno familiar a través de Integrity Family.")
                .context("Segunda mitad del siglo XX y siglo XXI — Colombia en paz relativa, " +
                         "acceso a educación superior para los hijos, era digital.")
                .keyChallenge("Criar 5 hijos con integridad en un mundo de crecientes distracciones. " +
                              "Mantener la cohesión familiar mientras cada hijo construye su propia vida.")
                .keyAchievement("5 hijos formados en valores, con educación y visión propia. " +
                                "Un hogar que se convirtió en referente de familia íntegra.")
                .periodStart("1957").periodEnd("2040").build());

        // Gen +1: Los hijos
        genInfoRepo.save(LineageGenerationInfo.builder()
                .lineage(lineage).generationLevel(1).generationType("current")
                .title("La Generación Actual — Hijos de Jesús María y Mariana")
                .summary("William, Luz Marina, Jesús María, Sandra Patricia y Martha Cecilia. " +
                         "Cada uno porta el apellido López Blanco como sello de su origen. " +
                         "Son la evidencia viva del legado de sus padres.")
                .context("Generación nacida entre los 80s y 90s. Nativos de la transición " +
                         "analógico-digital. Colombianos con acceso a educación y mundo global.")
                .keyChallenge("Mantener la unidad familiar mientras construyen sus propias vidas, " +
                              "familias y proyectos. Honrar el apellido que llevan.")
                .keyAchievement("Por construir — son los protagonistas activos del siguiente capítulo " +
                                "de la historia familiar.")
                .periodStart("1980").periodEnd("2060").build());

        // Gen +2: Nietos
        genInfoRepo.save(LineageGenerationInfo.builder()
                .lineage(lineage).generationLevel(2).generationType("future")
                .title("La Generación Futura — Nietos López Blanco")
                .summary("Los nietos de Jesús María y Mariana. Recibirán el legado completo " +
                         "del árbol que hoy se está construyendo conscientemente.")
                .context("Siglo XXI avanzado — el mundo que serán depende de lo que " +
                         "sembramos hoy. Integrity Family trabaja para que su herencia sea sólida.")
                .keyChallenge("Mantener viva la identidad familiar en un mundo de transformaciones " +
                              "sin precedentes.")
                .keyAchievement("Serán la evidencia viva de que el árbol de evolución " +
                                "de la familia López Blanco trasciende generaciones.")
                .periodStart("2005").periodEnd("2080").build());
    }

    // ══════════════════════════════════════════════════════════════════════
    // MIEMBROS
    // ══════════════════════════════════════════════════════════════════════

    private List<LineageMember> seedMembers(FamilyLineage lineage) {

        // ── GEN -2: BISABUELOS (raíces por reconstruir) ───────────────────
        LineageMember bisAbueloPatLopez = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("López")
                .avatarInitials("?L").avatarColor("#3b1c08")
                .generation(-2).generationType("founding").status("deceased")
                .birthYear(1885).birthYearApproximate(true).deathYear(1960)
                .origin("Colombia").roleLabel("Bisabuelo Paterno")
                .confidenceLevel(20).dataSource("Tradición oral")
                .story("Bisabuelo paterno de Jesús María. Historia por reconstruir con la familia.")
                .valores("Trabajo, fe y persistencia")
                .build());

        LineageMember bisAbuelaPatLopez = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("García")
                .avatarInitials("?G").avatarColor("#3b1c08")
                .generation(-2).generationType("founding").status("deceased")
                .birthYear(1890).birthYearApproximate(true)
                .origin("Colombia").roleLabel("Bisabuela Paterna")
                .confidenceLevel(15).dataSource("Tradición oral")
                .build());

        LineageMember bisAbueloMatBlanco = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("Blanco")
                .avatarInitials("?B").avatarColor("#3b1c08")
                .generation(-2).generationType("founding").status("deceased")
                .birthYear(1888).birthYearApproximate(true).deathYear(1958)
                .origin("Colombia").roleLabel("Bisabuelo Materno")
                .confidenceLevel(20).dataSource("Tradición oral")
                .valores("Honestidad y servicio comunitario")
                .build());

        LineageMember bisAbuelaMatEnriquez = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("Enríquez")
                .avatarInitials("?E").avatarColor("#3b1c08")
                .generation(-2).generationType("founding").status("deceased")
                .birthYear(1892).birthYearApproximate(true)
                .origin("Colombia").roleLabel("Bisabuela Materna")
                .confidenceLevel(15).dataSource("Tradición oral")
                .build());

        // ── GEN -1: ABUELOS DE JESÚS MARÍA ───────────────────────────────
        LineageMember abueloPatJesus = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("López García")
                .avatarInitials("?L").avatarColor("#5c2d0a")
                .generation(-1).generationType("builder").status("deceased")
                .birthYear(1920).birthYearApproximate(true).deathYear(1995)
                .origin("Colombia").roleLabel("Abuelo Paterno")
                .confidenceLevel(40).dataSource("Testimonio familiar")
                .story("Abuelo paterno de Jesús María. Padre que transmitió el valor del trabajo.")
                .valores("Disciplina, trabajo arduo, amor a la tierra")
                .aprendizajes("Que con esfuerzo y fe se puede salir adelante")
                .build());

        LineageMember abuelaPatJesus = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("?")
                .avatarInitials("?").avatarColor("#5c2d0a")
                .generation(-1).generationType("builder").status("deceased")
                .birthYear(1924).birthYearApproximate(true)
                .origin("Colombia").roleLabel("Abuela Paterna")
                .confidenceLevel(35).dataSource("Testimonio familiar")
                .tradiciones("La oración del rosario en familia y las reuniones de Navidad")
                .build());

        LineageMember abueloMatBlanco = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("Blanco")
                .avatarInitials("?B").avatarColor("#5c2d0a")
                .generation(-1).generationType("builder").status("deceased")
                .birthYear(1918).birthYearApproximate(true)
                .origin("Colombia").roleLabel("Abuelo Materno")
                .confidenceLevel(40).dataSource("Testimonio familiar")
                .story("Abuelo materno de Mariana Blanco Enríquez.")
                .build());

        LineageMember abuelaMatEnriquez = saveMember(lineage, LineageMember.builder()
                .firstName("?").lastName("Enríquez")
                .avatarInitials("?E").avatarColor("#5c2d0a")
                .generation(-1).generationType("builder").status("deceased")
                .birthYear(1922).birthYearApproximate(true)
                .origin("Colombia").roleLabel("Abuela Materna")
                .confidenceLevel(35).dataSource("Testimonio familiar")
                .build());

        // ── GEN 0: JESÚS MARÍA Y MARIANA ─────────────────────────────────
        LineageMember jesusMariaLopez = saveMember(lineage, LineageMember.builder()
                .firstName("Jesús María")
                .lastName("López García")
                .avatarInitials("JM")
                .avatarColor("#b45309")
                .generation(0).generationType("responsible")
                .isAnchor(true)           // ← PATRIARCA — nodo ancla del árbol
                .status("alive")
                .birthYear(1957)
                .birthYearApproximate(false)
                .origin("Colombia")
                .roleLabel("Patriarca")
                .confidenceLevel(100)
                .dataSource("Registro oficial")
                .familyMemberId(1L)       // family_members.id = 1 "Jesus Maria" PADRE
                .story(
                    "Jesús María López García, nacido en 1957. Patriarca de la familia López Blanco. " +
                    "Esposo de Mariana Blanco Enríquez, padre de 5 hijos. " +
                    "Fundador del sistema Integrity Family para la transformación generacional de familias colombianas.")
                .valores("Integridad, fe, responsabilidad familiar y visión multigeneracional")
                .aprendizajes(
                    "Que una familia necesita gobierno consciente, no solo amor. " +
                    "Que el legado se construye con decisiones diarias, no con grandes gestos.")
                .erroresSuperados(
                    "La improvisación en el gobierno familiar de generaciones anteriores. " +
                    "La falta de un sistema que preserve la historia y dirija el futuro.")
                .tradiciones("La reunión familiar mensual de revisión, celebración y oración")
                .misionesCumplidas(
                    "Formar 5 hijos con valores sólidos. " +
                    "Crear Integrity Family como plataforma de transformación generacional.")
                .legadoPersonal(
                    "Un sistema vivo de gobierno familiar y un árbol de evolución " +
                    "que las generaciones López Blanco heredarán, enriquecerán y multiplicarán.")
                .build());

        LineageMember marianaBlanco = saveMember(lineage, LineageMember.builder()
                .firstName("Mariana")
                .lastName("Blanco Enríquez")
                .avatarInitials("MB")
                .avatarColor("#b45309")
                .generation(0).generationType("responsible")
                .status("alive")
                .birthYear(1960).birthYearApproximate(true)
                .origin("Colombia")
                .roleLabel("Matrona")
                .confidenceLevel(95)
                .dataSource("Registro familiar")
                .familyMemberId(2L)       // family_members.id = 2 "Mariana Blanco Enriquez" MADRE
                .story(
                    "Mariana Blanco Enríquez, matrona de la familia López Blanco. " +
                    "Compañera de vida de Jesús María. Co-artífice del hogar y de los 5 hijos.")
                .valores("Amor incondicional, paciencia, fe y fortaleza de hogar")
                .aprendizajes("Que el amor que se da en el hogar es la inversión más rentable de la vida")
                .tradiciones("Las reuniones dominicales, la oración diaria, la cocina familiar como espacio de encuentro")
                .legadoPersonal("Un hogar donde todos sus hijos saben que son amados y tienen raíces firmes")
                .build());

        // ── GEN +1: LOS 5 HIJOS ───────────────────────────────────────────
        LineageMember william = saveMember(lineage, LineageMember.builder()
                .firstName("William")
                .lastName("López Blanco")
                .avatarInitials("WL")
                .avatarColor("#d97706")
                .generation(1).generationType("current")
                .status("alive")
                .birthYear(1980).birthYearApproximate(true)
                .origin("Colombia")
                .roleLabel("Guardián del Legado")
                .confidenceLevel(95)
                .dataSource("Registro familiar")
                .familyMemberId(3L)       // family_members.id = 3
                .story(
                    "William López Blanco, hijo primogénito de Jesús María y Mariana. " +
                    "Responsable de implementar el sistema Integrity Family en la familia. " +
                    "Guardián del legado generacional.")
                .valores("Liderazgo familiar, visión sistémica, compromiso con el legado")
                .misionesCumplidas("Implementación de Integrity Family como herramienta de gobierno familiar")
                .build());

        LineageMember luzMarina = saveMember(lineage, LineageMember.builder()
                .firstName("Luz Marina")
                .lastName("López Blanco")
                .avatarInitials("LM")
                .avatarColor("#d97706")
                .generation(1).generationType("current")
                .status("alive")
                .birthYear(1982).birthYearApproximate(true)
                .origin("Colombia")
                .roleLabel("Hija")
                .confidenceLevel(95)
                .dataSource("Registro familiar")
                .familyMemberId(4L)       // family_members.id = 4
                .story("Luz Marina López Blanco, hija de Jesús María y Mariana.")
                .build());

        LineageMember jesusMarijaHijo = saveMember(lineage, LineageMember.builder()
                .firstName("Jesús María")
                .lastName("López Blanco")
                .avatarInitials("JML")
                .avatarColor("#d97706")
                .generation(1).generationType("current")
                .status("alive")
                .birthYear(1984).birthYearApproximate(true)
                .origin("Colombia")
                .roleLabel("Hijo")
                .confidenceLevel(95)
                .dataSource("Registro familiar")
                .familyMemberId(5L)       // family_members.id = 5
                .story("Jesús María López Blanco, hijo de Jesús María y Mariana. Porta el nombre del padre.")
                .build());

        LineageMember sandraPatricia = saveMember(lineage, LineageMember.builder()
                .firstName("Sandra Patricia")
                .lastName("López Blanco")
                .avatarInitials("SP")
                .avatarColor("#d97706")
                .generation(1).generationType("current")
                .status("alive")
                .birthYear(1986).birthYearApproximate(true)
                .origin("Colombia")
                .roleLabel("Hija")
                .confidenceLevel(95)
                .dataSource("Registro familiar")
                .familyMemberId(6L)       // family_members.id = 6
                .story("Sandra Patricia López Blanco, hija de Jesús María y Mariana.")
                .build());

        LineageMember marthaCecilia = saveMember(lineage, LineageMember.builder()
                .firstName("Martha Cecilia")
                .lastName("López Blanco")
                .avatarInitials("MC")
                .avatarColor("#d97706")
                .generation(1).generationType("current")
                .status("alive")
                .birthYear(1988).birthYearApproximate(true)
                .origin("Colombia")
                .roleLabel("Hija")
                .confidenceLevel(95)
                .dataSource("Registro familiar")
                .familyMemberId(7L)       // family_members.id = 7
                .story("Martha Cecilia López Blanco, hija menor de Jesús María y Mariana.")
                .build());

        // ── GEN +2: NIETOS (por agregar desde la UI) ──────────────────────
        // Los nietos se agregarán progresivamente desde la interfaz de Integrity Family
        // a medida que la familia los incorpore al árbol.

        log.info(">>>> [LINAJE] 15 miembros sembrados en 5 generaciones.");
        return List.of(
                bisAbueloPatLopez, bisAbuelaPatLopez,
                bisAbueloMatBlanco, bisAbuelaMatEnriquez,
                abueloPatJesus, abuelaPatJesus,
                abueloMatBlanco, abuelaMatEnriquez,
                jesusMariaLopez, marianaBlanco,
                william, luzMarina, jesusMarijaHijo, sandraPatricia, marthaCecilia
        );
    }

    // ══════════════════════════════════════════════════════════════════════
    // RELACIONES
    // Índices en la lista members:
    //  0=bisAbueloPatLopez    1=bisAbuelaPatLopez
    //  2=bisAbueloMatBlanco   3=bisAbuelaMatEnriquez
    //  4=abueloPatJesus       5=abuelaPatJesus
    //  6=abueloMatBlanco      7=abuelaMatEnriquez
    //  8=jesusMariaLopez      9=marianaBlanco
    // 10=william             11=luzMarina
    // 12=jesusMariaHijo      13=sandraPatricia
    // 14=marthaCecilia
    // ══════════════════════════════════════════════════════════════════════

    private void seedRelationships(FamilyLineage lineage, List<LineageMember> m) {
        // Parejas bisabuelos
        saveRel(lineage, m.get(0),  m.get(1),  "biological", true);
        saveRel(lineage, m.get(2),  m.get(3),  "biological", true);
        // Parejas abuelos
        saveRel(lineage, m.get(4),  m.get(5),  "biological", true);
        saveRel(lineage, m.get(6),  m.get(7),  "biological", true);
        // Bisabuelos → Abuelos
        saveRel(lineage, m.get(0),  m.get(4),  "biological", false); // BisAb López → Ab López
        saveRel(lineage, m.get(2),  m.get(6),  "biological", false); // BisAb Blanco → Ab Blanco
        // Pareja patriarcal
        saveRel(lineage, m.get(8),  m.get(9),  "biological", true);  // Jesús María ↔ Mariana
        // Abuelos → Patriarcas
        saveRel(lineage, m.get(4),  m.get(8),  "biological", false); // Ab.Pat → Jesús María
        saveRel(lineage, m.get(6),  m.get(9),  "biological", false); // Ab.Mat → Mariana
        // Patriarcas → 5 hijos
        saveRel(lineage, m.get(8),  m.get(10), "biological", false); // Jesús María → William
        saveRel(lineage, m.get(8),  m.get(11), "biological", false); // Jesús María → Luz Marina
        saveRel(lineage, m.get(8),  m.get(12), "biological", false); // Jesús María → Jesús María hijo
        saveRel(lineage, m.get(8),  m.get(13), "biological", false); // Jesús María → Sandra Patricia
        saveRel(lineage, m.get(8),  m.get(14), "biological", false); // Jesús María → Martha Cecilia
        saveRel(lineage, m.get(9),  m.get(10), "biological", false); // Mariana → William
        saveRel(lineage, m.get(9),  m.get(11), "biological", false); // Mariana → Luz Marina
        saveRel(lineage, m.get(9),  m.get(12), "biological", false); // Mariana → Jesús María hijo
        saveRel(lineage, m.get(9),  m.get(13), "biological", false); // Mariana → Sandra Patricia
        saveRel(lineage, m.get(9),  m.get(14), "biological", false); // Mariana → Martha Cecilia
    }

    // ── HELPERS ────────────────────────────────────────────────────────────

    private LineageMember saveMember(FamilyLineage lineage, LineageMember m) {
        m.setLineage(lineage);
        return memberRepo.save(m);
    }

    private void saveRel(FamilyLineage lineage,
                         LineageMember from, LineageMember to,
                         String type, boolean isCouple) {
        relRepo.save(LineageRelationship.builder()
                .lineage(lineage).fromMember(from).toMember(to)
                .relationshipType(type).isCouple(isCouple).build());
    }
}
