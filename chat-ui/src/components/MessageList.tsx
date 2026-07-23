import React, { useEffect, useRef } from 'react';
import ReactMarkdown from 'react-markdown';
import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter';
import { oneLight } from 'react-syntax-highlighter/dist/esm/styles/prism';
import remarkGfm from 'remark-gfm';
import { ChatMessage } from '../types/chat';
import SourcePanel from './SourcePanel';
import './MessageList.css';

interface MessageListProps {
  messages: ChatMessage[];
}

const MessageList: React.FC<MessageListProps> = ({ messages }) => {
  const messagesEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const messageArray = Array.isArray(messages)
    ? messages.filter(msg => msg && msg.role && msg.content && msg.id && msg.timestamp)
    : [];

  return (
    <div className="message-list">
      {messageArray.length === 0 ? (
        <div className="empty-state">
          <h2>Welcome to Java 25 RAG Assistant</h2>
          <p>Ask me anything about Java 25 features and documentation!</p>
        </div>
      ) : (
        messageArray.map(message => (
          <div key={message.id} className={`message ${message.role}`}>
            <div className="message-header">
              <span className="message-role">
                {message.role === 'user' ? '👤 You' : '🤖 Assistant'}
              </span>
              <span className="message-time">
                {new Date(message.timestamp).toLocaleTimeString()}
              </span>
            </div>
            <div className="message-content">
              {message.role === 'user' ? (
                <p>{message.content}</p>
              ) : (
                <ReactMarkdown
                  remarkPlugins={[remarkGfm]}
                  components={{
                    code({ inline, className, children }: any) {
                      const match = /language-(\w+)/.exec(className || '');
                      if (!inline) {
                        return (
                          <SyntaxHighlighter
                            style={oneLight}
                            language={match?.[1] || 'text'}
                            PreTag="div"
                            customStyle={{
                              margin: '12px 0',
                              borderRadius: '8px',
                              border: '1px solid #e5e7eb',
                              background: '#f8fafc',
                              fontSize: '13px',
                            }}
                            codeTagProps={{
                              style: {
                                fontFamily:
                                  'ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace',
                              },
                            }}
                          >
                            {String(children).replace(/\n$/, '')}
                          </SyntaxHighlighter>
                        );
                      }

                      return <code className={`inline-code ${className || ''}`}>{children}</code>;
                    },
                  }}
                >
                  {message.content}
                </ReactMarkdown>
              )}
              {message.sources && message.sources.length > 0 && (
                <SourcePanel sources={message.sources} />
              )}
            </div>
          </div>
        ))
      )}
      <div ref={messagesEndRef} />
    </div>
  );
};

export default MessageList;
