package com.integrityfamily.documentation.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.documentation.domain.DocumentCategory;
import com.integrityfamily.documentation.domain.ProjectDocument;
import com.integrityfamily.documentation.dto.DocumentationDtos.*;
import com.integrityfamily.documentation.repository.ProjectDocumentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentationService — Unit Tests")
class DocumentationServiceTest {

    @Mock ProjectDocumentRepository repository;
    @Mock AiProvider                aiProvider;

    @InjectMocks DocumentationService service;

    // ── Helpers ───────────────────────────────────────────────────────────────

    private ProjectDocument doc(String code, DocumentCategory cat) {
        ProjectDocument d = new ProjectDocument();
        d.setId(1L);
        d.setCode(code);
        d.setTitle("Título " + code);
        d.setCategory(cat);
        d.setContent("Contenido del documento " + code);
        d.setSummary("Resumen " + code);
        d.setVersion("1.0");
        d.setStatus("ACTIVE");
        d.setTags("tag1,tag2");
        d.setCreatedAt(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        return d;
    }

    // ── listAll() ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("listAll()")
    class ListAll {

        @Test
        @DisplayName("retorna todos los documentos activos")
        void returnsAllActive() {
            when(repository.findAllActive()).thenReturn(List.of(
                doc("PT-ERS-01", DocumentCategory.PROJECT),
                doc("AI-SCN-01", DocumentCategory.AI)
            ));

            DocumentListResponse result = service.listAll();

            assertThat(result.getDocuments()).hasSize(2);
            assertThat(result.getTotal()).isEqualTo(2);
        }

        @Test
        @DisplayName("retorna lista vacía si no hay documentos")
        void returnsEmptyWhenNone() {
            when(repository.findAllActive()).thenReturn(List.of());

            DocumentListResponse result = service.listAll();

            assertThat(result.getDocuments()).isEmpty();
            assertThat(result.getTotal()).isZero();
        }
    }

    // ── listByCategory() ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("listByCategory()")
    class ListByCategory {

        @Test
        @DisplayName("filtra por categoría correctamente")
        void filtersByCategory() {
            when(repository.findByCategoryAndStatusOrderByTitleAsc(DocumentCategory.AI, "ACTIVE"))
                .thenReturn(List.of(doc("AI-SCN-01", DocumentCategory.AI)));

            DocumentListResponse result = service.listByCategory(DocumentCategory.AI);

            assertThat(result.getDocuments()).hasSize(1);
            assertThat(result.getDocuments().get(0).getCode()).isEqualTo("AI-SCN-01");
        }

        @Test
        @DisplayName("retorna vacío si no hay docs en esa categoría")
        void emptyForUnknownCategory() {
            when(repository.findByCategoryAndStatusOrderByTitleAsc(DocumentCategory.RESEARCH, "ACTIVE"))
                .thenReturn(List.of());

            DocumentListResponse result = service.listByCategory(DocumentCategory.RESEARCH);

            assertThat(result.getDocuments()).isEmpty();
        }
    }

    // ── getByCode() ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getByCode()")
    class GetByCode {

        @Test
        @DisplayName("retorna el documento cuando existe")
        void returnsDocWhenFound() {
            when(repository.findByCode("PT-ERS-01"))
                .thenReturn(Optional.of(doc("PT-ERS-01", DocumentCategory.PROJECT)));

            DocumentDetailResponse result = service.getByCode("PT-ERS-01");

            assertThat(result.getCode()).isEqualTo("PT-ERS-01");
            assertThat(result.getCategory()).isEqualTo(DocumentCategory.PROJECT);
            assertThat(result.getContent()).contains("Contenido del documento");
        }

        @Test
        @DisplayName("lanza excepción si el código no existe")
        void throwsWhenNotFound() {
            when(repository.findByCode("INEXISTENTE")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getByCode("INEXISTENTE"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("INEXISTENTE");
        }
    }

    // ── search() ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("search()")
    class Search {

        @Test
        @DisplayName("delega la búsqueda al repositorio")
        void delegatesToRepository() {
            when(repository.searchByKeyword("ICF"))
                .thenReturn(List.of(doc("PT-ICF-01", DocumentCategory.PROJECT)));

            DocumentListResponse result = service.search("ICF");

            assertThat(result.getDocuments()).hasSize(1);
            assertThat(result.getDocuments().get(0).getCode()).isEqualTo("PT-ICF-01");
        }

        @Test
        @DisplayName("retorna vacío si no hay coincidencias")
        void emptyWhenNoMatch() {
            when(repository.searchByKeyword("xyz123")).thenReturn(List.of());

            DocumentListResponse result = service.search("xyz123");

            assertThat(result.getDocuments()).isEmpty();
        }
    }

    // ── query() ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("query()")
    class Query {

        @Test
        @DisplayName("llama al proveedor IA con el contexto de los documentos relevantes")
        void callsAiProviderWithContext() {
            when(repository.searchByKeyword(anyString()))
                .thenReturn(List.of(doc("PT-ICF-01", DocumentCategory.PROJECT)));
            when(aiProvider.generateResponse(anyString(), any(AiContext.class)))
                .thenReturn("El ICF se calcula como promedio de 4 dimensiones.");

            QueryResponse result = service.query("¿Cómo se calcula el ICF?");

            assertThat(result.getAnswer()).contains("ICF");
            assertThat(result.getSources()).hasSize(1);
            verify(aiProvider).generateResponse(
                argThat(prompt -> prompt.contains("ICF") && prompt.contains("documentación oficial")),
                any(AiContext.class)
            );
        }

        @Test
        @DisplayName("usa documentos por defecto si la búsqueda no encuentra nada específico")
        void usesDefaultDocsWhenNoKeywordMatch() {
            when(repository.searchByKeyword(anyString())).thenReturn(List.of());
            when(repository.findAllActive()).thenReturn(List.of(
                doc("PT-ERS-01", DocumentCategory.PROJECT),
                doc("PT-ARQ-01", DocumentCategory.PROJECT)
            ));
            when(aiProvider.generateResponse(anyString(), any(AiContext.class)))
                .thenReturn("Respuesta genérica.");

            QueryResponse result = service.query("¿Qué es?");

            assertThat(result.getAnswer()).isEqualTo("Respuesta genérica.");
            verify(aiProvider).generateResponse(anyString(), any(AiContext.class));
        }

        @Test
        @DisplayName("incluye la pregunta del usuario en el prompt enviado a la IA")
        void includesQuestionInPrompt() {
            String pregunta = "¿Cuáles son las convenciones de Flyway?";
            when(repository.searchByKeyword(anyString())).thenReturn(List.of());
            when(repository.findAllActive()).thenReturn(List.of(doc("DEV-MIG-01", DocumentCategory.DEVELOPMENT)));
            when(aiProvider.generateResponse(anyString(), any(AiContext.class))).thenReturn("Respuesta.");

            service.query(pregunta);

            verify(aiProvider).generateResponse(
                argThat(prompt -> prompt.contains(pregunta)),
                any(AiContext.class)
            );
        }
    }
}
