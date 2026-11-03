package com.adaptiverag.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** One Tavily search hit; field names match Tavily's JSON so it deserializes directly. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WebResult(String title, String url, String content, Double score) {
}
