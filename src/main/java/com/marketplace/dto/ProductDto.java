package com.marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@AllArgsConstructor
public class ProductDto {
    private Long id;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
    private Instant createdAt;

    private Long categoryId;
    private String categoryName;

    private Long sellerId;
    private String sellerStoreName;
}