package com.adaptiverag.model;

import java.util.List;

public record QueryResponse(String sessionId, RouteDecision route, String answer, List<Source> sources) {

    /** Handlers don't know about sessions; RagService attaches the sessionId afterwards. */
    public QueryResponse(RouteDecision route, String answer, List<Source> sources) {
        this(null, route, answer, sources);
    }

    public QueryResponse withSessionId(String sessionId) {
        return new QueryResponse(sessionId, route, answer, sources);
    }
}
