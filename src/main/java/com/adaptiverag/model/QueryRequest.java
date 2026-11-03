package com.adaptiverag.model;

/** sessionId is optional: omit it to start a new conversation, send it back to continue one. */
public record QueryRequest(String query, String sessionId) {
}
