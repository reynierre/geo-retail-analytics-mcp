package com.geocom.retail.application.port;

import reactor.core.publisher.Flux;

/**
 * Port for LLM integration.
 * Defined in Application layer, implemented in Infrastructure.
 * Allows switching between Grok (dev) and Ollama (prod).
 */
public interface LlmPort {

    /**
     * Stream response from LLM with tool calling support.
     *
     * @param prompt User prompt
     * @param tools  Tools available to the LLM
     * @return Stream of response chunks
     */
    Flux<String> streamResponse(String prompt, Object tools);

    /**
     * Get complete response from LLM with tool calling support.
     *
     * @param prompt User prompt
     * @param tools  Tools available to the LLM
     * @return Complete response
     */
    String getResponse(String prompt, Object tools);
}
