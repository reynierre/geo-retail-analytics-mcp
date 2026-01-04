package com.geocom.retail.infrastructure.adapter.in.web;

import com.geocom.retail.application.dto.ChatRequest;
import com.geocom.retail.application.dto.ChatResponse;
import com.geocom.retail.application.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * REST Controller for chat endpoints.
 * Supports both streaming (SSE) and synchronous responses.
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatService chatService;

    /**
     * Stream chat response using Server-Sent Events.
     * Used for real-time streaming of LLM responses.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody @Valid ChatRequest request) {
        log.info("Received streaming chat request: {}", truncate(request.message()));

        return chatService.streamChat(request.message())
            .map(chunk -> ServerSentEvent.<String>builder()
                .data(chunk)
                .build())
            .doOnSubscribe(s -> log.debug("Client connected to stream"))
            .doOnComplete(() -> log.debug("Stream completed"))
            .doOnError(e -> log.error("Stream error: {}", e.getMessage()));
    }

    /**
     * Synchronous chat endpoint.
     * Waits for complete response before returning.
     */
    @PostMapping
    public Mono<ChatResponse> chat(@RequestBody @Valid ChatRequest request) {
        log.info("Received sync chat request: {}", truncate(request.message()));

        return chatService.chat(request.message());
    }

    private String truncate(String text) {
        return text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
