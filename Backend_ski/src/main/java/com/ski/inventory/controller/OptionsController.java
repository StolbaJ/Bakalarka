package com.ski.inventory.controller;

import com.ski.inventory.model.CommonModificationOption;
import com.ski.inventory.model.StrukturaOption;
import com.ski.inventory.repository.CommonModificationOptionRepository;
import com.ski.inventory.repository.StrukturaOptionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/technician/options")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
@CrossOrigin(origins = "*")
public class OptionsController {

    private final StrukturaOptionRepository strukturaOptionRepository;
    private final CommonModificationOptionRepository commonModificationOptionRepository;

    public OptionsController(StrukturaOptionRepository strukturaOptionRepository,
                             CommonModificationOptionRepository commonModificationOptionRepository) {
        this.strukturaOptionRepository = strukturaOptionRepository;
        this.commonModificationOptionRepository = commonModificationOptionRepository;
    }

    @GetMapping("/struktury")
    public ResponseEntity<List<StrukturaOptionDto>> getStruktury() {
        List<StrukturaOptionDto> list = strukturaOptionRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(o -> new StrukturaOptionDto(o.getId(), o.getName(), o.getSortOrder()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/upravy")
    public ResponseEntity<List<ModificationOptionDto>> getUpravy() {
        List<ModificationOptionDto> list = commonModificationOptionRepository.findAllByOrderBySortOrderAscNameAsc()
                .stream()
                .map(o -> new ModificationOptionDto(o.getId(), o.getName(), o.getDescription(), o.getSortOrder(), o.isRequiresWorkDescription()))
                .toList();
        return ResponseEntity.ok(list);
    }

    @PostMapping("/struktury")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StrukturaOptionDto> addStruktura(@RequestBody CreateStrukturaRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        StrukturaOption opt = new StrukturaOption();
        opt.setName(request.name().trim());
        opt.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        opt = strukturaOptionRepository.save(opt);
        return ResponseEntity.ok(new StrukturaOptionDto(opt.getId(), opt.getName(), opt.getSortOrder()));
    }

    @DeleteMapping("/struktury/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteStruktura(@PathVariable Long id) {
        if (!strukturaOptionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        strukturaOptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upravy")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModificationOptionDto> addUprava(@RequestBody CreateModificationRequest request) {
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        CommonModificationOption opt = new CommonModificationOption();
        opt.setName(request.name().trim());
        opt.setDescription(request.description() != null ? request.description().trim() : null);
        opt.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);
        opt.setRequiresWorkDescription(request.requiresWorkDescription() != null && request.requiresWorkDescription());
        opt = commonModificationOptionRepository.save(opt);
        return ResponseEntity.ok(new ModificationOptionDto(opt.getId(), opt.getName(), opt.getDescription(), opt.getSortOrder(), opt.isRequiresWorkDescription()));
    }

    @PatchMapping("/upravy/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ModificationOptionDto> updateUprava(@PathVariable Long id, @RequestBody UpdateModificationRequest request) {
        return commonModificationOptionRepository.findById(id)
                .map(opt -> {
                    if (request.requiresWorkDescription() != null) {
                        opt.setRequiresWorkDescription(request.requiresWorkDescription());
                    }
                    opt = commonModificationOptionRepository.save(opt);
                    return ResponseEntity.ok(new ModificationOptionDto(opt.getId(), opt.getName(), opt.getDescription(), opt.getSortOrder(), opt.isRequiresWorkDescription()));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/upravy/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUprava(@PathVariable Long id) {
        if (!commonModificationOptionRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        commonModificationOptionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public record StrukturaOptionDto(Long id, String name, int sortOrder) {}
    public record ModificationOptionDto(Long id, String name, String description, int sortOrder, boolean requiresWorkDescription) {}
    public record CreateStrukturaRequest(String name, Integer sortOrder) {}
    public record CreateModificationRequest(String name, String description, Integer sortOrder, Boolean requiresWorkDescription) {}
    public record UpdateModificationRequest(Boolean requiresWorkDescription) {}
}
