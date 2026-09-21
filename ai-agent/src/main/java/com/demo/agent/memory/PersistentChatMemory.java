package com.demo.agent.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class PersistentChatMemory implements ChatMemory {

    private static final Logger log = LoggerFactory.getLogger(PersistentChatMemory.class);

    private final Map<String, List<Message>> conversationStore = new ConcurrentHashMap<>();
    private final String historyFilePath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record SerializedMessage(String messageType, String content) {}

    public PersistentChatMemory(@Value("${app.storage.chat-history-file:data/chat-history.json}") String historyFilePath) {
        this.historyFilePath = historyFilePath;
    }

    @PostConstruct
    public void init() {
        File file = new File(historyFilePath);
        if (file.exists() && file.length() > 0) {
            try {
                Map<String, List<SerializedMessage>> savedData = objectMapper.readValue(
                        file, new TypeReference<Map<String, List<SerializedMessage>>>() {}
                );
                for (var entry : savedData.entrySet()) {
                    List<Message> messages = new ArrayList<>();
                    for (SerializedMessage sm : entry.getValue()) {
                        messages.add(deserializeMessage(sm));
                    }
                    conversationStore.put(entry.getKey(), Collections.synchronizedList(messages));
                }
                log.info("Restored {} conversation sessions from {}", conversationStore.size(), file.getAbsolutePath());
            } catch (Exception e) {
                log.warn("Failed to load chat history from {}: {}", historyFilePath, e.getMessage());
            }
        }
    }

    @Override
    public void add(String conversationId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty()) {
            return;
        }
        List<Message> history = conversationStore.computeIfAbsent(
                conversationId, k -> Collections.synchronizedList(new ArrayList<>())
        );
        history.addAll(messages);
        persist();
    }

    @Override
    public void add(String conversationId, Message message) {
        if (conversationId != null && message != null) {
            add(conversationId, List.of(message));
        }
    }

    @Override
    public List<Message> get(String conversationId, int lastN) {
        List<Message> history = conversationStore.get(conversationId);
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        synchronized (history) {
            if (lastN <= 0 || history.size() <= lastN) {
                return new ArrayList<>(history);
            }
            return new ArrayList<>(history.subList(history.size() - lastN, history.size()));
        }
    }

    @Override
    public void clear(String conversationId) {
        conversationStore.remove(conversationId);
        persist();
    }

    public Set<String> getConversationIds() {
        return new HashSet<>(conversationStore.keySet());
    }

    private synchronized void persist() {
        try {
            File file = new File(historyFilePath);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }

            Map<String, List<SerializedMessage>> toSave = new HashMap<>();
            for (var entry : conversationStore.entrySet()) {
                List<SerializedMessage> serialized = entry.getValue().stream()
                        .map(this::serializeMessage)
                        .collect(Collectors.toList());
                toSave.put(entry.getKey(), serialized);
            }

            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, toSave);
        } catch (Exception e) {
            log.warn("Failed to persist chat history: {}", e.getMessage());
        }
    }

    private SerializedMessage serializeMessage(Message msg) {
        return new SerializedMessage(msg.getMessageType().getValue(), msg.getContent());
    }

    private Message deserializeMessage(SerializedMessage sm) {
        String type = sm.messageType() != null ? sm.messageType().toLowerCase() : "user";
        return switch (type) {
            case "assistant" -> new AssistantMessage(sm.content());
            case "system" -> new SystemMessage(sm.content());
            default -> new UserMessage(sm.content());
        };
    }
}
