package com.integrityfamily.integration;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.User;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import com.integrityfamily.domain.repository.UserRepository;
import com.integrityfamily.risk.service.RiskService;
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

/**
 * Test de integración: motor de riesgo dinámico Sentinel.
 *
 * Verifica que el algoritmo calculateDynamicRisk produce los niveles correctos
 * según ICF, tiempo en programa (currentMilestone) y estado de crisis,
 * y que los snapshots se persisten y recuperan correctamente de MySQL.
 *
 * Umbrales del algoritmo (documentados aquí para referencia futura):
 *   Meses ≤ 6  → BAJO si ICF ≥ 70 | MEDIO si ICF ≥ 40 | ALTO si ICF < 40
 *   Meses ≤ 18 → BAJO si ICF ≥ 80 | MEDIO si ICF ≥ 55 | ALTO si ICF < 55
 *   Meses > 18 → BAJO si ICF ≥ 90 | MEDIO si ICF ≥ 70 | ALTO si ICF < 70
 *   hasCrisis  → siempre CRITICO (ignora ICF)
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@DisplayName("E2E: Motor de Riesgo Sentinel — cálculo dinámico y persistencia")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EvaluationRiskIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url",
            () -> "jdbc:mysql://localhost:3307/integrity_family_risk_test" +
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
            "CREATE DATABASE IF NOT EXISTS integrity_family_risk_test " +
            "CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        conn.close();
    }

    @AfterAll
    static void dropTestDatabase() throws Exception {
        var conn = java.sql.DriverManager.getConnection(
            "jdbc:mysql://localhost:3307/?useSSL=false&allowPublicKeyRetrieval=true",
            "root", "root123");
        conn.createStatement().execute("DROP DATABASE IF EXISTS integrity_family_risk_test");
        conn.close();
    }

    @Autowired RiskService riskService;
    @Autowired FamilyRepository familyRepository;
    @Autowired RiskSnapshotRepository riskSnapshotRepository;
    @Autowired UserRepository userRepository;
    @MockBean  RabbitTemplate rabbitTemplate;

    // Tres familias con distintos "tiempos en programa" simulados via currentMilestone
    private static Long familyNuevaId;   // milestone null → 0 meses (umbrales 6m)
    private static Long familiaMediaId;  // milestone "12" → 12 meses (umbrales 18m)
    private static Long familiaVeteraId; // milestone "24" → 24 meses (umbrales >18m)

    @BeforeEach
    void setupFamilias() {
        if (familyNuevaId != null) return;

        familyNuevaId   = crearFamiliaConOwner("risk-owner-nueva@integrityfamily-test.com",
                              "María Nueva",   "Familia Nueva — Risk Test",  "NUEVA-RISK-001",  null);
        familiaMediaId  = crearFamiliaConOwner("risk-owner-media@integrityfamily-test.com",
                              "Pedro Medio",   "Familia Media — Risk Test",  "MEDIA-RISK-001",  "12");
        familiaVeteraId = crearFamiliaConOwner("risk-owner-vetera@integrityfamily-test.com",
                              "Laura Vetera",  "Familia Vetera — Risk Test", "VETERA-RISK-001", "24");
    }

    private Long crearFamiliaConOwner(String email, String nombre, String famNombre,
                                       String codigo, String milestone) {
        User owner = new User();
        owner.setEmail(email);
        owner.setPasswordHash("$2a$10$test.hash");
        owner.setFullName(nombre);
        owner.setEnabled(true);
        owner.setRoles(List.of());
        userRepository.save(owner);

        Family f = new Family();
        f.setName(famNombre);
        f.setFamilyCode(codigo);
        f.setCreatedBy(owner);
        f.setCurrentMilestone(milestone);
        return familyRepository.save(f).getId();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Familia nueva (0 meses) — umbrales: BAJO≥70, MEDIO≥40, ALTO<40
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(1)
    @DisplayName("PASO 1: Familia nueva — ICF 75 produce riesgo BAJO")
    void paso1_familia_nueva_icf_alto_es_bajo_riesgo() {
        Family f = familyRepository.findById(familyNuevaId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 75.0, false);

        assertThat(snap.getId()).isNotNull();
        assertThat(snap.getRiskLevel()).isEqualTo("BAJO");
        assertThat(snap.getIcf()).isEqualTo(75.0);
        assertThat(snap.getHasCrisis()).isFalse();
        assertThat(snap.getConsciousnessLevel()).isEqualTo(4);   // 60≤ICF<80 → nivel 4
        assertThat(snap.getConsciousnessLabel()).isEqualTo("Madurando");
    }

    @Test @Order(2)
    @DisplayName("PASO 2: Familia nueva — ICF 55 produce riesgo MEDIO")
    void paso2_familia_nueva_icf_medio_es_medio_riesgo() {
        Family f = familyRepository.findById(familyNuevaId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 55.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("MEDIO");
        assertThat(snap.getConsciousnessLevel()).isEqualTo(3);   // 40≤ICF<60 → nivel 3
        assertThat(snap.getConsciousnessLabel()).isEqualTo("Consciente");
    }

    @Test @Order(3)
    @DisplayName("PASO 3: Familia nueva — ICF 30 produce riesgo ALTO")
    void paso3_familia_nueva_icf_bajo_es_alto_riesgo() {
        Family f = familyRepository.findById(familyNuevaId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 30.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("ALTO");
        assertThat(snap.getConsciousnessLevel()).isEqualTo(2);   // 20≤ICF<40 → nivel 2
        assertThat(snap.getConsciousnessLabel()).isEqualTo("Reactiva");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  hasCrisis = true → siempre CRITICO
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(4)
    @DisplayName("PASO 4: Crisis activa → CRITICO independientemente del ICF")
    void paso4_crisis_activa_siempre_es_critico() {
        Family f = familyRepository.findById(familyNuevaId).orElseThrow();
        // ICF alto (95) pero hay crisis → debe ser CRITICO
        RiskSnapshot snap = riskService.calculateAndCreate(f, 95.0, true);

        assertThat(snap.getRiskLevel()).isEqualTo("CRITICO");
        assertThat(snap.getHasCrisis()).isTrue();
        // Nota: RiskService modifica family.sentinelActive en memoria pero no lo persiste explícitamente.
        // Verificamos el estado en el objeto retornado (snapshot), no re-cargando la familia.
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Familia media (12 meses) — umbrales más exigentes: BAJO≥80, MEDIO≥55
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(5)
    @DisplayName("PASO 5: Familia media 12m — ICF 75 es MEDIO (umbral BAJO subió a 80)")
    void paso5_familia_media_umbrales_mas_exigentes() {
        Family f = familyRepository.findById(familiaMediaId).orElseThrow();
        // Con 0 meses 75 sería BAJO; con 12 meses el umbral sube a 80
        RiskSnapshot snap = riskService.calculateAndCreate(f, 75.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("MEDIO");
    }

    @Test @Order(6)
    @DisplayName("PASO 6: Familia media 12m — ICF 85 produce BAJO (supera umbral 80)")
    void paso6_familia_media_icf_sobre_umbral_es_bajo() {
        Family f = familyRepository.findById(familiaMediaId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 85.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("BAJO");
        assertThat(snap.getConsciousnessLevel()).isEqualTo(5);   // ICF≥80 → nivel 5
        assertThat(snap.getConsciousnessLabel()).isEqualTo("Plena");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Familia veterana (24 meses) — umbrales máximos: BAJO≥90, MEDIO≥70
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(7)
    @DisplayName("PASO 7: Familia veterana 24m — ICF 85 es MEDIO (umbral BAJO es 90)")
    void paso7_familia_veterana_umbral_maximo() {
        Family f = familyRepository.findById(familiaVeteraId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 85.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("MEDIO");
    }

    @Test @Order(8)
    @DisplayName("PASO 8: Familia veterana 24m — ICF 65 es ALTO (umbral MEDIO es 70)")
    void paso8_familia_veterana_icf_bajo_umbral_medio_es_alto() {
        Family f = familyRepository.findById(familiaVeteraId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 65.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("ALTO");
    }

    @Test @Order(9)
    @DisplayName("PASO 9: Familia veterana 24m — ICF 92 produce BAJO")
    void paso9_familia_veterana_icf_excelente_es_bajo() {
        Family f = familyRepository.findById(familiaVeteraId).orElseThrow();
        RiskSnapshot snap = riskService.calculateAndCreate(f, 92.0, false);

        assertThat(snap.getRiskLevel()).isEqualTo("BAJO");
        assertThat(snap.getConsciousnessLevel()).isEqualTo(5);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Persistencia y recuperación
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(10)
    @DisplayName("PASO 10: Todos los snapshots se persisten y son recuperables por familia")
    void paso10_persistencia_snapshots_por_familia() {
        // La familia nueva tiene snapshots de pasos 1-4
        List<RiskSnapshot> snapsNueva = riskService.findByFamilyId(familyNuevaId);
        assertThat(snapsNueva).hasSizeGreaterThanOrEqualTo(4);

        // Están ordenados por fecha descendente (más reciente primero)
        for (int i = 0; i < snapsNueva.size() - 1; i++) {
            assertThat(snapsNueva.get(i).getCreatedAt())
                .isAfterOrEqualTo(snapsNueva.get(i + 1).getCreatedAt());
        }

        // Todos tienen familia correcta
        assertThat(snapsNueva)
            .allMatch(s -> s.getFamily().getId().equals(familyNuevaId));

        // Recuperar por ID individual funciona
        Long primerSnapId = snapsNueva.get(0).getId();
        RiskSnapshot byId = riskService.findById(primerSnapId);
        assertThat(byId.getId()).isEqualTo(primerSnapId);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Niveles de consciencia — tabla completa
    // ════════════════════════════════════════════════════════════════════════

    @Test @Order(11)
    @DisplayName("PASO 11: Niveles de consciencia cubren toda la escala ICF 0-100")
    void paso11_niveles_consciencia_escala_completa() {
        Family f = familyRepository.findById(familyNuevaId).orElseThrow();

        // Nivel 1: ICF < 20
        RiskSnapshot n1 = riskService.calculateAndCreate(f, 10.0, false);
        assertThat(n1.getConsciousnessLevel()).isEqualTo(1);
        assertThat(n1.getConsciousnessLabel()).isEqualTo("Inconsciente");

        // Nivel 2: 20 ≤ ICF < 40
        RiskSnapshot n2 = riskService.calculateAndCreate(f, 25.0, false);
        assertThat(n2.getConsciousnessLevel()).isEqualTo(2);
        assertThat(n2.getConsciousnessLabel()).isEqualTo("Reactiva");

        // Nivel 3: 40 ≤ ICF < 60
        RiskSnapshot n3 = riskService.calculateAndCreate(f, 50.0, false);
        assertThat(n3.getConsciousnessLevel()).isEqualTo(3);
        assertThat(n3.getConsciousnessLabel()).isEqualTo("Consciente");

        // Nivel 4: 60 ≤ ICF < 80
        RiskSnapshot n4 = riskService.calculateAndCreate(f, 70.0, false);
        assertThat(n4.getConsciousnessLevel()).isEqualTo(4);
        assertThat(n4.getConsciousnessLabel()).isEqualTo("Madurando");

        // Nivel 5: ICF ≥ 80
        RiskSnapshot n5 = riskService.calculateAndCreate(f, 90.0, false);
        assertThat(n5.getConsciousnessLevel()).isEqualTo(5);
        assertThat(n5.getConsciousnessLabel()).isEqualTo("Plena");
    }
}
