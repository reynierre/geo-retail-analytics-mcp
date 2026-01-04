# Angular 21 Skill

## Overview

This skill provides comprehensive knowledge for developing Angular 21 applications with modern patterns including Signals, Zoneless change detection, linkedSignal, resource API, and the new control flow syntax.

## Angular 21 Key Features

### 1. Signals (Stable)

Signals are Angular's reactive primitive for managing state:

```typescript
import { signal, computed, effect } from '@angular/core';

// Create a signal
const count = signal(0);

// Read value (call as function)
console.log(count()); // 0

// Update value
count.set(5);
count.update(n => n + 1);

// Computed signal (derived state - automatically updates)
const doubled = computed(() => count() * 2);

// Effect (side effects - runs when dependencies change)
effect(() => {
  console.log('Count changed to:', count());
});
```

### 2. linkedSignal (Dependent State)

For state that depends on other signals but can also be modified independently:

```typescript
import { signal, linkedSignal } from '@angular/core';

// Source signal
const selectedCategory = signal('electronics');

// Linked signal - resets when source changes, but can be modified independently
const selectedProduct = linkedSignal({
  source: selectedCategory,
  computation: (category) => getDefaultProductForCategory(category)
});

// Can still be set independently
selectedProduct.set('laptop-123');

// When selectedCategory changes, selectedProduct resets to computation result
```

### 3. resource() API (Async Data Fetching)

For reactive async data loading that integrates with signals:

```typescript
import { resource, signal } from '@angular/core';

@Component({...})
export class ProductComponent {
  private productId = signal('123');
  
  // Resource automatically fetches when productId changes
  product = resource({
    request: () => ({ id: this.productId() }),
    loader: async ({ request }) => {
      const response = await fetch(`/api/products/${request.id}`);
      return response.json();
    }
  });
  
  // Access in template
  // product.value() - the loaded data
  // product.isLoading() - loading state
  // product.error() - any error
  // product.status() - 'idle' | 'loading' | 'resolved' | 'error'
}
```

### 4. Zoneless Change Detection (Default)

Angular 21 defaults to zoneless mode for better performance:

```typescript
// app.config.ts
import { ApplicationConfig, provideZonelessChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZonelessChangeDetection(), // No more Zone.js!
    provideRouter(routes),
    provideHttpClient(withFetch())    // Use fetch API
  ]
};
```

### 5. Control Flow Syntax (Built-in)

New built-in control flow replaces structural directives:

```html
<!-- Conditional rendering -->
@if (isLoading()) {
  <app-spinner />
} @else if (error()) {
  <app-error [message]="error()" />
} @else {
  <app-content [data]="data()" />
}

<!-- Loop with tracking (ALWAYS use track) -->
@for (item of items(); track item.id) {
  <app-item [data]="item" />
} @empty {
  <p>No items found</p>
}

<!-- Switch -->
@switch (status()) {
  @case ('loading') { <app-spinner /> }
  @case ('error') { <app-error /> }
  @case ('success') { <app-content /> }
  @default { <p>Unknown status</p> }
}
```

### 6. Deferred Loading (@defer)

Lazy load parts of templates for better performance:

```html
@defer (on viewport) {
  <app-heavy-component />
} @placeholder {
  <div class="skeleton h-32 w-full animate-pulse bg-gray-200" />
} @loading (minimum 500ms) {
  <app-spinner />
} @error {
  <p>Failed to load component</p>
}

<!-- Other triggers -->
@defer (on idle) { ... }           <!-- When browser is idle -->
@defer (on timer(2s)) { ... }      <!-- After 2 seconds -->
@defer (on interaction) { ... }    <!-- On user interaction -->
@defer (when condition()) { ... }  <!-- When condition is true -->
@defer (on hover) { ... }          <!-- On mouse hover -->
```

### 7. Standalone Components (Default - No standalone: true needed)

All components are standalone by default in Angular 21:

```typescript
import { Component, ChangeDetectionStrategy, signal, input, output, inject } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-feature',
  // standalone: true is NOT needed - it's the default
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `...`
})
export class FeatureComponent {
  // Use inject() instead of constructor injection
  private readonly service = inject(FeatureService);
}
```

### 8. input() and output() Functions

Replace @Input() and @Output() decorators:

```typescript
import { Component, input, output, model } from '@angular/core';

@Component({
  selector: 'app-item',
  template: `
    <div (click)="itemClicked.emit(data())">
      {{ data().name }}
    </div>
  `
})
export class ItemComponent {
  // Required input
  readonly data = input.required<ItemData>();
  
  // Optional input with default
  readonly disabled = input(false);
  
  // Input with transform
  readonly count = input(0, { transform: numberAttribute });
  
  // Output
  readonly itemClicked = output<ItemData>();
  
  // Two-way binding (model)
  readonly selected = model(false);
}

// Usage in parent:
// <app-item [data]="item" [(selected)]="isSelected" (itemClicked)="onItemClick($event)" />
```

### 9. RxJS Interop with Signals

Convert between Signals and Observables:

```typescript
import { toSignal, toObservable } from '@angular/core/rxjs-interop';
import { interval } from 'rxjs';

@Component({...})
export class TimerComponent {
  // Observable to Signal
  private counter$ = interval(1000);
  counter = toSignal(this.counter$, { initialValue: 0 });
  
  // Signal to Observable
  private searchTerm = signal('');
  searchTerm$ = toObservable(this.searchTerm);
  
  // With requireSync for BehaviorSubject
  private behavior$ = new BehaviorSubject('initial');
  behaviorSignal = toSignal(this.behavior$, { requireSync: true });
}
```

### 10. Host Bindings (Modern Approach)

Use `host` object instead of decorators:

```typescript
@Component({
  selector: 'app-button',
  // Use host object instead of @HostBinding/@HostListener
  host: {
    'class': 'btn',
    '[class.btn-primary]': 'primary()',
    '[class.btn-disabled]': 'disabled()',
    '[attr.aria-disabled]': 'disabled()',
    '[attr.tabindex]': 'disabled() ? -1 : 0',
    '(click)': 'onClick($event)',
    '(keydown.enter)': 'onClick($event)'
  },
  template: `<ng-content />`
})
export class ButtonComponent {
  readonly primary = input(false);
  readonly disabled = input(false);
  
  onClick(event: Event) {
    if (!this.disabled()) {
      // Handle click
    }
  }
}
```

### 11. NgOptimizedImage

Always use for static images:

```typescript
import { NgOptimizedImage } from '@angular/common';

@Component({
  selector: 'app-hero',
  imports: [NgOptimizedImage],
  template: `
    <!-- LCP image - mark as priority -->
    <img 
      ngSrc="/assets/hero.jpg" 
      width="800" 
      height="600" 
      priority
      alt="Hero image"
    />
    
    <!-- Responsive image -->
    <img 
      ngSrc="/assets/product.jpg" 
      fill
      sizes="(max-width: 768px) 100vw, 50vw"
      alt="Product"
    />
  `
})
export class HeroComponent {}
```

### 12. Typed Reactive Forms

Always use typed forms:

```typescript
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-profile-form',
  imports: [ReactiveFormsModule],
  template: `
    <form [formGroup]="form" (ngSubmit)="onSubmit()">
      <input formControlName="name" />
      <input formControlName="email" type="email" />
      
      @if (form.controls.email.errors?.['email']) {
        <span class="error">Invalid email</span>
      }
      
      <button type="submit" [disabled]="form.invalid">Submit</button>
    </form>
  `
})
export class ProfileFormComponent {
  private fb = inject(FormBuilder);
  
  // Fully typed form
  form = this.fb.group({
    name: ['', [Validators.required, Validators.minLength(2)]],
    email: ['', [Validators.required, Validators.email]]
  });
  
  onSubmit() {
    if (this.form.valid) {
      // form.value is typed: { name: string | null, email: string | null }
      const { name, email } = this.form.getRawValue();
      // getRawValue() returns non-null: { name: string, email: string }
    }
  }
}
```

## Project Setup

### Create New Project

```bash
# Create Angular 21 project
ng new my-app --style=css --routing=true --ssr=false

# Add Tailwind CSS 4
npm install -D tailwindcss @tailwindcss/postcss postcss

# Create postcss.config.js
echo 'export default { plugins: { "@tailwindcss/postcss": {} } }' > postcss.config.js

# Add to styles.css
echo '@import "tailwindcss";' > src/styles.css
```

### Recommended Project Structure (Scope Rule)

```
src/app/
├── app.ts                    # Root component (no .component suffix)
├── app.config.ts             # App configuration
├── app.routes.ts             # Route configuration
├── features/
│   ├── chat/
│   │   ├── chat.ts           # Main feature component
│   │   ├── chat.routes.ts    # Feature routes
│   │   ├── components/
│   │   │   ├── chat-input.ts
│   │   │   ├── chat-message.ts
│   │   │   └── chat-history.ts
│   │   ├── services/
│   │   │   └── chat.service.ts
│   │   └── models/
│   │       └── message.model.ts
│   └── shared/               # ONLY for 2+ feature usage
│       ├── components/
│       │   ├── spinner.ts
│       │   └── markdown.ts
│       ├── pipes/
│       │   └── relative-time.pipe.ts
│       └── directives/
│           └── auto-scroll.directive.ts
└── core/                     # Singleton services
    ├── services/
    │   ├── api.service.ts
    │   └── auth.service.ts
    └── interceptors/
        └── auth.interceptor.ts
```

### Path Aliases (tsconfig.json)

```json
{
  "compilerOptions": {
    "paths": {
      "@features/*": ["src/app/features/*"],
      "@shared/*": ["src/app/features/shared/*"],
      "@core/*": ["src/app/core/*"]
    }
  }
}
```

## Common Patterns

### Service with Signal State Store

```typescript
import { Injectable, signal, computed, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

interface ChatState {
  messages: Message[];
  isLoading: boolean;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  private readonly http = inject(HttpClient);
  
  // Private state signal
  private readonly state = signal<ChatState>({
    messages: [],
    isLoading: false,
    error: null
  });
  
  // Public readonly selectors
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
  
  setError(error: string | null): void {
    this.state.update(s => ({ ...s, error, isLoading: false }));
  }
  
  clearMessages(): void {
    this.state.update(s => ({ ...s, messages: [] }));
  }
}
```

### SSE Streaming Service

```typescript
import { Injectable, inject } from '@angular/core';

interface StreamCallbacks {
  onChunk: (chunk: string) => void;
  onComplete: (fullResponse: string) => void;
  onError: (error: Error) => void;
}

@Injectable({ providedIn: 'root' })
export class StreamingService {
  private readonly apiUrl = '/api';

  async streamChat(message: string, callbacks: StreamCallbacks): Promise<void> {
    const response = await fetch(`${this.apiUrl}/chat/stream`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message })
    });

    if (!response.ok) {
      throw new Error(`HTTP error: ${response.status}`);
    }

    if (!response.body) {
      throw new Error('No response body');
    }

    const reader = response.body.getReader();
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
}
```

### Chat Component with Streaming

```typescript
import { Component, signal, computed, inject, ChangeDetectionStrategy, ElementRef, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ChatService } from './services/chat.service';
import { StreamingService } from './services/streaming.service';
import { ChatMessageComponent } from './components/chat-message';
import { SpinnerComponent } from '@shared/components/spinner';

@Component({
  selector: 'app-chat',
  imports: [FormsModule, ChatMessageComponent, SpinnerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col h-screen bg-gray-50">
      <!-- Messages -->
      <div 
        #messagesContainer
        class="flex-1 overflow-y-auto p-4 space-y-4"
      >
        @for (message of chatService.messages(); track message.id) {
          <app-chat-message [message]="message" />
        } @empty {
          <div class="text-center text-gray-500 py-8">
            <p>No messages yet. Start a conversation!</p>
          </div>
        }
        
        @if (isStreaming()) {
          <div class="flex items-start gap-3">
            <div class="bg-white rounded-lg p-4 shadow-sm max-w-[80%]">
              <p class="whitespace-pre-wrap">{{ streamingContent() }}</p>
              <span class="inline-block w-2 h-4 bg-blue-500 animate-pulse ml-1"></span>
            </div>
          </div>
        }
      </div>
      
      <!-- Input -->
      <div class="border-t bg-white p-4">
        <div class="flex gap-3 max-w-4xl mx-auto">
          <textarea
            [(ngModel)]="userInput"
            (keydown.enter)="onEnterKey($event)"
            [disabled]="isStreaming()"
            class="flex-1 p-3 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500"
            rows="2"
            placeholder="Type your message..."
          ></textarea>
          <button
            (click)="sendMessage()"
            [disabled]="isStreaming() || !userInput.trim()"
            class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
          >
            @if (isStreaming()) {
              <app-spinner size="small" />
            } @else {
              Send
            }
          </button>
        </div>
      </div>
    </div>
  `
})
export class ChatComponent {
  protected readonly chatService = inject(ChatService);
  private readonly streamingService = inject(StreamingService);
  
  private readonly messagesContainer = viewChild<ElementRef>('messagesContainer');
  
  protected readonly streamingContent = signal('');
  protected readonly isStreaming = signal(false);
  protected userInput = '';
  
  async sendMessage(): Promise<void> {
    const content = this.userInput.trim();
    if (!content || this.isStreaming()) return;
    
    // Add user message
    this.chatService.addMessage({
      id: crypto.randomUUID(),
      role: 'user',
      content,
      timestamp: new Date()
    });
    
    this.userInput = '';
    this.isStreaming.set(true);
    this.streamingContent.set('');
    this.scrollToBottom();
    
    try {
      await this.streamingService.streamChat(content, {
        onChunk: (chunk) => {
          this.streamingContent.update(c => c + chunk);
          this.scrollToBottom();
        },
        onComplete: (fullResponse) => {
          this.chatService.addMessage({
            id: crypto.randomUUID(),
            role: 'assistant',
            content: fullResponse,
            timestamp: new Date()
          });
          this.streamingContent.set('');
          this.isStreaming.set(false);
          this.scrollToBottom();
        },
        onError: (error) => {
          console.error('Chat error:', error);
          this.chatService.setError(error.message);
          this.isStreaming.set(false);
        }
      });
    } catch (error) {
      this.isStreaming.set(false);
    }
  }
  
  protected onEnterKey(event: KeyboardEvent): void {
    if (!event.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }
  
  private scrollToBottom(): void {
    const container = this.messagesContainer()?.nativeElement;
    if (container) {
      setTimeout(() => {
        container.scrollTop = container.scrollHeight;
      }, 0);
    }
  }
}
```

### Functional HTTP Interceptor

```typescript
import { HttpInterceptorFn, HttpRequest, HttpHandlerFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '@core/services/auth.service';

export const authInterceptor: HttpInterceptorFn = (
  request: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const authService = inject(AuthService);
  const token = authService.getToken();
  
  if (token) {
    const clonedRequest = request.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`
      }
    });
    return next(clonedRequest);
  }
  
  return next(request);
};

// Register in app.config.ts
export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withFetch(),
      withInterceptors([authInterceptor])
    )
  ]
};
```

### Lazy Loaded Routes

```typescript
// app.routes.ts
import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home').then(m => m.HomeComponent)
  },
  {
    path: 'chat',
    loadComponent: () => import('./features/chat/chat').then(m => m.ChatComponent)
  },
  {
    path: 'dashboard',
    loadChildren: () => import('./features/dashboard/dashboard.routes').then(m => m.routes)
  }
];
```

## Best Practices Summary

### DO ✅

1. **Use Signals** for all component state
2. **Use computed()** for derived state
3. **Use inject()** function instead of constructor injection
4. **Use input()/output()** functions instead of decorators
5. **Use @if/@for/@switch** control flow instead of *ngIf/*ngFor
6. **Use @defer** for lazy loading heavy components
7. **Use track** in all @for loops
8. **Use OnPush** change detection (default with Zoneless)
9. **Use class/style bindings** instead of ngClass/ngStyle
10. **Use host object** instead of @HostBinding/@HostListener
11. **Use NgOptimizedImage** for all static images
12. **Use fetch API** for streaming (SSE)
13. **Use typed reactive forms**
14. **Use providedIn: 'root'** for singleton services

### DON'T ❌

1. **Don't use standalone: true** - it's the default
2. **Don't use NgModules** for feature organization
3. **Don't use @Input()/@Output()** decorators
4. **Don't use *ngIf/*ngFor/*ngSwitch** directives
5. **Don't use ngClass/ngStyle** directives
6. **Don't use @HostBinding/@HostListener** decorators
7. **Don't use constructor injection** - use inject()
8. **Don't use the `any` type** - use proper types or `unknown`
9. **Don't mutate signals** - use update() or set()
10. **Don't use lifecycle hooks when signals work** - prefer computed/effect
11. **Don't forget track** in @for loops
12. **Don't use .component/.service/.module** suffixes in filenames

## Testing with Vitest

```typescript
import { describe, it, expect, beforeEach } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ChatService } from './chat.service';

describe('ChatService', () => {
  let service: ChatService;
  let httpTesting: HttpTestingController;
  
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        ChatService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    
    service = TestBed.inject(ChatService);
    httpTesting = TestBed.inject(HttpTestingController);
  });
  
  it('should add message to state', () => {
    const message = { id: '1', role: 'user', content: 'Hello', timestamp: new Date() };
    
    service.addMessage(message);
    
    expect(service.messages()).toContain(message);
    expect(service.hasMessages()).toBe(true);
  });
  
  it('should clear all messages', () => {
    service.addMessage({ id: '1', role: 'user', content: 'Hello', timestamp: new Date() });
    
    service.clearMessages();
    
    expect(service.messages()).toHaveLength(0);
    expect(service.hasMessages()).toBe(false);
  });
});
```

## References

- Angular 21 Documentation: https://angular.dev
- Signals Guide: https://angular.dev/guide/signals
- linkedSignal: https://angular.dev/guide/signals/linked-signal
- resource API: https://angular.dev/guide/signals/resource
- Control Flow: https://angular.dev/guide/templates/control-flow
- Deferred Loading: https://angular.dev/guide/templates/defer
- Zoneless: https://angular.dev/guide/zoneless
- Style Guide: https://angular.dev/style-guide
