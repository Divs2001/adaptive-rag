package com.adaptiverag.service;

import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.model.RouteDecision;
import com.adaptiverag.service.handler.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Routes each query first, then dispatches it to the handler for that route. */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final QueryRouterService router;
    private final Map<RouteDecision, QueryHandler> handlers = new EnumMap<>(RouteDecision.class);

    public RagService(QueryRouterService router, List<QueryHandler> handlers) {
        this.router = router;
        // Spring injects every QueryHandler bean; index them by the route they serve
        handlers.forEach(handler -> this.handlers.put(handler.route(), handler));
        if (!this.handlers.containsKey(RouteDecision.FALLBACK)) {
            throw new IllegalStateException("No QueryHandler registered for fallback route " + RouteDecision.FALLBACK);
        }
    }

    public QueryResponse answer(String query) {
        RouteDecision decision = router.route(query);
        QueryHandler handler = handlers.get(decision);
        if (handler == null) {
            log.warn("No handler for route {} yet, using {}", decision, RouteDecision.FALLBACK);
            handler = handlers.get(RouteDecision.FALLBACK);
        }
        return handler.handle(query);
    }
}
