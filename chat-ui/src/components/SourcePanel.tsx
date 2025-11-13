import React from 'react';
import { SourceReference } from '../types/chat';
import './SourcePanel.css';

interface SourcePanelProps {
  sources: SourceReference[];
}

const SourcePanel: React.FC<SourcePanelProps> = ({ sources }) => {
  if (!sources || sources.length === 0) {
    return null;
  }

  return (
    <div className="source-panel">
      <div className="source-header">Sources:</div>
      <div className="source-list">
        {sources.map((source, index) => (
          <div key={index} className="source-item">
            <span className="source-icon">📄</span>
            <div className="source-details">
              <div className="source-filename">{source.filename}</div>
              {source.section && (
                <div className="source-section">{source.section}</div>
              )}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default SourcePanel;
