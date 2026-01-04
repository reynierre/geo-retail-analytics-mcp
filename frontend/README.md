# Frontend - Angular 19 Chat UI

## Overview

Interfaz de chat conversacional para consultas de analytics usando Angular 19 con Signals y streaming SSE.

## Tech Stack

- **Angular**: 19.x (compatible con 21 cuando este disponible)
- **Styling**: Tailwind CSS 3.4.x
- **State**: Signals (no NgRx needed)
- **HTTP**: fetch API with SSE
- **Build**: Angular CLI

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
├── tailwind.config.js
├── proxy.conf.json           # Proxy /api -> localhost:8081
├── src/
│   ├── index.html
│   ├── main.ts
│   ├── styles.css            # Tailwind + custom styles
│   ├── environments/
│   │   ├── environment.ts
│   │   └── environment.prod.ts
│   └── app/
│       ├── app.component.ts
│       ├── app.config.ts
│       ├── app.routes.ts
│       ├── core/
│       │   └── services/
│       │       └── api.service.ts        # Health checks
│       ├── features/
│       │   └── chat/
│       │       ├── chat.component.ts     # Main container
│       │       ├── models/
│       │       │   └── message.model.ts
│       │       ├── components/
│       │       │   ├── chat-input.component.ts
│       │       │   └── chat-message.component.ts
│       │       └── services/
│       │           └── chat.service.ts   # SSE streaming
│       └── shared/
│           ├── components/
│           │   └── spinner.component.ts
│           └── pipes/
│               └── relative-time.pipe.ts
```

## Key Features

### Signals for Reactive State

```typescript
// chat.service.ts
readonly messages = signal<Message[]>([]);
readonly isStreaming = signal(false);
readonly error = signal<string | null>(null);

// Computed values
readonly hasMessages = computed(() => this.messages().length > 0);
```

### SSE Streaming with fetch API

```typescript
const response = await fetch('/api/chat/stream', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ message })
});

const reader = response.body?.getReader();
while (true) {
  const { done, value } = await reader.read();
  if (done) break;
  // Process SSE chunks
}
```

### Standalone Components

```typescript
@Component({
  selector: 'app-chat-message',
  standalone: true,
  imports: [CommonModule],
  template: `...`
})
export class ChatMessageComponent {
  message = input.required<Message>();
}
```

## Commands

```bash
# Development (with proxy to backend)
ng serve

# Build production
ng build --configuration=production

# Run tests
npm test
```

## Environment Configuration

```typescript
// src/environments/environment.ts
export const environment = {
  production: false,
  apiUrl: '/api'  // Proxied to backend
};
```

## Proxy Configuration

El archivo `proxy.conf.json` redirige `/api/*` al backend:

```json
{
  "/api": {
    "target": "http://localhost:8081",
    "secure": false,
    "changeOrigin": true
  }
}
```

## UI Features

- Chat con streaming en tiempo real
- Sugerencias de consultas rapidas
- Indicador de carga animado
- Formateo basico de Markdown
- Scroll automatico a mensajes nuevos
- Boton para limpiar conversacion
- Mensajes de error con opcion de reintento
