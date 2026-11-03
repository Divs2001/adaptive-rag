package com.adaptiverag.service;

import com.adaptiverag.model.ChatMessage;
import com.adaptiverag.model.RouteDecision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the rewriter against the real local LLM: follow-ups must gain the missing context,
 * and standalone questions must not pick up unrelated context from the conversation.
 */
@SpringBootTest
class QueryRewriterEvaluationTest {

    private static final List<ChatMessage> FOOTBALL_HISTORY = List.of(
            ChatMessage.user("s1", "Who won the 2022 FIFA World Cup?"),
            ChatMessage.assistant("s1", "Argentina won the 2022 FIFA World Cup, beating France on penalties.",
                    RouteDecision.GENERAL));

    @Autowired
    private QueryRewriterService rewriter;

    @Test
    void followUpGetsTheMissingContext() {
        String rewritten = rewriter.rewrite(FOOTBALL_HISTORY, "Who was the captain of that team?");

        assertThat(rewritten).containsIgnoringCase("Argentina");
    }

    @Test
    void standaloneQuestionsDoNotPickUpUnrelatedContext() {
        List<String> standaloneQuestions = List.of(
                "Can you tell me about the notice period information from the document?",
                "What does my uploaded PDF say about leave policy?",
                "Explain what a hash map is");

        for (String question : standaloneQuestions) {
            String rewritten = rewriter.rewrite(FOOTBALL_HISTORY, question);
            System.out.printf("'%s' -> '%s'%n", question, rewritten);

            assertThat(rewritten)
                    .doesNotContainIgnoringCase("FIFA")
                    .doesNotContainIgnoringCase("World Cup")
                    .doesNotContainIgnoringCase("Argentina");
        }
    }
}
