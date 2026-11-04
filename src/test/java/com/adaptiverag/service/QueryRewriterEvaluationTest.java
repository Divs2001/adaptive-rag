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

    private static final List<ChatMessage> CV_HISTORY = List.of(
            ChatMessage.user("s2", "Tell me about my experience from the CV"),
            ChatMessage.assistant("s2", "According to your CV, you work as a Software Engineer at Acme Corp "
                    + "since 2023, and before that you were an intern at Beta Labs.", RouteDecision.DOCUMENT));

    @Autowired
    private QueryRewriterService rewriter;

    @Test
    void followUpGetsTheMissingContext() {
        String rewritten = rewriter.rewrite(FOOTBALL_HISTORY, "Who was the captain of that team?");

        assertThat(rewritten).containsIgnoringCase("Argentina");
    }

    @Test
    void documentFollowUpsKeepTheDocumentReference() {
        List<String> followUps = List.of(
                "tell them in ascending order of the year",
                "Can you tell me about those in more detail?");

        for (String followUp : followUps) {
            String rewritten = rewriter.rewrite(CV_HISTORY, followUp);
            System.out.printf("'%s' -> '%s'%n", followUp, rewritten);

            // Keeping "CV"/"resume" lets the router pick DOCUMENT and gives the vector search more to match
            assertThat(rewritten).containsPattern("(?i)\\b(cv|resume)\\b");
        }
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
