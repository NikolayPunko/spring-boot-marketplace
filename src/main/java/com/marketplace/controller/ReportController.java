package com.marketplace.controller;

import com.marketplace.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ReportController {

    private final ReportRepository repo;

    @GetMapping("/top-products")
    public List<Map<String, Object>> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return repo.topProducts(limit);
    }

    @GetMapping("/top-sellers")
    public List<Map<String, Object>> topSellers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return repo.topSellers(start, end);
    }

    @GetMapping("/avg-check-by-month")
    public List<Map<String, Object>> avgCheckByMonth() {
        return repo.avgCheckByMonth();
    }

    @GetMapping("/category-revenue-share")
    public List<Map<String, Object>> categoryRevenueShare() {
        return repo.categoryRevenueShare();
    }

    @GetMapping("/users-without-orders")
    public List<Map<String, Object>> usersWithoutOrders() {
        return repo.usersWithoutOrders();
    }

    @GetMapping("/order-details/{orderId}")
    public List<Map<String, Object>> orderDetails(@PathVariable long orderId) {
        return repo.orderDetails(orderId);
    }

    @GetMapping("/payments-by-status")
    public List<Map<String, Object>> paymentsByStatus() {
        return repo.paymentsByStatus();
    }

    @GetMapping("/low-stock")
    public List<Map<String, Object>> lowStock(@RequestParam(defaultValue = "5") int threshold) {
        return repo.lowStock(threshold);
    }

    @GetMapping("/logins-by-day")
    public List<Map<String, Object>> loginsByDay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return repo.loginsByDay(start, end);
    }

    @GetMapping("/audit-summary")
    public List<Map<String, Object>> auditSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return repo.auditSummary(start, end);
    }
}