import { Component, ChangeDetectionStrategy, output, signal, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SpinnerComponent } from '@shared/components/spinner';

@Component({
  selector: 'app-chat-input',
  imports: [FormsModule, SpinnerComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="border-t bg-white p-4 shadow-lg">
      <div class="flex gap-3 max-w-4xl mx-auto">
        <textarea
          [(ngModel)]="inputValue"
          (keydown)="onKeydown($event)"
          [disabled]="isDisabled()"
          class="flex-1 p-3 border border-gray-300 rounded-lg resize-none focus:outline-none focus:ring-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed transition-all"
          rows="2"
          placeholder="Pregunta sobre las ventas, ej: ¿Cuánto vendió el local 001 este mes?"
          autofocus
        ></textarea>
        <button
          (click)="handleSend()"
          [disabled]="isDisabled() || !inputValue.trim()"
          class="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-colors font-semibold flex items-center justify-center min-w-[100px]"
        >
          @if (isDisabled()) {
            <app-spinner size="small" />
          } @else {
            Enviar
          }
        </button>
      </div>
    </div>
  `
})
export class ChatInputComponent {
  readonly sendMessage = output<string>();
  readonly isDisabled = signal(false);

  protected inputValue = '';

  setDisabled(disabled: boolean): void {
    this.isDisabled.set(disabled);
  }

  clear(): void {
    this.inputValue = '';
  }

  protected handleSend(): void {
    const value = this.inputValue.trim();
    if (!value || this.isDisabled()) return;

    this.sendMessage.emit(value);
    this.clear();
  }

  protected onKeydown(event: KeyboardEvent): void {
    if (event.key === 'Enter' && !event.shiftKey && !event.ctrlKey) {
      event.preventDefault();
      this.handleSend();
    }
  }
}
