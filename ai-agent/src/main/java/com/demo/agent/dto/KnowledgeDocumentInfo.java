package com.demo.agent.dto;

import java.time.Instant;

public record KnowledgeDocumentInfo(
    String filename,
    int chunkCount,
    long fileSizeBytes,
    String contentType,
    Instant indexedAt
) {}
