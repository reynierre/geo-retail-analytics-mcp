# Frontend - Angular 21 Chat UI

## Overview

Interfaz de chat conversacional para consultas de analytics usando Angular 21 con Signals, Zoneless change detection, y streaming SSE.

## Tech Stack

| Technology | Version | Purpose |
|------------|---------|---------|
| **Angular** | 21.x | Framework principal |
| **TypeScript** | 5.6.x | Type safety |
| **Tailwind CSS** | 4.x | Styling utility-first |
| **Signals** | Built-in | State management reactivo |
| **Zoneless** | Default | Change detection optimizado |
| **Vitest** | 3.x | Unit testing |
| **fetch API** | Native | SSE streaming |

## Key Features

- ✅ **Zoneless Change Detection** - Sin Zone.js, mejor performance
- ✅ **Signals** - Estado reactivo con `signal()`, `computed()`, `effect()`
- ✅ **linkedSignal** - Estado dependiente que puede ser modificado
- ✅ **resource()** - Fetching async reactivo
- ✅ **Control Flow** - `@if`, `@for`, `@switch`, `@defer`
- ✅ **Standalone Components** - Sin NgModules
- ✅ **SSE Streaming** - Respuestas en tiempo real
- ✅ **OnPush** - Change detection optimizado

## Quick Start

```bash
# Install dependencies
npm install

# Start development server (proxies /api to backend:8081)
ng serve

# Open http://localhost:4200
```

## Project Structure

```
frontend/
├── package.json
├── angular.json
├── tsconfig.json
├── postcss.config.js            # Tailwind CSS 4 config
├── proxy.conf.json              # Proxy /api -> localhost:8081
├── src/
│   ├── index.html
│   ├── main.ts
│   ├── styles.css               # @import "tailwindcss"
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   └── app/
│       ├── app.ts               # Root component
│       ├── app.config.ts        # Zoneless + HttpClient + Router
│       ├── app.routes.ts
│       ├── core/
│       │   ├── services/
│       │   │   ├── api.service.ts
│       │   │   └── auth.service.ts
│       │   └── interceptors/
│       │       └── auth.interceptor.ts
│       ├── features/
│       │   ├── chat/
│       │   │   ├── chat.ts              # Main chat component
│       │   │   ├── chat.routes.ts
│       │   │   ├── models/
│       │   │   │   └── message.model.ts
│       │   │   ├── components/
│       │   │   │   ├── chat-input.ts
│       │   │   │   ├── chat-message.ts
│       │   │   │   └── chat-history.ts
│       │   │   └── services/
│       │   │       ├── chat.service.ts
│       │   │       └── streaming.service.ts
│       │   └── shared/              # Solo para 2+ features
│       │       ├── components/
│       │       │   ├── spinner.ts
│       │       │   └── markdown.ts
│       │       ├── pipes/
│       │       │   └── relative-time.pipe.ts
│       │       └── directives/
│       │           └── auto-scroll.directive.ts
```

## App Configuration

```typescript
// app.config.ts
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withFetch, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from '@core/interceptors/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),  // Sin Zone.js
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withFetch(), withInterceptors([authInterceptor]))
  ]
};
```

## Signals para State Management

```typescript
// chat.service.ts
import { Injectable, signal, computed } from '@angular/core';

interface ChatState {
  messages: Message[];
  isLoading: boolean;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  // Private state
  private readonly state = signal<ChatState>({
    messages: [],
    isLoading: false,
    error: null
  });
  
  // Public selectors (readonly)
  readonly messages = computed(() => this.state().messages);
  readonly isLoading = computed(() => this.state().isLoading);
  readonly error = computed(() => this.state().error);
  readonly hasMessages = computed(() => this.state().messages.length > 0);
  
  // Actions
  addMessage(message: Message): void {
    this.state.update(s => ({
      ...s,
      messages: [...s.messages, message]
    }));
  }
  
  setLoading(loading: boolean): void {
    this.state.update(s => ({ ...s, isLoading: loading }));
  }
  
  clearMessages(): void {
    this.state.update(s => ({ ...s, messages: [], error: null }));
  }
}
```

## SSE Streaming con fetch API

```typescript
// streaming.service.ts
@Injectable({ providedIn: 'root' })
export class StreamingService {
  async streamChat(
    message: string,
    callbacks: {
      onChunk: (chunk: string) => void;
      onComplete: (fullResponse: string) => void;
      onError: (error: Error) => void;
    }
  ): Promise<void> {
    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    });

    if (!response.ok) throw new Error(`HTTP ${response.status}`);
    if (!response.body) throw new Error('No response body');

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullResponse = '';

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      for (const line of chunk.split('\n')) {
        if (line.startsWith('data: ')) {
          const data = line.slice(6);
          if (data === '[DONE]') {
            callbacks.onComplete(fullResponse);
            return;
          }
          try {
            const parsed = JSON.parse(data);
            if (parsed.content) {
              fullResponse += parsed.content;
              callbacks.onChunk(parsed.content);
            }
          } catch { /* skip non-JSON */ }
        }
      }
    }
    callbacks.onComplete(fullResponse);
  }
}
```

## Standalone Components (Sin `standalone: true`)

```typescript
// chat-message.ts
import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { DatePipe } from '@angular/common';

@Component({
  selector: 'app-chat-message',
  // standalone: true NO es necesario - es el default en Angular 21
  imports: [DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    'class': 'block',
    '[class.user-message]': 'message().role === "user"',
    '[class.assistant-message]': 'message().role === "assistant"'
  },
  template: `
    <div class="flex gap-3 p-4 rounded-lg" 
         [class]="message().role === 'user' ? 'bg-blue-50 ml-12' : 'bg-gray-50 mr-12'">
      <div class="flex-1">
        <p class="text-sm font-medium mb-1">
          {{ message().role === 'user' ? 'You' : 'Assistant' }}
        </p>
        <p class="whitespace-pre-wrap">{{ message().content }}</p>
        <span class="text-xs text-gray-400 mt-2 block">
          {{ message().timestamp | date:'short' }}
        </span>
      </div>
    </div>
  `
})
export class ChatMessageComponent {
  // input() function en lugar de @Input() decorator
  readonly message = input.required<Message>();
}
```

## Control Flow (@if, @for, @switch, @defer)

```html
<!-- chat.ts template -->
<div class="flex flex-col h-screen">
  <!-- Messages -->
  <div class="flex-1 overflow-y-auto p-4 space-y-4">
    @for (message of chatService.messages(); track message.id) {
      <app-chat-message [message]="message" />
    } @empty {
      <div class="text-center py-8 text-gray-500">
        <p>No messages yet. Start a conversation!</p>
      </div>
    }
    
    @if (isStreaming()) {
      <div class="bg-gray-50 p-4 rounded-lg mr-12">
        <p class="whitespace-pre-wrap">{{ streamingContent() }}</p>
        <span class="inline-block w-2 h-4 bg-blue-500 animate-pulse"></span>
      </div>
    }
  </div>
  
  <!-- Heavy component loaded on demand -->
  @defer (on viewport) {
    <app-suggestions [context]="chatService.messages()" />
  } @placeholder {
    <div class="h-24 bg-gray-100 animate-pulse rounded"></div>
  }
  
  <!-- Input -->
  <app-chat-input 
    [disabled]="isStreaming()" 
    (messageSent)="sendMessage($event)" 
  />
</div>
```

## Commands

```bash
# Development (with proxy to backend)
ng serve

# Build production
ng build --configuration=production

# Run tests with Vitest
npm test

# Run tests with coverage
npm test -- --coverage

# Lint
ng lint

# Generate component
ng generate component features/feature-name
```

## Environment Configuration

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: '/api'  // Proxied to backend
};

// src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: '/api'
};
```

## Proxy Configuration

```json
// proxy.conf.json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

## Testing con Vitest

```typescript
// chat.service.spec.ts
import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { ChatService } from './chat.service';

describe('ChatService', () => {
  let service: ChatService;
  
  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChatService);
  });
  
  it('should add message', () => {
    const message = {
      id: '1',
      role: 'user' as const,
      content: 'Hello',
      timestamp: new Date()
    };
    
    service.addMessage(message);
    
    expect(service.messages()).toContain(message);
    expect(service.hasMessages()).toBe(true);
  });
  
  it('should clear messages', () => {
    service.addMessage({ id: '1', role: 'user', content: 'Test', timestamp: new Date() });
    
    service.clearMessages();
    
    expect(service.messages()).toHaveLength(0);
  });
});
```

## UI Features

- 💬 Chat con streaming en tiempo real
- 💡 Sugerencias de consultas rápidas
- ⏳ Indicador de carga animado (skeleton + pulse)
- 📝 Formateo básico de Markdown
- 📜 Scroll automático a mensajes nuevos
- 🗑️ Botón para limpiar conversación
- ⚠️ Mensajes de error con opción de reintento
- ♿ Accesibilidad WCAG AA

## Performance Optimizations

1. **Zoneless** - Sin overhead de Zone.js
2. **OnPush** - Solo actualiza cuando cambian inputs o signals
3. **@defer** - Lazy loading de componentes pesados
4. **track** - Tracking eficiente en @for loops
5. **computed()** - Memoización automática de estado derivado
6. **Tailwind CSS** - CSS purgado en producción
7. **esbuild** - Build ultra-rápido

## Angular 21 Best Practices Applied

| Pattern | Implementation |
|---------|---------------|
| State Management | Signals (`signal`, `computed`) |
| Dependency Injection | `inject()` function |
| Component Inputs | `input()` function |
| Component Outputs | `output()` function |
| Control Flow | `@if`, `@for`, `@switch` |
| Lazy Loading | `@defer` blocks |
| Change Detection | Zoneless + OnPush |
| HTTP | `provideHttpClient(withFetch())` |
| Styling | Tailwind CSS 4 |
| Testing | Vitest |
