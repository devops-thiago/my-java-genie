import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { ChatMessage, QueryStatus } from '../types/chat';
import { queryChat, getHistory, clearHistory, ApiError } from '../services/api';
import websocketService from '../services/websocket';
import { v4 as uuidv4 } from 'uuid';

interface ChatContextType {
  messages: ChatMessage[];
  sessionId: string;
  isLoading: boolean;
  loadingStatus?: string;
  error: string | null;
  sendMessage: (message: string) => Promise<void>;
  clearConversation: () => Promise<void>;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

const SESSION_ID_KEY = 'chat_session_id';

const getOrCreateSessionId = (): string => {
  let sessionId = localStorage.getItem(SESSION_ID_KEY);
  if (!sessionId) {
    sessionId = uuidv4();
    localStorage.setItem(SESSION_ID_KEY, sessionId);
  }
  return sessionId;
};

interface ChatProviderProps {
  children: ReactNode;
}

export const ChatProvider: React.FC<ChatProviderProps> = ({ children }) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [sessionId] = useState<string>(getOrCreateSessionId());
  const [isLoading, setIsLoading] = useState(false);
  const [loadingStatus, setLoadingStatus] = useState<string | undefined>();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Load conversation history on mount
    const loadHistory = async () => {
      try {
        const history = await getHistory(sessionId);
        // Ensure we always set an array and filter out invalid messages
        const validHistory = Array.isArray(history) 
          ? history.filter(msg => msg && msg.role && msg.content) 
          : [];
        setMessages(validHistory);
      } catch (err) {
        console.error('Error loading history:', err);
        // Set empty array on error
        setMessages([]);
        // Don't show error for empty history
        if (err instanceof ApiError && err.statusCode !== 404) {
          setError('Failed to load conversation history');
        }
      }
    };

    loadHistory();

    // Connect WebSocket
    websocketService.connect(sessionId);

    // Subscribe to status updates
    const unsubscribe = websocketService.onStatusUpdate((status: QueryStatus) => {
      setLoadingStatus(status.status);
      
      if (status.status === 'ERROR') {
        setIsLoading(false);
        setError(status.message || 'An error occurred');
      } else if (status.status === 'COMPLETE') {
        setIsLoading(false);
        setLoadingStatus(undefined);
      }
    });

    return () => {
      unsubscribe();
      websocketService.disconnect();
    };
  }, [sessionId]);

  const sendMessage = async (messageText: string): Promise<void> => {
    setError(null);
    setIsLoading(true);

    // Add user message immediately
    const userMessage: ChatMessage = {
      id: uuidv4(),
      role: 'user',
      content: messageText,
      timestamp: new Date().toISOString(),
    };
    setMessages(prev => [...prev, userMessage]);

    try {
      const response = await queryChat(sessionId, messageText);
      
      // Convert backend response to ChatMessage format
      const assistantMessage: ChatMessage = {
        id: uuidv4(),
        role: 'assistant',
        content: response.answer,
        timestamp: new Date().toISOString(),
        sources: response.sources,
      };
      
      // Add assistant message
      setMessages(prev => [...prev, assistantMessage]);
    } catch (err) {
      console.error('Error sending message:', err);
      const errorMessage = err instanceof ApiError 
        ? err.message 
        : 'Failed to send message. Please try again.';
      setError(errorMessage);
    } finally {
      setIsLoading(false);
      setLoadingStatus(undefined);
    }
  };

  const clearConversation = async (): Promise<void> => {
    try {
      await clearHistory(sessionId);
      setMessages([]);
      setError(null);
    } catch (err) {
      console.error('Error clearing history:', err);
      setError('Failed to clear conversation history');
    }
  };

  const value: ChatContextType = {
    messages,
    sessionId,
    isLoading,
    loadingStatus,
    error,
    sendMessage,
    clearConversation,
  };

  return <ChatContext.Provider value={value}>{children}</ChatContext.Provider>;
};

export const useChatContext = (): ChatContextType => {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChatContext must be used within a ChatProvider');
  }
  return context;
};
