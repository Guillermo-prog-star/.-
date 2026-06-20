package com.integrityfamily.alexa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.integrityfamily.domain.FamilyChapterProgress;
import com.integrityfamily.domain.repository.FamilyChapterProgressRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChapterService {

    private final FamilyChapterProgressRepository progressRepo;
    private final ObjectMapper objectMapper;

    @Getter
    private List<Map<String, Object>> chapters;

    @PostConstruct
    public void loadChapters() {
        try {
            var resource = new ClassPathResource("chapters.json");
            chapters = objectMapper.readValue(resource.getInputStream(),
                    new TypeReference<>() {});
            log.info("Loaded {} chapters from chapters.json", chapters.size());
        } catch (Exception e) {
            log.error("Failed to load chapters.json", e);
            chapters = List.of();
        }
    }

    public Map<String, Object> getChapter(int number) {
        return chapters.stream()
                .filter(c -> ((Number) c.get("number")).intValue() == number)
                .findFirst()
                .orElse(null);
    }

    public int getCompletedCount(Long familyId) {
        return progressRepo.countCompletedByFamilyId(familyId);
    }

    public int getCurrentChapterNumber(Long familyId) {
        return progressRepo.findMaxChapterByFamilyId(familyId).orElse(0);
    }

    /** Returns the current (last started) chapter data for a family, or chapter 1 if none. */
    public Map<String, Object> getCurrentChapter(Long familyId) {
        int current = getCurrentChapterNumber(familyId);
        if (current == 0) {
            ensureStarted(familyId, 1);
            current = 1;
        }
        Map<String, Object> ch = getChapter(current);
        return ch != null ? ch : getChapter(1);
    }

    /** Opens the next chapter for a family (marks current as started). */
    public Map<String, Object> advanceToNextChapter(Long familyId) {
        int current = getCurrentChapterNumber(familyId);
        int next = Math.min(current + 1, 75);
        ensureStarted(familyId, next);
        return getChapter(next);
    }

    /** Marks a chapter as completed for a family. */
    public void completeChapter(Long familyId, int chapterNumber) {
        Optional<FamilyChapterProgress> opt = progressRepo.findByFamilyIdAndChapterNumber(familyId, chapterNumber);
        FamilyChapterProgress progress = opt.orElseGet(() -> FamilyChapterProgress.builder()
                .familyId(familyId)
                .chapterNumber(chapterNumber)
                .build());
        progress.setCompleted(true);
        progress.setCompletedAt(System.currentTimeMillis());
        progressRepo.save(progress);
    }

    private void ensureStarted(Long familyId, int chapterNumber) {
        if (progressRepo.findByFamilyIdAndChapterNumber(familyId, chapterNumber).isEmpty()) {
            progressRepo.save(FamilyChapterProgress.builder()
                    .familyId(familyId)
                    .chapterNumber(chapterNumber)
                    .build());
        }
    }

    public String getSeasonName(int chapterNumber) {
        Map<String, Object> ch = getChapter(chapterNumber);
        if (ch == null) return "";
        int season = ((Number) ch.get("season")).intValue();
        return "T" + season + " · " + ch.get("seasonName");
    }
}
