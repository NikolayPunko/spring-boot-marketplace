package com.marketplace.controller;

import com.marketplace.dto.CreateOrderRequest;
import com.marketplace.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // BUYER: создать заказ
    @PostMapping
    public Map<String, Object> create(@RequestBody CreateOrderRequest request,
                                      Authentication auth) {
        Long orderId = orderService.createOrder(request, auth);
        return Map.of("orderId", orderId);
    }

    // BUYER: мои заказы
    @GetMapping("/my")
    public List<Map<String, Object>> myOrders(Authentication auth) {
        return orderService.getMyOrders(auth);
    }

    // BUYER: детали моего заказа
    @GetMapping("/my/{orderId}")
    public Map<String, Object> myOrderDetails(@PathVariable long orderId,
                                              Authentication auth) {
        return orderService.getMyOrderDetails(orderId, auth);
    }

    // BUYER: отменить заказ
    @PostMapping("/my/{orderId}/cancel")
    public void cancel(@PathVariable long orderId,
                       Authentication auth) {
        orderService.cancelMyOrder(orderId, auth);
    }

    // SELLER: заказы по моим товарам
    @GetMapping("/seller")
    @PreAuthorize("hasRole('SELLER')")
    public List<Map<String, Object>> sellerOrders(Authentication auth) {
        return orderService.getSellerOrders(auth);
    }

    // ADMIN: все заказы
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> allOrders() {
        return orderService.getAllOrders();
    }

    // ADMIN: детали заказа
    @GetMapping("/admin/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> adminDetails(@PathVariable long orderId) {
        return orderService.getOrderDetailsAdmin(orderId);
    }

    // ADMIN: изменить статус
    @PutMapping("/admin/{orderId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public void updateStatus(@PathVariable long orderId,
                             @RequestParam String status) {
        orderService.updateOrderStatus(orderId, status);
    }
}