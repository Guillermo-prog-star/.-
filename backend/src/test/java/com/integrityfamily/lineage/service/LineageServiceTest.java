package com.integrityfamily.lineage.service;

import com.integrityfamily.common.exception.BusinessException;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.lineage.domain.FamilyLineage;
import com.integrityfamily.lineage.domain.LineageGenerationInfo;
import com.integrityfamily.lineage.domain.LineageMember;
import com.integrityfamily.lineage.domain.LineageRelationship;
import com.integrityfamily.lineage.dto.*;
import com.integrityfamily.lineage.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LineageService")
class LineageServiceTest {

    @Mock FamilyLineageRepository        lineageRepo;
    @Mock LineageMemberRepository        memberRepo;
    @Mock LineageRelationshipRepository  relRepo;
    @Mock LineageGenerationInfoRepository genInfoRepo;
    @Mock LineageEventRepository         eventRepo;
    @Mock FamilyRepository               familyRepo;
    @InjectMocks LineageService service;

    private static final long FAM_ID     = 1L;
    private static final long LINEAGE_ID = 10L;
    private static final long MEMBER_ID  = 20L;

    private final Family family = Family.builder().id(FAM_ID).name("Test").build();

    private FamilyLineage lineage() {
        return FamilyLineage.builder().id(LINEAGE_ID).family(family)
                .lineageCode("IF-LIN-TEST-0001").title("Linaje").build();
    }

    private LineageMember member(FamilyLineage lin) {
        return LineageMember.builder().id(MEMBER_ID).lineage(lin)
                .firstName("Juan").status("alive").generation(0).build();
    }

    private LineageMemberRequest memberReq() {
        return new LineageMemberRequest(
                "Juan", "Pérez", "JP", "#000", 0, "responsible", false, "alive",
                null, null, null, null, null, null, null, 80, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    // ── getByFamily ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByFamily")
    class GetByFamily {

        @Test
        @DisplayName("linaje no encontrado → BusinessException LINEAGE_NOT_FOUND")
        void notFound_throwsBusinessException() {
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByFamily(FAM_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Linaje no encontrado");
        }

        @Test
        @DisplayName("encontrado → retorna LineageResponse con lineageCode y familyId")
        void found_returnsResponse() {
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lineage()));
            when(memberRepo.findWithEventsByLineageId(LINEAGE_ID)).thenReturn(List.of());
            when(relRepo.findByLineageId(LINEAGE_ID)).thenReturn(List.of());
            when(genInfoRepo.findByLineageIdOrderByGenerationLevel(LINEAGE_ID)).thenReturn(List.of());

            LineageResponse resp = service.getByFamily(FAM_ID);

            assertThat(resp.lineageCode()).isEqualTo("IF-LIN-TEST-0001");
            assertThat(resp.familyId()).isEqualTo(FAM_ID);
        }
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("linaje ya existe → BusinessException LINEAGE_EXISTS")
        void alreadyExists_throwsConflict() {
            when(lineageRepo.existsByFamilyId(FAM_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.create(FAM_ID,
                    new CreateLineageRequest("T", null, null, null, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya tiene un linaje");
        }

        @Test
        @DisplayName("familia no encontrada → BusinessException FAMILY_NOT_FOUND")
        void familyNotFound_throwsNotFound() {
            when(lineageRepo.existsByFamilyId(FAM_ID)).thenReturn(false);
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(FAM_ID,
                    new CreateLineageRequest("T", null, null, null, null, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Familia no encontrada");
        }

        @Test
        @DisplayName("familia 'Test' → código IF-LIN-TEST-0001")
        void success_generatesCodeFromFamilyName() {
            when(lineageRepo.existsByFamilyId(FAM_ID)).thenReturn(false);
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            when(lineageRepo.save(any())).thenAnswer(inv -> {
                FamilyLineage l = inv.getArgument(0);
                return FamilyLineage.builder().id(LINEAGE_ID).family(family)
                        .lineageCode(l.getLineageCode()).title(l.getTitle()).build();
            });

            LineageResponse resp = service.create(FAM_ID,
                    new CreateLineageRequest("Linaje", null, null, null, null, null, null));

            assertThat(resp.lineageCode()).isEqualTo("IF-LIN-TEST-0001");
        }

        @Test
        @DisplayName("null anchorGeneration → 0; null maxPastGen → -2; null maxFutureGen → 2")
        void nullGenerationParams_usesDefaults() {
            when(lineageRepo.existsByFamilyId(FAM_ID)).thenReturn(false);
            when(familyRepo.findById(FAM_ID)).thenReturn(Optional.of(family));
            ArgumentCaptor<FamilyLineage> captor = ArgumentCaptor.forClass(FamilyLineage.class);
            when(lineageRepo.save(captor.capture())).thenAnswer(inv -> captor.getValue());

            service.create(FAM_ID,
                    new CreateLineageRequest("T", null, null, null, null, null, null));

            FamilyLineage saved = captor.getValue();
            assertThat(saved.getAnchorGeneration()).isEqualTo(0);
            assertThat(saved.getMaxPastGen()).isEqualTo(-2);
            assertThat(saved.getMaxFutureGen()).isEqualTo(2);
        }
    }

    // ── update ────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("linaje no encontrado → BusinessException")
        void notFound_throws() {
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(FAM_ID,
                    new CreateLineageRequest("Nuevo", null, null, null, null, null, null)))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("actualiza título y anchorGeneration en la entidad persistida")
        void success_updatesFields() {
            FamilyLineage lin = lineage();
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lin));
            when(lineageRepo.save(lin)).thenReturn(lin);
            when(memberRepo.findWithEventsByLineageId(LINEAGE_ID)).thenReturn(List.of());
            when(relRepo.findByLineageId(LINEAGE_ID)).thenReturn(List.of());
            when(genInfoRepo.findByLineageIdOrderByGenerationLevel(LINEAGE_ID)).thenReturn(List.of());

            service.update(FAM_ID, new CreateLineageRequest("Nuevo Título", "Desc", 1, -3, 3, "Visión", "1900"));

            assertThat(lin.getTitle()).isEqualTo("Nuevo Título");
            assertThat(lin.getAnchorGeneration()).isEqualTo(1);
        }
    }

    // ── addMember ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("addMember")
    class AddMember {

        @Test
        @DisplayName("linaje no encontrado → BusinessException")
        void lineageNotFound_throws() {
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addMember(FAM_ID, memberReq()))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("guarda miembro con status y generation del request")
        void success_savesMember() {
            FamilyLineage lin = lineage();
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lin));
            when(memberRepo.save(any())).thenReturn(member(lin));

            service.addMember(FAM_ID, memberReq());

            verify(memberRepo).save(argThat(m ->
                    "alive".equals(m.getStatus()) && m.getGeneration() == 0));
        }
    }

    // ── updateMember ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateMember")
    class UpdateMember {

        @Test
        @DisplayName("miembro no encontrado → BusinessException MEMBER_NOT_FOUND")
        void memberNotFound_throws() {
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.updateMember(FAM_ID, MEMBER_ID, memberReq()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Miembro no encontrado");
        }

        @Test
        @DisplayName("miembro pertenece a otra familia → BusinessException ACCESS_DENIED 403")
        void wrongFamily_throwsForbidden() {
            Family other = Family.builder().id(99L).name("Other").build();
            FamilyLineage otherLin = FamilyLineage.builder().id(50L).family(other).build();
            LineageMember m = LineageMember.builder().id(MEMBER_ID)
                    .lineage(otherLin).status("alive").generation(0).build();
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.of(m));

            assertThatThrownBy(() -> service.updateMember(FAM_ID, MEMBER_ID, memberReq()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Acceso denegado");
        }

        @Test
        @DisplayName("events=null → lista de eventos no se toca")
        void nullEvents_doesNotClearEvents() {
            FamilyLineage lin = lineage();
            LineageMember m = member(lin);
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.of(m));
            when(memberRepo.save(m)).thenReturn(m);

            LineageMemberRequest req = new LineageMemberRequest(
                    "Ana", "López", null, null, -1, null, false, "deceased",
                    null, null, null, null, null, null, null, null, null,
                    null, null, null, null, null, null, null,
                    null, null, null, null, null); // events = null

            service.updateMember(FAM_ID, MEMBER_ID, req);

            assertThat(m.getFirstName()).isEqualTo("Ana");
            assertThat(m.getEvents()).isEmpty();
        }
    }

    // ── deleteMember ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteMember")
    class DeleteMember {

        @Test
        @DisplayName("miembro no encontrado → BusinessException")
        void notFound_throws() {
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteMember(FAM_ID, MEMBER_ID))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("sin relaciones → elimina directamente el miembro")
        void success_deletesRelsThenMember() {
            FamilyLineage lin = lineage();
            LineageMember m = member(lin);
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.of(m));
            when(relRepo.findByLineageId(LINEAGE_ID)).thenReturn(List.of());

            service.deleteMember(FAM_ID, MEMBER_ID);

            verify(memberRepo).delete(m);
            verify(relRepo, never()).delete(any(LineageRelationship.class));
        }
    }

    // ── addRelationship ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("addRelationship")
    class AddRelationship {

        @Test
        @DisplayName("miembro origen no encontrado → BusinessException MEMBER_NOT_FOUND")
        void fromMemberNotFound_throws() {
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lineage()));
            when(memberRepo.findById(5L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.addRelationship(FAM_ID,
                    new LineageRelationshipRequest(5L, 6L, null, null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("origen");
        }

        @Test
        @DisplayName("tipo null → 'biological'; isCouple null → false")
        void nullType_usesDefaults() {
            FamilyLineage lin = lineage();
            LineageMember from = LineageMember.builder().id(5L).lineage(lin).status("alive").generation(-1).build();
            LineageMember to   = LineageMember.builder().id(6L).lineage(lin).status("alive").generation(0).build();
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lin));
            when(memberRepo.findById(5L)).thenReturn(Optional.of(from));
            when(memberRepo.findById(6L)).thenReturn(Optional.of(to));
            when(relRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addRelationship(FAM_ID, new LineageRelationshipRequest(5L, 6L, null, null));

            verify(relRepo).save(argThat(r ->
                    "biological".equals(r.getRelationshipType()) && Boolean.FALSE.equals(r.getIsCouple())));
        }
    }

    // ── deleteRelationship ───────────────────────────────────────────────────

    @Nested
    @DisplayName("deleteRelationship")
    class DeleteRelationship {

        @Test
        @DisplayName("relación no encontrada → BusinessException REL_NOT_FOUND")
        void notFound_throws() {
            when(relRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteRelationship(FAM_ID, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Relación no encontrada");
        }

        @Test
        @DisplayName("encontrada → elimina la relación")
        void found_deletes() {
            LineageRelationship rel = LineageRelationship.builder().id(99L).build();
            when(relRepo.findById(99L)).thenReturn(Optional.of(rel));

            service.deleteRelationship(FAM_ID, 99L);

            verify(relRepo).delete(rel);
        }
    }

    // ── upsertGenerationInfo ──────────────────────────────────────────────────

    @Nested
    @DisplayName("upsertGenerationInfo")
    class UpsertGenerationInfo {

        @Test
        @DisplayName("no existe → crea nuevo con generationLevel del request")
        void notFound_createsNew() {
            FamilyLineage lin = lineage();
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lin));
            when(genInfoRepo.findByLineageIdAndGenerationLevel(LINEAGE_ID, -1)).thenReturn(Optional.empty());
            when(genInfoRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            GenerationInfoRequest req = new GenerationInfoRequest(-1, "founding", "Abuelos", null, null, null, null, null, null);
            GenerationInfoResponse resp = service.upsertGenerationInfo(FAM_ID, req);

            assertThat(resp.generationLevel()).isEqualTo(-1);
        }

        @Test
        @DisplayName("ya existe → actualiza campos en el registro existente")
        void found_updatesExisting() {
            FamilyLineage lin = lineage();
            LineageGenerationInfo existing = LineageGenerationInfo.builder()
                    .id(1L).lineage(lin).generationLevel(-1).title("Viejo").build();
            when(lineageRepo.findByFamilyId(FAM_ID)).thenReturn(Optional.of(lin));
            when(genInfoRepo.findByLineageIdAndGenerationLevel(LINEAGE_ID, -1)).thenReturn(Optional.of(existing));
            when(genInfoRepo.save(existing)).thenReturn(existing);

            service.upsertGenerationInfo(FAM_ID, new GenerationInfoRequest(-1, null, "Nuevo", "Resumen", null, null, null, null, null));

            assertThat(existing.getTitle()).isEqualTo("Nuevo");
            assertThat(existing.getSummary()).isEqualTo("Resumen");
        }
    }

    // ── addEvent / deleteEvent ────────────────────────────────────────────────

    @Nested
    @DisplayName("addEvent / deleteEvent")
    class Events {

        @Test
        @DisplayName("addEvent sin eventType → guarda con tipo por defecto 'milestone'")
        void addEvent_defaultsToMilestoneType() {
            FamilyLineage lin = lineage();
            LineageMember m = member(lin);
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.of(m));
            when(eventRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.addEvent(FAM_ID, MEMBER_ID, new LineageEventRequest("1950", "Boda", null, null, null, null));

            verify(eventRepo).save(argThat(e -> "milestone".equals(e.getEventType())));
        }

        @Test
        @DisplayName("deleteEvent no encontrado → BusinessException EVENT_NOT_FOUND")
        void deleteEvent_notFound_throws() {
            FamilyLineage lin = lineage();
            LineageMember m = member(lin);
            when(memberRepo.findById(MEMBER_ID)).thenReturn(Optional.of(m));
            when(eventRepo.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.deleteEvent(FAM_ID, MEMBER_ID, 99L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Evento no encontrado");
        }
    }
}
