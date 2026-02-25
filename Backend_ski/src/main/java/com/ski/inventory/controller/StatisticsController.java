package com.ski.inventory.controller;

import com.ski.inventory.model.ServiceTaskStatus;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.ServiceTaskItemRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/statistics")
@PreAuthorize("hasRole('ADMIN')")
@CrossOrigin(origins = "*")
public class StatisticsController {

    private static final int TOP_LIMIT = 10;

    private final OrderTaskRepository orderTaskRepository;
    private final ServiceTaskItemRepository serviceTaskItemRepository;

    public StatisticsController(OrderTaskRepository orderTaskRepository, ServiceTaskItemRepository serviceTaskItemRepository) {
        this.orderTaskRepository = orderTaskRepository;
        this.serviceTaskItemRepository = serviceTaskItemRepository;
    }

    /**
     * Statistiky úkolů (lyží) v objednávkách: počty dle stavu v daném období, průměrný čas a denní přehled.
     */
    @GetMapping
    public ResponseEntity<StatisticsResponse> getStatistics(
            @RequestParam(defaultValue = "7d") String period) {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = parsePeriodStart(period, to);

        long waiting = orderTaskRepository.countByStatusAndCreatedAtBetween(ServiceTaskStatus.CEKA, from, to);
        long inProgress = orderTaskRepository.countByStatusAndCreatedAtBetween(ServiceTaskStatus.PROBIHA, from, to);
        long completed = orderTaskRepository.countByStatusAndCreatedAtBetween(ServiceTaskStatus.DOKONCENO, from, to);
        Double avgHours = orderTaskRepository.averageCompletionHoursBetween(from, to);

        List<DailyStats> daily = buildDailyStats(from, to);
        List<NameCount> topTaskTypes = getTopTaskTypes(from, to);
        List<NameCount> topStructures = getTopStructures(from, to);

        return ResponseEntity.ok(new StatisticsResponse(
                waiting,
                inProgress,
                completed,
                avgHours != null ? Math.round(avgHours * 10) / 10.0 : null,
                period,
                daily,
                topTaskTypes,
                topStructures
        ));
    }

    private List<NameCount> getTopTaskTypes(LocalDateTime from, LocalDateTime to) {
        return serviceTaskItemRepository.countByTaskNameAndTaskCreatedAtBetween(from, to).stream()
                .limit(TOP_LIMIT)
                .map(row -> new NameCount((String) row[0], ((Number) row[1]).intValue()))
                .toList();
    }

    private List<NameCount> getTopStructures(LocalDateTime from, LocalDateTime to) {
        return orderTaskRepository.countByTargetStrukturaAndCreatedAtBetween(from, to).stream()
                .limit(TOP_LIMIT)
                .map(row -> new NameCount((String) row[0], ((Number) row[1]).intValue()))
                .toList();
    }

    private List<DailyStats> buildDailyStats(LocalDateTime from, LocalDateTime to) {
        LocalDate fromDate = from.toLocalDate();
        LocalDate toDate = to.toLocalDate();
        List<Object[]> createdRows = orderTaskRepository.countCreatedByDayBetween(from, to);
        List<Object[]> completedRows = orderTaskRepository.countCompletedByDayBetween(from, to);
        Map<LocalDate, Long> createdByDay = toMap(createdRows);
        Map<LocalDate, Long> completedByDay = toMap(completedRows);
        List<DailyStats> result = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            result.add(new DailyStats(
                    d.toString(),
                    createdByDay.getOrDefault(d, 0L).intValue(),
                    completedByDay.getOrDefault(d, 0L).intValue()
            ));
        }
        return result;
    }

    private static Map<LocalDate, Long> toMap(List<Object[]> rows) {
        return rows.stream().collect(Collectors.toMap(
                row -> toLocalDate(row[0]),
                row -> ((Number) row[1]).longValue()
        ));
    }

    private static LocalDate toLocalDate(Object value) {
        if (value instanceof java.sql.Date d) return d.toLocalDate();
        if (value instanceof LocalDate ld) return ld;
        if (value instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
        throw new IllegalArgumentException("Unexpected date type: " + (value != null ? value.getClass() : "null"));
    }

    private static LocalDateTime parsePeriodStart(String period, LocalDateTime to) {
        int days = 7;
        if (period != null) {
            switch (period) {
                case "30d" -> days = 30;
                case "90d" -> days = 90;
                case "1y" -> days = 365;
                default -> { /* 7d */ }
            }
        }
        return to.minusDays(days);
    }

    public record StatisticsResponse(
            long waiting,
            long inProgress,
            long completed,
            Double averageCompletionHours,
            String period,
            List<DailyStats> daily,
            List<NameCount> topTaskTypes,
            List<NameCount> topStructures
    ) {}

    public record DailyStats(String date, int created, int completed) {}

    public record NameCount(String name, int count) {}
}
