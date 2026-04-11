package com.ski.inventory.controller;

import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.security.AuthCookieService;
import com.ski.inventory.service.JwtService;
import org.springframework.security.test.context.support.WithMockUser;
import com.ski.inventory.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SwaggerEntryController.class)
@Import(AuthCookieService.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-characters-long-for-hs256",
        "jwt.expiration=86400000",
        "app.auth.cookie-secure=false"
})
class SwaggerEntryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtService jwtService;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private PasswordService passwordService;

    @Test
    @WithMockUser
    void swaggerEntry_validAdminToken_setsCookieAndRedirects() throws Exception {
        String adminToken = "valid.admin.token";
        when(jwtService.extractRole(adminToken)).thenReturn("ADMIN");
        when(jwtService.extractUsername(adminToken)).thenReturn("adminUser");
        when(jwtService.validateToken(adminToken, "adminUser")).thenReturn(true);
        when(jwtService.generateSwaggerEntryToken()).thenReturn("swagger.short.token");

        mockMvc.perform(get("/api/swagger-entry").param("token", adminToken))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"))
                .andExpect(cookie().exists(SwaggerEntryController.SWAGGER_ACCESS_COOKIE))
                .andExpect(cookie().value(SwaggerEntryController.SWAGGER_ACCESS_COOKIE, "swagger.short.token"))
                .andExpect(cookie().httpOnly(SwaggerEntryController.SWAGGER_ACCESS_COOKIE, true));
    }

    @Test
    @WithMockUser
    void swaggerEntry_nonAdminRole_returns401() throws Exception {
        String techToken = "technician.token";
        when(jwtService.extractRole(techToken)).thenReturn("TECHNICIAN");
        when(jwtService.extractUsername(techToken)).thenReturn("techUser");
        when(jwtService.validateToken(techToken, "techUser")).thenReturn(true);

        mockMvc.perform(get("/api/swagger-entry").param("token", techToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser
    void swaggerEntry_expiredAdminToken_returns401() throws Exception {
        String expiredToken = "expired.admin.token";
        when(jwtService.extractRole(expiredToken)).thenReturn("ADMIN");
        when(jwtService.extractUsername(expiredToken)).thenReturn("adminUser");
        when(jwtService.validateToken(expiredToken, "adminUser")).thenReturn(false);

        mockMvc.perform(get("/api/swagger-entry").param("token", expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void swaggerEntry_adminFromSecurityContext_noQueryToken_redirects() throws Exception {
        when(jwtService.generateSwaggerEntryToken()).thenReturn("swagger.short.token");

        mockMvc.perform(get("/api/swagger-entry"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui.html"))
                .andExpect(cookie().exists(SwaggerEntryController.SWAGGER_ACCESS_COOKIE));
    }

    @Test
    @WithMockUser
    void swaggerEntry_invalidToken_throwsException_returns401() throws Exception {
        String badToken = "broken.token";
        when(jwtService.extractRole(badToken)).thenThrow(new RuntimeException("JWT parse error"));

        mockMvc.perform(get("/api/swagger-entry").param("token", badToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerEntry_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/swagger-entry").param("token", "sometoken"))
                .andExpect(status().isUnauthorized());
    }
}
