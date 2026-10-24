package com.adaptiverag.model;

import org.springframework.ai.document.Document;

public record Source(String filename, int page, Double score) {

    public static Source from(Document chunk) {
        Object page = chunk.getMetadata().get("page");
        return new Source(
                String.valueOf(chunk.getMetadata().get("filename")),
                page instanceof Number number ? number.intValue() : 0,
                chunk.getScore());
    }
}
