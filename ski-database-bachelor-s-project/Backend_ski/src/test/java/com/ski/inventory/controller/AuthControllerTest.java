package com.ski.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.security.AuthCookieService;
import com.ski.inventory.service.AuthenticationService;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({GlobalExceptionHandler.class, AuthCookieService.class})
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-characters-long-for-hs256",
        "jwt.expiration=86400000",
        "app.auth.cookie-secure=false"
})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationService authenticationService;
    @MockBean
    private UserService userService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;

    @Test
    void login_validCredentials_returns200AndTokens() throws Exception {
        AuthenticationService.AuthResponse response = new AuthenticationService.AuthResponse("access", 1L, "user", "ADMIN", "Full Name", "user@example.com");
        when(authenticationService.authenticate("admin", "pass")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"pass\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void login_invalidCredentials_returns401WithErrorBody() throws Exception {
        when(authenticationService.authenticate(anyString(), anyString()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }

    @Test
    void refresh_noAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refresh_validToken_returns200() throws Exception {
        AuthenticationService.AuthResponse response = new AuthenticationService.AuthResponse("newToken", 1L, "user", "ADMIN", "Full", "a@b.com");
        when(authenticationService.refreshToken("valid-token")).thenReturn(response);

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    void refresh_invalidToken_returns401() throws Exception {
        when(authenticationService.refreshToken("invalid")).thenThrow(new BadCredentialsException("Invalid"));

        mockMvc.perform(post("/api/auth/refresh")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void changePassword_valid_returns200() throws Exception {
        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"oldpass\",\"newPassword\":\"newpass1\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changePassword_wrongCurrent_returns401() throws Exception {
        doThrow(new BadCredentialsException("Wrong password"))
                .when(userService).changeOwnPassword("wrong", "newpass1");

        mockMvc.perform(patch("/api/auth/change-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"newpass1\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankUsername_returns400WithValidationBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.username").exists());
    }

    @Test
    void loginCustomer_valid_returns200() throws Exception {
        AuthenticationService.AuthResponse response = new AuthenticationService.AuthResponse("t", 1L, "customer", "CUSTOMER", "Name", null);
        when(authenticationService.authenticateCustomer("ORD-1", "+420123456789")).thenReturn(response);

        mockMvc.perform(post("/api/auth/login/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"ORD-1\",\"phone\":\"+420123456789\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("customer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void loginCustomer_orderNotFound_returns401() throws Exception {
        when(authenticationService.authenticateCustomer("BAD", "123"))
                .thenThrow(new BadCredentialsException("Not found"));

        mockMvc.perform(post("/api/auth/login/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderNumber\":\"BAD\",\"phone\":\"123\"}"))
                .andExpect(status().isUnauthorized());
    }
}
