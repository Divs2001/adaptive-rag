package com.adaptiverag.model;

public enum RouteDecision {
    DOCUMENT,
    GENERAL,
    WEB;

    /** Route used when the LLM reply can't be parsed: grounded and safe, it refuses rather than guesses. */
    public static final RouteDecision FALLBACK = DOCUMENT;

    /**
     * LLMs don't always obey "one word only" (e.g. "Category: WEB." or "**GENERAL**"),
     * so pick the route name that appears first in the reply instead of requiring an exact match.
     */
    public static RouteDecision fromLlmReply(String reply) {
        if (reply == null) {
            return FALLBACK;
        }
        String upper = reply.toUpperCase();
        RouteDecision first = FALLBACK;
        int firstIndex = Integer.MAX_VALUE;
        for (RouteDecision decision : values()) {
            int index = upper.indexOf(decision.name());
            if (index >= 0 && index < firstIndex) {
                first = decision;
                firstIndex = index;
            }
        }
        return first;
    }
}
