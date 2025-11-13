export interface ChatMessage {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: string;
  sources?: SourceReference[];
}

export interface SourceReference {
  filename: string;
  section?: string;
  chunkIndex: number;
}

export interface ChatRequest {
  sessionId: string;
  message: string;
}

export interface ChatResponse {
  sessionId: string;
  answer: string;
  sources: SourceReference[];
  tokenUsage?: {
    promptTokens: number;
    completionTokens: number;
    totalTokens: number;
  };
  responseTimeMs?: number;
}

export interface QueryStatus {
  status: 'EMBEDDING' | 'SEARCHING' | 'GENERATING' | 'COMPLETE' | 'ERROR';
  message?: string;
}
