import React from 'react';
import { ChatProvider, useChatContext } from './context/ChatContext';
import ChatWindow from './components/ChatWindow';
import './App.css';

const ChatApp: React.FC = () => {
  const { messages, sendMessage, clearConversation, isLoading, loadingStatus, error } = useChatContext();

  return (
    <div className="app">
      {error && (
        <div className="error-banner">
          <span>⚠️ {error}</span>
        </div>
      )}
      <ChatWindow
        messages={messages}
        onSendMessage={sendMessage}
        onClearHistory={clearConversation}
        isLoading={isLoading}
        loadingStatus={loadingStatus}
      />
    </div>
  );
};

const App: React.FC = () => {
  return (
    <ChatProvider>
      <ChatApp />
    </ChatProvider>
  );
};

export default App;
