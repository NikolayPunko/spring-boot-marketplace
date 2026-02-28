package com.marketplace.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "seller_payouts")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class SellerPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Seller seller;

    private BigDecimal amount;

    @Column(name = "payout_date")
    private Instant payoutDate;
}