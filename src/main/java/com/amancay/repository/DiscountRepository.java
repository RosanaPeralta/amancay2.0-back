package com.amancay.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.amancay.entity.Discount;

public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByDescription(String description);
}