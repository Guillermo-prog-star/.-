package com.integrityfamily.movie.scheduler;

import com.integrityfamily.domain.repository.FamilyRepository;
import com.integrityfamily.movie.service.FamilyMovieService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Genera la Película Familiar automáticamente el primer día de cada
 * trimestre nuevo, cubriendo el trimestre que acaba de terminar.
 * Cron: 01 de enero, abril, julio y octubre a las 06:00 AM.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MovieGenerationScheduler {

    private final FamilyMovieService movieService;
    private final FamilyRepository   familyRepository;

    @Scheduled(cron = "0 0 6 1 1,4,7,10 *")
    public void generateQuarterlyMovies() {
        LocalDate yesterday = LocalDate.now().minusDays(1); // último día del trimestre anterior
        LocalDate quarterStart = yesterday.withDayOfMonth(1).minusMonths(2);

        log.info("[MOVIE-SCHEDULER] Generando películas trimestrales {} → {}", quarterStart, yesterday);

        familyRepository.findAll().forEach(family -> {
            try {
                movieService.generate(family.getId(), quarterStart, yesterday);
                log.info("[MOVIE-SCHEDULER] Película generada para familia {}", family.getId());
            } catch (Exception e) {
                log.warn("[MOVIE-SCHEDULER] Error para familia {}: {}", family.getId(), e.getMessage());
            }
        });
    }
}
