package com.marketplace.repository;

import com.marketplace.model.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    List<ProductReview> findByProductId(Long productId);

    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);
}