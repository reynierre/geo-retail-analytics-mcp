import { Injectable, signal, computed } from '@angular/core';
import { Message } from '../models/message.model';

interface ChatState {
  messages: Message[];
  isLoading: boolean;
  error: string | null;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
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

  updateLastMessage(content: string): void {
    this.state.update(s => {
      const messages = [...s.messages];
      if (messages.length > 0) {
        const lastMessage = { ...messages[messages.length - 1] };
        lastMessage.content = content;
        messages[messages.length - 1] = lastMessage;
      }
      return { ...s, messages };
    });
  }

  setLoading(loading: boolean): void {
    this.state.update(s => ({ ...s, isLoading: loading }));
  }

  setError(error: string | null): void {
    this.state.update(s => ({ ...s, error, isLoading: false }));
  }

  clearMessages(): void {
    this.state.update(s => ({ ...s, messages: [], error: null }));
  }

  clearError(): void {
    this.state.update(s => ({ ...s, error: null }));
  }
}
