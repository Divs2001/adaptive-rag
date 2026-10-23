package com.adaptiverag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class GenerationService {

    private static final String SYSTEM_PROMPT = """
            You are a helpful assistant. Answer clearly and concisely.
            If you don't know the answer, say so instead of guessing.
            """;

    private final ChatClient chatClient;

    public GenerationService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    public String generate(String query) {
        return chatClient.prompt()
                .user(query)
                .call()
                .content();
    }
}
