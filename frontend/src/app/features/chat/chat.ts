import { Component, signal, inject, ChangeDetectionStrategy, ElementRef, viewChild, AfterViewInit } from '@angular/core';
import { ChatService } from './services/chat.service';
import { StreamingService } from './services/streaming.service';
import { ChatMessageComponent } from './components/chat-message';
import { ChatInputComponent } from './components/chat-input';
import { Message } from './models/message.model';

@Component({
  selector: 'app-chat',
  imports: [ChatMessageComponent, ChatInputComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="flex flex-col h-screen bg-gray-50">
      <!-- Header -->
      <div class="bg-white border-b shadow-sm p-4">
        <div class="max-w-4xl mx-auto">
          <h1 class="text-2xl font-bold text-gray-900">Geo Retail Analytics</h1>
          <p class="text-sm text-gray-600">Consulta tus datos de ventas en lenguaje natural</p>
        </div>
      </div>

      <!-- Messages Container -->
      <div
        #messagesContainer
        class="flex-1 overflow-y-auto p-4"
      >
        <div class="max-w-4xl mx-auto">
          @if (!chatService.hasMessages() && !isStreaming()) {
            <div class="flex flex-col items-center justify-center h-full text-center py-12">
              <svg class="w-16 h-16 text-gray-300 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path>
              </svg>
              <h2 class="text-xl font-semibold text-gray-700 mb-2">Bienvenido al Chat de Analytics</h2>
              <p class="text-gray-500 mb-4">Pregunta lo que necesites sobre tus ventas</p>
              <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 max-w-md">
                <p class="text-sm text-blue-900 font-semibold mb-2">Ejemplos de consultas:</p>
                <ul class="text-sm text-blue-800 space-y-1 text-left">
                  <li>• ¿Cuánto vendió el local 001 este mes?</li>
                  <li>• Muéstrame el ranking de tiendas por ventas</li>
                  <li>• ¿Cuáles son los productos más vendidos?</li>
                  <li>• ¿Cómo fue el tráfico por hora hoy?</li>
                </ul>
              </div>
            </div>
          } @else {
            <div class="space-y-2">
              @for (message of chatService.messages(); track message.id) {
                <app-chat-message [message]="message" />
              }

              @if (isStreaming()) {
                <app-chat-message [message]="streamingMessage()" />
              }
            </div>
          }

          @if (chatService.error()) {
            <div class="bg-red-50 border border-red-200 rounded-lg p-4 my-4">
              <div class="flex items-start gap-3">
                <svg class="w-5 h-5 text-red-600 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"></path>
                </svg>
                <div class="flex-1">
                  <h3 class="text-sm font-semibold text-red-900">Error</h3>
                  <p class="text-sm text-red-700">{{ chatService.error() }}</p>
                </div>
                <button
                  (click)="chatService.clearError()"
                  class="text-red-600 hover:text-red-800"
                >
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>
                  </svg>
                </button>
              </div>
            </div>
          }
        </div>
      </div>

      <!-- Input -->
      <app-chat-input
        (sendMessage)="handleSendMessage($event)"
        #chatInput
      />
    </div>
  `
})
export class ChatComponent implements AfterViewInit {
  protected readonly chatService = inject(ChatService);
  private readonly streamingService = inject(StreamingService);

  private readonly messagesContainer = viewChild<ElementRef>('messagesContainer');
  private readonly chatInput = viewChild<ChatInputComponent>('chatInput');

  protected readonly isStreaming = signal(false);
  protected readonly streamingMessage = signal<Message>({
    id: 'streaming',
    role: 'assistant',
    content: '',
    timestamp: new Date(),
    isStreaming: true
  });

  ngAfterViewInit(): void {
    this.scrollToBottom();
  }

  protected async handleSendMessage(content: string): Promise<void> {
    if (!content.trim() || this.isStreaming()) return;

    // Add user message
    this.chatService.addMessage({
      id: crypto.randomUUID(),
      role: 'user',
      content,
      timestamp: new Date()
    });

    this.scrollToBottom();
    this.isStreaming.set(true);
    this.chatInput()?.setDisabled(true);

    // Reset streaming message
    this.streamingMessage.set({
      id: 'streaming',
      role: 'assistant',
      content: '',
      timestamp: new Date(),
      isStreaming: true
    });

    try {
      await this.streamingService.streamChat(content, {
        onChunk: (chunk) => {
          this.streamingMessage.update(msg => ({
            ...msg,
            content: msg.content + chunk
          }));
          this.scrollToBottom();
        },
        onComplete: (fullResponse) => {
          this.chatService.addMessage({
            id: crypto.randomUUID(),
            role: 'assistant',
            content: fullResponse,
            timestamp: new Date()
          });
          this.isStreaming.set(false);
          this.chatInput()?.setDisabled(false);
          this.scrollToBottom();
        },
        onError: (error) => {
          console.error('Chat error:', error);
          this.chatService.setError(error.message);
          this.isStreaming.set(false);
          this.chatInput()?.setDisabled(false);
        }
      });
    } catch (error) {
      console.error('Unexpected error:', error);
      this.chatService.setError('Error inesperado al enviar el mensaje');
      this.isStreaming.set(false);
      this.chatInput()?.setDisabled(false);
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
