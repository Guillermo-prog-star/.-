package com.integrityfamily.risk.service;

import com.integrityfamily.adaptive.AdaptivePlanService;
import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.common.event.EventPublisher;
import com.integrityfamily.common.event.EventTopics;
import com.integrityfamily.common.event.FamilyCrisisEvent;
import com.integrityfamily.common.event.FamilyIcfRecalculatedEvent;
import com.integrityfamily.domain.CriticalDay;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.CriticalDayRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * SDD IMPLEMENTATION: Gestión unificada de Protocolos Sentinel y Registro Histórico.
 * Integra el almacenamiento en base de datos y la generación de guías de contención de IA en tiempo real.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CrisisServiceImpl implements CrisisService {

    private final CriticalDayRepository repository;
    private final FamilyRepository familyRepository;
    private final RiskSnapshotRepository riskSnapshotRepository;
    private final AiProvider aiProvider;
    private final ContextSynthesizer contextSynthesizer;
    private final WhatsAppService whatsAppService;
    private final EventPublisher eventPublisher;
    private final AdaptivePlanService adaptivePlanService;

    @Override
    @Transactional
    public CriticalDay registerCrisis(Long familyId, Long memberId, String category, String description,
            String emotion) {
        log.warn("🚨 [CRISIS-REG] Nueva crisis registrada: Familia {}, Categoría {}, Emoción {}",
                familyId, category, emotion);

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada: " + familyId, "NOT_FOUND", HttpStatus.NOT_FOUND));

        // 1. Sintetizar el contexto clínico completo de la familia
        log.info("[CRISIS] Sintetizando contexto para generación de contención...");
        AiContext context = contextSynthesizer.synthesize(family, "CRISIS");

        // 2. Formular prompt clínico y solicitar contención empática a Claude
        String prompt = String.format(
                "ALERTA SENTINEL: Se ha registrado una situación crítica en el hogar.\n" +
                "Categoría del conflicto: %s\n" +
                "Emoción prevalente: %s\n" +
                "Descripción del incidente: \"%s\"\n\n" +
                "Como Mentor de Integridad Familiar, genera una guía de contención de crisis inmediata, " +
                "empática, pragmática y clínicamente asertiva. Estructura la respuesta con 3 pasos breves de acción " +
                "y una frase reflexiva de cierre.",
                category, emotion != null ? emotion : "No especificada", description
        );

        String containmentGuide;
        try {
            log.info("[CRISIS] Solicitando inferencia inteligente a Claude...");
            containmentGuide = aiProvider.generateResponse(prompt, context);
        } catch (Exception e) {
            log.error("[CRISIS] Error en la llamada al proveedor de IA, utilizando respuesta de contención por defecto: {}", e.getMessage());
            containmentGuide = "### Guía de Contención Inmediata (Modo Seguro)\n" +
                    "1. **Respiración consciente:** Detengan el intercambio verbal y respiren hondo por 2 minutos.\n" +
                    "2. **Espacio seguro:** Permitan que cada integrante tome distancia física hasta que el ritmo cardíaco se regule.\n" +
                    "3. **Acuerdo de aplazamiento:** Acuerden dialogar sobre esto mañana bajo reglas de respeto mutuo.";
        }

        // 3. Persistir el día crítico en la base de datos
        CriticalDay cd = CriticalDay.builder()
                .familyId(familyId)
                .memberId(memberId)
                .category(category)
                .description(description)
                .emotion(emotion)
                .aiContainmentGuide(containmentGuide)
                .createdAt(LocalDateTime.now())
                .build();

        CriticalDay saved = repository.save(cd);
        log.info("[CRISIS] Incidente registrado y persistido con éxito en base de datos. ID: {}", saved.getId());

        // 4. Activar el estado Sentinel en la familia
        family.setSentinelActive(true);
        familyRepository.save(family);

        // ── CASCADE SISTÉMICA (FALLA 3 resuelta) ────────────────────────────
        // Una crisis NO puede ser un módulo aislado. Debe impactar:
        // ICF + planes + prioridad IA + alertas + seguimiento + intervención

        // 4b. Publicar evento de crisis al Event Bus Familiar
        FamilyCrisisEvent crisisEvent = FamilyCrisisEvent.of(
                familyId, saved.getId(), category, emotion, description);
        eventPublisher.publish(crisisEvent);
        log.info("[CRISIS] {} publicado al Event Bus para familia {}", EventTopics.CRISIS_TRIGGERED, familyId);

        // 4c. Recalcular ICF con penalización por crisis
        triggerIcfRecalculationAfterCrisis(family, saved);

        // 4d. Re-evaluar planes adaptativos — la crisis puede requerir soft-reset
        triggerAdaptivePlanReassessment(familyId);

        // 5. [FIX SDD] Despachar la guía de contención de Claude y las alertas por WhatsApp
        try {
            log.info("[CRISIS] Despachando guía de contención de Claude por WhatsApp a la Familia ID: {}", familyId);
            String mainMessage = String.format(
                "🚨 *ALERTA SENTINEL: REPORTE DE CRISIS EN EL HOGAR* 🚨\n\n" +
                "Se ha registrado una situación de *%s*.\n" +
                "Emoción prevalente: *%s*\n" +
                "Descripción: \"%s\"\n\n" +
                "💡 *GUÍA DE CONTENCIÓN EMOCIONAL DE CLAUDE:*\n\n%s",
                category, emotion != null ? emotion : "No especificada", description, containmentGuide
            );
            whatsAppService.sendToFamily(family, mainMessage);

            List<FamilyMember> members = family.getMembers();
            if (members != null && !members.isEmpty()) {
                log.info("[CRISIS] Enviando alertas de WhatsApp personalizadas por rol a {} miembros...", members.size());
                for (FamilyMember member : members) {
                    if (member.isActive()) {
                        String shortContext = String.format("Crisis de %s registrada. Emoción: %s.", 
                                category, emotion != null ? emotion : "Tensión");
                        whatsAppService.sendPersonalizedMessage(member, "CRISIS_ALERT", shortContext);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[CRISIS] Error enviando notificaciones de WhatsApp para crisis: {}", e.getMessage());
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CriticalDay> getHistory(Long familyId) {
        log.info("📊 [CRISIS-HIST] Recuperando historial para familia {}", familyId);
        return repository.findByFamilyIdOrderByCreatedAtDesc(familyId);
    }

    @Override
    @Transactional
    public void activateProtocol(Long familyId, String reason) {
        log.error("🛡️ [SENTINEL-ACTIVATE] Protocolo de emergencia forzado para ID: {} por: {}", familyId, reason);
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada", "NOT_FOUND", HttpStatus.NOT_FOUND));
        family.setSentinelActive(true);
        familyRepository.save(family);
    }

    @Override
    @Transactional
    public void handleMemberCrisis(Long familyId, List<FamilyMember> involvedMembers, String observation) {
        log.info("⚜️ [SENTINEL-FamilyMember] Procesando crisis para {} miembros en familia {}",
                involvedMembers != null ? involvedMembers.size() : 0, familyId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isUnderCrisis(Long familyId) {
        Family family = familyRepository.findById(familyId).orElse(null);
        return family != null && Boolean.TRUE.equals(family.getSentinelActive());
    }

    // ── Cascada sistémica privada ─────────────────────────────────────────────

    /**
     * Recalcula ICF con penalización por crisis y publica FamilyIcfRecalculatedEvent.
     *
     * La crisis fuerza el nivel de riesgo a CRITICO independientemente del ICF numérico.
     * El snapshot anterior se recupera para comparar evolución longitudinal.
     */
    private void triggerIcfRecalculationAfterCrisis(Family family, CriticalDay crisis) {
        try {
            // Obtener último snapshot para comparar delta
            Optional<RiskSnapshot> lastSnapshot = riskSnapshotRepository
                    .findByFamilyIdOrderByCreatedAtDesc(family.getId())
                    .stream().findFirst();

            double previousIcf = lastSnapshot.map(RiskSnapshot::getIcf).orElse(50.0);
            String previousRisk = lastSnapshot.map(RiskSnapshot::getRiskLevel).orElse("MODERADO");

            // Crisis → ICF penalizado (reduce 15 puntos, mínimo 10)
            double penalizedIcf = Math.max(10.0, previousIcf - 15.0);

            RiskSnapshot crisisSnapshot = RiskSnapshot.builder()
                    .family(family)
                    .icf(penalizedIcf)
                    .riskLevel("CRITICO")
                    .hasCrisis(true)
                    .consciousnessLevel(5)
                    .consciousnessLabel("Inconsciente")
                    .createdAt(LocalDateTime.now())
                    .build();
            riskSnapshotRepository.save(crisisSnapshot);

            FamilyIcfRecalculatedEvent icfEvent = new FamilyIcfRecalculatedEvent(
                    family.getId(),
                    previousIcf, penalizedIcf,
                    previousRisk, "CRITICO",
                    0.0, 0.0, 0.0, 0.0, // dimensiones no disponibles en modo crisis
                    "CRISIS",
                    LocalDateTime.now()
            );
            eventPublisher.publish(icfEvent);
            log.warn("[CRISIS-CASCADE] ICF penalizado: {} → {} | Riesgo: {} → CRITICO | familia {}",
                    String.format("%.1f", previousIcf), String.format("%.1f", penalizedIcf),
                    previousRisk, family.getId());

        } catch (Exception e) {
            log.error("[CRISIS-CASCADE] Error recalculando ICF post-crisis: {}", e.getMessage());
        }
    }

    /**
     * Re-evalúa los planes adaptativos después de una crisis.
     *
     * Una crisis puede requerir un soft-reset del plan porque:
     *   - La adherencia cae a 0% de golpe
     *   - La dimensión de comunicación colapsa
     *   - El contexto emocional invalida las misiones actuales
     */
    private void triggerAdaptivePlanReassessment(Long familyId) {
        try {
            adaptivePlanService.evaluateAndProposeForFamily(familyId);
            log.info("[CRISIS-CASCADE] Re-evaluación adaptativa completada para familia {}", familyId);
        } catch (Exception e) {
            log.error("[CRISIS-CASCADE] Error en re-evaluación adaptativa post-crisis: {}", e.getMessage());
        }
    }
}
