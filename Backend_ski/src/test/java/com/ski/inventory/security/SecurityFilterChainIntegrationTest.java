package com.ski.inventory.security;

import com.ski.inventory.repository.*;
import com.ski.inventory.service.EmailService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integrace test SecurityConfig.securityFilterChain – načte plný Spring kontext
 * bez databáze (všechna JPA repo jsou mockována) a ověří, že URL-based security rules fungují.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration," +
                        "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "spring.mail.host=localhost",
                "jwt.secret=test-secret-key-minimum-32-characters-long-for-hs256",
                "jwt.expiration=3600000"
        })
@AutoConfigureMockMvc
class SecurityFilterChainIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock all JPA repository beans so Spring doesn't need a real database
    @MockBean private UserRepository userRepository;
    @MockBean private OrderRepository orderRepository;
    @MockBean private CustomerRepository customerRepository;
    @MockBean private SkiRepository skiRepository;
    @MockBean private OrderTaskRepository orderTaskRepository;
    @MockBean private ServiceTaskItemRepository serviceTaskItemRepository;
    @MockBean private QrScanLogRepository qrScanLogRepository;
    @MockBean private UserAuditLogRepository userAuditLogRepository;
    @MockBean private CommonModificationOptionRepository commonModificationOptionRepository;
    @MockBean private StrukturaOptionRepository strukturaOptionRepository;

    // Mock external service dependencies
    @MockBean private EmailService emailService;
    @MockBean private HealthEndpoint healthEndpoint;

    @org.junit.jupiter.api.BeforeEach
    void setupMocks() {
        // Return empty page for SkiRepository findAll (used by SkiController)
        org.mockito.Mockito.when(skiRepository.findAll(org.mockito.ArgumentMatchers.any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(org.springframework.data.domain.Page.empty());
    }

    // ===================== URL-based security rules =====================

    @Test
    void adminEndpoint_withoutAuth_accessDenied() throws Exception {
        // Unauthenticated users are denied access (401 or 403)
        int status = mockMvc.perform(get("/api/admin/users"))
                .andReturn().getResponse().getStatus();
        assert status == 401 || status == 403 : "Expected 401 or 403, got " + status;
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void adminEndpoint_technicianRole_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminEndpoint_adminRole_notForbidden() throws Exception {
        // Admin should be able to access admin endpoints (might get 500 without DB, but not 401/403)
        int status = mockMvc.perform(get("/api/admin/users"))
                .andReturn().getResponse().getStatus();
        // Should NOT be 401 or 403
        assert status != 401 && status != 403;
    }

    @Test
    void technicianEndpoint_withoutAuth_accessDenied() throws Exception {
        int status = mockMvc.perform(get("/api/technician/skis"))
                .andReturn().getResponse().getStatus();
        assert status == 401 || status == 403 : "Expected 401 or 403, got " + status;
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void technicianEndpoint_customerRole_returns403() throws Exception {
        mockMvc.perform(get("/api/technician/skis"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void technicianEndpoint_technicianRole_isAllowed() throws Exception {
        // TECHNICIAN should be allowed through to the controller (may return 200 or 500)
        mockMvc.perform(get("/api/technician/skis"))
                .andExpect(status().isOk());
    }

    @Test
    void publicEndpoint_withoutAuth_isNotForbidden() throws Exception {
        // Public endpoint should not be blocked by Spring Security (permitAll)
        // Controller may return 401 for invalid token, but NOT security-level 403
        int status = mockMvc.perform(get("/api/public/orders/view").param("token", "dummy"))
                .andReturn().getResponse().getStatus();
        assert status != 403 : "Security should not block public endpoints with 403, got " + status;
    }

    @Test
    void authEndpoint_withoutAuth_isAccessible() throws Exception {
        // /api/auth/** is permitAll, so not blocked by security (405 MethodNotAllowed for GET is OK)
        int status = mockMvc.perform(get("/api/auth/login"))
                .andReturn().getResponse().getStatus();
        assert status != 403 : "Auth endpoints should be public, got " + status;
    }

    @Test
    void csrfPing_setsReadableXsrfCookie() throws Exception {
        mockMvc.perform(get("/api/auth/csrf-ping"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false));
    }

    @Test
    void healthEndpoint_withoutAuth_isAccessible() throws Exception {
        int status = mockMvc.perform(get("/actuator/health"))
                .andReturn().getResponse().getStatus();
        assert status != 403 : "Health endpoint should be public, got " + status;
    }

    @Test
    void customerEndpoint_withoutAuth_accessDenied() throws Exception {
        int status = mockMvc.perform(get("/api/customer/orders"))
                .andReturn().getResponse().getStatus();
        assert status == 401 || status == 403 : "Expected 401 or 403, got " + status;
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void customerEndpoint_technicianRole_returns403() throws Exception {
        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isForbidden());
    }
}
