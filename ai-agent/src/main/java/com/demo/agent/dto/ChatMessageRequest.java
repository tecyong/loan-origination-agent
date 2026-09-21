package com.demo.agent.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageRequest(
    @NotBlank(message = "Message cannot be empty")
    String message,
    String conversationId
) {
    public String getEffectiveConversationId() {
        if (conversationId == null || conversationId.isBlank()) {
            return "default-session";
        }
        return conversationId.trim();
    }
}
