package com.eximee.los.repository;

import com.eximee.los.domain.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {
    Optional<LoanProduct> findByCode(String code);
    List<LoanProduct> findByIsActiveTrue();
}
