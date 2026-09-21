package com.eximee.los.dto;

import com.eximee.los.domain.DocumentType;
import java.time.OffsetDateTime;

public record DocumentDto(
    Long id,
    DocumentType documentType,
    String fileName,
    Long fileSize,
    String contentType,
    OffsetDateTime uploadedAt
) {}
