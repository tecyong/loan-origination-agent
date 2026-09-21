package com.demo.agent.dto;

import java.util.Map;

public record SearchResultDto(
    String content,
    double score,
    Map<String, Object> metadata
) {}
