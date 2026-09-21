package com.demo.agent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * High-performance, zero-latency local embedding model that projects text
 * into a normalized 384-dimensional dense vector space using n-gram feature hashing.
 * Guarantees zero network calls, zero external library download dependencies, and zero API costs.
 */
public class LocalFallbackEmbeddingModel extends AbstractEmbeddingModel {

    public static final int VECTOR_DIMENSIONS = 384;

    @Override
    public float[] embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public float[] embed(String text) {
        return computeDenseVector(text);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> instructions = request.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            float[] vector = computeDenseVector(instructions.get(i));
            embeddings.add(new Embedding(vector, i));
        }
        return new EmbeddingResponse(embeddings);
    }

    private float[] computeDenseVector(String text) {
        float[] vector = new float[VECTOR_DIMENSIONS];
        if (text == null || text.isBlank()) {
            return vector;
        }

        String[] tokens = text.toLowerCase().split("\\W+");
        for (String token : tokens) {
            if (token.isBlank()) continue;
            int hash = Math.abs(token.hashCode());
            int idx1 = hash % VECTOR_DIMENSIONS;
            int idx2 = (hash / VECTOR_DIMENSIONS) % VECTOR_DIMENSIONS;
            vector[idx1] += 1.0f;
            vector[idx2] += 0.5f;

            // Character n-grams for typo resilience
            if (token.length() >= 3) {
                for (int i = 0; i < token.length() - 2; i++) {
                    int subHash = Math.abs(token.substring(i, i + 3).hashCode());
                    vector[subHash % VECTOR_DIMENSIONS] += 0.25f;
                }
            }
        }

        // L2 normalize vector for cosine similarity
        double sumSquares = 0.0;
        for (float v : vector) {
            sumSquares += v * v;
        }
        if (sumSquares > 0) {
            float norm = (float) Math.sqrt(sumSquares);
            for (int i = 0; i < VECTOR_DIMENSIONS; i++) {
                vector[i] /= norm;
            }
        }
        return vector;
    }
}
