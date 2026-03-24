package com.marketplace.service;

import com.marketplace.dto.ProductCreateRequest;
import com.marketplace.dto.ProductUpdateRequest;
import com.marketplace.model.*;
import com.marketplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SellerRepository sellerRepository;
    private final UserRepository userRepository;

    // Получить все товары
    public List<Product> getAll() {
        return productRepository.findAll();
    }

    // Получить товар по ID
    public Product getById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found"));
    }

    // Создание товара
    public Product create(ProductCreateRequest req, Authentication auth) {

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        Seller seller = sellerRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.FORBIDDEN,
                                "Only SELLER can create products"));

        Category category = categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Category not found"));

        Product product = new Product();
        product.setSeller(seller);
        product.setCategory(category);
        product.setName(req.getName());
        product.setPrice(req.getPrice());
        product.setStockQuantity(req.getStockQuantity());
        product.setCreatedAt(Instant.now());

        return productRepository.save(product);
    }

    // Обновление товара
    public Product update(Long productId,
                          ProductUpdateRequest req,
                          Authentication auth) {

        Product product = getById(productId);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {

            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            Seller seller = sellerRepository.findByUserId(user.getId())
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN));

            if (!product.getSeller().getId().equals(seller.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can edit only your products");
            }
        }

        if (req.getCategoryId() != null) {
            Category category = categoryRepository.findById(req.getCategoryId())
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                    "Category not found"));
            product.setCategory(category);
        }

        if (req.getName() != null)
            product.setName(req.getName());

        if (req.getPrice() != null)
            product.setPrice(req.getPrice());

        if (req.getStockQuantity() != null)
            product.setStockQuantity(req.getStockQuantity());

        return productRepository.save(product);
    }

    // Удаление товара
    public void delete(Long productId, Authentication auth) {

        Product product = getById(productId);

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin) {

            User user = userRepository.findByEmail(auth.getName())
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.UNAUTHORIZED));

            Seller seller = sellerRepository.findByUserId(user.getId())
                    .orElseThrow(() ->
                            new ResponseStatusException(HttpStatus.FORBIDDEN));

            if (!product.getSeller().getId().equals(seller.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "You can delete only your products");
            }
        }

        product.setStockQuantity(0);

        productRepository.save(product);
    }
}