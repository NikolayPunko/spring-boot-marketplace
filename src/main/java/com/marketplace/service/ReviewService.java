package com.marketplace.service;

import com.marketplace.dto.CreateReviewRequest;
import com.marketplace.model.User;
import com.marketplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;

    public Map<String, Object> createOrUpdate(CreateReviewRequest req, Authentication auth) {
        if (req == null || req.getProductId() == null || req.getRating() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "productId and rating required");
        }

        int rating = req.getRating();
        if (rating < 1 || rating > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "rating must be 1..5");
        }

        User user = userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));

        // Проверим, что товар существует
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id = ?",
                Integer.class,
                req.getProductId()
        );
        if (exists == null || exists == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
        }

        // Если отзыв уже есть — обновим, иначе вставим
        Long reviewId = null;
        try {
            reviewId = jdbcTemplate.queryForObject(
                    "SELECT id FROM product_reviews WHERE product_id = ? AND user_id = ? LIMIT 1",
                    Long.class,
                    req.getProductId(),
                    user.getId()
            );
        } catch (Exception ignored) {
            // нет строки — это нормально
        }

        if (reviewId == null) {
            jdbcTemplate.update(
                    "INSERT INTO product_reviews(product_id, user_id, rating) VALUES (?, ?, ?)",
                    req.getProductId(),
                    user.getId(),
                    rating
            );
            return Map.of("status", "created");
        } else {
            jdbcTemplate.update(
                    "UPDATE product_reviews SET rating = ? WHERE id = ?",
                    rating,
                    reviewId
            );
            return Map.of("status", "updated");
        }
    }

    public List<Map<String, Object>> getByProduct(long productId) {
        return jdbcTemplate.queryForList(
                """
                SELECT 
                    pr.id,
                    u.email,
                    pr.rating,
                    pr.product_id
                FROM product_reviews pr
                JOIN users u ON u.id = pr.user_id
                WHERE pr.product_id = ?
                ORDER BY pr.id DESC
                """,
                productId
        );
    }
}