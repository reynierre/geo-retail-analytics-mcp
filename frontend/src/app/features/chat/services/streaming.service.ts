import { Injectable } from '@angular/core';

interface StreamCallbacks {
  onChunk: (chunk: string) => void;
  onComplete: (fullResponse: string) => void;
  onError: (error: Error) => void;
}

@Injectable({ providedIn: 'root' })
export class StreamingService {
  private readonly apiUrl = '/api';

  async streamChat(message: string, callbacks: StreamCallbacks): Promise<void> {
    try {
      const response = await fetch(`${this.apiUrl}/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream'
        },
        body: JSON.stringify({ message })
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
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
              const data = line.slice(6).trim();

              if (data === '[DONE]') {
                callbacks.onComplete(fullResponse);
                return;
              }

              if (data) {
                try {
                  const parsed = JSON.parse(data);
                  if (parsed.content) {
                    fullResponse += parsed.content;
                    callbacks.onChunk(parsed.content);
                  } else if (typeof parsed === 'string') {
                    fullResponse += parsed;
                    callbacks.onChunk(parsed);
                  }
                } catch {
                  // Plain text chunk (not JSON)
                  fullResponse += data;
                  callbacks.onChunk(data);
                }
              }
            }
          }
        }

        callbacks.onComplete(fullResponse);
      } catch (error) {
        throw error;
      } finally {
        reader.releaseLock();
      }
    } catch (error) {
      callbacks.onError(error instanceof Error ? error : new Error(String(error)));
    }
  }
}
