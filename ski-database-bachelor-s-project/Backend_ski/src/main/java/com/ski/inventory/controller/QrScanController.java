package com.ski.inventory.controller;

import com.ski.inventory.model.QrScanLog;
import com.ski.inventory.model.Ski;
import com.ski.inventory.model.User;
import com.ski.inventory.repository.QrScanLogRepository;
import com.ski.inventory.repository.SkiRepository;
import com.ski.inventory.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/technician/qr-scans")
@PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
@CrossOrigin(origins = "*")
public class QrScanController {

    private static final int MAX_RECENT = 10;

    private final QrScanLogRepository qrScanLogRepository;
    private final SkiRepository skiRepository;
    private final UserRepository userRepository;

    public QrScanController(QrScanLogRepository qrScanLogRepository, SkiRepository skiRepository, UserRepository userRepository) {
        this.qrScanLogRepository = qrScanLogRepository;
        this.skiRepository = skiRepository;
        this.userRepository = userRepository;
    }

    /**
     * Uloží naskenovanou lyži (posledních 10 na uživatele, starší se mažou).
     * scannedValue = ID lyže (číslo) nebo číslo lyže (skiNumber).
     */
    @PostMapping
    public ResponseEntity<QrScanEntryResponse> recordScan(@RequestBody RecordQrScanRequest request) {
        if (request.scannedValue() == null || request.scannedValue().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        Optional<Ski> skiOpt = resolveSki(request.scannedValue().trim());
        if (skiOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Ski ski = skiOpt.get();
        QrScanLog log = new QrScanLog();
        log.setUser(user);
        log.setSki(ski);
        log.setScannedAt(LocalDateTime.now());
        qrScanLogRepository.save(log);

        // Ponechat jen posledních 10
        List<QrScanLog> top10 = qrScanLogRepository.findByUserIdOrderByScannedAtDesc(user.getId(), org.springframework.data.domain.Pageable.ofSize(MAX_RECENT));
        Set<Long> keepIds = top10.stream().map(QrScanLog::getId).collect(Collectors.toSet());
        qrScanLogRepository.deleteByUserIdAndIdNotIn(user.getId(), keepIds);

        return ResponseEntity.ok(toEntryResponse(log));
    }

    /**
     * Vrátí posledních 10 naskenovaných lyží přihlášeného uživatele (včetně skenů z mobilu).
     */
    @GetMapping
    public ResponseEntity<List<QrScanEntryResponse>> getRecentScans() {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).build();
        }
        List<QrScanLog> logs = qrScanLogRepository.findByUserIdOrderByScannedAtDesc(user.getId(), org.springframework.data.domain.Pageable.ofSize(MAX_RECENT));
        List<QrScanEntryResponse> list = logs.stream().map(this::toEntryResponse).toList();
        return ResponseEntity.ok(list);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getName() == null) {
            return null;
        }
        return userRepository.findByUsernameAndActiveTrue(auth.getName()).orElse(null);
    }

    private Optional<Ski> resolveSki(String value) {
        if (value.matches("\\d+")) {
            return skiRepository.findById(Long.parseLong(value));
        }
        return skiRepository.findBySkiNumber(value);
    }

    private QrScanEntryResponse toEntryResponse(QrScanLog log) {
        Ski s = log.getSki();
        String skiInfo = s != null ? (s.getBrand() + " " + s.getModel() + " " + s.getLength()) : "";
        String skiNumber = s != null ? s.getSkiNumber() : null;
        Long skiId = s != null ? s.getId() : null;
        String scannedAt = log.getScannedAt() != null ? log.getScannedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : null;
        return new QrScanEntryResponse(log.getId(), skiId, skiNumber, skiInfo, scannedAt);
    }

    public record RecordQrScanRequest(String scannedValue) {}
    public record QrScanEntryResponse(Long id, Long skiId, String skiNumber, String skiInfo, String scannedAt) {}
}
