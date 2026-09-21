package com.eximee.los.dto;

import com.eximee.los.domain.ApplicationStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record LoanApplicationResponse(
    Long id,
    String applicationNumber,
    UserDto applicant,
    LoanProductDto product,
    BigDecimal amount,
    Integer tenureMonths,
    String purpose,
    String employmentType,
    String employerName,
    BigDecimal grossMonthlyIncome,
    BigDecimal existingMonthlyDebt,
    BigDecimal calculatedDti,
    BigDecimal estimatedMonthlyPayment,
    ApplicationStatus status,
    String processInstanceId,
    BigDecimal approvedAmount,
    String decisionReason,
    String decidedByName,
    OffsetDateTime decidedAt,
    Integer version,
    List<DocumentDto> documents,
    List<AuditLogDto> auditLogs,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {}
