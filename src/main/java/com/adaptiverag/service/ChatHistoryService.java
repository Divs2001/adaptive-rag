package com.adaptiverag.service;

import com.adaptiverag.model.ChatMessage;
import com.adaptiverag.model.RouteDecision;
import com.adaptiverag.repository.ChatHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatHistoryService {

    private final ChatHistoryRepository repository;
    private final int maxMessages;

    public ChatHistoryService(ChatHistoryRepository repository,
                              @Value("${rag.chat-history.max-messages:6}") int maxMessages) {
        this.repository = repository;
        this.maxMessages = maxMessages;
    }

    /** Full conversation, oldest first. */
    public List<ChatMessage> all(String sessionId) {
        return repository.findBySessionIdOrderByTimestampAsc(sessionId);
    }

    /**
     * The last few messages, oldest first. Only a window is used for prompts so a long
     * conversation can't grow the prompt without limit.
     */
    public List<ChatMessage> recent(String sessionId) {
        List<ChatMessage> newestFirst = new ArrayList<>(
                repository.findBySessionIdOrderByTimestampDesc(sessionId, PageRequest.of(0, maxMessages)));
        return newestFirst.reversed();
    }

    public void saveExchange(String sessionId, String question, String answer, RouteDecision route) {
        repository.saveAll(List.of(
                ChatMessage.user(sessionId, question),
                ChatMessage.assistant(sessionId, answer, route)));
    }
}
