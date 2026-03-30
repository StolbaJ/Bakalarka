package com.ski.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ski.inventory.model.User;
import com.ski.inventory.model.UserAuditLog;
import com.ski.inventory.model.UserAuditAction;
import com.ski.inventory.model.UserRole;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.repository.UserAuditLogRepository;
import com.ski.inventory.repository.UserRepository;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.PasswordService;
import com.ski.inventory.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private UserAuditLogRepository auditLogRepository;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private PasswordService passwordService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_returnsUserList() throws Exception {
        User user = makeUser(1L, "admin", UserRole.ADMIN);
        when(userRepository.findAll()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].username").value("admin"))
                .andExpect(jsonPath("$[0].role").value("ADMIN"))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_emptyList_returnsEmptyArray() throws Exception {
        when(userRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getAllUsers_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUsers_multipleUsers_returnsList() throws Exception {
        User u1 = makeUser(1L, "admin", UserRole.ADMIN);
        User u2 = makeUser(2L, "tech", UserRole.TECHNICIAN);
        when(userRepository.findAll()).thenReturn(List.of(u1, u2));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].username").value("tech"))
                .andExpect(jsonPath("$[1].role").value("TECHNICIAN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_invalidRole_returns400() throws Exception {
        UserController.CreateUserRequest req = new UserController.CreateUserRequest(
                "user1", "pass123", "INVALID", null, null, false);

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation failed"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_validRequest_returns201() throws Exception {
        User created = makeUser(5L, "newtech", UserRole.TECHNICIAN);
        when(userService.createUser(eq("newtech"), eq("pass123"), eq(UserRole.TECHNICIAN),
                eq("New Tech"), eq("tech@test.cz"), eq(false))).thenReturn(created);

        UserController.CreateUserRequest req = new UserController.CreateUserRequest(
                "newtech", "pass123", "TECHNICIAN", "New Tech", "tech@test.cz", false);

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newtech"))
                .andExpect(jsonPath("$.role").value("TECHNICIAN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createUser_withGeneratedPassword_returns201() throws Exception {
        User created = makeUser(6L, "genuser", UserRole.TECHNICIAN);
        created.setEmail("gen@test.cz");
        when(userService.createUser(eq("genuser"), isNull(), eq(UserRole.TECHNICIAN),
                eq("Gen User"), eq("gen@test.cz"), eq(true))).thenReturn(created);

        UserController.CreateUserRequest req = new UserController.CreateUserRequest(
                "genuser", null, "TECHNICIAN", "Gen User", "gen@test.cz", true);

        mockMvc.perform(post("/api/admin/users")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateRole_existingUser_returns200() throws Exception {
        User updated = makeUser(1L, "tech", UserRole.ADMIN);
        when(userService.updateRole(1L, UserRole.ADMIN)).thenReturn(updated);

        UserController.UpdateRoleRequest req = new UserController.UpdateRoleRequest("ADMIN");

        mockMvc.perform(patch("/api/admin/users/1/role")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void resetPassword_existingUser_returns200() throws Exception {
        when(userService.resetPassword(1L)).thenReturn("NewSecurePass!");

        mockMvc.perform(post("/api/admin/users/1/reset-password")
                        .with(csrf()))
                .andExpect(status().isOk())
                // showPasswordOnReset defaults to false, so newPassword should be null
                .andExpect(jsonPath("$.newPassword").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deactivateUser_existingUser_returns200() throws Exception {
        User deactivated = makeUser(2L, "tech", UserRole.TECHNICIAN);
        deactivated.setActive(false);
        when(userService.deactivateUser(2L)).thenReturn(deactivated);

        mockMvc.perform(patch("/api/admin/users/2/deactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reactivateUser_existingUser_returns200() throws Exception {
        User reactivated = makeUser(2L, "tech", UserRole.TECHNICIAN);
        reactivated.setActive(true);
        when(userService.reactivateUser(2L)).thenReturn(reactivated);

        mockMvc.perform(patch("/api/admin/users/2/reactivate").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_noFilter_returnsPaged() throws Exception {
        UserAuditLog log = new UserAuditLog();
        log.setId(1L);
        log.setAction(UserAuditAction.CREATE);
        log.setTargetUserId(5L);
        log.setTargetUsername("newuser");
        log.setPerformedBy("admin");
        log.setDetails("Account created");
        log.setCreatedAt(LocalDateTime.now());

        Page<UserAuditLog> page = new PageImpl<>(List.of(log), PageRequest.of(0, 50), 1);
        when(auditLogRepository.findAllByOrderByCreatedAtDesc(any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/users/audit-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].action").value("CREATE"))
                .andExpect(jsonPath("$.content[0].targetUsername").value("newuser"))
                .andExpect(jsonPath("$.content[0].performedBy").value("admin"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAuditLog_withUserIdFilter_returnsFiltered() throws Exception {
        Page<UserAuditLog> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 50), 0);
        when(auditLogRepository.findByTargetUserIdOrderByCreatedAtDesc(eq(5L), any())).thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/users/audit-log").param("userId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    private User makeUser(Long id, String username, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setFullName("Full Name");
        user.setEmail("user@test.cz");
        user.setActive(true);
        return user;
    }
}
