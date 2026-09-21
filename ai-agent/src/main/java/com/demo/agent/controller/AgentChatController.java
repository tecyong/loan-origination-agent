package com.demo.agent.controller;

import com.demo.agent.dto.ChatMessageRequest;
import com.demo.agent.dto.ChatMessageResponse;
import com.demo.agent.dto.ToolExecutionInfo;
import com.demo.agent.memory.PersistentChatMemory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.AbstractChatMemoryAdvisor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api")
@Tag(name = "Agent Chat", description = "Endpoints for interacting with the Gemini AI Agent via JSON and SSE streams")
public class AgentChatController {

    private static final Logger log = LoggerFactory.getLogger(AgentChatController.class);

    private final ChatClient chatClient;
    private final PersistentChatMemory chatMemory;

    public AgentChatController(ChatClient chatClient, PersistentChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
    }

    @PostMapping("/chat")
    @Operation(summary = "Send chat message (Synchronous)", description = "Sends a message to the agent and returns the complete answer with metadata")
    public ResponseEntity<ChatMessageResponse> chat(@Valid @RequestBody ChatMessageRequest request) {
        long startTime = System.currentTimeMillis();
        String convId = request.getEffectiveConversationId();

        try {
            String content = chatClient.prompt()
                    .user(request.message())
                    .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, convId))
                    .call()
                    .content();

            long duration = System.currentTimeMillis() - startTime;
            ChatMessageResponse response = new ChatMessageResponse(
                    content != null ? content : "No response generated.",
                    convId,
                    List.of(),
                    List.of(),
                    duration
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error invoking chat agent: {}", e.getMessage(), e);
            long duration = System.currentTimeMillis() - startTime;
            ChatMessageResponse errorResponse = new ChatMessageResponse(
                    "Error executing agent request: " + e.getMessage() +
                    "\n\n*Tip: Ensure your GEMINI_API_KEY environment variable is set.*",
                    convId,
                    List.of(),
                    List.of(),
                    duration
            );
            return ResponseEntity.ok(errorResponse);
        }
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat message (Server-Sent Events)", description = "Streams tokens in real-time as the agent generates the response")
    public SseEmitter streamChat(
            @RequestParam("message") String message,
            @RequestParam(value = "conversationId", defaultValue = "default-session") String conversationId) {

        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout

        CompletableFuture.runAsync(() -> {
            try {
                // Send initial status event
                emitter.send(SseEmitter.event().name("status").data("Agent reasoning and selecting tools..."));

                Flux<String> stream = chatClient.prompt()
                        .user(message)
                        .advisors(a -> a.param(AbstractChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, conversationId))
                        .stream()
                        .content();

                stream.subscribe(
                        chunk -> {
                            try {
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.send(SseEmitter.event().name("chunk").data(chunk));
                                }
                            } catch (IOException e) {
                                log.warn("Client disconnected during SSE stream: {}", e.getMessage());
                            }
                        },
                        error -> {
                            log.error("Streaming error: {}", error.getMessage());
                            try {
                                emitter.send(SseEmitter.event().name("error")
                                        .data("Agent stream error: " + error.getMessage() + ". (Check GEMINI_API_KEY)"));
                                emitter.complete();
                            } catch (IOException ignored) {}
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException ignored) {}
                        }
                );
            } catch (Exception e) {
                log.error("Error setting up stream: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error")
                            .data("Failed to start stream: " + e.getMessage() + ". Check GEMINI_API_KEY."));
                    emitter.complete();
                } catch (IOException ignored) {}
            }
        });

        return emitter;
    }

    @GetMapping("/conversations")
    @Operation(summary = "List conversation sessions", description = "Lists all conversation session IDs stored in memory")
    public ResponseEntity<Set<String>> getConversations() {
        return ResponseEntity.ok(chatMemory.getConversationIds());
    }

    @DeleteMapping("/conversations/{conversationId}")
    @Operation(summary = "Clear conversation session", description = "Clears the memory of a specific conversation session")
    public ResponseEntity<Map<String, String>> clearConversation(@PathVariable String conversationId) {
        chatMemory.clear(conversationId);
        return ResponseEntity.ok(Map.of("message", "Conversation " + conversationId + " cleared successfully."));
    }
}
