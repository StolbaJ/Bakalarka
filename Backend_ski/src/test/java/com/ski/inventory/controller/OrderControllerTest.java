package com.ski.inventory.controller;

import com.ski.inventory.model.*;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.repository.*;
import com.ski.inventory.service.EmailService;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.OrderCreatedNotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private OrderTaskRepository orderTaskRepository;
    @MockBean
    private ServiceTaskItemRepository serviceTaskItemRepository;
    @MockBean
    private CommonModificationOptionRepository commonModificationOptionRepository;
    @MockBean
    private SkiRepository skiRepository;
    @MockBean
    private CustomerRepository customerRepository;
    @MockBean
    private EmailService emailService;
    @MockBean
    private OrderCreatedNotificationService orderCreatedNotificationService;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getAllOrders_returnsPaginated() throws Exception {
        Order order = createOrder(1L, "ORD-001", null);
        when(orderRepository.findAllByOrderByCreatedAtDesc(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1L));
        when(orderTaskRepository.countByOrderId(1L)).thenReturn(0L);
        when(orderTaskRepository.countByOrderIdAndStatus(eq(1L), eq(ServiceTaskStatus.DOKONCENO))).thenReturn(0L);

        mockMvc.perform(get("/api/technician/orders").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.content[0].taskCount").value(0))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getAllOrders_withSearch_callsSearchRepository() throws Exception {
        Order order = createOrder(1L, "ORD-002", "Novák");
        when(orderRepository.searchByOrderNumberOrCustomerNameOrderByCreatedAtDesc(eq("Novák"), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(order), PageRequest.of(0, 20), 1L));
        when(orderTaskRepository.countByOrderId(1L)).thenReturn(2L);

        mockMvc.perform(get("/api/technician/orders").param("search", "Novák").param("page", "0").param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].orderNumber").value("ORD-002"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrder_byId_exists_returns200() throws Exception {
        Order order = createOrder(1L, "ORD-001", null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/technician/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-001"))
                .andExpect(jsonPath("$.tasks", hasSize(0)));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getOrder_byId_notFound_returns404() throws Exception {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/technician/orders/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrderByNumber_exists_returns200() throws Exception {
        Order order = createOrder(1L, "ORD-123", null);
        when(orderRepository.findByOrderNumber("ORD-123")).thenReturn(Optional.of(order));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/technician/orders/by-number/ORD-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-123"));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void getOrderByNumber_notFound_returns404() throws Exception {
        when(orderRepository.findByOrderNumber("NEZNAMA")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/technician/orders/by-number/NEZNAMA"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrder_emptySkiIds_returns400() throws Exception {
        mockMvc.perform(post("/api/technician/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skiIds\":[],\"customerId\":null,\"dueDate\":null,\"priority\":null,\"status\":null,\"notes\":null,\"price\":null,\"pohodaId\":null,\"targetStruktura\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrder_validRequest_returns200() throws Exception {
        Order savedOrder = createOrder(1L, null, null);
        Ski ski = new Ski();
        ski.setId(10L);
        ski.setSkiNumber("SKI-1");
        ski.setBrand("B");
        ski.setModel("M");
        ski.setLength("170");
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(savedOrder));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(1L)).thenReturn(List.of());
        when(skiRepository.findById(10L)).thenReturn(Optional.of(ski));

        mockMvc.perform(post("/api/technician/orders")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skiIds\":[10],\"customerId\":null,\"dueDate\":\"2025-12-31\",\"priority\":\"STREDNI\",\"status\":\"NOVE\",\"notes\":\"\",\"price\":null,\"pohodaId\":null,\"targetStruktura\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tasks", hasSize(0)));
        verify(orderRepository).save(any(Order.class));
        verify(orderTaskRepository).save(any(OrderTask.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrder_exists_returns200() throws Exception {
        Order order = createOrder(1L, "ORD-001", null);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(1L)).thenReturn(List.of());

        mockMvc.perform(patch("/api/technician/orders/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"Updated\",\"priority\":\"VYSOKA\",\"status\":\"VE_ZPRACOVANI\",\"price\":100,\"pohodaId\":null,\"customerId\":null,\"dueDate\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Updated"));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @WithMockUser(roles = "TECHNICIAN")
    void updateOrder_notFound_returns404() throws Exception {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(patch("/api/technician/orders/999")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"x\",\"priority\":null,\"status\":null,\"price\":null,\"pohodaId\":null,\"customerId\":null,\"dueDate\":null}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void addTaskToOrder_missingSkiId_returns400() throws Exception {
        mockMvc.perform(post("/api/technician/orders/1/tasks")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"skiId\":null,\"targetStruktura\":null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllOrders_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/technician/orders"))
                .andExpect(status().isUnauthorized());
    }

    private static Order createOrder(Long id, String orderNumber, String customerName) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber != null ? orderNumber : "ORD-" + id);
        order.setCreatedAt(LocalDateTime.now());
        order.setDueDate(LocalDate.now().plusDays(7));
        order.setPriority(ServiceTaskPriority.STREDNI);
        order.setStatus(OrderStatus.NOVE);
        order.setNotes(null);
        order.setPrice(BigDecimal.ZERO);
        if (customerName != null) {
            Customer c = new Customer();
            c.setId(1L);
            c.setName(customerName);
            c.setCustomerNumber("C-1");
            order.setCustomer(c);
        }
        return order;
    }
}
