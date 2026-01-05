package uy.com.geocom.retail;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Geo Retail Analytics Application.
 *
 * A conversational analytics system for retail data using LLM with tool calling.
 * Supports dual LLM configuration:
 * - dev profile: Uses Grok (xAI) free tier
 * - prod profile: Uses Ollama local LLM
 */
@SpringBootApplication
public class RetailAnalyticsApp {

    public static void main(String[] args) {
        SpringApplication.run(RetailAnalyticsApp.class, args);
    }
}
