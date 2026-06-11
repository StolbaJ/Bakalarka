package com.ski.inventory.controller;

import com.ski.inventory.model.ServiceTaskStatus;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.SkiRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/technician/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
@CrossOrigin(origins = "*")
public class DashboardController {

    private static final int PERIOD_DAYS = 7;

    private final OrderTaskRepository orderTaskRepository;
    private final SkiRepository skiRepository;

    public DashboardController(OrderTaskRepository orderTaskRepository, SkiRepository skiRepository) {
        this.orderTaskRepository = orderTaskRepository;
        this.skiRepository = skiRepository;
    }

    /** Souhrn pro úvodní stránku: inventář lyží a servisní metriky za posledních 7 dní. */
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(PERIOD_DAYS);

        long waiting = orderTaskRepository.countByStatusAndCreatedAtBetween(ServiceTaskStatus.CEKA, from, to);
        long inProgress = orderTaskRepository.countByStatusAndCreatedAtBetween(ServiceTaskStatus.PROBIHA, from, to);
        long completed = orderTaskRepository.countByStatusAndCreatedAtBetween(ServiceTaskStatus.DOKONCENO, from, to);
        Double avgHours = orderTaskRepository.averageCompletionHoursBetween(from, to);

        return ResponseEntity.ok(new DashboardSummaryResponse(
                skiRepository.count(),
                waiting + inProgress,
                completed,
                avgHours != null ? Math.round(avgHours * 10) / 10.0 : null
        ));
    }

    public record DashboardSummaryResponse(
            long totalSkis,
            long inService,
            long completed,
            Double averageCompletionHours
    ) {}
}
