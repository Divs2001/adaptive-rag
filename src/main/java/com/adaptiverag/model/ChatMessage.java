package com.adaptiverag.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** One turn of a conversation, stored in the chat_messages collection. */
@Document(collection = "chat_messages")
public record ChatMessage(
        @Id String id,
        @Indexed String sessionId,
        Role role,
        String content,
        RouteDecision route,
        Instant timestamp) {

    public enum Role { USER, ASSISTANT }

    public static ChatMessage user(String sessionId, String content) {
        return new ChatMessage(null, sessionId, Role.USER, content, null, Instant.now());
    }

    public static ChatMessage assistant(String sessionId, String content, RouteDecision route) {
        return new ChatMessage(null, sessionId, Role.ASSISTANT, content, route, Instant.now());
    }
}
