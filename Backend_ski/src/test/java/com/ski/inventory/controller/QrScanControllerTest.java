package com.ski.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ski.inventory.model.*;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.security.AuthCookieService;
import com.ski.inventory.repository.QrScanLogRepository;
import com.ski.inventory.repository.SkiRepository;
import com.ski.inventory.repository.UserRepository;
import com.ski.inventory.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(QrScanController.class)
@Import(AuthCookieService.class)
@TestPropertySource(properties = {
        "jwt.secret=test-secret-key-minimum-32-characters-long-for-hs256",
        "jwt.expiration=86400000",
        "app.auth.cookie-secure=false"
})
class QrScanControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QrScanLogRepository qrScanLogRepository;
    @MockBean
    private SkiRepository skiRepository;
    @MockBean
    private UserRepository userRepository;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void recordScan_bySkiNumber_savesAndReturns200() throws Exception {
        User user = makeUser(1L, "tech1");
        Ski ski = makeSki(10L, "SKI-100", "Fischer", "RC4", "170");

        when(userRepository.findByUsernameAndActiveTrue("tech1")).thenReturn(Optional.of(user));
        when(skiRepository.findBySkiNumber("SKI-100")).thenReturn(Optional.of(ski));

        QrScanLog savedLog = new QrScanLog();
        savedLog.setId(1L);
        savedLog.setUser(user);
        savedLog.setSki(ski);
        savedLog.setScannedAt(LocalDateTime.now());
        when(qrScanLogRepository.save(any(QrScanLog.class))).thenReturn(savedLog);
        when(qrScanLogRepository.findByUserIdOrderByScannedAtDesc(eq(1L), any())).thenReturn(List.of(savedLog));

        QrScanController.RecordQrScanRequest req = new QrScanController.RecordQrScanRequest("SKI-100");

        mockMvc.perform(post("/api/technician/qr-scans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skiNumber").value("SKI-100"))
                .andExpect(jsonPath("$.skiInfo").value("Fischer RC4 170"));
    }

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void recordScan_bySkiId_savesAndReturns200() throws Exception {
        User user = makeUser(1L, "tech1");
        Ski ski = makeSki(42L, "SKI-042", "Head", "Monster", "175");

        when(userRepository.findByUsernameAndActiveTrue("tech1")).thenReturn(Optional.of(user));
        when(skiRepository.findById(42L)).thenReturn(Optional.of(ski));

        QrScanLog savedLog = new QrScanLog();
        savedLog.setId(2L);
        savedLog.setUser(user);
        savedLog.setSki(ski);
        savedLog.setScannedAt(LocalDateTime.now());
        when(qrScanLogRepository.save(any(QrScanLog.class))).thenReturn(savedLog);
        when(qrScanLogRepository.findByUserIdOrderByScannedAtDesc(eq(1L), any())).thenReturn(List.of(savedLog));

        QrScanController.RecordQrScanRequest req = new QrScanController.RecordQrScanRequest("42");

        mockMvc.perform(post("/api/technician/qr-scans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skiNumber").value("SKI-042"));
    }

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void recordScan_blankScannedValue_returns400() throws Exception {
        QrScanController.RecordQrScanRequest req = new QrScanController.RecordQrScanRequest("  ");

        mockMvc.perform(post("/api/technician/qr-scans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void recordScan_skiNotFound_returns404() throws Exception {
        User user = makeUser(1L, "tech1");
        when(userRepository.findByUsernameAndActiveTrue("tech1")).thenReturn(Optional.of(user));
        when(skiRepository.findBySkiNumber("NONEXISTENT")).thenReturn(Optional.empty());

        QrScanController.RecordQrScanRequest req = new QrScanController.RecordQrScanRequest("NONEXISTENT");

        mockMvc.perform(post("/api/technician/qr-scans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void getRecentScans_returnsListForCurrentUser() throws Exception {
        User user = makeUser(1L, "tech1");
        Ski ski = makeSki(1L, "SKI-001", "Atomic", "Redster", "165");

        QrScanLog log = new QrScanLog();
        log.setId(1L);
        log.setUser(user);
        log.setSki(ski);
        log.setScannedAt(LocalDateTime.now());

        when(userRepository.findByUsernameAndActiveTrue("tech1")).thenReturn(Optional.of(user));
        when(qrScanLogRepository.findByUserIdOrderByScannedAtDesc(eq(1L), any())).thenReturn(List.of(log));

        mockMvc.perform(get("/api/technician/qr-scans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].skiNumber").value("SKI-001"))
                .andExpect(jsonPath("$[0].skiInfo").value("Atomic Redster 165"));
    }

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void getRecentScans_noScans_returnsEmptyList() throws Exception {
        User user = makeUser(1L, "tech1");
        when(userRepository.findByUsernameAndActiveTrue("tech1")).thenReturn(Optional.of(user));
        when(qrScanLogRepository.findByUserIdOrderByScannedAtDesc(eq(1L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/technician/qr-scans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getRecentScans_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/technician/qr-scans"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "tech1", roles = "TECHNICIAN")
    void recordScan_moreThan10Logs_deletesOldest() throws Exception {
        User user = makeUser(1L, "tech1");
        Ski ski = makeSki(1L, "SKI-001", "Atomic", "Redster", "165");

        when(userRepository.findByUsernameAndActiveTrue("tech1")).thenReturn(Optional.of(user));
        when(skiRepository.findBySkiNumber("SKI-001")).thenReturn(Optional.of(ski));

        QrScanLog savedLog = new QrScanLog();
        savedLog.setId(11L);
        savedLog.setUser(user);
        savedLog.setSki(ski);
        savedLog.setScannedAt(LocalDateTime.now());
        when(qrScanLogRepository.save(any(QrScanLog.class))).thenReturn(savedLog);

        // Return 10 logs (max)
        List<QrScanLog> top10 = List.of(savedLog);
        when(qrScanLogRepository.findByUserIdOrderByScannedAtDesc(eq(1L), any())).thenReturn(top10);

        QrScanController.RecordQrScanRequest req = new QrScanController.RecordQrScanRequest("SKI-001");

        mockMvc.perform(post("/api/technician/qr-scans")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());

        verify(qrScanLogRepository).deleteByUserIdAndIdNotIn(eq(1L), anySet());
    }

    private User makeUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(UserRole.TECHNICIAN);
        user.setActive(true);
        return user;
    }

    private Ski makeSki(Long id, String skiNumber, String brand, String model, String length) {
        Ski ski = new Ski();
        ski.setId(id);
        ski.setSkiNumber(skiNumber);
        ski.setBrand(brand);
        ski.setModel(model);
        ski.setLength(length);
        ski.setCondition(SkiCondition.DOBRY);
        ski.setStatus(SkiStatus.DOSTUPNY);
        return ski;
    }
}
