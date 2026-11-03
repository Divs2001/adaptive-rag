package com.adaptiverag.service.handler;

import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.model.RouteDecision;
import com.adaptiverag.model.Source;
import com.adaptiverag.model.WebResult;
import com.adaptiverag.service.GenerationService;
import com.adaptiverag.service.WebSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Component
public class WebSearchQueryHandler implements QueryHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSearchQueryHandler.class);

    private static final String NO_RESULTS = "I couldn't find anything relevant on the web for that.";
    private static final String SEARCH_UNAVAILABLE = "Web search is unavailable right now, so I can't answer that reliably.";

    private final WebSearchService webSearchService;
    private final GenerationService generationService;

    public WebSearchQueryHandler(WebSearchService webSearchService, GenerationService generationService) {
        this.webSearchService = webSearchService;
        this.generationService = generationService;
    }

    @Override
    public RouteDecision route() {
        return RouteDecision.WEB;
    }

    @Override
    public QueryResponse handle(String query) {
        List<WebResult> results;
        try {
            results = webSearchService.search(query);
        } catch (RestClientException | IllegalStateException e) {
            // Don't fall back to the LLM's own knowledge: WEB queries are exactly the ones it would get wrong
            log.error("Web search failed for '{}'", query, e);
            return new QueryResponse(route(), SEARCH_UNAVAILABLE, List.of());
        }
        if (results.isEmpty()) {
            return new QueryResponse(route(), NO_RESULTS, List.of());
        }
        String answer = generationService.generateFromWebResults(query, results);
        return new QueryResponse(route(), answer, results.stream().map(Source::from).toList());
    }
}
