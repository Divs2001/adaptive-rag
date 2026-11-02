package com.adaptiverag.service.handler;

import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.model.RouteDecision;
import com.adaptiverag.model.Source;
import com.adaptiverag.service.DocumentService;
import com.adaptiverag.service.GenerationService;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DocumentQueryHandler implements QueryHandler {

    private static final String NO_RELEVANT_DOCUMENTS =
            "I couldn't find anything relevant to that in the uploaded documents.";

    private final DocumentService documentService;
    private final GenerationService generationService;

    public DocumentQueryHandler(DocumentService documentService, GenerationService generationService) {
        this.documentService = documentService;
        this.generationService = generationService;
    }

    @Override
    public RouteDecision route() {
        return RouteDecision.DOCUMENT;
    }

    @Override
    public QueryResponse handle(String query) {
        List<Document> chunks = documentService.search(query);
        if (chunks.isEmpty()) {
            // Nothing relevant in the documents: skip the LLM call rather than let it guess
            return new QueryResponse(route(), NO_RELEVANT_DOCUMENTS, List.of());
        }
        String answer = generationService.generateFromContext(query, chunks);
        return new QueryResponse(route(), answer, chunks.stream().map(Source::from).toList());
    }
}
