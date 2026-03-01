package com.marketplace.controller;

import com.marketplace.repository.LogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final LogRepository repo;

    // AUDIT LOG
    // Пример:
    // /api/admin/logs/audit?start=2026-03-01&end=2026-03-10&tableName=products&action=UPDATE&limit=200
    @GetMapping("/audit")
    public List<Map<String, Object>> audit(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) String action,
            @RequestParam(defaultValue = "200") int limit
    ) {
        if (limit <= 0) limit = 200;
        if (limit > 1000) limit = 1000;
        return repo.audit(start, end, tableName, action, limit);
    }

    // LOGIN HISTORY
    // Пример:
    // /api/admin/logs/logins?start=2026-03-01&end=2026-03-10&email=admin&limit=200
    @GetMapping("/logins")
    public List<Map<String, Object>> logins(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(required = false) String email,
            @RequestParam(defaultValue = "200") int limit
    ) {
        if (limit <= 0) limit = 200;
        if (limit > 1000) limit = 1000;
        return repo.logins(start, end, email, limit);
    }

    // для фронта: список доступных table_name
    @GetMapping("/audit/table-names")
    public List<Map<String, Object>> auditTableNames() {
        return repo.auditTableNames();
    }

    // для фронта: список доступных action
    @GetMapping("/audit/actions")
    public List<Map<String, Object>> auditActions() {
        return repo.auditActions();
    }
}