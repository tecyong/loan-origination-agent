package com.eximee.los.dto;

import java.time.OffsetDateTime;

public record AuditLogDto(
    Long id,
    String actorName,
    String actorEmail,
    String fromStatus,
    String toStatus,
    String actionType,
    String notes,
    OffsetDateTime createdAt
) {}
