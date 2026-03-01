package com.marketplace.controller;

import com.marketplace.dto.ProductCreateRequest;
import com.marketplace.dto.ProductDto;
import com.marketplace.dto.ProductUpdateRequest;
import com.marketplace.model.Product;
import com.marketplace.model.Seller;
import com.marketplace.model.User;
import com.marketplace.repository.ProductRepository;
import com.marketplace.repository.SellerRepository;
import com.marketplace.repository.UserRepository;
import com.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final SellerRepository sellerRepository;
    private final ProductRepository productRepository;

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

    @GetMapping("/my")
    @PreAuthorize("hasRole('SELLER')")
    public List<ProductDto> myProducts(Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Seller profile not found"));

        List<Product> products = productRepository.findBySellerId(seller.getId());

        return products.stream()
                .map(p -> new ProductDto(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getStockQuantity(),
                        p.getCreatedAt(),
                        p.getCategory() != null ? p.getCategory().getId() : null,
                        p.getCategory() != null ? p.getCategory().getName() : null,
                        p.getSeller() != null ? p.getSeller().getId() : null,
                        p.getSeller() != null ? p.getSeller().getStoreName() : null
                ))
                .toList();
    }
}