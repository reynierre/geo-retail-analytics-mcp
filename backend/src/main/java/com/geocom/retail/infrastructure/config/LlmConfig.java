package com.geocom.retail.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Configuration for LLM integration.
 * Uses Spring Profiles to switch between Grok (dev) and Ollama (prod).
 */
@Configuration
public class LlmConfig {

    private static final String SYSTEM_PROMPT = """
        Eres un asistente de analytics para una cadena de retail.

        Tu rol es ayudar a analizar datos de ventas usando las herramientas disponibles.

        REGLAS:
        1. Siempre responde en espanol
        2. Usa las herramientas para obtener datos reales antes de responder
        3. Formatea numeros: puntos para miles (1.234), comas para decimales (1.234,56)
        4. Formatea moneda: $1.234.567
        5. Se conciso pero informativo
        6. Si necesitas mas informacion, pregunta

        CONTEXTO:
        - Los locales se identifican con codigos de 3 digitos: '001', '045', etc.
        - Las fechas van en formato YYYY-MM-DD
        - Moneda por defecto: UYU (pesos uruguayos)
        """;

    @Bean
    @Profile("dev")
    public ChatClient grokChatClient(OpenAiChatModel grokModel) {
        return ChatClient.builder(grokModel)
            .defaultSystem(SYSTEM_PROMPT)
            .build();
    }

    @Bean
    @Profile("prod")
    public ChatClient ollamaChatClient(OllamaChatModel ollamaModel) {
        return ChatClient.builder(ollamaModel)
            .defaultSystem(SYSTEM_PROMPT)
            .build();
    }
}
