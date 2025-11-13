import axios, { AxiosError } from 'axios';
import { ChatRequest, ChatResponse, ChatMessage } from '../types/chat';

const API_BASE_URL = process.env.REACT_APP_API_URL || '/api';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 120000, // 120 seconds (2 minutes) - allows time for LLM processing with large context
});

export class ApiError extends Error {
  constructor(
    message: string,
    public statusCode?: number,
    public originalError?: any
  ) {
    super(message);
    this.name = 'ApiError';
  }
}

const handleApiError = (error: AxiosError): never => {
  if (error.response) {
    // Server responded with error status
    const message = (error.response.data as any)?.message || error.message;
    throw new ApiError(
      message,
      error.response.status,
      error.response.data
    );
  } else if (error.request) {
    // Request made but no response
    throw new ApiError(
      'No response from server. Please check your connection.',
      undefined,
      error
    );
  } else {
    // Error setting up request
    throw new ApiError(error.message, undefined, error);
  }
};

export const queryChat = async (
  sessionId: string,
  message: string
): Promise<ChatResponse> => {
  try {
    const request: ChatRequest = { sessionId, message };
    const response = await apiClient.post<ChatResponse>('/chat/query', request);
    return response.data;
  } catch (error) {
    return handleApiError(error as AxiosError);
  }
};

export const getHistory = async (sessionId: string): Promise<ChatMessage[]> => {
  try {
    const response = await apiClient.get<ChatMessage[]>('/chat/history', {
      params: { sessionId },
    });
    // Ensure we always return an array
    return Array.isArray(response.data) ? response.data : [];
  } catch (error) {
    const axiosError = error as AxiosError;
    // Return empty array for 404 (session not found)
    if (axiosError.response?.status === 404) {
      return [];
    }
    return handleApiError(axiosError);
  }
};

export const clearHistory = async (sessionId: string): Promise<void> => {
  try {
    await apiClient.delete('/chat/history', {
      params: { sessionId },
    });
  } catch (error) {
    return handleApiError(error as AxiosError);
  }
};

const apiService = {
  queryChat,
  getHistory,
  clearHistory,
};

export default apiService;
