package com.integrityfamily.bitacora.service;

import com.integrityfamily.bitacora.dto.JournalDtos.*;
import com.integrityfamily.domain.*;
import com.integrityfamily.domain.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class JournalServiceTest {

    @Mock
    private PlanTaskRepository planTaskRepository;

    @Mock
    private FamilyRepository familyRepository;

    @Mock
    private TaskEvidenceRepository taskEvidenceRepository;

    @Mock
    private ReflectionRepository reflectionRepository;

    @Mock
    private LearningEntryRepository learningEntryRepository;

    @Mock
    private JournalEntryRepository journalEntryRepository;

    @InjectMocks
    private JournalService journalService;

    private Family family;
    private ImprovementPlan plan;
    private PlanTask task;

    @BeforeEach
    void setUp() {
        family = Family.builder().id(1L).name("Familia Lopez").members(List.of(new FamilyMember(), new FamilyMember(), new FamilyMember())).build();
        plan = ImprovementPlan.builder().id(10L).family(family).build();
        task = PlanTask.builder().id(100L).plan(plan).title("Cena sin celulares").dimension("COMUNICACION").completed(true).build();
    }

    @Test
    @DisplayName("Caso 1: Subir evidencia de tarea exitosamente")
    void shouldUploadEvidenceSuccessfully() {
        Mockito.when(planTaskRepository.findById(100L)).thenReturn(Optional.of(task));
        Mockito.when(taskEvidenceRepository.save(any(TaskEvidence.class))).thenAnswer(i -> {
            TaskEvidence e = i.getArgument(0);
            e.setId(500L);
            return e;
        });

        EvidenceUploadRequest req = new EvidenceUploadRequest("PHOTO", "Foto de la mesa", "Canasta llena de celulares", "http://img.com/1.jpg", null, "Padre");
        TaskEvidence result = journalService.uploadEvidence(100L, req);

        assertNotNull(result);
        assertEquals(500L, result.getId());
        assertEquals(EvidenceType.PHOTO, result.getEvidenceType());
        assertEquals(EvidenceStatus.SUBMITTED, result.getStatus());
        assertEquals("Foto de la mesa", result.getTitle());
    }

    @Test
    @DisplayName("Caso 2: Crear reflexión y generar automáticamente aprendizaje y bitácora")
    void shouldCreateReflectionAndAutogenerateLearningAndJournal() {
        Mockito.when(planTaskRepository.findById(100L)).thenReturn(Optional.of(task));
        Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));

        Mockito.when(reflectionRepository.save(any(Reflection.class))).thenAnswer(i -> {
            Reflection r = i.getArgument(0);
            r.setId(600L);
            return r;
        });

        Mockito.when(learningEntryRepository.save(any(LearningEntry.class))).thenAnswer(i -> i.getArgument(0));
        Mockito.when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> i.getArgument(0));

        ReflectionCreateRequest req = new ReflectionCreateRequest(100L, 1L, 5, true, "Silencio inicial incómodo", "Nos escuchamos con mucha atención", true, "Madre");
        Reflection reflection = journalService.createReflection(req);

        assertNotNull(reflection);
        assertEquals(600L, reflection.getId());
        assertEquals(5, reflection.getEmotionalImpact());
        assertTrue(reflection.getCommunicationImproved());

        // Verificar interacciones de autogeneración
        Mockito.verify(learningEntryRepository, Mockito.times(1)).save(any(LearningEntry.class));
        Mockito.verify(journalEntryRepository, Mockito.times(1)).save(any(JournalEntry.class));
    }

    @Test
    @DisplayName("Caso 3: Calcular métricas longitudinales correctamente")
    void shouldCalculateLongitudinalMetricsSuccessfully() {
        Mockito.when(familyRepository.findById(1L)).thenReturn(Optional.of(family));
        Mockito.when(planTaskRepository.findAll()).thenReturn(List.of(task)); // 1 tarea completada (adherencia 1.0)
        Mockito.when(reflectionRepository.findByFamilyId(1L)).thenReturn(List.of(new Reflection(), new Reflection()));

        // Simular historial de bitácoras para evolución emocional (score actual 5 - previo 2 = +3.0)
        JournalEntry latest = JournalEntry.builder().moodAfter(5).createdAt(LocalDateTime.now()).build();
        JournalEntry oldest = JournalEntry.builder().moodAfter(2).createdAt(LocalDateTime.now().minusWeeks(2)).build();
        Mockito.when(journalEntryRepository.findByFamilyIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(latest, oldest));

        LongitudinalMetricsDto metrics = journalService.getMetrics(1L);

        assertNotNull(metrics);
        assertEquals(1.0, metrics.adherenceRate());
        assertEquals(3, metrics.activeMembersCount());
        assertEquals(2, metrics.completedReflectionsCount());
        assertEquals(3.0, metrics.emotionalEvolutionScore());
        assertEquals(3, metrics.persistenceWeeks()); // 2 semanas pasadas + 1 = 3
    }
}
