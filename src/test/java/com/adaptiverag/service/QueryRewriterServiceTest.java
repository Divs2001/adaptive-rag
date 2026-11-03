package com.adaptiverag.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QueryRewriterServiceTest {

    @Test
    void keepsACleanReplyAsIs() {
        assertThat(QueryRewriterService.clean("Who captained Argentina in 2022?"))
                .isEqualTo("Who captained Argentina in 2022?");
    }

    @Test
    void stripsLabelQuotesAndExtraLines() {
        assertThat(QueryRewriterService.clean("Standalone question: \"Who captained Argentina in 2022?\"\nThis resolves 'that team'."))
                .isEqualTo("Who captained Argentina in 2022?");
    }

    @Test
    void emptyOrMissingReplyBecomesBlank() {
        assertThat(QueryRewriterService.clean(null)).isEmpty();
        assertThat(QueryRewriterService.clean("   \n ")).isEmpty();
    }
}
