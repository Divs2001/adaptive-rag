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
                       Any question about the user's personal details (my experience, my skills,
                       my projects, my education, my salary, my notice period) is DOCUMENT, because
                       those can only be known from the user's files.
            GENERAL  - general knowledge, concepts, definitions, explanations, math or coding help,
                       and historical facts or past events from before 2024 (including past results
                       of sports events or elections), answerable without looking anything up.
            WEB      - needs current or live information: news, today's weather, prices, live scores,
                       latest versions or releases, or events from 2024 onwards.

            Examples:
            "What does my resume say about my Kafka experience?" = DOCUMENT
            "Summarize the uploaded project plan" = DOCUMENT
            "How many years of experience do I have?" = DOCUMENT
            "Explain how a hash map works" = GENERAL
            "What is 15 percent of 240?" = GENERAL
            "Who won the 2011 Cricket World Cup?" = GENERAL
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
