package com.adaptiverag.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RouteDecisionTest {

    @Test
    void parsesCleanAndNoisyReplies() {
        assertThat(RouteDecision.fromLlmReply("WEB")).isEqualTo(RouteDecision.WEB);
        assertThat(RouteDecision.fromLlmReply("  general\n")).isEqualTo(RouteDecision.GENERAL);
        assertThat(RouteDecision.fromLlmReply("Category: **DOCUMENT**.")).isEqualTo(RouteDecision.DOCUMENT);
    }

    @Test
    void picksTheFirstRouteMentioned() {
        assertThat(RouteDecision.fromLlmReply("GENERAL, not WEB")).isEqualTo(RouteDecision.GENERAL);
    }

    @Test
    void unparseableReplyFallsBack() {
        assertThat(RouteDecision.fromLlmReply("I'm not sure")).isEqualTo(RouteDecision.FALLBACK);
        assertThat(RouteDecision.fromLlmReply(null)).isEqualTo(RouteDecision.FALLBACK);
    }
}
