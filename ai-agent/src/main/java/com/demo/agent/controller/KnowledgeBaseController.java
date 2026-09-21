package com.demo.agent.controller;

import com.demo.agent.dto.KnowledgeDocumentInfo;
import com.demo.agent.dto.SearchResultDto;
import com.demo.agent.rag.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
@Tag(name = "Knowledge Base", description = "Endpoints for managing and querying the RAG knowledge vector store")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload document", description = "Upload a text, markdown, or PDF document to be chunked and indexed in the vector store")
    public ResponseEntity<?> uploadDocument(@RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Uploaded file is empty"));
            }
            KnowledgeDocumentInfo docInfo = knowledgeBaseService.ingestFile(file);
            return ResponseEntity.ok(docInfo);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/documents")
    @Operation(summary = "List indexed documents", description = "Retrieve list of all currently indexed documents in the knowledge base")
    public ResponseEntity<List<KnowledgeDocumentInfo>> getIndexedDocuments() {
        return ResponseEntity.ok(knowledgeBaseService.getIndexedDocuments());
    }

    @GetMapping("/search")
    @Operation(summary = "Query knowledge base", description = "Search the vector store directly with similarity score")
    public ResponseEntity<List<SearchResultDto>> search(
            @RequestParam("q") String query,
            @RequestParam(value = "topK", defaultValue = "4") int topK,
            @RequestParam(value = "minScore", defaultValue = "0.0") double minScore) {
        return ResponseEntity.ok(knowledgeBaseService.search(query, topK, minScore));
    }
}
