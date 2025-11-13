import React from 'react';
import MessageList from './MessageList';
import MessageInput from './MessageInput';
import LoadingIndicator from './LoadingIndicator';
import { ChatMessage } from '../types/chat';
import './ChatWindow.css';

interface ChatWindowProps {
  messages: ChatMessage[];
  onSendMessage: (message: string) => void;
  onClearHistory: () => void;
  isLoading: boolean;
  loadingStatus?: string;
}

const ChatWindow: React.FC<ChatWindowProps> = ({
  messages,
  onSendMessage,
  onClearHistory,
  isLoading,
  loadingStatus,
}) => {
  return (
    <div className="chat-window">
      <div className="chat-header">
        <div className="chat-title">
          <h1>Java 25 RAG Assistant</h1>
          <p>Ask questions about Java 25 documentation</p>
        </div>
        <button className="clear-button" onClick={onClearHistory}>
          Clear History
        </button>
      </div>
      <MessageList messages={messages} />
      {isLoading && <LoadingIndicator status={loadingStatus} />}
      <MessageInput onSendMessage={onSendMessage} disabled={isLoading} />
    </div>
  );
};

export default ChatWindow;
