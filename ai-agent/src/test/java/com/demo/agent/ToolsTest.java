package com.demo.agent;

import com.demo.agent.rag.KnowledgeBaseService;
import com.demo.agent.rag.LocalFallbackEmbeddingModel;
import com.demo.agent.tools.GenericRestApiTool;
import com.demo.agent.tools.KnowledgeBaseTool;
import com.demo.agent.tools.MockCrmTool;
import com.demo.agent.tools.WeatherApiTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.SimpleVectorStore;

import static org.junit.jupiter.api.Assertions.*;

class ToolsTest {

    private WeatherApiTool weatherTool;
    private MockCrmTool crmTool;
    private GenericRestApiTool restTool;
    private KnowledgeBaseTool kbTool;
    private KnowledgeBaseService kbService;

    @BeforeEach
    void setUp() {
        weatherTool = new WeatherApiTool();
        crmTool = new MockCrmTool();
        restTool = new GenericRestApiTool();

        SimpleVectorStore vectorStore = new SimpleVectorStore(new LocalFallbackEmbeddingModel());
        kbService = new KnowledgeBaseService(vectorStore, "target/test-knowledge-store.json");
        kbService.indexSeedDocuments();
        kbTool = new KnowledgeBaseTool(kbService);
    }

    @Test
    void testMockCrmToolCustomerLookup() {
        var response = crmTool.apply(new MockCrmTool.Request("customer", "CUST-8812"));
        assertNotNull(response);
        assertTrue(response.result().contains("Alice Johnson"));
        assertTrue(response.result().contains("Platinum VIP"));
    }

    @Test
    void testMockCrmToolOrderLookup() {
        var response = crmTool.apply(new MockCrmTool.Request("order", "ORD-1042"));
        assertNotNull(response);
        assertTrue(response.result().contains("PROD-AI-EDGE"));
        assertTrue(response.result().contains("Shipped in Transit"));
    }

    @Test
    void testMockCrmToolInventoryLookup() {
        var response = crmTool.apply(new MockCrmTool.Request("inventory", "PROD-AI-EDGE"));
        assertNotNull(response);
        assertTrue(response.result().contains("Neural Compute Edge Accelerator"));
        assertTrue(response.result().contains("42"));
    }

    @Test
    void testKnowledgeBaseToolSearch() {
        var response = kbTool.apply(new KnowledgeBaseTool.Request("refund policy for opened items"));
        assertNotNull(response);
        assertNotNull(response.answer());
        assertTrue(response.answer().contains("return-refund-policy.md") || response.answer().contains("restocking fee") || response.answer().contains("Found"));
    }

    @Test
    void testGenericRestApiToolValidation() {
        var response = restTool.apply(new GenericRestApiTool.Request("GET", "", null));
        assertNotNull(response);
        assertTrue(response.output().contains("Error: URL parameter is required"));
    }
}
