package com.adaptiverag.service;

import com.adaptiverag.model.WebResult;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Live web search via the Tavily API, which returns page content already extracted for LLM use. */
@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private final RestClient restClient;
    private final String apiKey;
    private final int maxResults;

    public WebSearchService(@Value("${rag.web-search.tavily.base-url}") String baseUrl,
                            @Value("${rag.web-search.tavily.api-key:}") String apiKey,
                            @Value("${rag.web-search.max-results:5}") int maxResults) {
        // JDK HttpClient with explicit timeouts, so a slow or unreachable API can't hang a request
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(20));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.apiKey = apiKey;
        this.maxResults = maxResults;
        if (apiKey.isBlank()) {
            log.warn("TAVILY_API_KEY is not set; the WEB route will be unavailable");
        }
    }

    public List<WebResult> search(String query) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("TAVILY_API_KEY is not set");
        }
        TavilyResponse response = restClient.post()
                .uri("/search")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("query", query, "max_results", maxResults))
                .retrieve()
                .body(TavilyResponse.class);

        List<WebResult> results = response == null || response.results() == null ? List.of() : response.results();
        log.info("Tavily returned {} results for '{}'", results.size(), query);
        return results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TavilyResponse(List<WebResult> results) {
    }
}
