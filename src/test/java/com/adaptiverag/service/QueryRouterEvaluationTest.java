package com.adaptiverag.service;

import com.adaptiverag.model.RouteDecision;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.adaptiverag.model.RouteDecision.DOCUMENT;
import static com.adaptiverag.model.RouteDecision.GENERAL;
import static com.adaptiverag.model.RouteDecision.WEB;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluates the router against labelled queries using the real local LLM.
 * Queries are deliberately different from the few-shot examples in the prompt,
 * so this measures generalisation rather than memorisation.
 */
@SpringBootTest
class QueryRouterEvaluationTest {

    private static final Map<String, RouteDecision> LABELLED_QUERIES = new LinkedHashMap<>();

    static {
        LABELLED_QUERIES.put("What skills are listed in my uploaded resume?", DOCUMENT);
        LABELLED_QUERIES.put("According to the PDF, what are the day 2 tasks?", DOCUMENT);
        LABELLED_QUERIES.put("Which tech stack does the project plan document mention?", DOCUMENT);
        LABELLED_QUERIES.put("Tell me about my work experience in detail", DOCUMENT);
        LABELLED_QUERIES.put("List my jobs in ascending order of the year", DOCUMENT);
        LABELLED_QUERIES.put("What is the difference between TCP and UDP?", GENERAL);
        LABELLED_QUERIES.put("Write a Java method to reverse a string", GENERAL);
        LABELLED_QUERIES.put("Why is the sky blue?", GENERAL);
        LABELLED_QUERIES.put("Who won the 2022 FIFA World Cup?", GENERAL);
        LABELLED_QUERIES.put("In which year did the Berlin Wall fall?", GENERAL);
        LABELLED_QUERIES.put("What is the current price of Bitcoin?", WEB);
        LABELLED_QUERIES.put("What are today's top technology news headlines?", WEB);
        LABELLED_QUERIES.put("What is the latest stable version of Spring Boot?", WEB);
        LABELLED_QUERIES.put("Who won yesterday's Premier League match?", WEB);
    }

    @Autowired
    private QueryRouterService router;

    @Test
    void routesLabelledQueriesCorrectly() {
        int correct = 0;
        for (Map.Entry<String, RouteDecision> entry : LABELLED_QUERIES.entrySet()) {
            RouteDecision actual = router.route(entry.getKey());
            boolean ok = actual == entry.getValue();
            if (ok) {
                correct++;
            }
            System.out.printf("%s expected=%-8s actual=%-8s %s%n",
                    ok ? "PASS" : "FAIL", entry.getValue(), actual, entry.getKey());
        }
        double accuracy = (double) correct / LABELLED_QUERIES.size();
        System.out.printf("Routing accuracy: %d/%d (%.0f%%)%n", correct, LABELLED_QUERIES.size(), accuracy * 100);

        // A small local model won't be perfect; fail only if routing is clearly broken
        assertThat(accuracy).isGreaterThanOrEqualTo(0.75);
    }
}
