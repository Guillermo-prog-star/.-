package com.integrityfamily.bitacora.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.ai.service.ContextSynthesizer;
import com.integrityfamily.bitacora.dto.SprintDtos.*;
import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SprintService {

    private final FamilyRepository familyRepository;
    private final FamilySprintRepository sprintRepository;
    private final SprintMissionRepository missionRepository;
    private final SprintDailyRepository dailyRepository;
    private final SprintRetrospectiveRepository retrospectiveRepository;
    
    private final AiProvider aiProvider;
    private final ContextSynthesizer contextSynthesizer;

    @Transactional(readOnly = true)
    public SprintResponse getActiveSprint(Long familyId) {
        Optional<FamilySprint> activeSprintOpt = sprintRepository.findActiveSprintForFamily(familyId);
        if (activeSprintOpt.isEmpty()) {
            return null;
        }
        return mapToSprintResponse(activeSprintOpt.get());
    }

    @Transactional(readOnly = true)
    public List<SprintResponse> getSprintHistory(Long familyId) {
        return sprintRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).stream()
                .map(this::mapToSprintResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public SprintResponse createSprint(Long familyId, CreateSprintRequest request) {
        log.info("🚀 [SPRINT] Creando nuevo sprint de evolución para Familia ID: {}", familyId);
        
        Optional<FamilySprint> activeSprintOpt = sprintRepository.findActiveSprintForFamily(familyId);
        if (activeSprintOpt.isPresent()) {
            throw new BusinessException("Ya existe un sprint activo para esta familia. Cierra o cancela el actual primero.", "SPRINT_ALREADY_ACTIVE", HttpStatus.CONFLICT);
        }

        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new BusinessException("Familia no encontrada para ID: " + familyId, "FAMILY_NOT_FOUND", HttpStatus.NOT_FOUND));

        int duration = request.durationDays() != null ? request.durationDays() : 7;
        if (duration != 7 && duration != 15) {
            throw new BusinessException("La duración del sprint debe ser de 7 o 15 días.", "INVALID_SPRINT_DURATION", HttpStatus.BAD_REQUEST);
        }

        FamilySprint sprint = FamilySprint.builder()
                .family(family)
                .objective(request.objective())
                .riskDimension(request.riskDimension() != null ? request.riskDimension().toLowerCase() : "comunicacion")
                .durationDays(duration)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(duration))
                .status("ACTIVE")
                .missions(new ArrayList<>())
                .build();

        FamilySprint savedSprint = sprintRepository.save(sprint);

        if (request.missions() != null && !request.missions().isEmpty()) {
            List<SprintMission> missions = request.missions().stream()
                    .map(desc -> SprintMission.builder()
                            .sprint(savedSprint)
                            .description(desc)
                            .status("PENDING")
                            .build())
                    .collect(Collectors.toList());
            missionRepository.saveAll(missions);
            savedSprint.setMissions(missions);
        }

        return mapToSprintResponse(savedSprint);
    }

    @Transactional
    public SprintResponse toggleMission(Long sprintId, Long missionId) {
        log.info("🎯 [SPRINT] Cambiando estado de misión ID: {} en sprint ID: {}", missionId, sprintId);
        
        FamilySprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new BusinessException("Sprint no encontrado con ID: " + sprintId, "SPRINT_NOT_FOUND", HttpStatus.NOT_FOUND));

        SprintMission mission = missionRepository.findById(missionId)
                .orElseThrow(() -> new BusinessException("Misión no encontrada con ID: " + missionId, "MISSION_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!mission.getSprint().getId().equals(sprintId)) {
            throw new BusinessException("La misión no pertenece a este sprint.", "MISSION_NOT_IN_SPRINT", HttpStatus.BAD_REQUEST);
        }

        if ("PENDING".equals(mission.getStatus())) {
            mission.setStatus("COMPLETED");
            mission.setCompletedAt(LocalDateTime.now());
        } else {
            mission.setStatus("PENDING");
            mission.setCompletedAt(null);
        }

        missionRepository.save(mission);
        return mapToSprintResponse(sprint);
    }

    @Transactional
    public SprintDailyResponse submitDailyCheckin(Long sprintId, CreateDailyCheckinRequest request) {
        log.info("📝 [DAILY] Registrando check-in diario para miembro: {} en sprint ID: {}", request.memberName(), sprintId);

        FamilySprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new BusinessException("Sprint no encontrado con ID: " + sprintId, "SPRINT_NOT_FOUND", HttpStatus.NOT_FOUND));

        LocalDate today = LocalDate.now();
        boolean exists = dailyRepository.existsBySprintIdAndMemberNameAndCheckinDate(sprintId, request.memberName(), today);
        if (exists) {
            throw new BusinessException("Ya registraste tu Check-in Diario el día de hoy.", "CHECKIN_ALREADY_TODAY", HttpStatus.CONFLICT);
        }

        SprintDaily daily = SprintDaily.builder()
                .sprint(sprint)
                .memberName(request.memberName())
                .checkinDate(today)
                .yesterdayText(request.yesterdayText())
                .todayText(request.todayText())
                .blockagesText(request.blockagesText())
                .resolutionText(request.resolutionText())
                .emotionalIndicator(request.emotionalIndicator())
                .build();

        SprintDaily savedDaily = dailyRepository.save(daily);
        return mapToSprintDailyResponse(savedDaily);
    }

    @Transactional
    public SprintResponse closeSprintAndCreateRetrospective(Long sprintId, CloseSprintRequest request) {
        log.info("🏁 [SPRINT] Cerrando sprint ID: {} y generando retrospectiva", sprintId);

        FamilySprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new BusinessException("Sprint no encontrado con ID: " + sprintId, "SPRINT_NOT_FOUND", HttpStatus.NOT_FOUND));

        if (!"ACTIVE".equals(sprint.getStatus())) {
            throw new BusinessException("El sprint ya no está activo.", "SPRINT_NOT_ACTIVE", HttpStatus.CONFLICT);
        }

        List<SprintMission> missions = missionRepository.findBySprintId(sprintId);
        List<SprintDaily> dailies = dailyRepository.findBySprintIdOrderByCheckinDateDesc(sprintId);

        // 1. Calcular Consistencia Evolutiva (1-10)
        int completedMissionsCount = (int) missions.stream().filter(m -> "COMPLETED".equals(m.getStatus())).count();
        double missionRatio = missions.isEmpty() ? 1.0 : (double) completedMissionsCount / missions.size();
        
        // Cumplimiento misiones: hasta 4 puntos
        double missionPoints = missionRatio * 4.0;
        
        // Frecuencia de dailies (ajustada por duración): hasta 4 puntos
        double expectedDailies = sprint.getDurationDays() * 1.5; // Estimando 1.5 dailies promedio diarios por familia
        double dailyRatio = Math.min(1.0, dailies.size() / expectedDailies);
        double dailyPoints = dailyRatio * 4.0;
        
        // Bienestar emocional / interacciones (1-10 en la retro): hasta 2 puntos
        double interactionsRatio = (request.positiveInteractions() != null ? request.positiveInteractions() : 5) / 10.0;
        double tensionRatio = (request.tensionLevel() != null ? request.tensionLevel() : 5) / 10.0;
        double emotionalPoints = (interactionsRatio * 2.0) + ((1.0 - tensionRatio) * 1.0);
        
        int consistencyScore = (int) Math.round(Math.min(10.0, Math.max(1.0, missionPoints + dailyPoints + emotionalPoints)));

        // 2. Generar Feedback IA Adaptativo
        String aiFeedback = generateAiSprintFeedback(sprint, missions, dailies, request, consistencyScore);

        SprintRetrospective retro = SprintRetrospective.builder()
                .sprint(sprint)
                .whatWentWell(request.whatWentWell())
                .whatWasDifficult(request.whatWasDifficult())
                .whatLearned(request.whatLearned())
                .whatToAdjust(request.whatToAdjust())
                .tensionLevel(request.tensionLevel())
                .mindfulCompliance(request.mindfulCompliance())
                .sharedTime(request.sharedTime())
                .positiveInteractions(request.positiveInteractions())
                .emotionalPersistence(request.emotionalPersistence())
                .consistencyScore(consistencyScore)
                .aiFeedback(aiFeedback)
                .build();

        retrospectiveRepository.save(retro);

        sprint.setStatus("COMPLETED");
        sprint.setEndDate(LocalDate.now());
        sprintRepository.save(sprint);

        return mapToSprintResponse(sprint);
    }

    private String generateAiSprintFeedback(
            FamilySprint sprint,
            List<SprintMission> missions,
            List<SprintDaily> dailies,
            CloseSprintRequest retroRequest,
            int consistencyScore
    ) {
        try {
            log.info("🧠 [SPRINT-AI] Generando retrospectiva inteligente con Claude para sprint ID: {}", sprint.getId());
            AiContext context = contextSynthesizer.synthesize(sprint.getFamily(), "ANALYSIS");

            StringBuilder mText = new StringBuilder();
            for (SprintMission m : missions) {
                mText.append(String.format("- [%s] %s\n", "COMPLETED".equals(m.getStatus()) ? "✓" : " ", m.getDescription()));
            }

            StringBuilder dText = new StringBuilder();
            int limit = Math.min(10, dailies.size());
            for (int i = 0; i < limit; i++) {
                SprintDaily d = dailies.get(i);
                dText.append(String.format("- %s (%s): Ayer avanzó: \"%s\" | Hoy se propuso: \"%s\" | Bloqueo: \"%s\" | Estado emocional: %s\n",
                        d.getMemberName(), d.getCheckinDate(), d.getYesterdayText(), d.getTodayText(), d.getBlockagesText(), d.getEmotionalIndicator()));
            }

            String prompt = String.format(
                "RETROSPECTIVA DE SPRINT FAMILIAR E INFORME ADAPTATIVO:\n\n" +
                "Hola Claude. Como Mentor de Integridad Familiar, realiza un análisis reflexivo y clínicamente " +
                "asertivo de los avances logrados por la familia en su último Sprint de Evolución Familiar.\n\n" +
                "=== DATOS DEL SPRINT ===\n" +
                "Objetivo Principal: %s\n" +
                "Dimensión Evaluada: %s\n" +
                "Duración Configurada: %d días\n" +
                "Consistencia Evolutiva Calculada: %d / 10\n\n" +
                "=== MÉTRICAS REPORTADAS POR LA FAMILIA (Escala 1-10) ===\n" +
                "- Nivel de Tensión Relacional: %d\n" +
                "- Cumplimiento Consciente: %d\n" +
                "- Tiempo de Calidad Compartido: %d\n" +
                "- Interacciones Positivas: %d\n" +
                "- Persistencia Emocional: %d\n\n" +
                "=== ESTADO DE LAS MISIONES DEL SPRINT ===\n" +
                "%s\n" +
                "=== DAILY CHECK-INS REGISTRADOS ===\n" +
                "%s\n" +
                "=== RESPUESTAS DE LA RETROSPECTIVA ===\n" +
                "- ¿Qué salió bien?: \"%s\"\n" +
                "- ¿Qué fue difícil?: \"%s\"\n" +
                "- ¿Qué aprendimos?: \"%s\"\n" +
                "- ¿Qué ajustaremos?: \"%s\"\n\n" +
                "=== INSTRUCCIÓN ===\n" +
                "Genera un reporte empático, asertivo, estructurado y premium en markdown que contenga:\n\n" +
                "1. **Análisis del Microciclo de Conciencia 💡**\n" +
                "   (Analiza el nivel de persistencia de la familia, las emociones predominantes en sus dailies y cómo se conectan los bloqueos cotidianos con su objetivo general).\n\n" +
                "2. **Ajuste y Recomendación para el Próximo Ciclo 🎯**\n" +
                "   (Dales una recomendación asombrosamente perspicaz y accionable para su próximo sprint de 7 o 15 días, indicando cómo adaptar sus hábitos y misiones conductuales para superar los bloqueos identificados).\n\n" +
                "Evita tecnicismos ágiles empresariales. Enfócate al 100%% en el crecimiento emocional, comunicación activa y consistencia.",
                sprint.getObjective(), sprint.getRiskDimension(), sprint.getDurationDays(), consistencyScore,
                retroRequest.tensionLevel(), retroRequest.mindfulCompliance(), retroRequest.sharedTime(),
                retroRequest.positiveInteractions(), retroRequest.emotionalPersistence(),
                mText.toString(), dText.toString(),
                retroRequest.whatWentWell(), retroRequest.whatWasDifficult(), retroRequest.whatLearned(), retroRequest.whatToAdjust()
            );

            return aiProvider.generateResponse(prompt, context);
        } catch (Exception e) {
            log.error("⚠️ [SPRINT-AI] Error consultando IA, usando fallback: {}", e.getMessage());
            return generateSprintFallbackFeedback(retroRequest, consistencyScore);
        }
    }

    private String generateSprintFallbackFeedback(CloseSprintRequest req, int consistencyScore) {
        StringBuilder sb = new StringBuilder();
        sb.append("### Análisis del Microciclo de Conciencia 💡\n\n");
        sb.append(String.format("La familia ha completado su Sprint con una **Consistencia Evolutiva de %d/10**. ", consistencyScore));
        if (consistencyScore > 7) {
            sb.append("Se observa una excelente sintonía familiar y una notable persistencia emocional en el cumplimiento de los compromisos diarios. ");
        } else if (consistencyScore > 4) {
            sb.append("Existe un nivel moderado de consistencia. Los bloqueos cotidianos, como la fatiga o la desconexión digital, han interrumpido ocasionalmente la progresión, pero el esfuerzo familiar es visible. ");
        } else {
            sb.append("La consistencia ha sido baja debido a bloqueos relacionales o falta de sincronización diaria. Es un momento ideal para revaluar la carga y simplificar las metas. ");
        }
        sb.append("\n\n### Ajuste y Recomendación para el Próximo Ciclo 🎯\n\n");
        sb.append("Para el próximo microciclo, sugerimos reducir la complejidad de las misiones y priorizar un único acuerdo familiar. ");
        sb.append("Aprovechen los momentos clave como el Check-In Diario al final del día para reestructurar la paciencia y el espacio personal, previniendo discusiones innecesarias.");
        return sb.toString();
    }

    private SprintResponse mapToSprintResponse(FamilySprint sprint) {
        List<SprintMissionResponse> missions = missionRepository.findBySprintId(sprint.getId()).stream()
                .map(m -> new SprintMissionResponse(m.getId(), m.getDescription(), m.getStatus(), m.getCompletedAt()))
                .collect(Collectors.toList());

        List<SprintDailyResponse> dailies = dailyRepository.findBySprintIdOrderByCheckinDateDesc(sprint.getId()).stream()
                .map(this::mapToSprintDailyResponse)
                .collect(Collectors.toList());

        SprintRetrospectiveResponse retroDto = retrospectiveRepository.findBySprintId(sprint.getId())
                .map(r -> new SprintRetrospectiveResponse(
                        r.getId(), r.getWhatWentWell(), r.getWhatWasDifficult(), r.getWhatLearned(), r.getWhatToAdjust(),
                        r.getTensionLevel(), r.getMindfulCompliance(), r.getSharedTime(), r.getPositiveInteractions(),
                        r.getEmotionalPersistence(), r.getConsistencyScore(), r.getAiFeedback(), r.getCreatedAt()
                )).orElse(null);

        return new SprintResponse(
                sprint.getId(),
                sprint.getFamily().getId(),
                sprint.getObjective(),
                sprint.getRiskDimension(),
                sprint.getDurationDays(),
                sprint.getStartDate(),
                sprint.getEndDate(),
                sprint.getStatus(),
                missions,
                dailies,
                retroDto,
                sprint.getCreatedAt()
        );
    }

    private SprintDailyResponse mapToSprintDailyResponse(SprintDaily daily) {
        return new SprintDailyResponse(
                daily.getId(),
                daily.getMemberName(),
                daily.getCheckinDate(),
                daily.getYesterdayText(),
                daily.getTodayText(),
                daily.getBlockagesText(),
                daily.getResolutionText(),
                daily.getEmotionalIndicator(),
                daily.getCreatedAt()
        );
    }
}
