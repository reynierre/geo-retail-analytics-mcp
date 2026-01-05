# Frontend - Angular 21 Chat UI

## Overview

Interfaz de chat conversacional para consultas de analytics usando Angular 21 con Signals, Zoneless Change Detection y streaming SSE.

## Tech Stack

- **Angular**: 21.0.3
- **TypeScript**: 5.9.2
- **Styling**: Tailwind CSS 4.0.0
- **State**: Signals (no NgRx needed)
- **HTTP**: fetch API with SSE
- **Change Detection**: Zoneless (provideZonelessChangeDetection)
- **Build**: esbuild (Angular CLI 21)

## Quick Start

```bash
# Install dependencies
npm install

# Start development server (proxies /api to backend:8081)
npm start
# or
npx ng serve

# Open http://localhost:4200
```

## Project Structure

```
frontend/
├── package.json
├── angular.json
├── postcss.config.js         # Tailwind CSS 4 config
├── proxy.conf.json           # Proxy /api -> localhost:8081
├── tsconfig.json             # TypeScript config with path aliases
├── public/                   # Static assets
└── src/
    ├── index.html
    ├── main.ts
    ├── styles.css            # Tailwind CSS 4 entry
    └── app/
        ├── app.ts            # Root component (no .component suffix)
        ├── app.config.ts     # App configuration with Zoneless
        ├── app.routes.ts     # Lazy loaded routes
        ├── core/             # Singleton services
        │   └── services/
        └── features/
            ├── chat/
            │   ├── chat.ts           # Main container
            │   ├── models/
            │   │   └── message.model.ts
            │   ├── components/
            │   │   ├── chat-input.ts
            │   │   └── chat-message.ts
            │   └── services/
            │       ├── chat.service.ts      # State management
            │       └── streaming.service.ts # SSE streaming
            └── shared/
                └── components/
                    └── spinner.ts
```

## Key Features

### Angular 21 Modern Patterns

**Zoneless Change Detection:**
```typescript
// app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(),
    provideRouter(routes),
    provideHttpClient(withFetch())
  ]
};
```

**Signals for Reactive State:**
```typescript
// chat.service.ts
private readonly state = signal<ChatState>({
  messages: [],
  isLoading: false,
  error: null
});

readonly messages = computed(() => this.state().messages);
readonly hasMessages = computed(() => this.state().messages.length > 0);
```

**Modern Control Flow:**
```typescript
@if (isStreaming()) {
  <app-spinner />
} @else if (error()) {
  <app-error [message]="error()" />
} @else {
  @for (message of messages(); track message.id) {
    <app-chat-message [message]="message" />
  }
}
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

**input() and output() Functions:**
```typescript
@Component({
  selector: 'app-chat-message',
  imports: [],  // standalone is default in Angular 21
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `...`
})
export class ChatMessageComponent {
  // Use input() function instead of @Input() decorator
  readonly message = input.required<Message>();

  // Use output() function instead of @Output() decorator
  readonly clicked = output<void>();
}
```

**inject() Function:**
```typescript
export class ChatComponent {
  // Use inject() instead of constructor injection
  protected readonly chatService = inject(ChatService);
  private readonly streamingService = inject(StreamingService);
}
```

## Commands

```bash
# Development (with proxy to backend)
npm start
# or
npx ng serve

# Build production
npm run build
# or
npx ng build

# Watch mode
npm run watch

# Run tests
npm test
```

## Build Output

El build genera archivos optimizados con:
- **Initial bundle**: ~291 KB (~82 KB gzipped)
- **Lazy loaded chat**: ~32 KB (~9 KB gzipped)
- **Total < 500 KB** (cumple con performance target)

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

- Chat con streaming SSE en tiempo real
- Sugerencias de consultas integradas
- Indicador de carga animado
- Scroll automático a mensajes nuevos
- Mensajes de error con opción de reintentar
- Diseño responsive con Tailwind CSS 4
- Lazy loading del componente Chat

## TypeScript Path Aliases

```typescript
// Configurados en tsconfig.json
import { ChatService } from '@features/chat/services/chat.service';
import { SpinnerComponent } from '@shared/components/spinner';
import { ApiService } from '@core/services/api.service';
```

## Angular 21 Best Practices Used

✅ Zoneless change detection
✅ Signals para estado reactivo
✅ Computed signals para valores derivados
✅ input()/output() functions (no decorators)
✅ inject() function (no constructor injection)
✅ Modern control flow (@if, @for, @else)
✅ Lazy loading con loadComponent()
✅ OnPush change detection strategy
✅ No .component suffix en archivos
✅ Tailwind CSS 4 con @tailwindcss/postcss
✅ TypeScript 5.9+ (strict mode)
✅ Path aliases configurados

## Backend Integration

El frontend espera que el backend esté corriendo en `http://localhost:8081` con los siguientes endpoints:

- `POST /api/chat/stream` - SSE streaming para chat
  - Request: `{ "message": "¿Cuánto vendió el local 001?" }`
  - Response: Server-Sent Events con chunks de texto

Ver `backend/` para la implementación Spring Boot.
