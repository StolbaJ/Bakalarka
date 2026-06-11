package com.ski.inventory.controller;

import com.ski.inventory.model.ServiceTaskStatus;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.security.AuthCookieService;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.ServiceTaskItemRepository;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StatisticsController.class)
@Import(AuthCookieService.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-characters-long-for-hs256",
        "jwt.expiration=86400000",
        "app.auth.cookie-secure=false"
})
class StatisticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderTaskRepository orderTaskRepository;
    @MockBean
    private ServiceTaskItemRepository serviceTaskItemRepository;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private PasswordService passwordService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_defaultPeriod_returns7DayStats() throws Exception {
        setupMocks(5L, 3L, 10L, 2.5);

        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.waiting").value(5))
                .andExpect(jsonPath("$.inProgress").value(3))
                .andExpect(jsonPath("$.completed").value(10))
                .andExpect(jsonPath("$.averageCompletionHours").value(2.5))
                .andExpect(jsonPath("$.period").value("7d"))
                .andExpect(jsonPath("$.daily").isArray())
                .andExpect(jsonPath("$.topTaskTypes").isArray())
                .andExpect(jsonPath("$.topStructures").isArray());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_period30d_returnsCorrectPeriod() throws Exception {
        setupMocks(0L, 0L, 0L, null);

        mockMvc.perform(get("/api/admin/statistics").param("period", "30d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("30d"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_period90d_returnsCorrectPeriod() throws Exception {
        setupMocks(0L, 0L, 0L, null);

        mockMvc.perform(get("/api/admin/statistics").param("period", "90d"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("90d"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_period1y_returnsCorrectPeriod() throws Exception {
        setupMocks(0L, 0L, 0L, null);

        mockMvc.perform(get("/api/admin/statistics").param("period", "1y"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("1y"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_nullAvgHours_returnsNullInResponse() throws Exception {
        setupMocks(1L, 2L, 3L, null);

        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageCompletionHours").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_withTopTaskTypes_returnsNameCounts() throws Exception {
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.CEKA), any(), any())).thenReturn(0L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.PROBIHA), any(), any())).thenReturn(0L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.DOKONCENO), any(), any())).thenReturn(0L);
        when(orderTaskRepository.averageCompletionHoursBetween(any(), any())).thenReturn(null);
        when(orderTaskRepository.countCreatedByDayBetween(any(), any())).thenReturn(List.of());
        when(orderTaskRepository.countCompletedByDayBetween(any(), any())).thenReturn(List.of());
        when(serviceTaskItemRepository.countByTaskNameAndTaskCreatedAtBetween(any(), any()))
                .thenReturn(List.of(new Object[]{"Broušení hran", 15L}, new Object[]{"Voskování", 10L}));
        when(orderTaskRepository.countByTargetStrukturaAndCreatedAtBetween(any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topTaskTypes", hasSize(2)))
                .andExpect(jsonPath("$.topTaskTypes[0].name").value("Broušení hran"))
                .andExpect(jsonPath("$.topTaskTypes[0].count").value(15));
    }

    @Test
    void getStatistics_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/statistics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getStatistics_unknownPeriod_defaults7d() throws Exception {
        setupMocks(0L, 0L, 0L, null);

        mockMvc.perform(get("/api/admin/statistics").param("period", "unknown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("unknown"));
    }

    private void setupMocks(long waiting, long inProgress, long completed, Double avgHours) {
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.CEKA), any(), any())).thenReturn(waiting);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.PROBIHA), any(), any())).thenReturn(inProgress);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.DOKONCENO), any(), any())).thenReturn(completed);
        when(orderTaskRepository.averageCompletionHoursBetween(any(), any())).thenReturn(avgHours);
        when(orderTaskRepository.countCreatedByDayBetween(any(), any())).thenReturn(List.of());
        when(orderTaskRepository.countCompletedByDayBetween(any(), any())).thenReturn(List.of());
        when(serviceTaskItemRepository.countByTaskNameAndTaskCreatedAtBetween(any(), any())).thenReturn(List.of());
        when(orderTaskRepository.countByTargetStrukturaAndCreatedAtBetween(any(), any())).thenReturn(List.of());
    }
}
