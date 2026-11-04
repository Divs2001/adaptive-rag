package com.adaptiverag.service;

import com.adaptiverag.model.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Turns a follow-up ("who was its captain?") into a standalone question using recent history,
 * so routing, vector search and web search all receive a complete question.
 */
@Service
public class QueryRewriterService {

    private static final Logger log = LoggerFactory.getLogger(QueryRewriterService.class);

    private static final String REWRITE_PROMPT = """
            Rewrite the follow-up question so it can be understood on its own, without the conversation.

            Rules:
            - Only change the follow-up if it contains a reference to the conversation, such as
              "it", "he", "they", "their", "that team", "the one before" or "what about ...".
            - Replace only that reference with what it refers to. Never add any other details
              from the conversation.
            - If the follow-up already makes sense on its own, return it EXACTLY unchanged, even if
              the conversation was about something else. Questions about "the document", "my file"
              or "the PDF" are always standalone.
            - If the conversation was about one of the user's documents (CV, resume, PDF, report) and
              the follow-up refers to its contents ("them", "those", "that", "in more detail"), keep
              that document in the rewritten question, e.g. "... from my CV".
            - Do NOT answer the question. Return only the question, nothing else.

            Examples (conversation about who won the 2018 FIFA World Cup, answered "France"):
            Follow-up: Who was their coach?
            Standalone question: Who was the coach of France's 2018 FIFA World Cup team?
            Follow-up: What about the one before that?
            Standalone question: Who won the 2014 FIFA World Cup?
            Follow-up: What does my resume say about my Java experience?
            Standalone question: What does my resume say about my Java experience?
            Follow-up: What is the capital of Japan?
            Standalone question: What is the capital of Japan?

            Examples (conversation about the projects listed in the user's resume):
            Follow-up: Sort them by year
            Standalone question: List the projects from my resume sorted by year
            Follow-up: Can you explain those in more detail?
            Standalone question: Explain the projects listed in my resume in more detail

            Now the real conversation:
            <conversation>
            {history}
            </conversation>

            Follow-up question: {question}
            Standalone question:
            """;

    // Long answers add little for resolving references but slow the prompt down
    private static final int MAX_MESSAGE_CHARS = 500;

    private final ChatClient chatClient;

    public QueryRewriterService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
                .defaultOptions(ChatOptions.builder().temperature(0.0))
                .build();
    }

    public String rewrite(List<ChatMessage> history, String question) {
        String transcript = history.stream()
                .map(message -> message.role() + ": " + abbreviate(message.content()))
                .collect(Collectors.joining("\n"));

        String reply = chatClient.prompt()
                .user(user -> user.text(REWRITE_PROMPT)
                        .param("history", transcript)
                        .param("question", question))
                .call()
                .content();

        String standalone = clean(reply);
        if (standalone.isBlank()) {
            return question;
        }
        log.info("Rewrote '{}' -> '{}'", question, standalone);
        return standalone;
    }

    /** Small models sometimes add a label, quotes or a second line; keep just the question. */
    static String clean(String reply) {
        if (reply == null) {
            return "";
        }
        String firstLine = reply.strip().lines().findFirst().orElse("").strip();
        if (firstLine.toLowerCase().startsWith("standalone question:")) {
            firstLine = firstLine.substring("standalone question:".length()).strip();
        }
        if (firstLine.length() >= 2 && firstLine.startsWith("\"") && firstLine.endsWith("\"")) {
            firstLine = firstLine.substring(1, firstLine.length() - 1).strip();
        }
        return firstLine;
    }

    private static String abbreviate(String text) {
        return text.length() <= MAX_MESSAGE_CHARS ? text : text.substring(0, MAX_MESSAGE_CHARS) + "...";
    }
}
