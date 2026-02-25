package com.ski.inventory.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.CompositeHealth;
import org.springframework.stereotype.Service;

import com.ski.inventory.monitoring.ServerErrorRecorder;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Agreguje zdraví aplikace, metriky a uptime pro admin monitoring.
 * Pouze pro role ADMIN přes /api/admin/monitoring.
 */
@Service
public class MonitoringService {

    private final HealthEndpoint healthEndpoint;
    private final MeterRegistry meterRegistry;
    private final ServerErrorRecorder serverErrorRecorder;

    public MonitoringService(HealthEndpoint healthEndpoint, MeterRegistry meterRegistry,
                             ServerErrorRecorder serverErrorRecorder) {
        this.healthEndpoint = healthEndpoint;
        this.meterRegistry = meterRegistry;
        this.serverErrorRecorder = serverErrorRecorder;
    }

    public MonitoringOverview getOverview() {
        HealthComponent healthComponent = healthEndpoint.health();
        String status = healthComponent.getStatus().getCode();
        long uptimeMs = getUptimeMs();

        double requestRatePerSecond = getRequestRatePerSecond();
        double responseTimeAvgMs = getResponseTimeAvgMs();
        double responseTimeMaxMs = getResponseTimeMaxMs();
        double errorRate = getErrorRate();

        Map<String, Object> dbInfo;
        Map<String, Object> diskInfo;
        if (healthComponent instanceof CompositeHealth composite) {
            dbInfo = getDatabaseInfo(composite);
            diskInfo = getDiskInfo(composite);
        } else {
            dbInfo = new HashMap<>();
            diskInfo = new HashMap<>();
        }
        Map<String, Object> memoryInfo = getMemoryInfo();
        List<ServerErrorRecorder.RecordedError> recentErrors = serverErrorRecorder.getRecent();

        return new MonitoringOverview(
                status,
                uptimeMs,
                formatUptime(uptimeMs),
                requestRatePerSecond,
                responseTimeAvgMs,
                responseTimeMaxMs,
                errorRate,
                dbInfo,
                diskInfo,
                memoryInfo,
                recentErrors,
                System.currentTimeMillis()
        );
    }

    private long getUptimeMs() {
        RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
        return rb.getUptime();
    }

    private static String formatUptime(long uptimeMs) {
        Duration d = Duration.ofMillis(uptimeMs);
        long days = d.toDays();
        long hours = d.toHoursPart();
        long minutes = d.toMinutesPart();
        long seconds = d.toSecondsPart();
        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        }
        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        }
        return String.format("%ds", seconds);
    }

    /** Sčítáme přes všechny timery (každá URI/status má vlastní), jinak by byl count 0. */
    private double getRequestRatePerSecond() {
        try {
            double count = meterRegistry.find("http.server.requests").timers().stream()
                    .mapToDouble(Timer::count)
                    .sum();
            long uptimeSec = Math.max(1, getUptimeMs() / 1000);
            return count / uptimeSec;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getResponseTimeAvgMs() {
        try {
            var timers = meterRegistry.find("http.server.requests").timers();
            double totalCount = timers.stream().mapToDouble(Timer::count).sum();
            if (totalCount == 0) return 0;
            double totalTimeMs = timers.stream()
                    .mapToDouble(t -> t.totalTime(TimeUnit.MILLISECONDS))
                    .sum();
            return totalTimeMs / totalCount;
        } catch (Exception e) {
            return 0;
        }
    }

    private double getResponseTimeMaxMs() {
        try {
            return meterRegistry.find("http.server.requests").timers().stream()
                    .mapToDouble(t -> t.max(TimeUnit.MILLISECONDS))
                    .max()
                    .orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private double getErrorRate() {
        try {
            double total = 0;
            double errors = 0;
            for (Timer t : meterRegistry.find("http.server.requests").timers()) {
                double c = t.count();
                total += c;
                String status = t.getId().getTag("status");
                if (status != null && status.startsWith("5")) {
                    errors += c;
                }
            }
            if (total == 0) return 0;
            return (errors / total) * 100.0;
        } catch (Exception e) {
            return 0;
        }
    }

    private Map<String, Object> getDatabaseInfo(CompositeHealth health) {
        Map<String, Object> out = new HashMap<>();
        try {
            HealthComponent db = health.getComponents().get("db");
            if (db instanceof Health h) {
                out.put("status", h.getStatus().getCode());
                if (h.getDetails() != null && !h.getDetails().isEmpty()) {
                    out.put("details", h.getDetails());
                }
            }
        } catch (Exception e) {
            out.put("status", "UNKNOWN");
            out.put("error", e.getMessage());
        }
        if (out.isEmpty()) out.put("status", "N/A");
        return out;
    }

    private Map<String, Object> getDiskInfo(CompositeHealth health) {
        Map<String, Object> out = new HashMap<>();
        try {
            HealthComponent disk = health.getComponents().get("diskSpace");
            if (disk instanceof Health h) {
                out.put("status", h.getStatus().getCode());
                if (h.getDetails() != null && !h.getDetails().isEmpty()) {
                    out.put("details", h.getDetails());
                }
            }
        } catch (Exception e) {
            out.put("status", "UNKNOWN");
        }
        if (out.isEmpty()) out.put("status", "N/A");
        return out;
    }

    /** Sčítáme jen heap (area=heap), jinak bychom brali jeden malý pool (např. 12 MB). */
    private Map<String, Object> getMemoryInfo() {
        Map<String, Object> out = new HashMap<>();
        try {
            double used = meterRegistry.find("jvm.memory.used").gauges().stream()
                    .filter(g -> "heap".equals(g.getId().getTag("area")))
                    .mapToDouble(Gauge::value)
                    .sum();
            double max = meterRegistry.find("jvm.memory.max").gauges().stream()
                    .filter(g -> "heap".equals(g.getId().getTag("area")))
                    .mapToDouble(Gauge::value)
                    .sum();
            out.put("usedBytes", (long) used);
            out.put("maxBytes", max > 0 ? (long) max : null);
            if (max > 0) {
                out.put("usedPercent", Math.round((used / max) * 100));
            }
        } catch (Exception e) {
            out.put("error", e.getMessage());
        }
        return out;
    }

    public record MonitoringOverview(
            String status,
            long uptimeMs,
            String uptimeFormatted,
            double requestRatePerSecond,
            double responseTimeAvgMs,
            double responseTimeMaxMs,
            double errorRatePercent,
            Map<String, Object> database,
            Map<String, Object> disk,
            Map<String, Object> memory,
            List<ServerErrorRecorder.RecordedError> recentErrors,
            long timestamp
    ) {}
}
