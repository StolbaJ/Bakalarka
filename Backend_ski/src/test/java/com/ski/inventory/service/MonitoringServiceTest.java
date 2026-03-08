package com.ski.inventory.service;

import com.ski.inventory.monitoring.ServerErrorRecorder;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MonitoringServiceTest {

    private HealthEndpoint healthEndpoint;
    private MeterRegistry meterRegistry;
    private ServerErrorRecorder serverErrorRecorder;
    private MonitoringService monitoringService;

    @BeforeEach
    void setUp() {
        healthEndpoint = mock(HealthEndpoint.class);
        meterRegistry = new SimpleMeterRegistry();
        serverErrorRecorder = mock(ServerErrorRecorder.class);
        when(healthEndpoint.health()).thenReturn(Health.up().build());
        when(serverErrorRecorder.getRecent()).thenReturn(List.of());
        monitoringService = new MonitoringService(healthEndpoint, meterRegistry, serverErrorRecorder);
    }

    @Test
    void getOverview_returnsOverviewWithStatusAndUptime() {
        MonitoringService.MonitoringOverview overview = monitoringService.getOverview();

        assertThat(overview.status()).isEqualTo("UP");
        assertThat(overview.uptimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(overview.uptimeFormatted()).isNotBlank();
        assertThat(overview.requestRatePerSecond()).isEqualTo(0);
        assertThat(overview.responseTimeAvgMs()).isEqualTo(0);
        assertThat(overview.responseTimeMaxMs()).isEqualTo(0);
        assertThat(overview.errorRatePercent()).isEqualTo(0);
        assertThat(overview.database()).isNotNull();
        assertThat(overview.disk()).isNotNull();
        assertThat(overview.memory()).isNotNull();
        assertThat(overview.recentErrors()).isEmpty();
        assertThat(overview.timestamp()).isPositive();
    }

}
