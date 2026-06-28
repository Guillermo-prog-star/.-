package com.integrityfamily.capital.service;

import com.integrityfamily.domain.FamilyCapitalSnapshot;
import com.integrityfamily.domain.FamilyCriticalEvent;
import com.integrityfamily.domain.FamilyLongitudinalState;
import com.integrityfamily.domain.ObservatorySnapshot;
import com.integrityfamily.domain.repository.FamilyCapitalSnapshotRepository;
import com.integrityfamily.domain.repository.FamilyCriticalEventRepository;
import com.integrityfamily.domain.repository.FamilyLongitudinalStateRepository;
import com.integrityfamily.domain.repository.ObservatorySnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Servicio del Observatorio del Desarrollo Familiar.
 *
 * Genera agregados mensuales anonimizados a partir de:
 *   - family_capital_snapshots  → distribución ICaF, dominios, madurez
 *   - family_longitudinal_state → tendencias IMPROVING/STABLE/DECLINING
 *   - family_critical_events    → eventos detectados/resueltos, días resolución
 *
 * PRIVACIDAD: ningún campo de salida contiene family_id, nombre, email
 * ni ningún identificador personal. Los datos son estadísticas poblacionales.
 *
 * Proceso de agregación por mes:
 *   1. Recolectar el snapshot ICaF más reciente de cada familia activa en el mes.
 *   2. Calcular distribución (avg, p25, mediana, p75) del ICaF.
 *   3. Calcular % de familias en cada nivel de madurez (1-5).
 *   4. Calcular promedios de los 11 dominios.
 *   5. Agregar métricas de eventos críticos del mes.
 *   6. Calcular tendencias desde estado longitudinal.
 *   7. Persistir en observatory_snapshots (UPSERT por mes).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ObservatoryService {

    private final FamilyCapitalSnapshotRepository capitalSnapshotRepo;
    private final FamilyCriticalEventRepository   criticalEventRepo;
    private final FamilyLongitudinalStateRepository longitudinalRepo;
    private final ObservatorySnapshotRepository   observatoryRepo;

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Genera (o regenera) el snapshot del Observatorio para un mes dado.
     * Idempotente: si ya existe para ese mes, lo actualiza.
     *
     * @param yearMonth mes a agregar
     * @return snapshot generado
     */
    @Transactional
    public ObservatorySnapshot generateForMonth(YearMonth yearMonth) {
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd   = yearMonth.atEndOfMonth();
        LocalDateTime dtFrom = monthStart.atStartOfDay();
        LocalDateTime dtTo   = monthEnd.atTime(23, 59, 59);

        log.info("[Observatorio] Generando snapshot para {} ({} → {})", yearMonth, monthStart, monthEnd);

        // 1. Snapshots ICaF del mes — uno por familia (el más reciente dentro del mes)
        List<FamilyCapitalSnapshot> monthSnapshots = capitalSnapshotRepo.findAllInMonth(dtFrom, dtTo);
        List<FamilyCapitalSnapshot> latestPerFamily = latestPerFamily(monthSnapshots);

        int familiesCount = latestPerFamily.size();
        log.info("[Observatorio] {} familias activas en {}", familiesCount, yearMonth);

        // 2. Distribución ICaF
        List<Double> icafValues = latestPerFamily.stream()
                .map(FamilyCapitalSnapshot::getIcaf)
                .sorted()
                .toList();

        double icafAvg    = average(icafValues);
        double icafP25    = percentile(icafValues, 25);
        double icafMedian = percentile(icafValues, 50);
        double icafP75    = percentile(icafValues, 75);

        // 3. Distribución por nivel de madurez
        Map<Integer, Long> madurezCounts = latestPerFamily.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getMadurezNivel() != null ? s.getMadurezNivel() : 1,
                        Collectors.counting()));

        double[] madurezPcts = new double[6]; // índices 1-5
        if (familiesCount > 0) {
            for (int n = 1; n <= 5; n++) {
                madurezPcts[n] = round2((double) madurezCounts.getOrDefault(n, 0L) / familiesCount * 100);
            }
        }

        // 4. Promedios por dominio
        double avgCohesion       = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomCohesion);
        double avgConfianza      = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomConfianza);
        double avgResiliencia    = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomResiliencia);
        double avgComunicacion   = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomComunicacion);
        double avgAutonomia      = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomAutonomia);
        double avgBienestar      = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomBienestar);
        double avgProposito      = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomProposito);
        double avgIntegracion    = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomIntegracion);
        double avgEmprendimiento = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomEmprendimiento);
        double avgLegado         = avgDomain(latestPerFamily, FamilyCapitalSnapshot::getDomLegado);

        // 5. Eventos críticos del mes
        List<FamilyCriticalEvent> detected = criticalEventRepo.findDetectedInRange(monthStart, monthEnd);
        List<FamilyCriticalEvent> resolved = criticalEventRepo.findResolvedInRange(monthStart, monthEnd);

        int eventsDetected = detected.size();
        int eventsResolved = resolved.size();
        double resolutionRate = eventsDetected > 0
                ? round2((double) eventsResolved / eventsDetected * 100) : 100.0;
        double avgDaysResolution = resolved.stream()
                .filter(e -> e.getDaysToResolution() != null)
                .mapToInt(FamilyCriticalEvent::getDaysToResolution)
                .average()
                .orElse(0.0);

        // 6. Tendencias desde estado longitudinal
        List<FamilyLongitudinalState> allStates = longitudinalRepo.findAll();
        long improving = allStates.stream()
                .filter(s -> "IMPROVING".equals(s.getIcafTrend())).count();
        long declining = allStates.stream()
                .filter(s -> "DECLINING".equals(s.getIcafTrend())).count();
        long stable    = allStates.stream()
                .filter(s -> "STABLE".equals(s.getIcafTrend())).count();

        // 7. Persistir (UPSERT)
        ObservatorySnapshot snapshot = observatoryRepo.findBySnapshotMonth(monthStart)
                .orElse(ObservatorySnapshot.builder().snapshotMonth(monthStart).build());

        snapshot.setFamiliesCount(familiesCount);
        snapshot.setIcafAvg(round2(icafAvg));
        snapshot.setIcafP25(round2(icafP25));
        snapshot.setIcafMedian(round2(icafMedian));
        snapshot.setIcafP75(round2(icafP75));

        snapshot.setNivel1Pct(madurezPcts[1]);
        snapshot.setNivel2Pct(madurezPcts[2]);
        snapshot.setNivel3Pct(madurezPcts[3]);
        snapshot.setNivel4Pct(madurezPcts[4]);
        snapshot.setNivel5Pct(madurezPcts[5]);

        snapshot.setEventsDetected(eventsDetected);
        snapshot.setEventsResolved(eventsResolved);
        snapshot.setResolutionRatePct(resolutionRate);
        snapshot.setAvgDaysResolution(round2(avgDaysResolution));

        snapshot.setFamiliesImproving((int) improving);
        snapshot.setFamiliesDeclining((int) declining);
        snapshot.setFamiliesStable((int) stable);

        snapshot.setAvgDomCohesion(round2(avgCohesion));
        snapshot.setAvgDomConfianza(round2(avgConfianza));
        snapshot.setAvgDomResiliencia(round2(avgResiliencia));
        snapshot.setAvgDomComunicacion(round2(avgComunicacion));
        snapshot.setAvgDomAutonomia(round2(avgAutonomia));
        snapshot.setAvgDomBienestar(round2(avgBienestar));
        snapshot.setAvgDomProposito(round2(avgProposito));
        snapshot.setAvgDomIntegracion(round2(avgIntegracion));
        snapshot.setAvgDomEmprendimiento(round2(avgEmprendimiento));
        snapshot.setAvgDomLegado(round2(avgLegado));

        ObservatorySnapshot saved = observatoryRepo.save(snapshot);

        log.info("[Observatorio] Snapshot {} generado | familias={} | ICaFavg={} | mejorando={}% | resolucion={}%",
                yearMonth, familiesCount,
                String.format("%.1f", icafAvg),
                familiesCount > 0 ? String.format("%.1f", (double) improving / familiesCount * 100) : "N/A",
                String.format("%.1f", resolutionRate));

        return saved;
    }

    /** Consulta los últimos N meses del Observatorio */
    @Transactional(readOnly = true)
    public List<ObservatorySnapshot> getHistory(int months) {
        return observatoryRepo.findAllOrderByMonthDesc().stream()
                .limit(months)
                .sorted(Comparator.comparing(ObservatorySnapshot::getSnapshotMonth))
                .toList();
    }

    /** Snapshot más reciente disponible */
    @Transactional(readOnly = true)
    public Optional<ObservatorySnapshot> getLatest() {
        return observatoryRepo.findLatest();
    }

    /** Rango personalizado */
    @Transactional(readOnly = true)
    public List<ObservatorySnapshot> getRange(LocalDate from, LocalDate to) {
        return observatoryRepo.findInRange(from, to);
    }

    // ── Helpers estadísticos ──────────────────────────────────────────────────

    /** Selecciona el snapshot más reciente por familia dentro de un conjunto */
    private List<FamilyCapitalSnapshot> latestPerFamily(List<FamilyCapitalSnapshot> snapshots) {
        return new ArrayList<>(snapshots.stream()
                .collect(Collectors.toMap(
                        s -> s.getFamily().getId(),
                        s -> s,
                        (a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b
                ))
                .values());
    }

    private double average(List<Double> sorted) {
        if (sorted.isEmpty()) return 0.0;
        return sorted.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    /** Percentil usando interpolación lineal */
    private double percentile(List<Double> sorted, double pct) {
        if (sorted.isEmpty()) return 0.0;
        if (sorted.size() == 1) return sorted.get(0);
        double index = (pct / 100.0) * (sorted.size() - 1);
        int lo = (int) Math.floor(index);
        int hi = (int) Math.ceil(index);
        if (lo == hi) return sorted.get(lo);
        double frac = index - lo;
        return sorted.get(lo) * (1 - frac) + sorted.get(hi) * frac;
    }

    @FunctionalInterface
    private interface DomainExtractor {
        Double extract(FamilyCapitalSnapshot s);
    }

    private double avgDomain(List<FamilyCapitalSnapshot> snapshots, DomainExtractor extractor) {
        return snapshots.stream()
                .map(extractor::extract)
                .filter(v -> v != null && v > 0)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
