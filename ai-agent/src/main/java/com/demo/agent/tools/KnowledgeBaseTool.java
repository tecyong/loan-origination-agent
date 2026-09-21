package com.demo.agent.tools;

import com.demo.agent.dto.SearchResultDto;
import com.demo.agent.rag.KnowledgeBaseService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component("knowledgeBaseSearch")
@Description("Search the company knowledge base for internal policies, employee handbook, return/refund rules, and API integration guides")
public class KnowledgeBaseTool implements Function<KnowledgeBaseTool.Request, KnowledgeBaseTool.Response> {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseTool.class);
    private final KnowledgeBaseService knowledgeBaseService;

    public record Request(
            @JsonProperty(required = true)
            @JsonPropertyDescription("The search query, e.g. 'refund policy for opened items', 'remote work stipend', or 'API rate limits'")
            String query
    ) {}

    public record Response(String answer) {}

    public KnowledgeBaseTool(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public Response apply(Request request) {
        String query = request.query();
        log.info("Executing KnowledgeBaseTool for query: '{}'", query);

        if (query == null || query.isBlank()) {
            return new Response("Please provide a search query.");
        }

        List<SearchResultDto> results = knowledgeBaseService.search(query, 3, 0.0);

        if (results.isEmpty()) {
            return new Response("No matching documents found in the Knowledge Base for: '" + query + "'.");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d relevant excerpts in Knowledge Base:\n\n", results.size()));

        for (int i = 0; i < results.size(); i++) {
            SearchResultDto doc = results.get(i);
            String source = doc.metadata() != null && doc.metadata().containsKey("source")
                    ? doc.metadata().get("source").toString()
                    : "Internal Document";

            sb.append(String.format("--- [Excerpt %d from %s] ---\n", i + 1, source));
            sb.append(doc.content().trim()).append("\n\n");
        }

        return new Response(sb.toString());
    }
}
