package com.demo.agent.config;

import com.demo.agent.memory.PersistentChatMemory;
import com.demo.agent.rag.LocalFallbackEmbeddingModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AgentConfiguration {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return new LocalFallbackEmbeddingModel();
    }

    @Bean
    public SimpleVectorStore vectorStore(EmbeddingModel embeddingModel) {
        return new SimpleVectorStore(embeddingModel);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder,
                                 PersistentChatMemory chatMemory,
                                 @Value("${app.agent.system-prompt}") String systemPrompt) {
        return chatClientBuilder
                .defaultSystem(systemPrompt)
                .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
                .defaultFunctions("weatherLookup", "crmLookup", "genericRestApi", "knowledgeBaseSearch")
                .build();
    }
}
