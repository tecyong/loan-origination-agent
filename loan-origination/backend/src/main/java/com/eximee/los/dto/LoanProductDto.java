package com.eximee.los.dto;

import java.math.BigDecimal;

public record LoanProductDto(
    Long id,
    String code,
    String name,
    String description,
    BigDecimal minAmount,
    BigDecimal maxAmount,
    Integer minTenureMonths,
    Integer maxTenureMonths,
    BigDecimal interestRate,
    Boolean isActive
) {}
