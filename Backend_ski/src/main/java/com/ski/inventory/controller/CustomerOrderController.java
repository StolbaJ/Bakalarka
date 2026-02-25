package com.ski.inventory.controller;

import com.ski.inventory.model.Order;
import com.ski.inventory.model.OrderTask;
import com.ski.inventory.model.ServiceTaskItem;
import com.ski.inventory.model.ServiceTaskStatus;
import com.ski.inventory.repository.OrderRepository;
import com.ski.inventory.repository.OrderTaskRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/api/customer")
@PreAuthorize("hasRole('CUSTOMER')")
@CrossOrigin(origins = "*")
public class CustomerOrderController {

    private final OrderRepository orderRepository;
    private final OrderTaskRepository orderTaskRepository;

    public CustomerOrderController(OrderRepository orderRepository, OrderTaskRepository orderTaskRepository) {
        this.orderRepository = orderRepository;
        this.orderTaskRepository = orderTaskRepository;
    }

    /**
     * Vrátí všechny objednávky přihlášeného zákazníka (v JWT je customer id jako subject).
     */
    @GetMapping("/orders")
    public ResponseEntity<List<OrderController.OrderDetailResponse>> getMyOrders() {
        Long customerId = getCustomerIdFromAuth();
        if (customerId == null) {
            return ResponseEntity.badRequest().build();
        }
        List<Order> orders = orderRepository.findByCustomerId(customerId);
        List<OrderController.OrderDetailResponse> list = orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(order -> toDetailResponse(order, orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId())))
                .toList();
        return ResponseEntity.ok(list);
    }

    private Long getCustomerIdFromAuth() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        try {
            return Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private OrderController.OrderDetailResponse toDetailResponse(Order order, java.util.List<OrderTask> taskList) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        java.util.List<OrderController.OrderTaskResponse> tasks = (taskList != null ? taskList : java.util.List.<OrderTask>of())
                .stream()
                .map(this::toTaskResponse)
                .toList();
        boolean orderDone = !tasks.isEmpty() && tasks.stream().allMatch(t -> ServiceTaskStatus.DOKONCENO.name().equals(t.status()));
        String priority = order.getPriority() != null ? order.getPriority().name() : null;
        String status = order.getStatus() != null ? order.getStatus().name() : null;
        Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        return new OrderController.OrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                customerId,
                customerName,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getDueDate() != null ? order.getDueDate().toString() : null,
                order.getNotes(),
                orderDone,
                priority,
                status,
                order.getPrice(),
                order.getPohodaId(),
                tasks
        );
    }

    private OrderController.OrderTaskResponse toTaskResponse(OrderTask task) {
        String skiInfo = task.getSki() != null
                ? task.getSki().getBrand() + " " + task.getSki().getModel() + " " + task.getSki().getLength()
                : null;
        String skiNumber = task.getSki() != null ? task.getSki().getSkiNumber() : null;
        Long skiId = task.getSki() != null ? task.getSki().getId() : null;
        String skiStruktura = task.getSki() != null ? task.getSki().getStruktura() : null;
        String targetStruktura = task.getTargetStruktura();
        java.util.List<OrderController.ServiceTaskItemResponse> items = (task.getTaskItems() != null ? task.getTaskItems() : java.util.List.<ServiceTaskItem>of())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderController.OrderTaskResponse(
                task.getId(),
                skiId,
                skiNumber,
                skiInfo,
                task.getStatus().name(),
                task.getPriority().name(),
                task.getAssignedTo() != null ? task.getAssignedTo().getFullName() : null,
                skiStruktura,
                targetStruktura,
                items
        );
    }

    private OrderController.ServiceTaskItemResponse toItemResponse(ServiceTaskItem item) {
        return new OrderController.ServiceTaskItemResponse(
                item.getId(),
                item.getTaskName(),
                item.getTaskInstruction(),
                item.getTaskDescription(),
                Boolean.TRUE.equals(item.getCompleted()),
                item.getCompletedAt() != null ? item.getCompletedAt().toString() : null,
                Boolean.TRUE.equals(item.getRequiresWorkDescription())
        );
    }
}
