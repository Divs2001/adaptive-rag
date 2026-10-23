package com.adaptiverag.controller;

import com.adaptiverag.model.QueryRequest;
import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.service.GenerationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final GenerationService generationService;

    public RagController(GenerationService generationService) {
        this.generationService = generationService;
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank");
        }
        return new QueryResponse(generationService.generate(request.query()));
    }
}
