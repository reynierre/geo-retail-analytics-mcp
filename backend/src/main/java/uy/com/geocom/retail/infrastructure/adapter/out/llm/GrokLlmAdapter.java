package uy.com.geocom.retail.infrastructure.adapter.out.llm;

import uy.com.geocom.retail.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * LLM adapter for Grok (xAI).
 * Uses OpenAI-compatible API.
 * Active in 'dev' profile for free tier development.
 */
@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class GrokLlmAdapter implements LlmPort {

    private final ChatClient chatClient;

    @Override
    public Flux<String> streamResponse(String prompt, Object tools) {
        log.debug("Streaming response from Grok for prompt: {}", truncate(prompt));

        return chatClient.prompt()
            .user(prompt)
            .tools(tools)
            .stream()
            .content()
            .doOnSubscribe(s -> log.debug("Started streaming from Grok"))
            .doOnComplete(() -> log.debug("Completed streaming from Grok"))
            .doOnError(e -> log.error("Error streaming from Grok: {}", e.getMessage()));
    }

    @Override
    public String getResponse(String prompt, Object tools) {
        log.debug("Getting response from Grok for prompt: {}", truncate(prompt));

        String response = chatClient.prompt()
            .user(prompt)
            .tools(tools)
            .call()
            .content();

        log.debug("Received response from Grok: {} chars", response != null ? response.length() : 0);
        return response;
    }

    private String truncate(String text) {
        return text != null && text.length() > 100 ? text.substring(0, 100) + "..." : text;
    }
}
