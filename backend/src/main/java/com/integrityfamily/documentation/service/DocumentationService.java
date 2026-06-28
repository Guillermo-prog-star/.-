package com.integrityfamily.documentation.service;

import com.integrityfamily.ai.dto.AiContext;
import com.integrityfamily.ai.provider.AiProvider;
import com.integrityfamily.documentation.domain.DocumentCategory;
import com.integrityfamily.documentation.domain.ProjectDocument;
import com.integrityfamily.documentation.dto.DocumentationDtos.*;
import com.integrityfamily.documentation.repository.ProjectDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class DocumentationService {

    private final ProjectDocumentRepository repository;
    private final AiProvider aiProvider;

    public DocumentListResponse listAll() {
        List<ProjectDocument> docs = repository.findAllActive();
        DocumentListResponse response = new DocumentListResponse();
        response.setDocuments(docs.stream().map(this::toSummary).toList());
        response.setTotal(docs.size());
        return response;
    }

    public DocumentListResponse listByCategory(DocumentCategory category) {
        List<ProjectDocument> docs = repository.findByCategoryAndStatusOrderByTitleAsc(category, "ACTIVE");
        DocumentListResponse response = new DocumentListResponse();
        response.setDocuments(docs.stream().map(this::toSummary).toList());
        response.setTotal(docs.size());
        return response;
    }

    public DocumentDetailResponse getByCode(String code) {
        ProjectDocument doc = repository.findByCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Documento no encontrado: " + code));
        return toDetail(doc);
    }

    public DocumentListResponse search(String query) {
        List<ProjectDocument> docs = repository.searchByKeyword(query);
        DocumentListResponse response = new DocumentListResponse();
        response.setDocuments(docs.stream().map(this::toSummary).toList());
        response.setTotal(docs.size());
        return response;
    }

    public QueryResponse query(String question) {
        log.info("[DOCS] Consulta IA sobre documentación: {}", question);

        // Buscar documentos relevantes por palabras clave de la pregunta
        List<ProjectDocument> relevant = findRelevantDocuments(question);

        // Construir contexto con el contenido de los documentos relevantes
        String docsContext = relevant.stream()
            .map(d -> "=== " + d.getTitle() + " (" + d.getCode() + ") ===\n" + d.getContent())
            .collect(Collectors.joining("\n\n"));

        String prompt = """
            Eres el consultor de documentación de Integrity Family.
            Responde únicamente basándote en la documentación oficial proporcionada a continuación.
            Si la información no está en los documentos, indícalo claramente.

            DOCUMENTACIÓN OFICIAL:
            %s

            PREGUNTA DEL USUARIO:
            %s

            Responde de forma clara, estructurada y citando el documento fuente cuando sea relevante.
            """.formatted(docsContext.isBlank() ? "(Sin documentos relevantes encontrados)" : docsContext, question);

        AiContext context = new AiContext(
            null, null, null, null, null, null, null,
            false, null, null, null, null, null,
            null, null, null, null, null, null,
            null, null, null, null, null, null, null, null
        );
        String answer = aiProvider.generateResponse(prompt, context);

        QueryResponse response = new QueryResponse();
        response.setAnswer(answer);
        response.setSources(relevant.stream().map(this::toSummary).toList());
        return response;
    }

    private List<ProjectDocument> findRelevantDocuments(String question) {
        // Búsqueda por palabras clave (las de más de 3 caracteres)
        String[] words = question.toLowerCase().split("\\s+");
        java.util.Set<ProjectDocument> found = new java.util.LinkedHashSet<>();
        for (String word : words) {
            if (word.length() > 3) {
                found.addAll(repository.searchByKeyword(word));
            }
        }
        // Si no encontró nada específico, devolver los primeros 5 documentos activos
        if (found.isEmpty()) {
            return repository.findAllActive().stream().limit(5).toList();
        }
        return found.stream().limit(8).toList();
    }

    private DocumentSummaryResponse toSummary(ProjectDocument d) {
        DocumentSummaryResponse r = new DocumentSummaryResponse();
        r.setId(d.getId());
        r.setCode(d.getCode());
        r.setTitle(d.getTitle());
        r.setCategory(d.getCategory());
        r.setSummary(d.getSummary());
        r.setVersion(d.getVersion());
        r.setTags(d.getTags());
        r.setUpdatedAt(d.getUpdatedAt());
        return r;
    }

    private DocumentDetailResponse toDetail(ProjectDocument d) {
        DocumentDetailResponse r = new DocumentDetailResponse();
        r.setId(d.getId());
        r.setCode(d.getCode());
        r.setTitle(d.getTitle());
        r.setCategory(d.getCategory());
        r.setContent(d.getContent());
        r.setSummary(d.getSummary());
        r.setVersion(d.getVersion());
        r.setStatus(d.getStatus());
        r.setTags(d.getTags());
        r.setCreatedAt(d.getCreatedAt());
        r.setUpdatedAt(d.getUpdatedAt());
        return r;
    }
}
