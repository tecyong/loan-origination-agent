package com.demo.agent.dto;

import java.util.List;

public record ChatMessageResponse(
    String response,
    String conversationId,
    List<ToolExecutionInfo> toolsUsed,
    List<String> sources,
    long durationMs
) {}
