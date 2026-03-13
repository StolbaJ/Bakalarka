package com.ski.inventory.controller;

import com.ski.inventory.model.*;
import com.ski.inventory.monitoring.ServerErrorRecorder;
import com.ski.inventory.repository.OrderRepository;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.service.JwtService;
import com.ski.inventory.service.PasswordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerOrderController.class)
class CustomerOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderRepository orderRepository;
    @MockBean
    private OrderTaskRepository orderTaskRepository;
    @MockBean
    private ServerErrorRecorder serverErrorRecorder;
    @MockBean
    private JwtService jwtService;
    @MockBean
    private PasswordService passwordService;

    @Test
    @WithMockUser(username = "10", roles = "CUSTOMER")
    void getMyOrders_returnsCustomerOrders() throws Exception {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Jan Novák");

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber("ORD-0001");
        order.setCustomer(customer);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.NOVE);
        order.setPriority(ServiceTaskPriority.STREDNI);

        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of(order));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-0001"))
                .andExpect(jsonPath("$[0].tasks").isArray());
    }

    @Test
    @WithMockUser(username = "10", roles = "CUSTOMER")
    void getMyOrders_emptyList_returnsEmptyArray() throws Exception {
        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of());

        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getMyOrders_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "10", roles = "CUSTOMER")
    void getMyOrders_multipleOrders_returnsSortedByDate() throws Exception {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Test");

        Order o1 = new Order();
        o1.setId(1L);
        o1.setOrderNumber("ORD-0001");
        o1.setCustomer(customer);
        o1.setCreatedAt(java.time.LocalDateTime.now().minusDays(2));
        o1.setStatus(OrderStatus.NOVE);
        o1.setPriority(ServiceTaskPriority.STREDNI);

        Order o2 = new Order();
        o2.setId(2L);
        o2.setOrderNumber("ORD-0002");
        o2.setCustomer(customer);
        o2.setCreatedAt(java.time.LocalDateTime.now());
        o2.setStatus(OrderStatus.VE_ZPRACOVANI);
        o2.setPriority(ServiceTaskPriority.VYSOKA);

        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of(o1, o2));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // Most recent first
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-0002"));
    }

    @Test
    @WithMockUser(username = "not-a-number", roles = "CUSTOMER")
    void getMyOrders_invalidCustomerIdInToken_returns400() throws Exception {
        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "10", roles = "CUSTOMER")
    void getMyOrders_orderWithTasks_returnsTasksInOrder() throws Exception {
        Customer customer = new Customer();
        customer.setId(10L);
        customer.setName("Test User");

        Order order = new Order();
        order.setId(2L);
        order.setOrderNumber("ORD-0002");
        order.setCustomer(customer);
        order.setCreatedAt(LocalDateTime.now());
        order.setStatus(OrderStatus.NOVE);
        order.setPriority(ServiceTaskPriority.STREDNI);

        Ski ski = new Ski();
        ski.setId(1L);
        ski.setSkiNumber("SKI-001");
        ski.setBrand("Fischer");
        ski.setModel("RC4");
        ski.setLength("170");

        OrderTask task = new OrderTask();
        task.setId(1L);
        task.setSki(ski);
        task.setStatus(ServiceTaskStatus.CEKA);
        task.setPriority(ServiceTaskPriority.STREDNI);
        task.setTaskItems(List.of());

        when(orderRepository.findByCustomerId(10L)).thenReturn(List.of(order));
        when(orderTaskRepository.findByOrderIdWithSkiAndItems(2L)).thenReturn(List.of(task));

        mockMvc.perform(get("/api/customer/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tasks", hasSize(1)))
                .andExpect(jsonPath("$[0].tasks[0].skiNumber").value("SKI-001"))
                .andExpect(jsonPath("$[0].tasks[0].status").value("CEKA"));
    }
}
