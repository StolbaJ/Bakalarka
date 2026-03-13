package com.ski.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ski.inventory.model.Ski;
import com.ski.inventory.model.SkiCondition;
import com.ski.inventory.model.SkiStatus;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.SkiRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SkiController.class)
class SkiControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SkiRepository skiRepository;
    @MockBean
    private OrderTaskRepository orderTaskRepository;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllSkis_returnsPaginated() throws Exception {
        Ski ski = createSki(1L, "SKI-001", "Brand", "Model");
        Page<Ski> page = new PageImpl<>(List.of(ski), PageRequest.of(0, 20), 1);
        when(skiRepository.findAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/technician/skis").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].skiNumber").value("SKI-001"))
                .andExpect(jsonPath("$.content[0].brand").value("Brand"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getSki_byId_exists() throws Exception {
        Ski ski = createSki(1L, "SKI-002", "A", "B");
        when(skiRepository.findById(1L)).thenReturn(Optional.of(ski));

        mockMvc.perform(get("/api/technician/skis/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.skiNumber").value("SKI-002"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSki_byId_notFound() throws Exception {
        when(skiRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/technician/skis/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getSkiByNumber_exists() throws Exception {
        Ski ski = createSki(1L, "SKI-NUM-1", "X", "Y");
        when(skiRepository.findBySkiNumber("SKI-NUM-1")).thenReturn(Optional.of(ski));

        mockMvc.perform(get("/api/technician/skis/by-number/SKI-NUM-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.skiNumber").value("SKI-NUM-1"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createSki_validRequest_returns201() throws Exception {
        SkiController.CreateSkiRequest req = new SkiController.CreateSkiRequest(
                "Brand", "Model", "170", 2023, "allround", BigDecimal.valueOf(3.5),
                "DOBRY", "DOSTUPNY", "A1", "notes", null, null, null, null, null, null, null);
        Ski saved = createSki(1L, "SKI-001", "Brand", "Model");
        when(skiRepository.save(any(Ski.class))).thenReturn(saved);

        mockMvc.perform(post("/api/technician/skis")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.brand").value("Brand"))
                .andExpect(jsonPath("$.model").value("Model"));
        verify(skiRepository).save(any(Ski.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSki_exists_returns200() throws Exception {
        Ski existing = createSki(1L, "SKI-001", "Old", "OldModel");
        SkiController.UpdateSkiRequest req = new SkiController.UpdateSkiRequest(
                "NewBrand", "NewModel", "175", 2024, "race", BigDecimal.valueOf(4.0),
                "VYORNY", "V_SERVISU", "B2", "updated", null, null, null, null, null, null, null);
        when(skiRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(skiRepository.save(any(Ski.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/technician/skis/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brand").value("NewBrand"))
                .andExpect(jsonPath("$.model").value("NewModel"));
        verify(skiRepository).save(existing);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateSki_notFound_returns404() throws Exception {
        when(skiRepository.findById(999L)).thenReturn(Optional.empty());
        SkiController.UpdateSkiRequest req = new SkiController.UpdateSkiRequest(
                "B", "M", "170", 2023, null, null, null, null, null, null, null, null, null, null, null, null, null);

        mockMvc.perform(put("/api/technician/skis/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSki_exists_returns204() throws Exception {
        when(skiRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/technician/skis/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(skiRepository).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteSki_notFound_returns404() throws Exception {
        when(skiRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/technician/skis/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllSkis_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/technician/skis"))
                .andExpect(status().isUnauthorized());
    }

    private static Ski createSki(Long id, String skiNumber, String brand, String model) {
        Ski ski = new Ski();
        ski.setId(id);
        ski.setSkiNumber(skiNumber);
        ski.setBrand(brand);
        ski.setModel(model);
        ski.setLength("170");
        ski.setYear(2023);
        ski.setCondition(SkiCondition.DOBRY);
        ski.setStatus(SkiStatus.DOSTUPNY);
        return ski;
    }
}
