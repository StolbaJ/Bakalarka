package com.ski.inventory.controller;

import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.MonitoringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MonitoringController.class)
class MonitoringControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MonitoringService monitoringService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOverview_returns200AndOverview() throws Exception {
        MonitoringService.MonitoringOverview overview = new MonitoringService.MonitoringOverview(
                "UP", 60_000L, "1m 0s", 1.5, 50.0, 200.0, 0.5,
                Map.of("status", "UP"), Map.of("status", "UP"), Map.of("usedBytes", 128_000_000L),
                List.of(), System.currentTimeMillis());
        when(monitoringService.getOverview()).thenReturn(overview);

        mockMvc.perform(get("/api/admin/monitoring/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.uptimeMs").value(60_000))
                .andExpect(jsonPath("$.uptimeFormatted").value("1m 0s"))
                .andExpect(jsonPath("$.requestRatePerSecond").value(1.5))
                .andExpect(jsonPath("$.errorRatePercent").value(0.5));
    }

    @Test
    void getOverview_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/monitoring/overview"))
                .andExpect(status().isUnauthorized());
    }
}
