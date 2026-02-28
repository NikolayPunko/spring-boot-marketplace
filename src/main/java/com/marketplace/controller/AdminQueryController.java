package com.marketplace.controller;

import com.marketplace.service.AdminQueryService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/query")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminQueryController {

    private final AdminQueryService adminQueryService;

    @PostMapping
    public List<Map<String, Object>> run(@RequestBody QueryRequest req) {
        return adminQueryService.executeSelect(req.getSql());
    }

    @Data
    public static class QueryRequest {
        private String sql;
    }
}