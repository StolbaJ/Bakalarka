package com.ski.inventory.controller;

import com.ski.inventory.model.*;
import com.ski.inventory.repository.CommonModificationOptionRepository;
import com.ski.inventory.repository.CustomerRepository;
import com.ski.inventory.repository.OrderRepository;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.ServiceTaskItemRepository;
import com.ski.inventory.repository.SkiRepository;
import com.ski.inventory.service.EmailService;
import com.ski.inventory.service.OrderCreatedNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/technician/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderRepository orderRepository;
    private final OrderTaskRepository orderTaskRepository;
    private final ServiceTaskItemRepository serviceTaskItemRepository;
    private final CommonModificationOptionRepository commonModificationOptionRepository;
    private final SkiRepository skiRepository;
    private final CustomerRepository customerRepository;
    private final EmailService emailService;
    private final OrderCreatedNotificationService orderCreatedNotificationService;

    public OrderController(OrderRepository orderRepository, OrderTaskRepository orderTaskRepository, ServiceTaskItemRepository serviceTaskItemRepository, CommonModificationOptionRepository commonModificationOptionRepository, SkiRepository skiRepository, CustomerRepository customerRepository, EmailService emailService, OrderCreatedNotificationService orderCreatedNotificationService) {
        this.orderRepository = orderRepository;
        this.orderTaskRepository = orderTaskRepository;
        this.serviceTaskItemRepository = serviceTaskItemRepository;
        this.commonModificationOptionRepository = commonModificationOptionRepository;
        this.skiRepository = skiRepository;
        this.customerRepository = customerRepository;
        this.emailService = emailService;
        this.orderCreatedNotificationService = orderCreatedNotificationService;
    }

    @PostMapping
    public ResponseEntity<OrderDetailResponse> createOrder(@RequestBody CreateOrderRequest request) {
        if (request.skiIds() == null || request.skiIds().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        Order order = new Order();
        if (request.customerId() != null) {
            customerRepository.findById(request.customerId()).ifPresent(order::setCustomer);
        }
        order.setDueDate(request.dueDate() != null ? LocalDate.parse(request.dueDate()) : null);
        order.setPriority(request.priority() != null ? ServiceTaskPriority.valueOf(request.priority()) : ServiceTaskPriority.STREDNI);
        order.setStatus(request.status() != null ? OrderStatus.valueOf(request.status()) : OrderStatus.NOVE);
        order.setNotes(request.notes());
        order.setPrice(request.price() != null ? request.price() : null);
        order.setPohodaId(request.pohodaId());
        Order savedOrder = orderRepository.save(order);
        List<String> targetList = request.targetStruktura() != null ? request.targetStruktura() : List.of();
        for (int i = 0; i < request.skiIds().size(); i++) {
            final int idx = i;
            final Long skiId = request.skiIds().get(i);
            skiRepository.findById(skiId).ifPresent(ski -> {
                OrderTask task = new OrderTask();
                task.setOrder(savedOrder);
                task.setSki(ski);
                task.setStatus(ServiceTaskStatus.CEKA);
                task.setPriority(savedOrder.getPriority() != null ? savedOrder.getPriority() : ServiceTaskPriority.STREDNI);
                String target = (idx < targetList.size() && targetList.get(idx) != null) ? targetList.get(idx) : null;
                task.setTargetStruktura(target != null ? target : (ski.getStruktura() != null ? ski.getStruktura() : ""));
                orderTaskRepository.save(task);
            });
        }
        orderRepository.flush();
        Order orderWithNumber = orderRepository.findById(savedOrder.getId()).orElse(savedOrder);
        List<OrderTask> tasksWithItems = orderTaskRepository.findByOrderIdWithSkiAndItems(orderWithNumber.getId());
        orderCreatedNotificationService.sendAndMarkSent(orderWithNumber);
        return ResponseEntity.ok(toDetailResponse(orderWithNumber, tasksWithItems));
    }

    @GetMapping
    public ResponseEntity<List<OrderSummaryResponse>> getAllOrders(
            @RequestParam(required = false) String search) {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        List<OrderSummaryResponse> result = orders.stream()
                .map(o -> toSummaryResponse(o, (int) orderTaskRepository.countByOrderId(o.getId())))
                .filter(o -> search == null || search.isBlank() || matchesSearch(o, search.trim()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDetailResponse> getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .map(order -> toDetailResponse(order, orderTaskRepository.findByOrderIdWithSkiAndItems(id)))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{orderNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderByNumber(@PathVariable String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber)
                .map(order -> toDetailResponse(order, orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId())))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-ski/{skiNumber}")
    public ResponseEntity<OrderDetailResponse> getOrderBySkiNumber(@PathVariable String skiNumber) {
        var tasks = skiRepository.findBySkiNumber(skiNumber)
                .map(ski -> orderTaskRepository.findBySkiId(ski.getId()))
                .orElse(List.of());
        if (tasks.isEmpty()) return ResponseEntity.notFound().build();
        Order order = tasks.get(0).getOrder();
        return ResponseEntity.ok(toDetailResponse(order, orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId())));
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<OrderDetailResponse> updateOrder(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderRequest request) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    if (request.notes() != null) order.setNotes(request.notes());
                    if (request.priority() != null) order.setPriority(ServiceTaskPriority.valueOf(request.priority()));
                    if (request.status() != null) order.setStatus(OrderStatus.valueOf(request.status()));
                    if (request.price() != null) order.setPrice(request.price());
                    if (request.pohodaId() != null) order.setPohodaId(request.pohodaId());
                    if (request.customerId() != null) {
                        order.setCustomer(request.customerId() == 0 ? null : customerRepository.findById(request.customerId()).orElse(null));
                    }
                    if (request.dueDate() != null) {
                        order.setDueDate(request.dueDate().isBlank() ? null : LocalDate.parse(request.dueDate()));
                    }
                    orderRepository.save(order);
                    List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
                    return ResponseEntity.ok(toDetailResponse(order, tasks));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{orderId}/tasks")
    public ResponseEntity<OrderDetailResponse> addTaskToOrder(
            @PathVariable Long orderId,
            @RequestBody AddTaskToOrderRequest request) {
        if (request.skiId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return orderRepository.findById(orderId)
                .flatMap(order -> skiRepository.findById(request.skiId())
                        .map(ski -> {
                            OrderTask task = new OrderTask();
                            task.setOrder(order);
                            task.setSki(ski);
                            task.setStatus(ServiceTaskStatus.CEKA);
                            task.setPriority(order.getPriority() != null ? order.getPriority() : ServiceTaskPriority.STREDNI);
                            String target = request.targetStruktura() != null && !request.targetStruktura().isBlank()
                                    ? request.targetStruktura().trim()
                                    : (ski.getStruktura() != null ? ski.getStruktura() : "");
                            task.setTargetStruktura(target);
                            orderTaskRepository.save(task);
                            List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
                            return ResponseEntity.<OrderDetailResponse>ok(toDetailResponse(order, tasks));
                        }))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{orderId}/tasks/{taskId}")
    public ResponseEntity<OrderDetailResponse> updateTask(
            @PathVariable Long orderId,
            @PathVariable Long taskId,
            @RequestBody UpdateTaskRequest request) {
        return orderTaskRepository.findById(taskId)
                .filter(t -> t.getOrder().getId().equals(orderId))
                .map(task -> {
                    if (request.status() != null) {
                        task.setStatus(ServiceTaskStatus.valueOf(request.status()));
                        if (ServiceTaskStatus.DOKONCENO.equals(task.getStatus())) {
                            task.setCompletedAt(java.time.LocalDateTime.now());
                        }
                    }
                    if (request.targetStruktura() != null) task.setTargetStruktura(request.targetStruktura());
                    orderTaskRepository.save(task);
                    if (ServiceTaskStatus.DOKONCENO.equals(task.getStatus())) {
                        applyStrukturaToSkisIfOrderComplete(task.getOrder());
                    }
                    Order order = task.getOrder();
                    List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
                    return ResponseEntity.ok(toDetailResponse(order, tasks));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /** Po dokončení celé objednávky zapíše cílovou strukturu do lyží a pošle zákazníkovi e-mail. */
    private void applyStrukturaToSkisIfOrderComplete(Order order) {
        List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
        boolean allDone = !tasks.isEmpty() && tasks.stream().allMatch(t -> ServiceTaskStatus.DOKONCENO.equals(t.getStatus()));
        if (!allDone) return;
        for (OrderTask task : tasks) {
            if (task.getSki() == null || task.getTargetStruktura() == null) continue;
            String current = task.getSki().getStruktura();
            if (task.getTargetStruktura().equals(current)) continue;
            task.getSki().setStruktura(task.getTargetStruktura());
            task.getSki().setStrukturaRecordedAt(java.time.LocalDateTime.now());
            skiRepository.save(task.getSki());
        }
        // Notifikace zákazníka, že objednávka je připravena k vyzvednutí
        if (order.getCustomer() != null) {
            String email = order.getCustomer().getEmail();
            if (email != null && !email.isBlank()) {
                emailService.sendOrderReadyNotification(email, order.getOrderNumber(), order.getCustomer().getName());
            }
        }
    }

    @PatchMapping("/{orderId}/tasks/{taskId}/items/{itemId}")
    public ResponseEntity<OrderDetailResponse> updateTaskItem(
            @PathVariable Long orderId,
            @PathVariable Long taskId,
            @PathVariable Long itemId,
            @RequestBody UpdateTaskItemRequest request) {
        return serviceTaskItemRepository.findById(itemId)
                .filter(item -> item.getTask().getId().equals(taskId) && item.getTask().getOrder().getId().equals(orderId))
                .map(item -> {
                    if (request.completed() != null && Boolean.TRUE.equals(request.completed())) {
                        var opt = commonModificationOptionRepository.findFirstByName(item.getTaskName());
                        boolean requiresDesc = opt.map(com.ski.inventory.model.CommonModificationOption::isRequiresWorkDescription)
                                .orElse(Boolean.TRUE.equals(item.getRequiresWorkDescription()));
                        if (requiresDesc) {
                            String desc = request.taskDescription() != null ? request.taskDescription() : item.getTaskDescription();
                            if (desc == null || desc.isBlank()) {
                                return ResponseEntity.badRequest().<OrderDetailResponse>build();
                            }
                        }
                    }
                    if (request.completed() != null) {
                        item.setCompleted(request.completed());
                        if (Boolean.TRUE.equals(request.completed())) {
                            item.setCompletedAt(java.time.LocalDateTime.now());
                        } else {
                            item.setCompletedAt(null);
                        }
                    }
                    if (request.taskDescription() != null) {
                        item.setTaskDescription(request.taskDescription());
                    }
                    if (request.taskInstruction() != null) {
                        item.setTaskInstruction(request.taskInstruction());
                    }
                    serviceTaskItemRepository.save(item);
                    OrderTask task = item.getTask();
                    boolean allDone = task.getTaskItems().stream().allMatch(i -> Boolean.TRUE.equals(i.getCompleted()));
                    if (allDone && !task.getTaskItems().isEmpty()) {
                        task.setStatus(ServiceTaskStatus.DOKONCENO);
                        task.setCompletedAt(java.time.LocalDateTime.now());
                        orderTaskRepository.save(task);
                        applyStrukturaToSkisIfOrderComplete(task.getOrder());
                    } else {
                        // Odznačení úkolu – vrátit task z DOKONCENO zpět na PROBIHA
                        if (ServiceTaskStatus.DOKONCENO.equals(task.getStatus())) {
                            task.setStatus(ServiceTaskStatus.PROBIHA);
                            task.setCompletedAt(null);
                            orderTaskRepository.save(task);
                        }
                    }
                    Order order = task.getOrder();
                    List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
                    return ResponseEntity.ok(toDetailResponse(order, tasks));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{orderId}/tasks/{taskId}/items")
    public ResponseEntity<OrderDetailResponse> addTaskItem(
            @PathVariable Long orderId,
            @PathVariable Long taskId,
            @RequestBody AddTaskItemRequest request) {
        return orderTaskRepository.findById(taskId)
                .filter(t -> t.getOrder().getId().equals(orderId))
                .map(task -> {
                    ServiceTaskItem item = new ServiceTaskItem();
                    item.setTask(task);
                    item.setTaskName(request.taskName() != null ? request.taskName() : "Nový úkon");
                    String instruction = request.taskInstruction() != null && !request.taskInstruction().isBlank()
                            ? request.taskInstruction().trim() : null;
                    if (request.modificationOptionId() != null) {
                        commonModificationOptionRepository.findById(request.modificationOptionId()).ifPresent(opt -> {
                            item.setRequiresWorkDescription(opt.isRequiresWorkDescription());
                            if (instruction == null && opt.getDescription() != null && !opt.getDescription().isBlank()) {
                                item.setTaskInstruction(opt.getDescription().trim());
                            }
                        });
                    }
                    if (instruction != null) item.setTaskInstruction(instruction);
                    item.setTaskDescription(request.taskDescription());
                    item.setCompleted(false);
                    serviceTaskItemRepository.save(item);
                    Order order = task.getOrder();
                    List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
                    return ResponseEntity.ok(toDetailResponse(order, tasks));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{orderId}/tasks/{taskId}/items/{itemId}")
    public ResponseEntity<OrderDetailResponse> deleteTaskItem(
            @PathVariable Long orderId,
            @PathVariable Long taskId,
            @PathVariable Long itemId) {
        return serviceTaskItemRepository.findById(itemId)
                .filter(item -> item.getTask().getId().equals(taskId) && item.getTask().getOrder().getId().equals(orderId))
                .map(item -> {
                    serviceTaskItemRepository.delete(item);
                    Order order = item.getTask().getOrder();
                    List<OrderTask> tasks = orderTaskRepository.findByOrderIdWithSkiAndItems(order.getId());
                    return ResponseEntity.ok(toDetailResponse(order, tasks));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    public record CreateOrderRequest(Long customerId, String dueDate, String priority, String status, String notes, BigDecimal price, Long pohodaId, List<Long> skiIds, List<String> targetStruktura) {}
    public record UpdateOrderRequest(String notes, String priority, String status, BigDecimal price, Long pohodaId, Long customerId, String dueDate) {}
    public record AddTaskToOrderRequest(Long skiId, String targetStruktura) {}
    public record AddTaskItemRequest(String taskName, String taskDescription, String taskInstruction, Long modificationOptionId) {}
    public record UpdateTaskRequest(String status, String targetStruktura) {}
    public record UpdateTaskItemRequest(Boolean completed, String taskDescription, String taskInstruction) {}

    private boolean matchesSearch(OrderSummaryResponse o, String search) {
        String s = search.toLowerCase();
        return (o.orderNumber() != null && o.orderNumber().toLowerCase().contains(s))
                || (o.customerName() != null && o.customerName().toLowerCase().contains(s));
    }

    private OrderSummaryResponse toSummaryResponse(Order order, int taskCount) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        long doneCount = orderTaskRepository.countByOrderIdAndStatus(order.getId(), ServiceTaskStatus.DOKONCENO);
        boolean orderDone = taskCount > 0 && doneCount == taskCount;
        String priority = order.getPriority() != null ? order.getPriority().name() : null;
        String status = order.getStatus() != null ? order.getStatus().name() : null;
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                customerName,
                order.getCreatedAt() != null ? order.getCreatedAt().toString() : null,
                order.getDueDate() != null ? order.getDueDate().toString() : null,
                taskCount,
                orderDone,
                priority,
                status,
                order.getPrice(),
                order.getPohodaId()
        );
    }

    private OrderDetailResponse toDetailResponse(Order order, List<OrderTask> taskList) {
        String customerName = order.getCustomer() != null ? order.getCustomer().getName() : null;
        Long customerId = order.getCustomer() != null ? order.getCustomer().getId() : null;
        List<OrderTaskResponse> tasks = (taskList != null ? taskList : List.<OrderTask>of())
                .stream()
                .map(this::toTaskResponse)
                .toList();
        boolean orderDone = !tasks.isEmpty() && tasks.stream().allMatch(t -> ServiceTaskStatus.DOKONCENO.name().equals(t.status()));
        String priority = order.getPriority() != null ? order.getPriority().name() : null;
        String status = order.getStatus() != null ? order.getStatus().name() : null;
        return new OrderDetailResponse(
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

    private OrderTaskResponse toTaskResponse(OrderTask task) {
        String skiInfo = task.getSki() != null
                ? task.getSki().getBrand() + " " + task.getSki().getModel() + " " + task.getSki().getLength()
                : null;
        String skiNumber = task.getSki() != null ? task.getSki().getSkiNumber() : null;
        Long skiId = task.getSki() != null ? task.getSki().getId() : null;
        String skiStruktura = task.getSki() != null ? task.getSki().getStruktura() : null;
        String targetStruktura = task.getTargetStruktura();
        List<ServiceTaskItemResponse> items = (task.getTaskItems() != null ? task.getTaskItems() : List.<ServiceTaskItem>of())
                .stream()
                .map(this::toItemResponse)
                .toList();
        return new OrderTaskResponse(
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

    private ServiceTaskItemResponse toItemResponse(ServiceTaskItem item) {
        return new ServiceTaskItemResponse(
                item.getId(),
                item.getTaskName(),
                item.getTaskInstruction(),
                item.getTaskDescription(),
                Boolean.TRUE.equals(item.getCompleted()),
                item.getCompletedAt() != null ? item.getCompletedAt().toString() : null,
                Boolean.TRUE.equals(item.getRequiresWorkDescription())
        );
    }

    public record OrderSummaryResponse(
            Long id,
            String orderNumber,
            String customerName,
            String createdAt,
            String dueDate,
            int taskCount,
            boolean orderDone,
            String priority,
            String status,
            java.math.BigDecimal price,
            Long pohodaId
    ) {}

    public record OrderDetailResponse(
            Long id,
            String orderNumber,
            Long customerId,
            String customerName,
            String createdAt,
            String dueDate,
            String notes,
            boolean orderDone,
            String priority,
            String status,
            java.math.BigDecimal price,
            Long pohodaId,
            List<OrderTaskResponse> tasks
    ) {}

    public record OrderTaskResponse(
            Long id,
            Long skiId,
            String skiNumber,
            String skiInfo,
            String status,
            String priority,
            String assignedTo,
            String skiStruktura,
            String targetStruktura,
            List<ServiceTaskItemResponse> taskItems
    ) {}

    public record ServiceTaskItemResponse(
            Long id,
            String taskName,
            String taskInstruction,
            String taskDescription,
            boolean completed,
            String completedAt,
            boolean requiresWorkDescription
    ) {}
}
