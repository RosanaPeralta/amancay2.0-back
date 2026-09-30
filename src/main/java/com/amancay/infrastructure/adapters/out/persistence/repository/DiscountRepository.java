package com.amancay.infrastructure.adapters.out.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.amancay.entity.Discount;

@NoRepositoryBean
public interface DiscountRepository extends JpaRepository<Discount, Long> {
    List<Discount> findByDescription(String description);
}