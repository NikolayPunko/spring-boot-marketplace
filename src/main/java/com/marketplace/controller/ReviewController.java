package com.marketplace.controller;

import com.marketplace.dto.CreateReviewRequest;
import com.marketplace.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // BUYER: поставить/обновить оценку
    @PostMapping
    @PreAuthorize("hasRole('BUYER')")
    public Map<String, Object> createOrUpdate(@RequestBody CreateReviewRequest req,
                                              Authentication auth) {
        return reviewService.createOrUpdate(req, auth);
    }

    // Публично/для всех авторизованных: список отзывов по товару
    @GetMapping("/product/{productId}")
    public List<Map<String, Object>> byProduct(@PathVariable long productId) {
        return reviewService.getByProduct(productId);
    }
}