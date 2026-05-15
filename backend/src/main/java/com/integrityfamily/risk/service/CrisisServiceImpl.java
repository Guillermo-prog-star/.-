package com.integrityfamily.risk.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.domain.CriticalDay;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.repository.CriticalDayRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.common.service.WhatsAppService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

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
    private final AiProvider aiProvider;
    private final ContextSynthesizer contextSynthesizer;
    private final WhatsAppService whatsAppService;

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
}
