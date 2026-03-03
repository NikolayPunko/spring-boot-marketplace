package com.marketplace.dto;

import lombok.Data;
import java.util.List;

@Data
public class CreateOrderRequest {

    private List<OrderItemRequest> items;
    private String address;

    @Data
    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

    }
}