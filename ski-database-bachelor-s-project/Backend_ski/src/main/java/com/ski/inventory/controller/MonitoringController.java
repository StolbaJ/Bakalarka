package com.ski.inventory.controller;

import com.ski.inventory.service.MonitoringService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitoring")
@PreAuthorize("hasRole('ADMIN')")
public class MonitoringController {

    private final MonitoringService monitoringService;

    public MonitoringController(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    /**
     * Přehled zdraví, metrik a dostupnosti backendu. Pouze pro ADMIN.
     * Frontend může volat periodicky a měřit dobu odpovědi (uptime monitoring).
     */
    @GetMapping("/overview")
    public ResponseEntity<MonitoringService.MonitoringOverview> getOverview() {
        return ResponseEntity.ok(monitoringService.getOverview());
    }
}
