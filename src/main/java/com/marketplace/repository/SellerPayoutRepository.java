package com.marketplace.repository;

import com.marketplace.model.SellerPayout;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SellerPayoutRepository extends JpaRepository<SellerPayout, Long> {
    List<SellerPayout> findBySellerId(Long sellerId);
}