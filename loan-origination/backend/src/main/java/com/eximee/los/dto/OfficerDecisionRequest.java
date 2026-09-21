package com.eximee.los.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public record OfficerDecisionRequest(
    @NotBlank(message = "Action is required (APPROVE, REJECT, REQUEST_INFO)")
    String action,

    BigDecimal approvedAmount,
    String decisionReason,
    String revisionNotes
) {}
