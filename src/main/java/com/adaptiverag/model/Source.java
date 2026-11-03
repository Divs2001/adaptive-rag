package com.adaptiverag.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.ai.document.Document;

/** Where part of an answer came from: a PDF page (title + page) or a web page (title + url). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Source(String title, Integer page, String url, Double score) {

    public static Source from(Document chunk) {
        Object page = chunk.getMetadata().get("page");
        return new Source(
                String.valueOf(chunk.getMetadata().get("filename")),
                page instanceof Number number ? number.intValue() : null,
                null,
                chunk.getScore());
    }

    public static Source from(WebResult result) {
        return new Source(result.title(), null, result.url(), result.score());
    }
}
