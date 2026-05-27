package com.integrityfamily.analytics.service;

import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.RiskSnapshot;
import com.integrityfamily.domain.repository.RiskSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para {@link RiskSnapshotService}.
 *
 * Documenta la lógica de resiliencia: el evento de crisis a RabbitMQ se
 * intenta publicar pero los errores de broker nunca propagan al caller.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RiskSnapshotService — Unit Tests")
class RiskSnapshotServiceTest {

    @Mock RiskSnapshotRepository repository;
    @Mock RabbitTemplate         rabbitTemplate;

    @InjectMocks RiskSnapshotService service;

    private Family family;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(5L).name("Los García").build();
    }

    // ───────────────────────────────────────────────────────────────────────
    //  saveSnapshot()
    // ───────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("saveSnapshot() — persistir y publicar evento")
    class SaveSnapshot {

        @Test
        @DisplayName("sin crisis: persiste el snapshot y NO publica a RabbitMQ")
        void saveSnapshot_noCrisis_noRabbitPublish() {
            RiskSnapshot snapshot = RiskSnapshot.builder()
                    .family(family).icf(65.0).riskLevel("LOW").hasCrisis(false)
                    .build();

            service.saveSnapshot(snapshot);

            verify(repository).save(snapshot);
            verifyNoInteractions(rabbitTemplate);
        }

        @Test
        @DisplayName("hasCrisis=null: persiste y NO publica (null es falso)")
        void saveSnapshot_nullCrisis_noRabbitPublish() {
            RiskSnapshot snapshot = RiskSnapshot.builder()
                    .family(family).icf(55.0).riskLevel("MEDIUM").hasCrisis(null)
                    .build();

            service.saveSnapshot(snapshot);

            verify(repository).save(snapshot);
            verifyNoInteractions(rabbitTemplate);
        }

        @Test
        @DisplayName("hasCrisis=true + family presente → publica evento con el ID de la familia")
        void saveSnapshot_hasCrisis_publishesWithFamilyId() {
            RiskSnapshot snapshot = RiskSnapshot.builder()
                    .family(family).icf(28.0).riskLevel("HIGH").hasCrisis(true)
                    .build();

            service.saveSnapshot(snapshot);

            verify(repository).save(snapshot);
            verify(rabbitTemplate).convertAndSend(
                    eq("x.ai.events"),
                    eq("crisis.detected"),
                    eq(5L)         // family.getId()
            );
        }

        @Test
        @DisplayName("hasCrisis=true + family null → publica con ID fallback 2L")
        void saveSnapshot_hasCrisis_nullFamily_usesFallbackId() {
            RiskSnapshot snapshot = RiskSnapshot.builder()
                    .family(null).icf(20.0).riskLevel("HIGH").hasCrisis(true)
                    .build();

            service.saveSnapshot(snapshot);

            verify(repository).save(snapshot);
            verify(rabbitTemplate).convertAndSend(
                    eq("x.ai.events"),
                    eq("crisis.detected"),
                    eq(2L)         // fallback hardcodeado
            );
        }

        @Test
        @DisplayName("error en RabbitMQ → no propaga la excepción al caller (resiliencia)")
        void saveSnapshot_rabbitError_doesNotPropagate() {
            RiskSnapshot snapshot = RiskSnapshot.builder()
                    .family(family).icf(15.0).riskLevel("HIGH").hasCrisis(true)
                    .build();

            doThrow(new RuntimeException("broker down"))
                    .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

            // No debe lanzar excepción
            service.saveSnapshot(snapshot);

            verify(repository).save(snapshot);  // el save sí ocurrió
        }
    }
}
