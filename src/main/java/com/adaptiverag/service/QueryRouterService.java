package com.adaptiverag.service;

import com.adaptiverag.model.RouteDecision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

/**
 * Classifies a query before any retrieval happens, so each query only pays
 * for the source it actually needs.
 */
@Service
public class QueryRouterService {

    private static final Logger log = LoggerFactory.getLogger(QueryRouterService.class);

    private static final String ROUTER_PROMPT = """
            You are the query router of a question-answering system.
            Classify the user's query into exactly one category:

            DOCUMENT - about the user's own uploaded files (resumes, reports, notes, manuals, plans),
                       or phrased like "my ...", "the document", "the PDF", "according to the file".
            GENERAL  - general knowledge, concepts, definitions, explanations, math or coding help
                       that a knowledgeable assistant can answer without looking anything up.
            WEB      - needs current or live information: news, today's weather, prices, scores,
                       latest versions or releases, or events after 2023.

            Examples:
            "What does my resume say about my Kafka experience?" = DOCUMENT
            "Summarize the uploaded project plan" = DOCUMENT
            "Explain how a hash map works" = GENERAL
            "What is 15 percent of 240?" = GENERAL
            "What is the weather in Jaipur today?" = WEB
            "Who won the most recent IPL final?" = WEB

            Respond with exactly one word: DOCUMENT, GENERAL, or WEB.

            Query: {query}
            """;

    private final ChatClient chatClient;

    public QueryRouterService(ChatClient.Builder chatClientBuilder) {
        // Temperature 0: classification should be deterministic, not creative
        this.chatClient = chatClientBuilder
                .defaultOptions(ChatOptions.builder().temperature(0.0))
                .build();
    }

    public RouteDecision route(String query) {
        String reply = chatClient.prompt()
                .user(user -> user.text(ROUTER_PROMPT).param("query", query))
                .call()
                .content();
        RouteDecision decision = RouteDecision.fromLlmReply(reply);
        log.info("Routed '{}' -> {} (raw reply: '{}')", query, decision, reply);
        return decision;
    }
}
