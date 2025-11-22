import React from 'react';
import { useAuth } from '../context/AuthContext';
import { NavigationProps } from '../types';
import './Home.css';

const Home: React.FC<NavigationProps> = ({ onNavigate }) => {
  const { user, logout } = useAuth();

  return (
    <div className="page-container home-page">
      <div className="header">
        <button className="btn-logout" onClick={logout}>
          <span className="btn-icon">🚪</span>
          Logout
        </button>
      </div>

      <div className="center-content">
        <div className="welcome-card">
          <div className="welcome-icon">👋</div>
          <h1>Welcome back, <span className="username-highlight">{user?.username}</span>!</h1>
          <p className="welcome-subtitle">Choose an action to get started with AI assistance</p>
        </div>

        <div className="action-cards">
          <div 
            className="action-card meeting-card"
            onClick={() => onNavigate('meeting')}
          >
            <div className="card-icon">📝</div>
            <h3>Create Meeting Summary</h3>
            <p>Generate professional meeting summaries (PV) with AI</p>
            <div className="card-arrow">→</div>
          </div>

          <div 
            className="action-card email-card"
            onClick={() => onNavigate('email')}
          >
            <div className="card-icon">✉️</div>
            <h3>Draft Email</h3>
            <p>Create polished emails with AI-powered assistance</p>
            <div className="card-arrow">→</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Home;

