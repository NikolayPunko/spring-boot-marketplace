package com.marketplace.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sellers")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Seller {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    @JsonIgnoreProperties({"password"})
    private User user;

    @Column(name = "store_name", nullable = false)
    private String storeName;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;
}