package uy.com.geocom.retail.application.service;

import uy.com.geocom.retail.application.dto.ChatResponse;
import uy.com.geocom.retail.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Application service for handling chat interactions.
 * Orchestrates between LLM and Tools.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final LlmPort llmPort;
    private final Object tools;  // Injected RetailAnalyticsTools

    public ChatService(LlmPort llmPort,
                       @org.springframework.beans.factory.annotation.Qualifier("retailAnalyticsTools") Object tools) {
        this.llmPort = llmPort;
        this.tools = tools;
    }

    /**
     * Stream chat response with tool calling.
     */
    public Flux<String> streamChat(String message) {
        log.info("Processing streaming chat message: {}", message);
        return llmPort.streamResponse(message, tools);
    }

    /**
     * Get complete chat response with tool calling.
     */
    public Mono<ChatResponse> chat(String message) {
        log.info("Processing sync chat message: {}", message);
        String response = llmPort.getResponse(message, tools);
        return Mono.just(new ChatResponse(response));
    }
}
