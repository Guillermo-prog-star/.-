package com.integrityfamily.integration;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.FamilyMember;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.MemberRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.guardian.domain.MissionCategory;
import com.integrityfamily.guardian.domain.MissionStatus;
import com.integrityfamily.guardian.dto.ActivateMissionRequest;
import com.integrityfamily.guardian.dto.MissionDto;
import com.integrityfamily.guardian.dto.GuardianStatusResponse;
import com.integrityfamily.guardian.dto.VoteRequest;
import com.integrityfamily.guardian.service.GuardianService;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Test de integración: flujo completo del Guardián Familiar.
 *
 * Flujo verificado:
 *   3 miembros votan → mayoría automática → guardián confirmado
 *   → activa misión → completa misión → puntuación aumenta
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@DisplayName("E2E: Guardián Familiar — votación, misiones y participación")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GuardianIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> "jdbc:mysql://localhost:3307/integrity_family_guardian_test" +
                  "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota" +
                  "&createDatabaseIfNotExist=true&connectionCollation=utf8mb4_unicode_ci");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "root123");
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
    }

    @BeforeAll
    static void createTestDatabase() throws Exception {
        var conn = java.sql.DriverManager.getConnection(
            "jdbc:mysql://localhost:3307/?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root123");
        conn.createStatement().execute(
            "CREATE DATABASE IF NOT EXISTS integrity_family_guardian_test " +
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        conn.close();
    }

    @AfterAll
    static void dropTestDatabase() throws Exception {
        var conn = java.sql.DriverManager.getConnection(
            "jdbc:mysql://localhost:3307/?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root123");
        conn.createStatement().execute("DROP DATABASE IF EXISTS integrity_family_guardian_test");
        conn.close();
    }

    @Autowired GuardianService guardianService;
    @Autowired FamilyRepository familyRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired UserRepository userRepository;
    @MockBean  RabbitTemplate rabbitTemplate;

    private static Long familyId;
    private static Long member1Id;  // candidato a guardián
    private static Long member2Id;
    private static Long member3Id;
    private static Long missionId;

    @BeforeEach
    void setupFamily() {
        if (familyId != null) return;

        User owner = new User();
        owner.setEmail("guardian-owner@integrityfamily-test.com");
        owner.setPasswordHash("$2a$10$test.hash");
        owner.setFullName("Ana García");
        owner.setEnabled(true);
        owner.setRoles(List.of());
        userRepository.save(owner);

        Family family = new Family();
        family.setName("Familia García — Test Guardián");
        family.setFamilyCode("GARCIA-GUARD-001");
        family.setCreatedBy(owner);
        familyId = familyRepository.save(family).getId();

        Family saved = familyRepository.findById(familyId).orElseThrow();

        FamilyMember m1 = FamilyMember.builder()
            .fullName("Ana García").email("ana@garcia-test.com")
            .active(true).family(saved).build();
        FamilyMember m2 = FamilyMember.builder()
            .fullName("Carlos García").email("carlos@garcia-test.com")
            .active(true).family(saved).build();
        FamilyMember m3 = FamilyMember.builder()
            .fullName("Sofía García").email("sofia@garcia-test.com")
            .active(true).family(saved).build();

        member1Id = memberRepository.save(m1).getId();
        member2Id = memberRepository.save(m2).getId();
        member3Id = memberRepository.save(m3).getId();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 1 — Estado inicial: sin guardián
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("PASO 1: Estado inicial — familia sin guardián y sin votos")
    void paso1_estado_inicial_sin_guardian() {
        GuardianStatusResponse status = guardianService.getStatus(familyId, member1Id);

        assertThat(status.familyId()).isEqualTo(familyId);
        assertThat(status.hasGuardian()).isFalse();
        assertThat(status.guardianMemberId()).isNull();
        assertThat(status.totalVotes()).isZero();
        assertThat(status.currentUserHasVoted()).isFalse();
        assertThat(status.voteCounts()).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 2 — Votación sin mayoría (1 de 3 votos)
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(2)
    @DisplayName("PASO 2: Un voto (1/3) no es suficiente para elegir guardián")
    void paso2_un_voto_no_resuelve_guardian() {
        // member2 vota por member1 — solo 1 de 3 miembros ha votado
        GuardianStatusResponse status = guardianService.vote(familyId,
            new VoteRequest(member2Id, member1Id));

        assertThat(status.totalVotes()).isEqualTo(1);
        assertThat(status.hasGuardian()).isFalse();  // 1/3 no es mayoría simple
        assertThat(status.voteCounts()).hasSize(1);
        assertThat(status.voteCounts().get(0).memberId()).isEqualTo(member1Id);
        assertThat(status.voteCounts().get(0).votes()).isEqualTo(1);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 3 — Mayoría automática (2 de 3 votos → guardián resuelto)
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(3)
    @DisplayName("PASO 3: Mayoría simple (2/3) resuelve guardián automáticamente")
    void paso3_mayoria_resuelve_guardian_automaticamente() {
        // member3 también vota por member1 → 2 de 3 miembros = mayoría simple
        GuardianStatusResponse status = guardianService.vote(familyId,
            new VoteRequest(member3Id, member1Id));

        assertThat(status.totalVotes()).isEqualTo(2);
        assertThat(status.hasGuardian()).isTrue();
        assertThat(status.guardianMemberId()).isEqualTo(member1Id);
        assertThat(status.guardianFullName()).isEqualTo("Ana García");
        assertThat(status.guardianSince()).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 4 — Cambio de voto
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(4)
    @DisplayName("PASO 4: Un miembro puede cambiar su voto (upsert)")
    void paso4_cambio_de_voto() {
        // member2 cambia su voto: antes por member1, ahora por member2
        GuardianStatusResponse status = guardianService.vote(familyId,
            new VoteRequest(member2Id, member2Id));

        // Sigue habiendo 2 votos totales (m2 y m3), solo cambiaron las nominaciones
        assertThat(status.totalVotes()).isEqualTo(2);

        // 1 voto por member1 (m3), 1 voto por member2 (m2) — empate, no hay nueva mayoría
        // El guardián sigue siendo member1 (ya fue confirmado en paso 3)
        assertThat(status.hasGuardian()).isTrue();
        assertThat(status.guardianMemberId()).isEqualTo(member1Id);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 5 — Confirmación directa del guardián
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(5)
    @DisplayName("PASO 5: Confirmar guardián directamente establece miembro como guardián")
    void paso5_confirmacion_directa_guardian() {
        // Confirmar explícitamente a member1 como guardián
        GuardianStatusResponse status = guardianService.confirmGuardian(familyId, member1Id);

        assertThat(status.hasGuardian()).isTrue();
        assertThat(status.guardianMemberId()).isEqualTo(member1Id);
        assertThat(status.guardianSince()).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 6 — Solo el guardián puede activar misiones
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(6)
    @DisplayName("PASO 6: No-guardián no puede activar misiones")
    void paso6_no_guardian_no_puede_activar_mision() {
        var req = new ActivateMissionRequest(
            "Cena en familia", "Cenar juntos sin pantallas",
            MissionCategory.CONEXION, 90, member2Id  // member2 NO es guardián
        );

        assertThatThrownBy(() -> guardianService.activateMission(familyId, req))
            .hasMessageContaining("Guardián");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 7 — El guardián activa una misión
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(7)
    @DisplayName("PASO 7: El guardián activa una misión familiar")
    void paso7_guardian_activa_mision() {
        var req = new ActivateMissionRequest(
            "Cena en familia sin pantallas",
            "Todos apagamos los celulares durante la cena del martes",
            MissionCategory.CONEXION,
            90,
            member1Id  // el guardián
        );

        MissionDto mission = guardianService.activateMission(familyId, req);

        assertThat(mission.id()).isNotNull();
        assertThat(mission.title()).isEqualTo("Cena en familia sin pantallas");
        assertThat(mission.category()).isEqualTo(MissionCategory.CONEXION);
        assertThat(mission.status()).isEqualTo(MissionStatus.ACTIVE);
        assertThat(mission.durationMinutes()).isEqualTo(90);
        assertThat(mission.createdByMemberId()).isEqualTo(member1Id);

        missionId = mission.id();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 8 — Segunda misión cancela la anterior automáticamente
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(8)
    @DisplayName("PASO 8: Activar segunda misión cancela la activa anterior")
    void paso8_segunda_mision_cancela_anterior() {
        var req = new ActivateMissionRequest(
            "Gratitud diaria", "Cada miembro dice algo por lo que está agradecido",
            MissionCategory.GRATITUD, 15, member1Id
        );

        MissionDto nueva = guardianService.activateMission(familyId, req);
        assertThat(nueva.status()).isEqualTo(MissionStatus.ACTIVE);

        // La lista de misiones incluye las dos, la primera cancelada
        List<MissionDto> misiones = guardianService.getMissions(familyId);
        long activas    = misiones.stream().filter(m -> m.status() == MissionStatus.ACTIVE).count();
        long canceladas = misiones.stream().filter(m -> m.status() == MissionStatus.CANCELLED).count();

        assertThat(activas).isEqualTo(1);
        assertThat(canceladas).isGreaterThanOrEqualTo(1);

        // Actualizar missionId a la nueva misión activa para el paso siguiente
        missionId = nueva.id();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 9 — Completar misión incrementa participation score
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(9)
    @DisplayName("PASO 9: Completar misión incrementa participationScore en 10 puntos")
    void paso9_completar_mision_incrementa_score() {
        int scoreAntes = familyRepository.findById(familyId).orElseThrow()
            .getParticipationScore();

        MissionDto completed = guardianService.completeMission(familyId, missionId, member1Id);

        assertThat(completed.status()).isEqualTo(MissionStatus.COMPLETED);
        assertThat(completed.completedAt()).isNotNull();

        int scoreDespues = familyRepository.findById(familyId).orElseThrow()
            .getParticipationScore();

        assertThat(scoreDespues).isEqualTo(scoreAntes + 10);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PASO 10 — Estado final coherente
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("PASO 10: Estado final — guardián activo, misiones completadas reflejadas")
    void paso10_estado_final_coherente() {
        GuardianStatusResponse status = guardianService.getStatus(familyId, member1Id);

        assertThat(status.hasGuardian()).isTrue();
        assertThat(status.guardianMemberId()).isEqualTo(member1Id);
        assertThat(status.completedMissions()).isGreaterThanOrEqualTo(1);
        assertThat(status.participationScore()).isGreaterThan(0);
        assertThat(status.activeMission()).isNull();  // la última misión fue completada
    }
}
