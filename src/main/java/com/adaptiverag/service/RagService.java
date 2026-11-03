package com.adaptiverag.service;

import com.adaptiverag.model.ChatMessage;
import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.model.RouteDecision;
import com.adaptiverag.service.handler.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Routes each query first, then dispatches it to the handler for that route. */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final QueryRouterService router;
    private final ChatHistoryService chatHistory;
    private final QueryRewriterService queryRewriter;
    private final Map<RouteDecision, QueryHandler> handlers = new EnumMap<>(RouteDecision.class);

    public RagService(QueryRouterService router,
                      ChatHistoryService chatHistory,
                      QueryRewriterService queryRewriter,
                      List<QueryHandler> handlers) {
        this.router = router;
        this.chatHistory = chatHistory;
        this.queryRewriter = queryRewriter;
        // Spring injects every QueryHandler bean; index them by the route they serve
        handlers.forEach(handler -> this.handlers.put(handler.route(), handler));
        if (!this.handlers.containsKey(RouteDecision.FALLBACK)) {
            throw new IllegalStateException("No QueryHandler registered for fallback route " + RouteDecision.FALLBACK);
        }
    }

    public QueryResponse answer(String query, String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        // Read before saving this turn, so history holds only earlier messages
        List<ChatMessage> history = chatHistory.recent(sessionId);
        String standalone = history.isEmpty() ? query : queryRewriter.rewrite(history, query);

        RouteDecision decision = router.route(standalone);
        QueryHandler handler = handlers.get(decision);
        if (handler == null) {
            log.warn("No handler for route {} yet, using {}", decision, RouteDecision.FALLBACK);
            handler = handlers.get(RouteDecision.FALLBACK);
        }
        QueryResponse response = handler.handle(standalone);

        // Store what the user actually typed; saved only after a successful answer,
        // so history never holds a question without its reply
        chatHistory.saveExchange(sessionId, query, response.answer(), response.route());
        return response.withSessionId(sessionId);
    }
}
