package com.ski.inventory.controller;

import com.ski.inventory.model.CommonModificationOption;
import com.ski.inventory.model.StrukturaOption;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.repository.CommonModificationOptionRepository;
import com.ski.inventory.repository.StrukturaOptionRepository;
import com.ski.inventory.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

@WebMvcTest(OptionsController.class)
class OptionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StrukturaOptionRepository strukturaOptionRepository;
    @MockBean
    private CommonModificationOptionRepository commonModificationOptionRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getStruktury_returnsList() throws Exception {
        StrukturaOption o = new StrukturaOption();
        o.setId(1L);
        o.setName("Struktura A");
        o.setSortOrder(0);
        when(strukturaOptionRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(o));

        mockMvc.perform(get("/api/technician/options/struktury"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Struktura A"))
                .andExpect(jsonPath("$[0].sortOrder").value(0));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUpravy_returnsList() throws Exception {
        CommonModificationOption o = new CommonModificationOption();
        o.setId(1L);
        o.setName("Úprava");
        o.setSortOrder(0);
        o.setRequiresWorkDescription(false);
        when(commonModificationOptionRepository.findAllByOrderBySortOrderAscNameAsc()).thenReturn(List.of(o));

        mockMvc.perform(get("/api/technician/options/upravy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Úprava"))
                .andExpect(jsonPath("$[0].requiresWorkDescription").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addStruktura_validRequest_returns200() throws Exception {
        StrukturaOption saved = new StrukturaOption();
        saved.setId(1L);
        saved.setName("New");
        saved.setSortOrder(1);
        when(strukturaOptionRepository.save(any(StrukturaOption.class))).thenReturn(saved);

        mockMvc.perform(post("/api/technician/options/struktury")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"New\",\"sortOrder\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("New"));
        verify(strukturaOptionRepository).save(any(StrukturaOption.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addStruktura_blankName_returns400() throws Exception {
        mockMvc.perform(post("/api/technician/options/struktury")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"   \",\"sortOrder\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteStruktura_exists_returns204() throws Exception {
        when(strukturaOptionRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/technician/options/struktury/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(strukturaOptionRepository).deleteById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteStruktura_notFound_returns404() throws Exception {
        when(strukturaOptionRepository.existsById(999L)).thenReturn(false);

        mockMvc.perform(delete("/api/technician/options/struktury/999").with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addUprava_validRequest_returns200() throws Exception {
        CommonModificationOption saved = new CommonModificationOption();
        saved.setId(1L);
        saved.setName("Úprava");
        saved.setSortOrder(0);
        saved.setRequiresWorkDescription(true);
        when(commonModificationOptionRepository.save(any(CommonModificationOption.class))).thenReturn(saved);

        mockMvc.perform(post("/api/technician/options/upravy")
                        .with(csrf())
                        .contentType("application/json")
                        .content("{\"name\":\"Úprava\",\"sortOrder\":0,\"requiresWorkDescription\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.requiresWorkDescription").value(true));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteUprava_exists_returns204() throws Exception {
        when(commonModificationOptionRepository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/technician/options/upravy/1").with(csrf()))
                .andExpect(status().isNoContent());
        verify(commonModificationOptionRepository).deleteById(1L);
    }

    @Test
    void getStruktury_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/technician/options/struktury"))
                .andExpect(status().isUnauthorized());
    }
}
