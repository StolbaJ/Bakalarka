package com.ski.inventory.controller;

import com.ski.inventory.model.Customer;
import com.ski.inventory.model.Order;
import com.ski.inventory.model.OrderTask;
import com.ski.inventory.model.ServiceTaskItem;
import com.ski.inventory.model.ServiceTaskStatus;
import com.ski.inventory.repository.OrderRepository;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.service.AuthenticationService;
import com.ski.inventory.service.OrderViewTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Veřejné endpointy pro zobrazení objednávky pomocí tokenu z e-mailu (bez přihlášení).
 */
@RestController
@RequestMapping("/api/public/orders")
@CrossOrigin(origins = "*")
public class PublicOrderController {

    private final OrderRepository orderRepository;
    private final OrderTaskRepository orderTaskRepository;
    private final OrderViewTokenService orderViewTokenService;
    private final AuthenticationService authenticationService;

    public PublicOrderController(OrderRepository orderRepository, OrderTaskRepository orderTaskRepository, OrderViewTokenService orderViewTokenService, AuthenticationService authenticationService) {
        this.orderRepository = orderRepository;
        this.orderTaskRepository = orderTaskRepository;
        this.orderViewTokenService = orderViewTokenService;
        this.authenticationService = authenticationService;
    }

    /**
     * Zobrazí objednávku podle tokenu z e-mailu. Vrátí detail objednávky a volitelný JWT pro přihlášení zákazníka,
     * aby mohl prohlížet všechny své objednávky.
     */
    @GetMapping("/view")
    public ResponseEntity<?> viewOrderByToken(@RequestParam String token) {
        var claims = orderViewTokenService.parseToken(token);
        if (claims.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("message", "Neplatný nebo expirovaný odkaz."));
        }
        OrderViewTokenService.OrderViewClaims c = claims.get();
        Order order = orderRepository.findById(c.orderId()).orElse(null);
        if (order == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Objednávka nebyla nalezena."));
        }
        Customer customer = order.getCustomer();
        if (customer == null) {
            return ResponseEntity.status(403).body(Map.of("message", "K objednávce není přiřazen zákazník."));
        }
        String storedPhoneNorm = OrderViewTokenService.normalizePhone(customer.getPhone());
        if (!storedPhoneNorm.equals(c.phoneNormalized())) {
            return ResponseEntity.status(403).body(Map.of("message", "Odkaz neodpovídá této objednávce."));
        }
        List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
        OrderController.OrderDetailResponse orderDetail = toDetailResponse(order, tasks);
        var authResponse = authenticationService.authenticateCustomer(order.getOrderNumber(), customer.getPhone());
        Map<String, Object> body = new HashMap<>();
        body.put("order", orderDetail);
        body.put("authToken", authResponse.token());
        body.put("authResponse", authResponse);
        return ResponseEntity.ok(body);
    }

    private OrderController.OrderDetailResponse toDetailResponse(Order order, List<OrderTask> taskList) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        List<OrderController.OrderTaskResponse> tasks = (taskList != null ? taskList : List.<OrderTask>of())
                .stream()
                .map(this::toTaskResponse)
                .toList();
        boolean orderDone = !tasks.isEmpty() && tasks.stream().allMatch(t -> ServiceTaskStatus.DOKONCENO.name().equals(t.status()));
        String priority = order.getPriority() != null ? order.getPriority().name() : null;
        String status = order.getStatus() != null ? order.getStatus().name() : null;
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
                order.getDiscount(),
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
        List<OrderController.ServiceTaskItemResponse> items = (task.getTaskItems() != null ? task.getTaskItems() : List.<ServiceTaskItem>of())
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
                Boolean.TRUE.equals(item.getRequiresWorkDescription()),
                item.getPrice()
        );
    }
}
