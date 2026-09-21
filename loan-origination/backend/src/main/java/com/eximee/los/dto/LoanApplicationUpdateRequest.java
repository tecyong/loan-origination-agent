package com.eximee.los.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LoanApplicationUpdateRequest(
    @NotNull(message = "Product ID is required")
    Long productId,

    @NotNull(message = "Loan amount is required")
    @DecimalMin(value = "100.00", message = "Minimum loan amount is 100.00")
    BigDecimal amount,

    @NotNull(message = "Tenure is required")
    @Min(value = 1, message = "Minimum tenure is 1 month")
    Integer tenureMonths,

    String purpose,
    String employmentType,
    String employerName,

    @DecimalMin(value = "0.00")
    BigDecimal grossMonthlyIncome,

    @DecimalMin(value = "0.00")
    BigDecimal existingMonthlyDebt
) {}
