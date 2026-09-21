package com.eximee.los.service;

import com.eximee.los.domain.LoanProduct;
import com.eximee.los.dto.LoanProductDto;
import com.eximee.los.repository.LoanProductRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoanProductService {

    private final LoanProductRepository productRepository;

    public LoanProductService(LoanProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<LoanProductDto> getAllActiveProducts() {
        return productRepository.findByIsActiveTrue().stream()
                .map(this::toDto)
                .toList();
    }

    public LoanProductDto getProductById(Long id) {
        LoanProduct product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found with ID: " + id));
        return toDto(product);
    }

    public LoanProduct getEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Loan product not found with ID: " + id));
    }

    private LoanProductDto toDto(LoanProduct p) {
        return new LoanProductDto(
                p.getId(),
                p.getCode(),
                p.getName(),
                p.getDescription(),
                p.getMinAmount(),
                p.getMaxAmount(),
                p.getMinTenureMonths(),
                p.getMaxTenureMonths(),
                p.getInterestRate(),
                p.getIsActive()
        );
    }
}
