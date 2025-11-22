import React, { useState, FormEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { api, isValidEmail } from '../utils/api';
import { ReviewProps, ReviewData } from '../types';
import './Email.css';

const Email: React.FC<ReviewProps> = ({ onNavigate, onReview, reviewData }) => {
  const { token, user, logout } = useAuth();
  const [recipients, setRecipients] = useState<string>('');
  const [content, setContent] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  React.useEffect(() => {
    if (reviewData) {
      setRecipients(reviewData.recipients || '');
      setContent(reviewData.details || '');
    }
  }, [reviewData]);

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();

    if (!recipients.trim() || !content.trim()) {
      setError('Please fill in all fields');
      setSuccess('');
      return;
    }

    const emailList = recipients.split(',').map(email => email.trim());
    for (const email of emailList) {
      if (!isValidEmail(email)) {
        setError(`Invalid email: ${email}`);
        setSuccess('');
        return;
      }
    }

    if (!token) {
      setError('Authentication required. Please log in.');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const data = await api.sendEmail(token, recipients, content, user?.id);

      if (data.status === 'success' && onReview) {
        setSuccess('Email generated! Redirecting to review...');
        setTimeout(() => {
          const reviewData: ReviewData = {
            type: 'email',
            id: data.emailId || '',
            subject: data.subject || 'Draft Email',
            content: data.generatedContent || '',
            recipients: recipients,
            details: content
          };
          onReview(reviewData);
          setRecipients('');
          setContent('');
        }, 1500);
      } else {
        setError(data.message || 'Failed to generate email');
      }
    } catch (error) {
      console.error('Email error:', error);
      setError('Network error. Try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container email-page">
      <div className="header">
        <button className="btn-secondary" onClick={() => onNavigate('home')}>
          <span className="btn-icon">←</span>
          Back
        </button>
        <button className="btn-logout" onClick={logout}>
          <span className="btn-icon">🚪</span>
          Logout
        </button>
      </div>

      <div className="form-card">
        <div className="card-header">
          <div className="page-icon">✉️</div>
          <h1>Draft Email</h1>
          <p className="subtitle">Compose your message and let AI polish it for you</p>
        </div>

        <form onSubmit={handleSubmit} className="form">
          <div className="form-group">
            <label htmlFor="email-recipients">
              <span className="label-icon">📧</span>
              Recipient Email(s)
            </label>
            <input
              type="text"
              id="email-recipients"
              value={recipients}
              onChange={(e) => setRecipients(e.target.value)}
              placeholder="email@example.com, another@example.com"
              className="input-field"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="email-content">
              <span className="label-icon">💬</span>
              Message
            </label>
            <textarea
              id="email-content"
              value={content}
              onChange={(e) => setContent(e.target.value)}
              rows={6}
              placeholder="Type your message here..."
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
                Generating...
              </span>
            ) : (
              <>
                <span className="btn-icon">✨</span>
                Generate & Review
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Email;

