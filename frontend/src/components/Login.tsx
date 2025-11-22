import React, { useState, FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../utils/api';
import { NavigationProps } from '../types';
import './Login.css';

const Login: React.FC<NavigationProps> = ({ onNavigate }) => {
  const { login } = useAuth();
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();
    
    if (!username.trim() || !password.trim()) {
      setError('Username and password required');
      return;
    }

    setError('');
    setLoading(true);

    try {
      const data = await api.login(username.trim(), password.trim());
      
      if (data.status === 'success' && data.user && data.token) {
        login(data.user, data.token);
        onNavigate('home');
        setUsername('');
        setPassword('');
      } else {
        setError(data.message || 'Login failed');
      }
    } catch (error) {
      console.error('Login error:', error);
      setError('Network error. Check connection.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container login-page">
      <div className="form-card">
        <div className="card-header">
          <div className="logo-container">
            <div className="logo-icon">⚡</div>
          </div>
          <h1>Welcome Back</h1>
          <p className="subtitle">Sign in to continue to Electro'Com</p>
        </div>
        
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="login-username">
              <span className="label-icon">👤</span>
              Username
            </label>
            <input
              type="text"
              id="login-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Enter your username"
              className="input-field"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">
              <span className="label-icon">🔒</span>
              Password
            </label>
            <input
              type="password"
              id="login-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
              className="input-field"
              disabled={loading}
            />
          </div>

          {error && <div className="error-message">{error}</div>}

          <button 
            type="submit" 
            className="btn-primary"
            disabled={loading}
          >
            {loading ? (
              <span className="button-loading">
                <span className="spinner-small"></span>
                Signing in...
              </span>
            ) : (
              'Sign In'
            )}
          </button>

          <div className="form-footer">
            <p>
              Don't have an account?{' '}
              <button 
                type="button"
                onClick={() => onNavigate('signup')}
                className="link-button"
              >
                Sign Up
              </button>
            </p>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;

