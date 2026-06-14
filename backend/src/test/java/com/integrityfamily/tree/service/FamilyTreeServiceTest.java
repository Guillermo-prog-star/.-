package com.integrityfamily.tree.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyGratitudeEntryRepository;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.tree.domain.FamilyTreeLink;
import com.integrityfamily.tree.domain.GenerationalMessage;
import com.integrityfamily.tree.dto.FamilyTreeDto;
import com.integrityfamily.tree.dto.HeritageDto;
import com.integrityfamily.tree.dto.LinkRequest;
import com.integrityfamily.tree.dto.MessageRequest;
import com.integrityfamily.tree.repository.FamilyTreeLinkRepository;
import com.integrityfamily.tree.repository.GenerationalMessageRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FamilyTreeService")
class FamilyTreeServiceTest {

    @Mock FamilyTreeLinkRepository       linkRepository;
    @Mock GenerationalMessageRepository  msgRepository;
    @Mock FamilyRepository               familyRepository;
    @Mock FamilyLegacyRepository         legacyRepository;
    @Mock FamilyDnaRepository            dnaRepository;
    @Mock FamilyGratitudeEntryRepository gratitudeRepository;
    @Mock TaskEvidenceRepository         evidenceRepository;
    @Spy  ObjectMapper                   objectMapper = new ObjectMapper();

    @InjectMocks FamilyTreeService service;

    private static final long CHILD  = 1L;
    private static final long PARENT = 2L;

    private Family family(long id, String name) {
        return Family.builder().id(id).name(name).familyCode("IF-" + id).build();
    }

    private FamilyTreeLink link(long parentId, long childId) {
        return FamilyTreeLink.builder().parentFamilyId(parentId).childFamilyId(childId).build();
    }

    /** Stubs lenient mínimos para que buildNode(id, ...) no falle. */
    private void stubBuildNode(long id) {
        lenient().when(familyRepository.findById(id)).thenReturn(Optional.of(family(id, "Familia " + id)));
        lenient().when(linkRepository.findByParentFamilyId(id)).thenReturn(List.of());
        lenient().when(linkRepository.findByChildFamilyId(id)).thenReturn(Optional.empty());
        lenient().when(evidenceRepository.findByFamilyId(id)).thenReturn(List.of());
        lenient().when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(id)).thenReturn(List.of());
        lenient().when(dnaRepository.findByFamilyId(id)).thenReturn(Optional.empty());
    }

    // ─── link() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("link")
    class Link {

        @Test
        @DisplayName("hijo ya tiene padre → IllegalStateException")
        void childAlreadyHasParent_throws() {
            when(linkRepository.existsByChildFamilyId(CHILD)).thenReturn(true);

            assertThatThrownBy(() -> service.link(CHILD, new LinkRequest("IF-2", "Juan", null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya tiene");
        }

        @Test
        @DisplayName("código de familia padre no existe → IllegalArgumentException")
        void parentCodeNotFound_throws() {
            when(linkRepository.existsByChildFamilyId(CHILD)).thenReturn(false);
            when(familyRepository.findAll()).thenReturn(List.of(family(PARENT, "Otra")));

            assertThatThrownBy(() -> service.link(CHILD, new LinkRequest("IF-INEXISTENTE", "Juan", null)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("IF-INEXISTENTE");
        }

        @Test
        @DisplayName("vínculo crearía ciclo → IllegalStateException")
        void cycleDetected_throws() {
            when(linkRepository.existsByChildFamilyId(CHILD)).thenReturn(false);
            Family parentFamily = Family.builder().id(PARENT).name("Padre").familyCode("IF-2").build();
            when(familyRepository.findAll()).thenReturn(List.of(parentFamily));
            // El padre (2) tiene como padre al hijo (1) → ciclo
            when(linkRepository.findByChildFamilyId(PARENT)).thenReturn(
                    Optional.of(link(CHILD, PARENT)));

            assertThatThrownBy(() -> service.link(CHILD, new LinkRequest("IF-2", "Juan", null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ciclo");
        }

        @Test
        @DisplayName("vínculo válido → guarda enlace con campos correctos")
        void valid_savesLink() {
            when(linkRepository.existsByChildFamilyId(CHILD)).thenReturn(false);
            Family parentFamily = Family.builder().id(PARENT).name("Padre").familyCode("IF-2").build();
            when(familyRepository.findAll()).thenReturn(List.of(parentFamily));
            when(linkRepository.findByChildFamilyId(PARENT)).thenReturn(Optional.empty());
            when(linkRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FamilyTreeLink result = service.link(CHILD, new LinkRequest("IF-2", "María", "enlace oficial"));

            assertThat(result.getParentFamilyId()).isEqualTo(PARENT);
            assertThat(result.getChildFamilyId()).isEqualTo(CHILD);
            assertThat(result.getLinkedByMember()).isEqualTo("María");
            assertThat(result.getNote()).isEqualTo("enlace oficial");
            verify(linkRepository).save(any(FamilyTreeLink.class));
        }
    }

    // ─── unlink() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unlink")
    class Unlink {

        @Test
        @DisplayName("vínculo existe → se elimina")
        void linkExists_deleted() {
            FamilyTreeLink existingLink = link(PARENT, CHILD);
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.of(existingLink));

            service.unlink(CHILD);

            verify(linkRepository).delete(existingLink);
        }

        @Test
        @DisplayName("no hay vínculo → sin excepción ni borrado")
        void noLink_noException() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.empty());

            assertThatCode(() -> service.unlink(CHILD)).doesNotThrowAnyException();
            verify(linkRepository, never()).delete(any());
        }
    }

    // ─── createMessage() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("createMessage")
    class CreateMessage {

        @Test
        @DisplayName("openInYear null → sealed=false (carta abierta inmediatamente)")
        void openInYearNull_sealedFalse() {
            when(msgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageRequest req = new MessageRequest("Pedro", "Legado", "Contenido", "LETTER", null, null);
            GenerationalMessage msg = service.createMessage(CHILD, req);

            assertThat(msg.getSealed()).isFalse();
            assertThat(msg.getOpenInYear()).isNull();
        }

        @Test
        @DisplayName("openInYear proporcionado → sealed=true")
        void openInYearSet_sealedTrue() {
            when(msgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageRequest req = new MessageRequest("Pedro", "Legado", "Contenido", "WISDOM", null, 2050);
            GenerationalMessage msg = service.createMessage(CHILD, req);

            assertThat(msg.getSealed()).isTrue();
            assertThat(msg.getOpenInYear()).isEqualTo(2050);
        }

        @Test
        @DisplayName("messageType null → tipo por defecto LETTER")
        void messageTypeNull_defaultsToLetter() {
            when(msgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageRequest req = new MessageRequest("Pedro", "Legado", "Contenido", null, null, null);
            GenerationalMessage msg = service.createMessage(CHILD, req);

            assertThat(msg.getMessageType()).isEqualTo("LETTER");
        }

        @Test
        @DisplayName("messageType proporcionado → se preserva el tipo")
        void messageTypeProvided_usesValue() {
            when(msgRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageRequest req = new MessageRequest("Pedro", "Título", "Contenido", "BLESSING", null, null);
            GenerationalMessage msg = service.createMessage(CHILD, req);

            assertThat(msg.getMessageType()).isEqualTo("BLESSING");
        }
    }

    // ─── buildTreeContextBlock() ──────────────────────────────────────────────

    @Nested
    @DisplayName("buildTreeContextBlock")
    class BuildTreeContextBlock {

        @Test
        @DisplayName("sin padre ni hijos → retorna null")
        void noParentNoChildren_returnsNull() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.empty());
            when(linkRepository.findByParentFamilyId(CHILD)).thenReturn(List.of());

            assertThat(service.buildTreeContextBlock(CHILD)).isNull();
        }

        @Test
        @DisplayName("tiene hijos → bloque indica número de descendientes directos")
        void withChildren_containsDescendantCount() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.empty());
            when(linkRepository.findByParentFamilyId(CHILD)).thenReturn(
                    List.of(link(CHILD, 10L), link(CHILD, 11L)));

            String block = service.buildTreeContextBlock(CHILD);

            assertThat(block).contains("Árbol Generacional:");
            assertThat(block).contains("Familias descendientes directas: 2");
        }

        @Test
        @DisplayName("tiene padre → bloque incluye nombre de la familia origen")
        void withParent_containsParentName() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.of(link(PARENT, CHILD)));
            when(linkRepository.findByParentFamilyId(CHILD)).thenReturn(List.of());
            when(linkRepository.findByChildFamilyId(PARENT)).thenReturn(Optional.empty());
            when(familyRepository.findById(PARENT)).thenReturn(Optional.of(family(PARENT, "Familia Ancestral")));

            String block = service.buildTreeContextBlock(CHILD);

            assertThat(block).contains("Árbol Generacional:");
            assertThat(block).contains("Familia Ancestral");
        }

        @Test
        @DisplayName("excepción interna → retorna null sin propagar")
        void internalException_returnsNull() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenThrow(new RuntimeException("DB error"));

            assertThat(service.buildTreeContextBlock(CHILD)).isNull();
        }
    }

    // ─── getFullTree() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getFullTree")
    class GetFullTree {

        @Test
        @DisplayName("familia sin ancestros ni hijos → retorna nodo único en generación 0")
        void leafFamily_returnsSingleRootNode() {
            stubBuildNode(CHILD);

            FamilyTreeDto dto = service.getFullTree(CHILD);

            assertThat(dto.familyId()).isEqualTo(CHILD);
            assertThat(dto.generation()).isZero();
            assertThat(dto.children()).isEmpty();
        }

        @Test
        @DisplayName("familia con un hijo → árbol con hijo en generación 1")
        void familyWithChild_treeHasChildAtGen1() {
            long childId = 10L;
            stubBuildNode(CHILD);
            stubBuildNode(childId);
            when(linkRepository.findByParentFamilyId(CHILD)).thenReturn(List.of(link(CHILD, childId)));

            FamilyTreeDto dto = service.getFullTree(CHILD);

            assertThat(dto.children()).hasSize(1);
            assertThat(dto.children().get(0).familyId()).isEqualTo(childId);
            assertThat(dto.children().get(0).generation()).isEqualTo(1);
        }
    }

    // ─── getAncestorLine() ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getAncestorLine")
    class GetAncestorLine {

        @Test
        @DisplayName("sin ancestros → lista con solo la familia actual")
        void noParent_returnsSingleton() {
            stubBuildNode(CHILD);

            List<FamilyTreeDto> line = service.getAncestorLine(CHILD);

            assertThat(line).hasSize(1);
            assertThat(line.get(0).familyId()).isEqualTo(CHILD);
        }

        @Test
        @DisplayName("con padre → ancestro en posición 0, familia en posición 1")
        void withParent_ancestorFirst() {
            stubBuildNode(CHILD);
            stubBuildNode(PARENT);
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.of(link(PARENT, CHILD)));

            List<FamilyTreeDto> line = service.getAncestorLine(CHILD);

            assertThat(line).hasSize(2);
            assertThat(line.get(0).familyId()).isEqualTo(PARENT);
            assertThat(line.get(1).familyId()).isEqualTo(CHILD);
        }
    }

    // ─── getHeritage() ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getHeritage")
    class GetHeritage {

        @Test
        @DisplayName("familia no encontrada → IllegalArgumentException")
        void familyNotFound_throws() {
            when(familyRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getHeritage(99L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("99");
        }

        @Test
        @DisplayName("sin ancestros → lista de herencia vacía")
        void noAncestors_emptyAncestorList() {
            when(familyRepository.findById(CHILD)).thenReturn(Optional.of(family(CHILD, "Hijo")));
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.empty());

            HeritageDto dto = service.getHeritage(CHILD);

            assertThat(dto.familyId()).isEqualTo(CHILD);
            assertThat(dto.familyName()).isEqualTo("Hijo");
            assertThat(dto.ancestors()).isEmpty();
        }

        @Test
        @DisplayName("con un ancestro → herencia incluye datos del padre")
        void withOneAncestor_heritagePopulated() {
            when(familyRepository.findById(CHILD)).thenReturn(Optional.of(family(CHILD, "Hijo")));
            when(familyRepository.findById(PARENT)).thenReturn(Optional.of(family(PARENT, "Padre")));
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.of(link(PARENT, CHILD)));
            when(linkRepository.findByChildFamilyId(PARENT)).thenReturn(Optional.empty());
            when(legacyRepository.findByFamilyId(PARENT)).thenReturn(Optional.empty());
            when(dnaRepository.findByFamilyId(PARENT)).thenReturn(Optional.empty());
            when(evidenceRepository.findByFamilyId(PARENT)).thenReturn(List.of());
            when(gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(PARENT)).thenReturn(List.of());
            when(msgRepository.findByFromFamilyIdOrderByCreatedAtDesc(PARENT)).thenReturn(List.of());

            HeritageDto dto = service.getHeritage(CHILD);

            assertThat(dto.ancestors()).hasSize(1);
            assertThat(dto.ancestors().get(0).familyName()).isEqualTo("Padre");
            assertThat(dto.ancestors().get(0).generation()).isEqualTo(1);
        }
    }

    // ─── getOwnMessages() ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("getOwnMessages")
    class GetOwnMessages {

        @Test
        @DisplayName("delega al repositorio y devuelve sus mensajes")
        void delegatesToRepository() {
            GenerationalMessage msg = GenerationalMessage.builder()
                    .fromFamilyId(CHILD).authorName("Pedro").content("Hola").build();
            when(msgRepository.findByFromFamilyIdOrderByCreatedAtDesc(CHILD)).thenReturn(List.of(msg));

            assertThat(service.getOwnMessages(CHILD)).containsExactly(msg);
        }
    }

    // ─── getReadableAncestorMessages() ───────────────────────────────────────

    @Nested
    @DisplayName("getReadableAncestorMessages")
    class GetReadableAncestorMessages {

        @Test
        @DisplayName("sin ancestros → lista vacía")
        void noAncestors_returnsEmptyList() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.empty());

            assertThat(service.getReadableAncestorMessages(CHILD)).isEmpty();
        }

        @Test
        @DisplayName("ancestro con mensaje legible (sealed=false) → se retorna el mensaje")
        void ancestorWithReadableMessage_returned() {
            when(linkRepository.findByChildFamilyId(CHILD)).thenReturn(Optional.of(link(PARENT, CHILD)));
            when(linkRepository.findByChildFamilyId(PARENT)).thenReturn(Optional.empty());
            GenerationalMessage readable = GenerationalMessage.builder()
                    .fromFamilyId(PARENT).authorName("Abuelo").content("Sabiduría").sealed(false).build();
            when(msgRepository.findByFromFamilyIdOrderByCreatedAtDesc(PARENT)).thenReturn(List.of(readable));

            List<GenerationalMessage> result = service.getReadableAncestorMessages(CHILD);

            assertThat(result).containsExactly(readable);
        }
    }
}
