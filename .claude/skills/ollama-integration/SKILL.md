# Ollama Integration Skill (Production LLM)

## Overview

This skill provides knowledge for setting up and integrating **Ollama** as the **production LLM**. Ollama runs locally, keeping all data on-premise for security and compliance.

## LLM Strategy

| Environment | LLM | Reason |
|-------------|-----|--------|
| Development | Grok (xAI) | Free tier, no local setup needed |
| MVP Demo | Grok (xAI) | Free, reliable for demos |
| **Production** | **Ollama** | **Local, data privacy, no API costs** |

## Why Ollama for Production?

1. **Data Privacy**: Production data never leaves your servers
2. **No API Costs**: Unlimited queries after hardware investment
3. **Low Latency**: No network roundtrips to external APIs
4. **Compliance**: Meets data residency and privacy requirements
5. **Reliability**: No external service dependencies
6. **Security**: Sensitive retail data stays on-premise

## Installation

### Linux / macOS

```bash
# Install Ollama
curl -fsSL https://ollama.com/install.sh | sh

# Verify installation
ollama --version

# Start Ollama server (runs on port 11434)
ollama serve

# In another terminal, pull recommended model
ollama pull llama3.1:8b
```

### Windows

```powershell
# Download from https://ollama.com/download/windows
# Run installer
# Open terminal and verify:
ollama --version
```

### Docker (Recommended for Production)

```yaml
# docker-compose.prod.yml
services:
  ollama:
    image: ollama/ollama:latest
    ports:
      - "11434:11434"
    volumes:
      - ollama_data:/root/.ollama
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: all
              capabilities: [gpu]  # GPU support for faster inference
    restart: unless-stopped

volumes:
  ollama_data:
```

## Recommended Models for Production

| Model | Size | RAM Required | Tool Calling | Use Case |
|-------|------|--------------|--------------|----------|
| **llama3.1:8b** | 4.7GB | 8GB | Native | Standard production |
| llama3.1:70b | 40GB | 64GB | Native | High quality, more resources |
| mistral:7b | 4.1GB | 8GB | Good | Alternative option |

### Download Production Model

```bash
# Recommended: Llama 3.1 8B (good balance of quality and resources)
ollama pull llama3.1:8b

# High-end: Llama 3.1 70B (requires 64GB+ RAM)
ollama pull llama3.1:70b
```

## Spring Boot Configuration (Production)

### Gradle Dependencies

```kotlin
// build.gradle.kts
dependencies {
    implementation("org.springframework.ai:spring-ai-ollama-spring-boot-starter")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:1.0.0")
    }
}
```

### Application Configuration

```yaml
# application-prod.yml
spring:
  config:
    activate:
      on-profile: prod

  ai:
    ollama:
      base-url: ${OLLAMA_BASE_URL:http://localhost:11434}
      chat:
        model: llama3.1:8b
        options:
          temperature: 0.7
          num-predict: 2000
          num-ctx: 8192
```

## Ollama Adapter (DDD Infrastructure Layer)

```java
package uy.com.geocom.retail.infrastructure.adapter.out.llm;

import uy.com.geocom.retail.application.port.LlmPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * Ollama LLM Adapter for PRODUCTION.
 * Runs locally - all data stays on-premise.
 */
@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class OllamaLlmAdapter implements LlmPort {

    private final OllamaChatModel ollamaModel;

    @Override
    public Flux<String> streamResponse(String prompt, Object tools) {
        log.info("Streaming response from Ollama (production - LOCAL)");

        return ChatClient.builder(ollamaModel)
            .defaultSystem(getSystemPrompt())
            .build()
            .prompt()
            .user(prompt)
            .tools(tools)
            .stream()
            .content();
    }

    @Override
    public String getResponse(String prompt, Object tools) {
        log.info("Getting response from Ollama (production)");

        return ChatClient.builder(ollamaModel)
            .defaultSystem(getSystemPrompt())
            .build()
            .prompt()
            .user(prompt)
            .tools(tools)
            .call()
            .content();
    }

    private String getSystemPrompt() {
        return """
            Eres un asistente de analytics para retail.
            Siempre respondes en espanol.
            Usas las herramientas disponibles para obtener datos reales.
            Formatea numeros con puntos para miles y comas para decimales.
            Se conciso pero informativo.
            """;
    }
}
```

## Running Production

```bash
# 1. Start Ollama
ollama serve

# 2. Start backend with prod profile
./gradlew bootRun --args='--spring.profiles.active=prod'
```

## API Reference

### Chat Endpoint with Tool Calling

```bash
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.1:8b",
  "messages": [
    {"role": "user", "content": "Cuanto vendio el local 001 en enero?"}
  ],
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_store_sales",
        "description": "Obtiene ventas de un local en un periodo",
        "parameters": {
          "type": "object",
          "properties": {
            "store_id": {"type": "string"},
            "start_date": {"type": "string"},
            "end_date": {"type": "string"}
          },
          "required": ["store_id", "start_date", "end_date"]
        }
      }
    }
  ],
  "stream": true
}'
```

## Production Performance Optimization

### Hardware Requirements

| Model | Min RAM | Recommended RAM | GPU |
|-------|---------|-----------------|-----|
| Llama 3.1 8B | 8GB | 16GB | Optional but recommended |
| Llama 3.1 70B | 64GB | 128GB | Required (24GB+ VRAM) |

### Optimization Settings

```yaml
spring:
  ai:
    ollama:
      chat:
        options:
          num-ctx: 4096      # Reduce context for speed
          num-predict: 1000  # Limit output tokens
          num-thread: 8      # Match CPU cores
          num-gpu: 999       # Use all GPU layers
```

### Pre-load Model (Avoid Cold Starts)

```bash
# Keep model loaded in memory for 24 hours
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.1:8b",
  "keep_alive": "24h"
}'
```

### Production Systemd Service

```ini
# /etc/systemd/system/ollama.service
[Unit]
Description=Ollama LLM Server
After=network.target

[Service]
Type=simple
User=ollama
ExecStart=/usr/local/bin/ollama serve
Restart=always
RestartSec=3

[Install]
WantedBy=multi-user.target
```

```bash
# Enable and start
sudo systemctl enable ollama
sudo systemctl start ollama
```

## Health Check

```java
@Component
@Profile("prod")
public class OllamaHealthIndicator implements HealthIndicator {

    private final WebClient webClient;

    public OllamaHealthIndicator(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public Health health() {
        try {
            var response = webClient.get()
                .uri("/api/tags")
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofSeconds(5));

            return Health.up()
                .withDetail("provider", "Ollama (Local)")
                .withDetail("status", "Running")
                .withDetail("models", response)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

## Troubleshooting

### Common Issues

```bash
# Check if Ollama is running
curl http://localhost:11434/api/tags

# Check GPU usage (if using GPU)
nvidia-smi

# View Ollama logs
journalctl -u ollama -f

# Restart Ollama
sudo systemctl restart ollama
```

### Memory Issues

```bash
# Reduce context size for lower memory usage
OLLAMA_NUM_CTX=2048 ollama run llama3.1:8b

# Unload models to free memory
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.1:8b",
  "keep_alive": 0
}'
```

## Production vs Development Comparison

| Aspect | Grok (Dev) | Ollama (Prod) |
|--------|------------|---------------|
| Setup | API key only | Install + model |
| Data Privacy | Sent to xAI | **Stays local** |
| Cost | Free tier limits | Hardware only |
| Latency | ~500ms (network) | ~200ms (local) |
| Reliability | Internet dependent | Self-hosted |
| Compliance | Limited | **Full control** |

## References

- Ollama: https://ollama.com
- Ollama API: https://github.com/ollama/ollama/blob/main/docs/api.md
- Spring AI Ollama: https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
- Model Library: https://ollama.com/library
