package com.adaptiverag.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TextChunkerTest {

    @Test
    void blankTextProducesNoChunks() {
        TextChunker chunker = new TextChunker(5, 2);

        assertThat(chunker.chunk("   ")).isEmpty();
        assertThat(chunker.chunk(null)).isEmpty();
    }

    @Test
    void textShorterThanChunkSizeIsASingleChunk() {
        TextChunker chunker = new TextChunker(5, 2);

        assertThat(chunker.chunk("one two  three\n")).containsExactly("one two three");
    }

    @Test
    void consecutiveChunksShareOverlapWords() {
        TextChunker chunker = new TextChunker(5, 2);

        assertThat(chunker.chunk("w1 w2 w3 w4 w5 w6 w7 w8 w9 w10 w11 w12"))
                .containsExactly(
                        "w1 w2 w3 w4 w5",
                        "w4 w5 w6 w7 w8",
                        "w7 w8 w9 w10 w11",
                        "w10 w11 w12");
    }

    @Test
    void overlapMustBeSmallerThanChunkSize() {
        assertThatThrownBy(() -> new TextChunker(5, 5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
