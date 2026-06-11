package com.ski.inventory.controller;

import com.ski.inventory.model.ServiceTaskStatus;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.security.AuthCookieService;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.SkiRepository;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@Import(AuthCookieService.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-characters-long-for-hs256",
        "jwt.expiration=86400000",
        "app.auth.cookie-secure=false"
})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderTaskRepository orderTaskRepository;
    @MockBean
    private SkiRepository skiRepository;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private PasswordService passwordService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_asAdmin_returnsAggregatedMetrics() throws Exception {
        when(skiRepository.count()).thenReturn(42L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.CEKA), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.PROBIHA), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.DOKONCENO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);
        when(orderTaskRepository.averageCompletionHoursBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2.47);

        mockMvc.perform(get("/api/technician/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSkis").value(42))
                .andExpect(jsonPath("$.inService").value(8))
                .andExpect(jsonPath("$.completed").value(10))
                .andExpect(jsonPath("$.averageCompletionHours").value(2.5));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getSummary_asTechnician_isAllowed() throws Exception {
        when(skiRepository.count()).thenReturn(0L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.CEKA), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.PROBIHA), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(orderTaskRepository.countByStatusAndCreatedAtBetween(eq(ServiceTaskStatus.DOKONCENO), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(orderTaskRepository.averageCompletionHoursBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(null);

        mockMvc.perform(get("/api/technician/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSkis").value(0))
                .andExpect(jsonPath("$.inService").value(0))
                .andExpect(jsonPath("$.completed").value(0))
                .andExpect(jsonPath("$.averageCompletionHours").doesNotExist());
    }
}
