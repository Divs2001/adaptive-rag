package com.adaptiverag.controller;

import com.adaptiverag.model.IngestionResult;
import com.adaptiverag.model.QueryRequest;
import com.adaptiverag.model.QueryResponse;
import com.adaptiverag.service.DocumentService;
import com.adaptiverag.service.RagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/rag")
public class RagController {

    private final RagService ragService;
    private final DocumentService documentService;

    public RagController(RagService ragService, DocumentService documentService) {
        this.ragService = ragService;
        this.documentService = documentService;
    }

    @PostMapping(value = "/documents/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionResult upload(@RequestParam("file") MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (file.isEmpty() || filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "a non-empty .pdf file is required");
        }
        return documentService.ingest(file.getResource(), filename);
    }

    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        if (request.query() == null || request.query().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank");
        }
        return ragService.answer(request.query());
    }
}
