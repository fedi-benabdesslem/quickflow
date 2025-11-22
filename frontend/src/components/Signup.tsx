import React, { useState, FormEvent } from 'react';
import { api, isValidEmail } from '../utils/api';
import { NavigationProps } from '../types';
import './Signup.css';

const Signup: React.FC<NavigationProps> = ({ onNavigate }) => {
  const [username, setUsername] = useState<string>('');
  const [email, setEmail] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [confirmPassword, setConfirmPassword] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();

    if (!username.trim() || !email.trim() || !password.trim() || !confirmPassword.trim()) {
      setError('All fields required');
      return;
    }

    if (password !== confirmPassword) {
      setError('Passwords do not match');
      return;
    }

    if (!isValidEmail(email)) {
      setError('Invalid email format');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const data = await api.signup(username.trim(), email.trim(), password.trim());
      
      if (data.status === 'success') {
        setSuccess(data.message || 'Account created!');
        setTimeout(() => {
          onNavigate('login');
          setUsername('');
          setEmail('');
          setPassword('');
          setConfirmPassword('');
        }, 2000);
      } else {
        setError(data.message || 'Signup failed');
      }
    } catch (error) {
      console.error('Signup error:', error);
      setError('Network error. Try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container signup-page">
      <div className="form-card">
        <div className="card-header">
          <div className="logo-container">
            <div className="logo-icon">⚡</div>
          </div>
          <h1>Create Account</h1>
          <p className="subtitle">Join Electro'Com and get started</p>
        </div>
        
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="form-group">
            <label htmlFor="signup-username">
              <span className="label-icon">👤</span>
              Username
            </label>
            <input
              type="text"
              id="signup-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="Choose a username"
              className="input-field"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="signup-email">
              <span className="label-icon">📧</span>
              Email
            </label>
            <input
              type="email"
              id="signup-email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="your@email.com"
              className="input-field"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="signup-password">
              <span className="label-icon">🔒</span>
              Password
            </label>
            <input
              type="password"
              id="signup-password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Choose a password"
              className="input-field"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="signup-confirm-password">
              <span className="label-icon">🔒</span>
              Confirm Password
            </label>
            <input
              type="password"
              id="signup-confirm-password"
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm your password"
              className="input-field"
              disabled={loading}
            />
          </div>

          {error && <div className="error-message">{error}</div>}
          {success && <div className="success-message">{success}</div>}

          <button 
            type="submit" 
            className="btn-primary"
            disabled={loading}
          >
            {loading ? (
              <span className="button-loading">
                <span className="spinner-small"></span>
                Creating account...
              </span>
            ) : (
              'Sign Up'
            )}
          </button>

          <div className="form-footer">
            <p>
              Already have an account?{' '}
              <button 
                type="button"
                onClick={() => onNavigate('login')}
                className="link-button"
              >
                Login
              </button>
            </p>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Signup;

