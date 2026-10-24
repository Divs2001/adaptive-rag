package com.adaptiverag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Talks to the real local Ollama (nomic-embed-text), so Ollama must be running.
 */
@SpringBootTest
class EmbeddingSanityTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void embeddingsHaveExpectedDimensionAndCaptureMeaning() {
        float[] dog = embeddingModel.embed("The dog is playing fetch in the park");
        float[] puppy = embeddingModel.embed("A puppy chases a ball on the grass");
        float[] tax = embeddingModel.embed("Quarterly corporate tax filing deadlines");

        assertThat(dog).hasSize(768);

        double related = cosineSimilarity(dog, puppy);
        double unrelated = cosineSimilarity(dog, tax);
        System.out.printf("dog vs puppy: %.3f | dog vs tax: %.3f%n", related, unrelated);

        assertThat(related).isGreaterThan(unrelated);
    }

    private static double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
