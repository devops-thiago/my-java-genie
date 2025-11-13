import React from 'react';
import './LoadingIndicator.css';

interface LoadingIndicatorProps {
  status?: string;
}

const LoadingIndicator: React.FC<LoadingIndicatorProps> = ({ status }) => {
  const getStatusMessage = () => {
    switch (status) {
      case 'EMBEDDING':
        return 'Processing your question...';
      case 'SEARCHING':
        return 'Searching documentation...';
      case 'GENERATING':
        return 'Generating answer...';
      default:
        return 'Processing...';
    }
  };

  return (
    <div className="loading-indicator">
      <div className="loading-spinner"></div>
      <span className="loading-text">{getStatusMessage()}</span>
    </div>
  );
};

export default LoadingIndicator;
