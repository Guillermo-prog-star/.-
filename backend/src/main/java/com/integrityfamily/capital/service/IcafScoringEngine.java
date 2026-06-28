package com.integrityfamily.capital.service;

import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.common.event.FamilyIcafRecalculatedEvent;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Motor de cálculo del Índice de Capital Familiar (ICaF).
 *
 * Fórmula: ICaF = Σ (dominio_i × peso_i), Σ pesos = 1.0
 *
 * Dominios y pesos:
 *   cohesion       20%  — ICF actual (4 dimensiones existentes)
 *   confianza      12%  — cuestionario de confianza mutua
 *   resiliencia    12%  — eventos críticos gestionados
 *   comunicacion   10%  — calidad comunicación (sprint dailies)
 *   autonomia       8%  — proyecto de vida activo por miembro
 *   bienestar       8%  — bienestar emocional
 *   proposito       8%  — propósito individual y familiar
 *   integracion     7%  — % miembros activos en sprints/rituales
 *   emprendimiento  5%  — proyectos económicos activos
 *   legado          5%  — transmisión de valores (módulos legado/dna)
 *   madurez         5%  — nivel evolutivo normalizado (1-5 → 0-100)
 *
 * Nivel de Madurez Familiar:
 *   1 — Supervivencia  ICaF < 30
 *   2 — Reactividad    ICaF 30-49
 *   3 — Organización   ICaF 50-64
 *   4 — Propósito      ICaF 65-79
 *   5 — Legado         ICaF ≥ 80
 *
 * Sprint S2: solo el dominio cohesion (ICF) está completamente instrumentado.
 * Los demás dominios usan estimaciones basadas en datos existentes hasta que
 * sus fuentes de datos propias se implementen (S3-S4).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IcafScoringEngine {

    private static final String ALGORITHM_VERSION = "ICAF_V1";

    // Pesos de los 11 dominios — deben sumar 1.0
    private static final double W_COHESION       = 0.20;
    private static final double W_CONFIANZA      = 0.12;
    private static final double W_RESILIENCIA    = 0.12;
    private static final double W_COMUNICACION   = 0.10;
    private static final double W_AUTONOMIA      = 0.08;
    private static final double W_BIENESTAR      = 0.08;
    private static final double W_PROPOSITO      = 0.08;
    private static final double W_INTEGRACION    = 0.07;
    private static final double W_EMPRENDIMIENTO = 0.05;
    private static final double W_LEGADO         = 0.05;
    private static final double W_MADUREZ        = 0.05;

    private final FamilyRepository familyRepository;
    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final FamilyCapitalSnapshotRepository snapshotRepo;
    private final IcafDomainResolver domainResolver;
    private final EventPublisher eventPublisher;

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Calcula y persiste el ICaF para una familia.
     * Publica FamilyIcafRecalculatedEvent al finalizar.
     *
     * @param familyId  ID de la familia
     * @param trigger   ASSESSMENT | SPRINT_CLOSE | CRITICAL_EVENT | SCHEDULED
     * @return resultado del cálculo
     */
    @Transactional
    public IcafResult compute(Long familyId, String trigger) {
        log.info("[ICaF] Calculando para familia {} | trigger={}", familyId, trigger);

        Family family = familyRepository.findById(familyId).orElse(null);
        if (family == null) {
            log.warn("[ICaF] Familia {} no encontrada — cálculo cancelado", familyId);
            return IcafResult.empty(familyId);
        }

        // Resolver los 11 dominios
        IcafDomains domains = domainResolver.resolve(familyId);

        // Calcular ICaF ponderado
        double icaf = round2(
            domains.cohesion()       * W_COHESION       +
            domains.confianza()      * W_CONFIANZA       +
            domains.resiliencia()    * W_RESILIENCIA     +
            domains.comunicacion()   * W_COMUNICACION    +
            domains.autonomia()      * W_AUTONOMIA       +
            domains.bienestar()      * W_BIENESTAR       +
            domains.proposito()      * W_PROPOSITO       +
            domains.integracion()    * W_INTEGRACION     +
            domains.emprendimiento() * W_EMPRENDIMIENTO  +
            domains.legado()         * W_LEGADO          +
            domains.madurezScore()   * W_MADUREZ
        );

        int madurez = computeMadurez(icaf);

        // ICaF anterior para el evento
        Optional<FamilyCapitalSnapshot> lastSnapshot =
                snapshotRepo.findTopByFamilyIdOrderByCreatedAtDesc(familyId);
        double previousIcaf   = lastSnapshot.map(FamilyCapitalSnapshot::getIcaf).orElse(icaf);
        int    previousMadurez = lastSnapshot.map(FamilyCapitalSnapshot::getMadurezNivel).orElse(madurez);

        // Persistir snapshot
        FamilyCapitalSnapshot snapshot = FamilyCapitalSnapshot.builder()
                .family(family)
                .icaf(icaf)
                .madurezNivel(madurez)
                .domCohesion(domains.cohesion())
                .domConfianza(domains.confianza())
                .domResiliencia(domains.resiliencia())
                .domComunicacion(domains.comunicacion())
                .domAutonomia(domains.autonomia())
                .domBienestar(domains.bienestar())
                .domProposito(domains.proposito())
                .domIntegracion(domains.integracion())
                .domEmprendimiento(domains.emprendimiento())
                .domLegado(domains.legado())
                .domMadurez(domains.madurezScore())
                .triggerType(trigger)
                .algorithmVersion(ALGORITHM_VERSION)
                .build();
        snapshotRepo.save(snapshot);

        // Publicar evento
        FamilyIcafRecalculatedEvent event = new FamilyIcafRecalculatedEvent(
                familyId,
                previousIcaf, icaf,
                previousMadurez, madurez,
                domains.cohesion(), domains.confianza(), domains.resiliencia(),
                domains.comunicacion(), domains.autonomia(), domains.bienestar(),
                domains.proposito(), domains.integracion(), domains.emprendimiento(),
                domains.legado(), domains.madurezScore(),
                trigger,
                LocalDateTime.now()
        );
        eventPublisher.publish(event);

        log.info("[ICaF] Familia {} | ICaF: {} → {} | Madurez: {} → {} | trigger={}",
                familyId,
                String.format("%.1f", previousIcaf), String.format("%.1f", icaf),
                previousMadurez, madurez, trigger);

        return new IcafResult(familyId, icaf, madurez, domains);
    }

    // ── Nivel de Madurez ─────────────────────────────────────────────────────

    public static int computeMadurez(double icaf) {
        if (icaf >= 80) return 5; // Legado
        if (icaf >= 65) return 4; // Propósito
        if (icaf >= 50) return 3; // Organización
        if (icaf >= 30) return 2; // Reactividad
        return 1;                  // Supervivencia
    }

    public static String madurezLabel(int nivel) {
        return switch (nivel) {
            case 5 -> "Legado";
            case 4 -> "Propósito";
            case 3 -> "Organización";
            case 2 -> "Reactividad";
            default -> "Supervivencia";
        };
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    // ── Tipos de resultado ────────────────────────────────────────────────────

    public record IcafResult(
            Long familyId,
            double icaf,
            int madurez,
            IcafDomains domains
    ) {
        public String madurezLabel() {
            return IcafScoringEngine.madurezLabel(madurez);
        }

        public static IcafResult empty(Long familyId) {
            return new IcafResult(familyId, 0.0, 1, IcafDomains.zeros());
        }
    }

    public record IcafDomains(
            double cohesion,
            double confianza,
            double resiliencia,
            double comunicacion,
            double autonomia,
            double bienestar,
            double proposito,
            double integracion,
            double emprendimiento,
            double legado,
            double madurezScore
    ) {
        public static IcafDomains zeros() {
            return new IcafDomains(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }
}
