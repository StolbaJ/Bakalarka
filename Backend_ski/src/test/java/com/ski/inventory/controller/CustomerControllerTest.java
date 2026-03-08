package com.ski.inventory.controller;

import com.ski.inventory.model.Customer;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.repository.CustomerRepository;
import com.ski.inventory.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getAllCustomers_returnsPaginated() throws Exception {
        Customer c = new Customer();
        c.setId(1L);
        c.setCustomerNumber("CUST-001");
        c.setName("Jan Novák");
        c.setPhone("+420123456789");
        c.setEmail("jan@example.com");
        when(customerRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(c), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/technician/customers").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].customerNumber").value("CUST-001"))
                .andExpect(jsonPath("$.content[0].name").value("Jan Novák"))
                .andExpect(jsonPath("$.content[0].email").value("jan@example.com"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getCustomer_exists_returns200() throws Exception {
        Customer c = new Customer();
        c.setId(1L);
        c.setCustomerNumber("CUST-001");
        c.setName("Jan Novák");
        c.setPhone("+420123456789");
        c.setEmail("jan@example.com");
        c.setAddress("Ulice 1");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(c));

        mockMvc.perform(get("/api/technician/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jan Novák"))
                .andExpect(jsonPath("$.address").value("Ulice 1"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getCustomer_notFound_returns404() throws Exception {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/technician/customers/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_validRequest_returns200() throws Exception {
        Customer saved = new Customer();
        saved.setId(1L);
        saved.setCustomerNumber("CUST-001");
        saved.setName("Jan Novák");
        saved.setPhone("+420123456789");
        saved.setEmail("jan@example.com");
        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        mockMvc.perform(post("/api/technician/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan Novák\",\"email\":\"jan@example.com\",\"phone\":\"+420123456789\",\"address\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Jan Novák"))
                .andExpect(jsonPath("$.email").value("jan@example.com"));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_blankName_returns400WithMessage() throws Exception {
        mockMvc.perform(post("/api/technician/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"email\":\"a@b.cz\",\"phone\":\"+420123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Jméno je povinné."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/technician/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan\",\"email\":\"not-an-email\",\"phone\":\"+420123456789\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Neplatný formát e-mailu."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createCustomer_phoneTooShort_returns400() throws Exception {
        mockMvc.perform(post("/api/technician/customers")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan\",\"email\":\"jan@example.com\",\"phone\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Telefonní číslo musí obsahovat 9–15 číslic."));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCustomer_exists_returns200() throws Exception {
        Customer existing = new Customer();
        existing.setId(1L);
        existing.setCustomerNumber("CUST-001");
        existing.setName("Jan");
        existing.setPhone("+420123456789");
        existing.setEmail("jan@example.com");
        when(customerRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/technician/customers/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan Novák\",\"email\":\"jan@example.com\",\"phone\":\"+420123456789\",\"address\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jan Novák"));
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateCustomer_notFound_returns404() throws Exception {
        when(customerRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/technician/customers/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Jan\",\"email\":\"j@j.cz\",\"phone\":\"+420123456789\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAllCustomers_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/technician/customers"))
                .andExpect(status().isUnauthorized());
    }
}
