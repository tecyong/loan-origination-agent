package com.demo.agent.rag;

import com.demo.agent.dto.KnowledgeDocumentInfo;
import com.demo.agent.dto.SearchResultDto;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);

    private final SimpleVectorStore vectorStore;
    private final String storagePath;
    private final Map<String, KnowledgeDocumentInfo> indexedDocuments = new ConcurrentHashMap<>();
    private final TokenTextSplitter textSplitter = new TokenTextSplitter();

    public KnowledgeBaseService(SimpleVectorStore vectorStore,
                                @Value("${app.storage.vector-store-file:data/knowledge-store.json}") String storagePath) {
        this.vectorStore = vectorStore;
        this.storagePath = storagePath;
    }

    @PostConstruct
    public void initialize() {
        File storeFile = new File(storagePath);
        if (storeFile.exists() && storeFile.length() > 0) {
            try {
                log.info("Loading existing vector store from: {}", storeFile.getAbsolutePath());
                vectorStore.load(storeFile);
                log.info("Loaded vector store successfully.");
            } catch (Exception e) {
                log.warn("Failed to load existing vector store, re-indexing seed documents: {}", e.getMessage());
                indexSeedDocuments();
            }
        } else {
            log.info("No existing vector store found. Indexing default seed documents...");
            indexSeedDocuments();
        }
    }

    public synchronized void indexSeedDocuments() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:docs/*.*");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename != null && !indexedDocuments.containsKey(filename)) {
                    log.info("Indexing seed document: {}", filename);
                    TextReader textReader = new TextReader(resource);
                    textReader.getCustomMetadata().put("source", filename);
                    textReader.getCustomMetadata().put("isSeed", true);

                    List<Document> docs = textReader.get();
                    List<Document> splitDocs = textSplitter.apply(docs);
                    vectorStore.add(splitDocs);

                    indexedDocuments.put(filename, new KnowledgeDocumentInfo(
                            filename,
                            splitDocs.size(),
                            resource.contentLength(),
                            "text/markdown",
                            Instant.now()
                    ));
                }
            }
            persistStore();
        } catch (Exception e) {
            log.error("Error indexing seed documents: {}", e.getMessage(), e);
        }
    }

    public synchronized KnowledgeDocumentInfo ingestFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "uploaded_document";
        String contentType = file.getContentType() != null ? file.getContentType() : "text/plain";
        long size = file.getSize();

        List<Document> documents = new ArrayList<>();

        if (originalFilename.toLowerCase().endsWith(".pdf")) {
            PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(file.getResource());
            documents = pdfReader.get();
        } else {
            String text = new String(file.getBytes(), StandardCharsets.UTF_8);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", originalFilename);
            metadata.put("contentType", contentType);
            documents.add(new Document(text, metadata));
        }

        List<Document> splitDocs = textSplitter.apply(documents);
        for (Document d : splitDocs) {
            d.getMetadata().put("source", originalFilename);
        }
        vectorStore.add(splitDocs);

        KnowledgeDocumentInfo info = new KnowledgeDocumentInfo(
                originalFilename,
                splitDocs.size(),
                size,
                contentType,
                Instant.now()
        );
        indexedDocuments.put(originalFilename, info);

        persistStore();
        log.info("Successfully ingested document: {} ({} chunks)", originalFilename, splitDocs.size());
        return info;
    }

    public List<SearchResultDto> search(String query, int topK, double minScore) {
        try {
            SearchRequest request = SearchRequest.query(query)
                    .withTopK(topK > 0 ? topK : 4)
                    .withSimilarityThreshold(minScore > 0 ? minScore : 0.0);

            List<Document> matches = vectorStore.similaritySearch(request);
            List<SearchResultDto> results = new ArrayList<>();

            for (Document doc : matches) {
                // Score can be present in metadata or null
                double score = 1.0;
                if (doc.getMetadata().containsKey("distance")) {
                    Object dist = doc.getMetadata().get("distance");
                    if (dist instanceof Number num) {
                        score = 1.0 - num.doubleValue();
                    }
                }
                results.add(new SearchResultDto(doc.getContent(), score, doc.getMetadata()));
            }
            return results;
        } catch (Exception e) {
            log.error("Error during vector similarity search for query '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<KnowledgeDocumentInfo> getIndexedDocuments() {
        return new ArrayList<>(indexedDocuments.values());
    }

    private void persistStore() {
        try {
            File storeFile = new File(storagePath);
            File parentDir = storeFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }
            vectorStore.save(storeFile);
            log.info("Vector store persisted to: {}", storeFile.getAbsolutePath());
        } catch (Exception e) {
            log.warn("Failed to persist vector store: {}", e.getMessage());
        }
    }
}
