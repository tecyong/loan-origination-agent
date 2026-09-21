package com.demo.agent.dto;

import java.time.Instant;

public record ToolExecutionInfo(
    String toolName,
    String inputSummary,
    String outputSummary,
    long durationMs,
    Instant timestamp
) {
    public static ToolExecutionInfo of(String toolName, String inputSummary, String outputSummary, long durationMs) {
        return new ToolExecutionInfo(toolName, inputSummary, outputSummary, durationMs, Instant.now());
    }
}
