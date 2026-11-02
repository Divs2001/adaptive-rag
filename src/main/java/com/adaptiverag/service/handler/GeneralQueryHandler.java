package com.adaptiverag.service.handler;

import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.model.RouteDecision;
import com.adaptiverag.service.GenerationService;
import org.springframework.stereotype.Component;

import java.util.List;

/** Answers straight from the model's own knowledge: no retrieval, no sources. */
@Component
public class GeneralQueryHandler implements QueryHandler {

    private final GenerationService generationService;

    public GeneralQueryHandler(GenerationService generationService) {
        this.generationService = generationService;
    }

    @Override
    public RouteDecision route() {
        return RouteDecision.GENERAL;
    }

    @Override
    public QueryResponse handle(String query) {
        return new QueryResponse(route(), generationService.generate(query), List.of());
    }
}
