package com.adaptiverag.service;

import com.adaptiverag.model.IngestionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final VectorStore vectorStore;
    private final TextChunker textChunker;
    private final int topK;
    private final double similarityThreshold;

    public DocumentService(VectorStore vectorStore,
                           TextChunker textChunker,
                           @Value("${rag.retrieval.top-k:4}") int topK,
                           @Value("${rag.retrieval.similarity-threshold:0.5}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.textChunker = textChunker;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * Embeds the query and returns the most similar chunks, dropping any below the
     * similarity threshold so unrelated questions come back empty instead of noisy.
     */
    public List<Document> search(String query) {
        List<Document> chunks = vectorStore.similaritySearch(SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .build());
        log.info("Retrieved {} chunks for query '{}'", chunks.size(), query);
        return chunks;
    }

    public IngestionResult ingest(Resource pdf, String filename) {
        List<Document> pages = new PagePdfDocumentReader(pdf).get();

        List<Document> chunks = new ArrayList<>();
        for (int pageIndex = 0; pageIndex < pages.size(); pageIndex++) {
            Document page = pages.get(pageIndex);
            // Chunk page by page so every chunk can cite the exact page it came from
            Object pageNumber = page.getMetadata()
                    .getOrDefault(PagePdfDocumentReader.METADATA_START_PAGE_NUMBER, pageIndex + 1);

            List<String> pieces = textChunker.chunk(page.getText());
            for (int i = 0; i < pieces.size(); i++) {
                chunks.add(new Document(pieces.get(i), Map.of(
                        "filename", filename,
                        "page", pageNumber,
                        "chunkIndex", i)));
            }
        }

        // Embeds each chunk via the EmbeddingModel, then upserts vector + text + metadata into Qdrant
        if (!chunks.isEmpty()) {
            vectorStore.add(chunks);
        }
        log.info("Ingested '{}': {} pages -> {} chunks", filename, pages.size(), chunks.size());
        return new IngestionResult(filename, pages.size(), chunks.size());
    }
}
