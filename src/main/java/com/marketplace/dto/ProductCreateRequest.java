package com.marketplace.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductCreateRequest {

    private Long categoryId;
    private String name;
    private BigDecimal price;
    private Integer stockQuantity;
}