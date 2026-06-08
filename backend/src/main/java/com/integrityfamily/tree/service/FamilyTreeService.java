package com.integrityfamily.tree.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.dna.repository.FamilyDnaRepository;
import com.integrityfamily.domain.Family;
import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.domain.repository.FamilyGratitudeEntryRepository;
import com.integrityfamily.domain.repository.TaskEvidenceRepository;
import com.integrityfamily.legado.repository.FamilyLegacyRepository;
import com.integrityfamily.tree.domain.FamilyTreeLink;
import com.integrityfamily.tree.domain.GenerationalMessage;
import com.integrityfamily.tree.dto.*;
import com.integrityfamily.tree.repository.FamilyTreeLinkRepository;
import com.integrityfamily.tree.repository.GenerationalMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FamilyTreeService {

    private final FamilyTreeLinkRepository    linkRepository;
    private final GenerationalMessageRepository msgRepository;
    private final FamilyRepository            familyRepository;
    private final FamilyLegacyRepository      legacyRepository;
    private final FamilyDnaRepository         dnaRepository;
    private final FamilyGratitudeEntryRepository gratitudeRepository;
    private final TaskEvidenceRepository      evidenceRepository;
    private final ObjectMapper objectMapper;

    private static final int MAX_DEPTH = 8; // máximo de generaciones a recorrer

    // ─── Vinculación ─────────────────────────────────────────────────────────

    @Transactional
    public FamilyTreeLink link(Long childFamilyId, LinkRequest req) {
        // Valida que la familia hijo no tenga ya un padre
        if (linkRepository.existsByChildFamilyId(childFamilyId)) {
            throw new IllegalStateException("Esta familia ya tiene una familia origen vinculada.");
        }

        // Busca la familia padre por código
        Family parent = familyRepository.findAll().stream()
                .filter(f -> req.parentFamilyCode().equals(f.getFamilyCode()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se encontró ninguna familia con el código: " + req.parentFamilyCode()));

        // Evita ciclos: el padre no puede ser descendiente del hijo
        if (isDescendant(childFamilyId, parent.getId())) {
            throw new IllegalStateException("No se puede vincular: esto crearía un ciclo en el árbol.");
        }

        FamilyTreeLink link = FamilyTreeLink.builder()
                .parentFamilyId(parent.getId())
                .childFamilyId(childFamilyId)
                .relationship("descendant")
                .linkedByMember(req.linkedByMember())
                .linkedAt(LocalDateTime.now())
                .note(req.note())
                .build();

        FamilyTreeLink saved = linkRepository.save(link);
        log.info("[TREE] Familia {} vinculada a familia padre {} ({})",
                childFamilyId, parent.getId(), parent.getName());
        return saved;
    }

    @Transactional
    public void unlink(Long childFamilyId) {
        linkRepository.findByChildFamilyId(childFamilyId)
                .ifPresent(linkRepository::delete);
    }

    // ─── Árbol generacional ───────────────────────────────────────────────────

    /**
     * Construye el árbol completo a partir de la raíz ancestral de esta familia.
     * Si la familia no tiene ancestros, ella misma es la raíz.
     */
    @Transactional(readOnly = true)
    public FamilyTreeDto getFullTree(Long familyId) {
        Long rootId = findRoot(familyId);
        return buildNode(rootId, 0, new HashSet<>());
    }

    /**
     * Devuelve solo la línea directa: ancestros → familia actual.
     */
    @Transactional(readOnly = true)
    public List<FamilyTreeDto> getAncestorLine(Long familyId) {
        List<Long> line = new ArrayList<>();
        Long current = familyId;
        Set<Long> visited = new HashSet<>();

        while (current != null && !visited.contains(current)) {
            visited.add(current);
            line.add(0, current); // insertar al inicio
            Optional<FamilyTreeLink> parentLink = linkRepository.findByChildFamilyId(current);
            current = parentLink.map(FamilyTreeLink::getParentFamilyId).orElse(null);
        }

        return line.stream()
                .map(id -> buildNode(id, line.indexOf(id), new HashSet<>()))
                .collect(Collectors.toList());
    }

    // ─── Herencia ─────────────────────────────────────────────────────────────

    /**
     * Recopila todo el legado que esta familia heredó de sus ancestros.
     */
    @Transactional(readOnly = true)
    public HeritageDto getHeritage(Long familyId) {
        Family family = familyRepository.findById(familyId)
                .orElseThrow(() -> new IllegalArgumentException("Familia no encontrada: " + familyId));

        List<HeritageDto.AncestorHeritage> ancestors = new ArrayList<>();
        Long current = familyId;
        int generation = 0;
        Set<Long> visited = new HashSet<>();

        // Recorre hacia arriba en el árbol
        Optional<FamilyTreeLink> parentLink = linkRepository.findByChildFamilyId(current);
        while (parentLink.isPresent() && !visited.contains(parentLink.get().getParentFamilyId())) {
            Long parentId = parentLink.get().getParentFamilyId();
            visited.add(parentId);
            generation++;

            Family parentFamily = familyRepository.findById(parentId).orElse(null);
            if (parentFamily == null) break;

            ancestors.add(buildAncestorHeritage(parentFamily, generation));
            parentLink = linkRepository.findByChildFamilyId(parentId);
        }

        // Invierte para mostrar el ancestro más lejano primero
        Collections.reverse(ancestors);

        return new HeritageDto(familyId, family.getName(), ancestors);
    }

    // ─── Mensajes generacionales ──────────────────────────────────────────────

    @Transactional
    public GenerationalMessage createMessage(Long fromFamilyId, MessageRequest req) {
        GenerationalMessage msg = GenerationalMessage.builder()
                .fromFamilyId(fromFamilyId)
                .toFamilyId(req.toFamilyId())
                .authorName(req.authorName())
                .subject(req.subject())
                .content(req.content())
                .messageType(req.messageType() != null ? req.messageType() : "LETTER")
                .sealed(req.openInYear() != null)
                .openInYear(req.openInYear())
                .createdAt(LocalDateTime.now())
                .build();

        GenerationalMessage saved = msgRepository.save(msg);
        log.info("[TREE] Mensaje generacional creado por familia {} — tipo: {}", fromFamilyId, msg.getMessageType());
        return saved;
    }

    /** Mensajes que esta familia puede leer: los de sus ancestros que ya están disponibles. */
    @Transactional(readOnly = true)
    public List<GenerationalMessage> getReadableAncestorMessages(Long familyId) {
        // Recopila IDs de familias ancestrales
        Set<Long> ancestorIds = collectAncestorIds(familyId);

        return ancestorIds.stream()
                .flatMap(aid -> msgRepository.findByFromFamilyIdOrderByCreatedAtDesc(aid).stream())
                .filter(GenerationalMessage::isReadableNow)
                .sorted(Comparator.comparing(GenerationalMessage::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    /** Mensajes que esta familia ha escrito para el futuro. */
    @Transactional(readOnly = true)
    public List<GenerationalMessage> getOwnMessages(Long familyId) {
        return msgRepository.findByFromFamilyIdOrderByCreatedAtDesc(familyId);
    }

    // ─── Bloque de contexto para IA ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public String buildTreeContextBlock(Long familyId) {
        try {
            Optional<FamilyTreeLink> parentLink = linkRepository.findByChildFamilyId(familyId);
            List<FamilyTreeLink> children = linkRepository.findByParentFamilyId(familyId);
            Set<Long> ancestorIds = collectAncestorIds(familyId);

            if (parentLink.isEmpty() && children.isEmpty()) return null;

            StringBuilder sb = new StringBuilder("Árbol Generacional:\n");

            if (!ancestorIds.isEmpty()) {
                sb.append("  Generaciones ancestrales: ").append(ancestorIds.size()).append("\n");
                parentLink.ifPresent(l -> {
                    familyRepository.findById(l.getParentFamilyId()).ifPresent(p ->
                            sb.append("  Familia origen directa: ").append(p.getName()).append("\n")
                    );
                });
            }

            if (!children.isEmpty()) {
                sb.append("  Familias descendientes directas: ").append(children.size()).append("\n");
            }

            return sb.toString();
        } catch (Exception e) {
            log.warn("[TREE] Error construyendo bloque de contexto: {}", e.getMessage());
            return null;
        }
    }

    // ─── Utilidades ───────────────────────────────────────────────────────────

    private Long findRoot(Long familyId) {
        Long current = familyId;
        Set<Long> visited = new HashSet<>();
        while (true) {
            if (visited.contains(current)) return current; // ciclo defensivo
            visited.add(current);
            Optional<FamilyTreeLink> parent = linkRepository.findByChildFamilyId(current);
            if (parent.isEmpty()) return current;
            current = parent.get().getParentFamilyId();
        }
    }

    private FamilyTreeDto buildNode(Long familyId, int generation, Set<Long> visited) {
        if (visited.contains(familyId) || generation > MAX_DEPTH) {
            return null;
        }
        visited.add(familyId);

        Family family = familyRepository.findById(familyId).orElse(null);
        if (family == null) return null;

        List<FamilyTreeLink> childLinks = linkRepository.findByParentFamilyId(familyId);
        List<FamilyTreeDto> children = childLinks.stream()
                .map(l -> buildNode(l.getChildFamilyId(), generation + 1, visited))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        long evidences = evidenceRepository.findByFamilyId(familyId).size();
        long gratitudes = gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(familyId).size();
        String dnaValues = dnaRepository.findByFamilyId(familyId)
                .map(d -> extractFirstValues(d.getValores()))
                .orElse(null);

        FamilyTreeLink link = linkRepository.findByChildFamilyId(familyId).orElse(null);

        return new FamilyTreeDto(
                familyId,
                family.getName(),
                family.getFamilyCode(),
                family.getCreatedAt(),
                generation,
                (int) family.getMembers().stream().filter(m -> m.isActive()).count(),
                evidences,
                gratitudes,
                dnaValues,
                link != null ? link.getLinkedByMember() : null,
                link != null ? link.getLinkedAt() : null,
                children
        );
    }

    private HeritageDto.AncestorHeritage buildAncestorHeritage(Family family, int generation) {
        var legacy = legacyRepository.findByFamilyId(family.getId()).orElse(null);
        var dna    = dnaRepository.findByFamilyId(family.getId()).orElse(null);

        long evidences  = evidenceRepository.findByFamilyId(family.getId()).size();
        long gratitudes = gratitudeRepository.findByFamilyIdOrderByCreatedAtDesc(family.getId()).size();

        List<HeritageDto.MessageSummary> messages = msgRepository
                .findByFromFamilyIdOrderByCreatedAtDesc(family.getId()).stream()
                .filter(GenerationalMessage::isReadableNow)
                .map(m -> new HeritageDto.MessageSummary(
                        m.getId(),
                        m.getSubject(),
                        m.getContent(),
                        m.getAuthorName(),
                        m.getMessageType(),
                        m.getCreatedAt().getYear()
                ))
                .collect(Collectors.toList());

        return new HeritageDto.AncestorHeritage(
                family.getId(),
                family.getName(),
                family.getFamilyCode(),
                generation,
                legacy != null ? legacy.getFoundingPrinciple() : null,
                legacy != null ? legacy.getFamilyMission()     : null,
                legacy != null ? legacy.getFamilyVision()      : null,
                legacy != null ? legacy.getHistoryLessons()    : null,
                legacy != null ? legacy.getHistoryRecognition(): null,
                dna != null ? extractFirstValues(dna.getValores())   : null,
                dna != null ? dna.getNarrativaIa() : null,
                messages,
                evidences,
                gratitudes
        );
    }

    private Set<Long> collectAncestorIds(Long familyId) {
        Set<Long> ids = new LinkedHashSet<>();
        Long current = familyId;
        Optional<FamilyTreeLink> link = linkRepository.findByChildFamilyId(current);
        while (link.isPresent() && !ids.contains(link.get().getParentFamilyId())) {
            ids.add(link.get().getParentFamilyId());
            link = linkRepository.findByChildFamilyId(link.get().getParentFamilyId());
        }
        return ids;
    }

    private boolean isDescendant(Long potentialAncestor, Long candidateId) {
        Set<Long> visited = new HashSet<>();
        Long current = candidateId;
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            if (current.equals(potentialAncestor)) return true;
            Optional<FamilyTreeLink> link = linkRepository.findByChildFamilyId(current);
            current = link.map(FamilyTreeLink::getParentFamilyId).orElse(null);
        }
        return false;
    }

    private String extractFirstValues(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<>() {});
            return list.stream().limit(3).collect(Collectors.joining(", "));
        } catch (Exception e) { return null; }
    }
}
