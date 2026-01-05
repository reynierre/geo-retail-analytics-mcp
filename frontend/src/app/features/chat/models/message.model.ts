export interface Message {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  isStreaming?: boolean;
}

export interface ChatRequest {
  message: string;
}

export interface ChatResponse {
  content: string;
  timestamp: string;
}
