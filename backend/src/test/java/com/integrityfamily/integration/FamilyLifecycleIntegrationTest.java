package com.integrityfamily.integration;

import com.integrityfamily.bitacora.dto.SprintDtos.CreateSprintRequest;
import com.integrityfamily.bitacora.dto.SprintDtos.SprintResponse;
import com.integrityfamily.bitacora.service.SprintService;
import com.integrityfamily.checklist.service.TaskEvidenceService;
import com.integrityfamily.documentary.dto.SubmitDocumentaryRequest;
import com.integrityfamily.documentary.service.DocumentaryService;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import com.integrityfamily.dto.EvaluationDtos;
import com.integrityfamily.evaluation.service.EvaluationService;
import com.integrityfamily.plan.service.PlanService;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;
import java.sql.DriverManager;
import org.junit.jupiter.api.Assumptions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integración E2E: flujo completo de una familia en Integrity Family.
 *
 * Flujo verificado con BD H2 real (Flyway aplicado):
 *   Familia → Evaluación (ICF) → Plan de mejora → Sprint
 *   → Evidencia → Documental
 *
 * RabbitMQ se mockea porque es mensajería asíncrona externa,
 * no parte de la lógica de negocio que queremos validar aquí.
 */
@Tag("integration")
@SpringBootTest
@ActiveProfiles("integration-test")
@DisplayName("E2E: Flujo completo Evaluación → Plan → Sprint → Evidencia → Documental")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FamilyLifecycleIntegrationTest {

    // BD de integración dedicada sobre el MySQL local (integrity-db:3307)
    // Se crea y destruye automáticamente via @BeforeAll / @AfterAll
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> "jdbc:mysql://localhost:3307/integrity_family_e2e_test" +
                  "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota" +
                  "&createDatabaseIfNotExist=true&connectionCollation=utf8mb4_unicode_ci");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "root123");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeAll
    static void createTestDatabase() throws Exception {
        try {
            DriverManager.getConnection(
                "jdbc:mysql://localhost:3307/?useSSL=false&allowPublicKeyRetrieval=true",
                "root", "root123").close();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "MySQL en localhost:3307 no disponible — test E2E omitido");
        }
        var conn = java.sql.DriverManager.getConnection(
            "jdbc:mysql://localhost:3307/?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root123");
        conn.createStatement().execute(
            "CREATE DATABASE IF NOT EXISTS integrity_family_e2e_test " +
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        // Forzar colación de sesión para evitar mezcla en V20+
        conn.createStatement().execute(
            "ALTER DATABASE integrity_family_e2e_test COLLATE utf8mb4_unicode_ci");
        conn.close();
    }

    @AfterAll
    static void dropTestDatabase() throws Exception {
        var conn = java.sql.DriverManager.getConnection(
            "jdbc:mysql://localhost:3307/?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root123");
        conn.createStatement().execute("DROP DATABASE IF EXISTS integrity_family_e2e_test");
        conn.close();
    }

    @Autowired EvaluationService   evaluationService;
    @Autowired PlanService         planService;
    @Autowired SprintService       sprintService;
    @Autowired TaskEvidenceService evidenceService;
    @Autowired DocumentaryService  documentaryService;

    @Autowired FamilyRepository    familyRepository;
    @Autowired UserRepository      userRepository;

    // RabbitMQ es infraestructura externa — no hace parte del flujo a probar
    @MockBean RabbitTemplate rabbitTemplate;

    // Estado compartido entre los 6 pasos (static = persiste entre @Test methods)
    private static Long familyId;
    private static Long evaluationId;
    private static Long planId;
    private static Long taskId;
    private static Long sprintId;
    private static Long evidenceId;

    @BeforeEach
    void ensureFamilyExists() {
        if (familyId != null) return;

        User owner = new User();
        owner.setEmail("william@integrityfamily-test.com");
        owner.setPasswordHash("$2a$10$test.hash.placeholder");
        owner.setFullName("William López Test");
        owner.setEnabled(true);
        owner.setRoles(List.of());
        userRepository.save(owner);

        Family family = new Family();
        family.setName("Familia López — Test E2E");
        family.setFamilyCode("LOPEZ-E2E-001");
        family.setCreatedBy(owner);
        familyId = familyRepository.save(family).getId();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 1 — Evaluación ICF
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("PASO 1: Crear y finalizar una evaluación ICF produce score y riskLevel")
    void paso1_evaluacion_icf() {
        // Iniciar evaluación
        var startReq = new EvaluationDtos.EvaluationStartRequest(familyId, null);
        Evaluation eval = evaluationService.start(startReq);

        assertThat(eval.getId()).isNotNull();
        assertThat(eval.getStatus()).isEqualTo(EvaluationStatus.STARTED);
        assertThat(eval.getFamily().getId()).isEqualTo(familyId);

        // Respuestas con questionId real (IDs 1-5 emociones, 17-21 comunicación,
        // 29-33 hábitos, 40-44 tiempos — datos de la migración seed)
        List<EvaluationDtos.AnswerDto> answers = List.of(
            new EvaluationDtos.AnswerDto(1L,  3, null),
            new EvaluationDtos.AnswerDto(2L,  4, null),
            new EvaluationDtos.AnswerDto(3L,  2, null),
            new EvaluationDtos.AnswerDto(4L,  3, null),
            new EvaluationDtos.AnswerDto(5L,  4, null),
            new EvaluationDtos.AnswerDto(17L, 2, null),
            new EvaluationDtos.AnswerDto(18L, 3, null),
            new EvaluationDtos.AnswerDto(19L, 2, null),
            new EvaluationDtos.AnswerDto(20L, 3, null),
            new EvaluationDtos.AnswerDto(21L, 2, null),
            new EvaluationDtos.AnswerDto(29L, 4, null),
            new EvaluationDtos.AnswerDto(30L, 3, null),
            new EvaluationDtos.AnswerDto(31L, 4, null),
            new EvaluationDtos.AnswerDto(32L, 3, null),
            new EvaluationDtos.AnswerDto(33L, 4, null),
            new EvaluationDtos.AnswerDto(40L, 3, null),
            new EvaluationDtos.AnswerDto(41L, 4, null),
            new EvaluationDtos.AnswerDto(42L, 3, null),
            new EvaluationDtos.AnswerDto(43L, 4, null),
            new EvaluationDtos.AnswerDto(44L, 3, null)
        );

        var finalizeReq = new EvaluationDtos.EvaluationFinalizeRequest(answers, null, null, null);
        var result = evaluationService.finalize(eval.getId(), finalizeReq);

        assertThat(result).isNotNull();
        assertThat(result.evaluation().getStatus()).isEqualTo(EvaluationStatus.FINALIZED);
        assertThat(result.evaluation().getIcf()).isNotNull().isGreaterThan(0.0);
        assertThat(result.evaluation().getRiskLevel()).isNotNull().isNotBlank();

        evaluationId = result.evaluation().getId();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 2 — Plan de mejora
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(2)
    @DisplayName("PASO 2: Generar plan determinístico desde la evaluación produce tareas reales")
    void paso2_plan_mejora() {
        assertThat(evaluationId).as("Paso 1 debe completarse primero").isNotNull();

        var planResponse = planService.generateDeterministicPlan(evaluationId);

        assertThat(planResponse).isNotNull();
        assertThat(planResponse.id()).isNotNull();
        assertThat(planResponse.familyId()).isEqualTo(familyId);
        assertThat(planResponse.tasks()).isNotNull().isNotEmpty();

        planId = planResponse.id();
        taskId = planResponse.tasks().get(0).id();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 3 — Sprint
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(3)
    @DisplayName("PASO 3: Crear sprint vinculado a la tarea del plan")
    void paso3_sprint() {
        assertThat(taskId).as("Paso 2 debe completarse primero").isNotNull();

        var req = new CreateSprintRequest(
            "Mejorar comunicación familiar",
            "comunicacion",
            7,
            List.of("Cena sin pantallas los martes", "10 min de conversación diaria"),
            taskId
        );

        SprintResponse sprint = sprintService.createSprint(familyId, req);

        assertThat(sprint).isNotNull();
        assertThat(sprint.id()).isNotNull();
        assertThat(sprint.familyId()).isEqualTo(familyId);
        assertThat(sprint.missions()).isNotNull().isNotEmpty();

        sprintId = sprint.id();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 4 — Evidencia
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(4)
    @DisplayName("PASO 4: Subir evidencia vinculada a la tarea del plan")
    void paso4_evidencia() {
        assertThat(taskId).as("Paso 2 debe completarse primero").isNotNull();

        TaskEvidence evidence = evidenceService.submitEvidence(
            taskId,
            familyId,
            EvidenceType.SELF_REFLECTION,
            "Cena del martes sin celulares",
            "Pasamos 45 minutos conversando sin interrupciones digitales.",
            null,
            null,
            "William López"
        );

        assertThat(evidence.getId()).isNotNull();
        assertThat(evidence.getStatus()).isEqualTo(EvidenceStatus.SUBMITTED);
        assertThat(evidence.getFamily().getId()).isEqualTo(familyId);
        assertThat(evidence.getTask().getId()).isEqualTo(taskId);
        assertThat(evidence.isValidated()).isFalse();

        evidenceId = evidence.getId();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 5 — Documental
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(5)
    @DisplayName("PASO 5: Crear documental familiar vinculando la evidencia del sprint")
    void paso5_documental() {
        assertThat(evidenceId).as("Paso 4 debe completarse primero").isNotNull();

        var req = new SubmitDocumentaryRequest();
        req.setFamilyId(familyId);
        req.setTaskId(taskId);
        req.setSprintId(sprintId);
        req.setTitle("Nuestra transformación — Sprint 1");
        req.setContent("Esta semana recuperamos la cena familiar como espacio de reconexión.");
        req.setSourceType(DocumentarySourceType.SPRINT_CLOSURE);
        req.setEvidenceIds(List.of(evidenceId));

        FamilyDocumentary documentary = documentaryService.createDocumentary(req);

        assertThat(documentary.getId()).isNotNull();
        assertThat(documentary.getTitle()).isEqualTo("Nuestra transformación — Sprint 1");
        assertThat(documentary.getSourceType()).isEqualTo(DocumentarySourceType.SPRINT_CLOSURE);
        assertThat(documentary.getFamily().getId()).isEqualTo(familyId);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 6 — Integridad transversal
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(6)
    @DisplayName("PASO 6: Todos los artefactos existen y están enlazados a la misma familia")
    void paso6_integridad_transversal() {
        assertThat(familyId).isNotNull();
        assertThat(evaluationId).isNotNull();
        assertThat(planId).isNotNull();
        assertThat(taskId).isNotNull();
        assertThat(sprintId).isNotNull();
        assertThat(evidenceId).isNotNull();

        assertThat(evaluationService.findByFamilyId(familyId))
            .isNotEmpty()
            .anyMatch(e -> e.getId().equals(evaluationId));

        assertThat(planService.findByFamilyId(familyId))
            .isNotEmpty()
            .anyMatch(p -> p.id().equals(planId));

        assertThat(evidenceService.getFamilyEvidences(familyId))
            .isNotEmpty()
            .anyMatch(e -> e.getId().equals(evidenceId));

        assertThat(documentaryService.getFamilyDocumentaries(familyId))
            .isNotEmpty()
            .anyMatch(d -> d.getTitle().contains("Sprint 1"));
    }
}
