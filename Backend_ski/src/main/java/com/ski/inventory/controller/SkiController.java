package com.ski.inventory.controller;

import com.ski.inventory.model.OrderTask;
import com.ski.inventory.model.ServiceTaskItem;
import com.ski.inventory.model.Ski;
import com.ski.inventory.model.SkiCondition;
import com.ski.inventory.model.SkiStatus;
import com.ski.inventory.model.SkiUsage;
import com.ski.inventory.repository.OrderTaskRepository;
import com.ski.inventory.repository.SkiRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technician/skis")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
@CrossOrigin(origins = "*")
public class SkiController {

    private final SkiRepository skiRepository;
    private final OrderTaskRepository orderTaskRepository;

    public SkiController(SkiRepository skiRepository, OrderTaskRepository orderTaskRepository) {
        this.skiRepository = skiRepository;
        this.orderTaskRepository = orderTaskRepository;
    }

    @GetMapping
    public ResponseEntity<PageResponse<SkiResponse>> getAllSkis(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (size < 1) size = 1;
        if (size > 100) size = 100;
        Pageable pageable = PageRequest.of(page, size);
        Page<Ski> skiPage = skiRepository.findAll(pageable);
        List<SkiResponse> content = skiPage.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(new PageResponse<>(
                content,
                skiPage.getTotalElements(),
                skiPage.getTotalPages(),
                skiPage.getSize(),
                skiPage.getNumber(),
                skiPage.isFirst(),
                skiPage.isLast()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkiResponse> getSki(@PathVariable Long id) {
        return skiRepository.findById(id)
                .map(ski -> ResponseEntity.ok(toResponse(ski)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{skiNumber}")
    public ResponseEntity<SkiResponse> getSkiByNumber(@PathVariable String skiNumber) {
        return skiRepository.findBySkiNumber(skiNumber)
                .map(ski -> ResponseEntity.ok(toResponse(ski)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/service-history")
    public ResponseEntity<List<ServiceHistoryEntry>> getSkiServiceHistory(@PathVariable Long id) {
        if (!skiRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        List<OrderTask> tasks = orderTaskRepository.findBySkiIdWithTaskItems(id);
        List<ServiceHistoryEntry> entries = tasks.stream()
                .flatMap(task -> task.getTaskItems().stream()
                        .map(item -> toHistoryEntry(task, item)))
                .sorted((a, b) -> {
                    java.time.LocalDateTime dateA = a.completedAt() != null
                            ? a.completedAt() : (a.orderDate() != null ? a.orderDate().atStartOfDay() : null);
                    java.time.LocalDateTime dateB = b.completedAt() != null
                            ? b.completedAt() : (b.orderDate() != null ? b.orderDate().atStartOfDay() : null);
                    if (dateA == null && dateB == null) return 0;
                    if (dateA == null) return 1;
                    if (dateB == null) return -1;
                    return dateB.compareTo(dateA);
                })
                .toList();
        return ResponseEntity.ok(entries);
    }

    private ServiceHistoryEntry toHistoryEntry(OrderTask task, ServiceTaskItem item) {
        return new ServiceHistoryEntry(
                item.getId(),
                task.getOrder().getOrderNumber(),
                item.getTaskName(),
                item.getTaskInstruction(),
                item.getTaskDescription(),
                item.getCompleted(),
                task.getCreatedAt() != null ? task.getCreatedAt().toLocalDate() : null,
                item.getCompletedAt() != null ? item.getCompletedAt().toLocalDate() : null,
                item.getCompletedAt()
        );
    }

    public record ServiceHistoryEntry(
            Long taskItemId,
            String orderNumber,
            String taskName,
            String taskInstruction,
            String taskDescription,
            Boolean completed,
            java.time.LocalDate orderDate,
            java.time.LocalDate completedDate,
            java.time.LocalDateTime completedAt
    ) {}

    @PostMapping
    public ResponseEntity<SkiResponse> createSki(@Valid @RequestBody CreateSkiRequest request) {
        Ski ski = new Ski();
        ski.setBrand(request.brand());
        ski.setModel(request.model());
        ski.setLength(request.length());
        ski.setYear(request.year());
        ski.setSkiType(request.skiType());
        ski.setWeightKg(request.weightKg());
        ski.setCondition(request.condition() != null ? SkiCondition.valueOf(request.condition()) : SkiCondition.DOBRY);
        ski.setStatus(request.status() != null ? SkiStatus.valueOf(request.status()) : SkiStatus.DOSTUPNY);
        ski.setLocation(request.location());
        ski.setNotes(request.notes());
        ski.setLastServiceDate(request.lastServiceDate());
        ski.setNextServiceDate(request.nextServiceDate());
        ski.setStruktura(request.struktura());
        if (request.struktura() != null && !request.struktura().isBlank()) {
            ski.setStrukturaRecordedAt(java.time.LocalDateTime.now());
        }
        ski.setStructureChangeCount(request.structureChangeCount() != null ? request.structureChangeCount() : 0);
        ski.setEan(request.ean());
        ski.setPartNo(request.partNo());
        ski.setSerialNo(request.serialNo());
        if (request.skiUsage() != null && !request.skiUsage().isBlank()) {
            try { ski.setSkiUsage(SkiUsage.valueOf(request.skiUsage())); } catch (IllegalArgumentException ignored) {}
        }

        Ski saved = skiRepository.save(ski);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkiResponse> updateSki(@PathVariable Long id, @Valid @RequestBody UpdateSkiRequest request) {
        return skiRepository.findById(id)
                .map(ski -> {
                    ski.setBrand(request.brand());
                    ski.setModel(request.model());
                    ski.setLength(request.length());
                    ski.setYear(request.year());
                    ski.setSkiType(request.skiType());
                    ski.setWeightKg(request.weightKg());
                    if (request.condition() != null) ski.setCondition(SkiCondition.valueOf(request.condition()));
                    if (request.status() != null) ski.setStatus(SkiStatus.valueOf(request.status()));
                    ski.setLocation(request.location());
                    ski.setNotes(request.notes());
                    ski.setLastServiceDate(request.lastServiceDate());
                    ski.setNextServiceDate(request.nextServiceDate());
                    ski.setStruktura(request.struktura());
                    if (request.struktura() != null && !request.struktura().isBlank()) {
                        ski.setStrukturaRecordedAt(java.time.LocalDateTime.now());
                    } else {
                        ski.setStrukturaRecordedAt(null);
                    }
                    if (request.structureChangeCount() != null) {
                        ski.setStructureChangeCount(request.structureChangeCount());
                    }
                    ski.setEan(request.ean());
                    ski.setPartNo(request.partNo());
                    ski.setSerialNo(request.serialNo());
                    if (request.skiUsage() != null && !request.skiUsage().isBlank()) {
                        try { ski.setSkiUsage(SkiUsage.valueOf(request.skiUsage())); } catch (IllegalArgumentException ignored) {}
                    } else {
                        ski.setSkiUsage(null);
                    }
                    return ResponseEntity.ok(toResponse(skiRepository.save(ski)));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSki(@PathVariable Long id) {
        if (!skiRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        skiRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private SkiResponse toResponse(Ski ski) {
        return new SkiResponse(
                ski.getId(),
                ski.getSkiNumber(),
                ski.getBrand(),
                ski.getModel(),
                ski.getLength(),
                ski.getYear(),
                ski.getSkiType(),
                ski.getWeightKg(),
                ski.getCondition().name(),
                ski.getStatus().name(),
                ski.getLocation(),
                ski.getNotes(),
                ski.getLastServiceDate() != null ? ski.getLastServiceDate().toString() : null,
                ski.getNextServiceDate() != null ? ski.getNextServiceDate().toString() : null,
                ski.getStruktura(),
                ski.getStrukturaRecordedAt() != null ? ski.getStrukturaRecordedAt().toString() : null,
                ski.getStructureChangeCount() != null ? ski.getStructureChangeCount() : 0,
                ski.getEan(),
                ski.getPartNo(),
                ski.getSerialNo(),
                ski.getSkiUsage() != null ? ski.getSkiUsage().name() : null
        );
    }

    public record SkiResponse(
            Long id,
            String skiNumber,
            String brand,
            String model,
            String length,
            Integer year,
            String skiType,
            java.math.BigDecimal weightKg,
            String condition,
            String status,
            String location,
            String notes,
            String lastServiceDate,
            String nextServiceDate,
            String struktura,
            String strukturaRecordedAt,
            Integer structureChangeCount,
            String ean,
            String partNo,
            String serialNo,
            String skiUsage
    ) {}

    public record CreateSkiRequest(
            @jakarta.validation.constraints.NotBlank String brand,
            @jakarta.validation.constraints.NotBlank String model,
            @jakarta.validation.constraints.NotBlank String length,
            Integer year,
            String skiType,
            java.math.BigDecimal weightKg,
            String condition,
            String status,
            String location,
            String notes,
            java.time.LocalDate lastServiceDate,
            java.time.LocalDate nextServiceDate,
            String struktura,
            Integer structureChangeCount,
            String ean,
            String partNo,
            String serialNo,
            String skiUsage
    ) {}

    public record UpdateSkiRequest(
            @jakarta.validation.constraints.NotBlank String brand,
            @jakarta.validation.constraints.NotBlank String model,
            @jakarta.validation.constraints.NotBlank String length,
            Integer year,
            String skiType,
            java.math.BigDecimal weightKg,
            String condition,
            String status,
            String location,
            String notes,
            java.time.LocalDate lastServiceDate,
            java.time.LocalDate nextServiceDate,
            String struktura,
            Integer structureChangeCount,
            String ean,
            String partNo,
            String serialNo,
            String skiUsage
    ) {}

    public record PageResponse<T>(List<T> content, long totalElements, int totalPages, int size, int number, boolean first, boolean last) {}
}
