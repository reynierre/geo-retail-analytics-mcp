# Angular 21 Skill

## Overview

This skill provides knowledge for developing Angular 21 applications with modern patterns including Signals, Zoneless change detection, and Signal Forms.

## Angular 21 Key Features

### 1. Signals (Stable)

Signals are Angular's reactive primitive for managing state:

```typescript
import { signal, computed, effect } from '@angular/core';

// Create a signal
const count = signal(0);

// Read value
console.log(count()); // 0

// Update value
count.set(5);
count.update(n => n + 1);

// Computed signal (derived state)
const doubled = computed(() => count() * 2);

// Effect (side effects)
effect(() => {
  console.log('Count changed to:', count());
});
```

### 2. Zoneless Change Detection

Angular 21 defaults to zoneless mode:

```typescript
// app.config.ts
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(), // No more Zone.js!
    provideRouter(routes),
    provideHttpClient()
  ]
};
```

### 3. Control Flow Syntax

New built-in control flow (replaces *ngIf, *ngFor):

```html
<!-- Conditional -->
@if (isLoading()) {
  <app-spinner />
} @else if (error()) {
  <app-error [message]="error()" />
} @else {
  <app-content [data]="data()" />
}

<!-- Loop with tracking -->
@for (item of items(); track item.id) {
  <app-item [data]="item" />
} @empty {
  <p>No items found</p>
}

<!-- Switch -->
@switch (status()) {
  @case ('loading') { <app-spinner /> }
  @case ('error') { <app-error /> }
  @default { <app-content /> }
}
```

### 4. Standalone Components (Default)

All components are standalone by default:

```typescript
@Component({
  selector: 'app-feature',
  standalone: true, // Default in v21
  imports: [CommonModule, RouterLink],
  template: `...`
})
export class FeatureComponent {}
```

### 5. Signal Forms (Experimental)

New reactive forms based on Signals:

```typescript
import { SignalFormBuilder } from '@angular/forms';

@Component({...})
export class FormComponent {
  private fb = inject(SignalFormBuilder);
  
  form = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]]
  });
  
  // Access value as signal
  nameValue = this.form.controls.name.value; // Signal<string>
  
  // Check validity as signal
  isValid = this.form.valid; // Signal<boolean>
}
```

### 6. Deferred Loading

Lazy load parts of templates:

```html
@defer (on viewport) {
  <app-heavy-component />
} @placeholder {
  <div class="skeleton" />
} @loading (minimum 500ms) {
  <app-spinner />
} @error {
  <p>Failed to load</p>
}
```

## Project Setup

### Create New Project

```bash
# Create Angular 21 project
ng new geo-retail-analytics-frontend --style=css --routing=true --ssr=false

# Add Tailwind CSS
npm install -D tailwindcss postcss autoprefixer
npx tailwindcss init

# Configure tailwind.config.js
module.exports = {
  content: ["./src/**/*.{html,ts}"],
  theme: { extend: {} },
  plugins: []
}

# Add to styles.css
@tailwind base;
@tailwind components;
@tailwind utilities;
```

### Recommended Project Structure

```
src/app/
├── app.component.ts
├── app.config.ts
├── app.routes.ts
├── features/
│   └── chat/
│       ├── chat.component.ts
│       ├── chat.routes.ts
│       ├── components/
│       │   ├── chat-input.component.ts
│       │   ├── chat-message.component.ts
│       │   └── chat-history.component.ts
│       └── services/
│           └── chat.service.ts
├── shared/
│   ├── components/
│   │   ├── spinner.component.ts
│   │   └── markdown.component.ts
│   ├── pipes/
│   │   └── relative-time.pipe.ts
│   └── directives/
│       └── auto-scroll.directive.ts
└── core/
    ├── services/
    │   ├── auth.service.ts
    │   └── api.service.ts
    └── interceptors/
        └── auth.interceptor.ts
```

## Common Patterns

### Chat Component with Streaming

```typescript
@Component({
  selector: 'app-chat',
  standalone: true,
  imports: [CommonModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col h-screen">
      <!-- Messages -->
      <div class="flex-1 overflow-y-auto p-4 space-y-4" #messagesContainer>
        @for (message of messages(); track message.id) {
          <app-chat-message [message]="message" />
        }
        
        @if (isStreaming()) {
          <div class="flex items-start gap-3">
            <div class="bg-gray-100 rounded-lg p-3 max-w-[80%]">
              {{ streamingContent() }}
              <span class="inline-block w-2 h-4 bg-gray-400 animate-pulse ml-1"></span>
            </div>
          </div>
        }
      </div>
      
      <!-- Input -->
      <div class="border-t p-4">
        <div class="flex gap-2">
          <textarea
            [(ngModel)]="userInput"
            (keydown.enter)="onEnterKey($event)"
            [disabled]="isStreaming()"
            class="flex-1 p-3 border rounded-lg resize-none focus:outline-none focus:ring-2"
            rows="2"
            placeholder="Escribe tu pregunta..."
          ></textarea>
          <button
            (click)="sendMessage()"
            [disabled]="isStreaming() || !userInput.trim()"
            class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50"
          >
            @if (isStreaming()) {
              <app-spinner size="small" />
            } @else {
              Enviar
            }
          </button>
        </div>
      </div>
    </div>
  `
})
export class ChatComponent {
  private chatService = inject(ChatService);
  
  messages = signal<Message[]>([]);
  streamingContent = signal('');
  isStreaming = signal(false);
  userInput = '';
  
  async sendMessage() {
    if (!this.userInput.trim() || this.isStreaming()) return;
    
    const userMessage: Message = {
      id: crypto.randomUUID(),
      role: 'user',
      content: this.userInput,
      timestamp: new Date()
    };
    
    this.messages.update(msgs => [...msgs, userMessage]);
    this.userInput = '';
    this.isStreaming.set(true);
    this.streamingContent.set('');
    
    try {
      await this.chatService.streamChat(userMessage.content, {
        onChunk: (chunk) => {
          this.streamingContent.update(c => c + chunk);
        },
        onComplete: (fullResponse) => {
          this.messages.update(msgs => [...msgs, {
            id: crypto.randomUUID(),
            role: 'assistant',
            content: fullResponse,
            timestamp: new Date()
          }]);
          this.streamingContent.set('');
          this.isStreaming.set(false);
        },
        onError: (error) => {
          console.error('Chat error:', error);
          this.isStreaming.set(false);
        }
      });
    } catch (error) {
      this.isStreaming.set(false);
    }
  }
  
  onEnterKey(event: KeyboardEvent) {
    if (!event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
}
```

### Chat Service with SSE

```typescript
@Injectable({ providedIn: 'root' })
export class ChatService {
  private apiUrl = inject(API_URL);

  async streamChat(
    message: string,
    callbacks: {
      onChunk: (chunk: string) => void;
      onComplete: (fullResponse: string) => void;
      onError: (error: Error) => void;
    }
  ): Promise<void> {
    const response = await fetch(`${this.apiUrl}/api/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.getToken()}`
      },
      body: JSON.stringify({ message })
    });

    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`);
    }

    const reader = response.body!.getReader();
    const decoder = new TextDecoder();
    let fullResponse = '';

    try {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
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
            } catch {
              // Skip non-JSON lines
            }
          }
        }
      }
      
      callbacks.onComplete(fullResponse);
    } catch (error) {
      callbacks.onError(error instanceof Error ? error : new Error(String(error)));
    }
  }

  private getToken(): string {
    return localStorage.getItem('auth_token') || '';
  }
}
```

### Auto-Scroll Directive

```typescript
@Directive({
  selector: '[appAutoScroll]',
  standalone: true
})
export class AutoScrollDirective {
  private element = inject(ElementRef);
  
  @Input() set appAutoScroll(trigger: any) {
    this.scrollToBottom();
  }
  
  private scrollToBottom() {
    setTimeout(() => {
      const el = this.element.nativeElement;
      el.scrollTop = el.scrollHeight;
    }, 0);
  }
}
```

## Best Practices

1. **Use Signals for all component state**
2. **Use computed() for derived values**
3. **Use effect() sparingly, prefer explicit method calls**
4. **Always track by unique ID in @for loops**
5. **Use OnPush change detection (default with Zoneless)**
6. **Prefer standalone components**
7. **Use inject() function over constructor injection**
8. **Lazy load routes for better bundle size**

## References

- Angular 21 Docs: https://angular.dev
- Angular Signals: https://angular.dev/guide/signals
- Tailwind CSS: https://tailwindcss.com
