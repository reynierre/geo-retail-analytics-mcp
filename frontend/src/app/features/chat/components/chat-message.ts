import { Component, ChangeDetectionStrategy, input } from '@angular/core';
import { Message } from '../models/message.model';

@Component({
  selector: 'app-chat-message',
  imports: [],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div [class]="containerClass()">
      <div [class]="bubbleClass()">
        <div class="flex items-start gap-2 mb-1">
          <span class="font-semibold text-sm">
            {{ message().role === 'user' ? 'Tú' : 'Asistente' }}
          </span>
          <span class="text-xs text-gray-500">
            {{ formatTime(message().timestamp) }}
          </span>
        </div>
        <div class="whitespace-pre-wrap break-words">
          {{ message().content }}
        </div>
        @if (message().isStreaming) {
          <span class="inline-block w-2 h-4 bg-blue-500 animate-pulse ml-1"></span>
        }
      </div>
    </div>
  `
})
export class ChatMessageComponent {
  readonly message = input.required<Message>();

  protected containerClass = () => {
    const isUser = this.message().role === 'user';
    return `flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`;
  };

  protected bubbleClass = () => {
    const isUser = this.message().role === 'user';
    const baseClass = 'max-w-[80%] rounded-lg p-4 shadow-sm';
    const colorClass = isUser
      ? 'bg-blue-600 text-white'
      : 'bg-white text-gray-900';
    return `${baseClass} ${colorClass}`;
  };

  protected formatTime(date: Date): string {
    return new Date(date).toLocaleTimeString('es-ES', {
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}
