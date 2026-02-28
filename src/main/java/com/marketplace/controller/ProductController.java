package com.marketplace.controller;

import com.marketplace.dto.ProductCreateRequest;
import com.marketplace.dto.ProductUpdateRequest;
import com.marketplace.model.Product;
import com.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    // Получить все товары
    @GetMapping
    public List<Product> getAll() {
        return productService.getAll();
    }

    // Получить товар по ID
    @GetMapping("/{id}")
    public Product getById(@PathVariable Long id) {
        return productService.getById(id);
    }

    // Создать товар
    @PostMapping
    public Product create(@RequestBody ProductCreateRequest req,
                          Authentication auth) {
        return productService.create(req, auth);
    }

    // Обновить товар
    @PutMapping("/{id}")
    public Product update(@PathVariable Long id,
                          @RequestBody ProductUpdateRequest req,
                          Authentication auth) {
        return productService.update(id, req, auth);
    }

    // Удалить товар
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id,
                       Authentication auth) {
        productService.delete(id, auth);
    }
}