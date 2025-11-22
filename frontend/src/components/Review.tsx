import React, { useState, useEffect, ChangeEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../utils/api';
import { ReviewProps } from '../types';
import './Review.css';

const Review: React.FC<ReviewProps> = ({ onNavigate, reviewData, setReviewData }) => {
  const { token, logout } = useAuth();
  const [subject, setSubject] = useState<string>('');
  const [content, setContent] = useState<string>('');
  const [recipients, setRecipients] = useState<string>('');
  const [people, setPeople] = useState<string>('');
  const [location, setLocation] = useState<string>('');
  const [date, setDate] = useState<string>('');
  const [error, setError] = useState<string>('');
  const [success, setSuccess] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    if (reviewData) {
      setSubject(reviewData.subject || '');
      setContent(reviewData.content || '');
      if (reviewData.type === 'email') {
        setRecipients(reviewData.recipients || '');
      } else {
        setPeople(reviewData.people || '');
        setLocation(reviewData.location || '');
        setDate(reviewData.date || '');
      }
    }
  }, [reviewData]);

  const handleSend = async (): Promise<void> => {
    if (!content.trim() || !subject.trim()) {
      setError('Subject and content required');
      return;
    }

    if (!token || !reviewData) {
      setError('Authentication required. Please log in.');
      return;
    }

    setError('');
    setSuccess('');
    setLoading(true);

    try {
      const requestData: Record<string, unknown> = {
        id: reviewData.id,
        subject: subject,
        content: content,
      };

      if (reviewData.type === 'email') {
        requestData.recipients = recipients.split(',').map(r => r.trim());
      } else {
        requestData.people = people.split(',').map(p => p.trim());
        requestData.location = location;
        requestData.date = date;
      }

      const data = await api.sendFinal(token, reviewData.type, requestData);

      if (data.status === 'success') {
        setSuccess(data.message || 'Sent successfully!');
        setTimeout(() => {
          onNavigate('home');
          clearForm();
        }, 2000);
      } else {
        setError(data.message || 'Failed to send');
      }
    } catch (error) {
      console.error('Send error:', error);
      setError('Network error. Try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleEdit = (): void => {
    if (reviewData) {
      if (reviewData.type === 'email') {
        onNavigate('email');
      } else {
        onNavigate('meeting');
      }
    }
  };

  const clearForm = (): void => {
    setSubject('');
    setContent('');
    setRecipients('');
    setPeople('');
    setLocation('');
    setDate('');
    setError('');
    setSuccess('');
    if (setReviewData) {
      setReviewData(null);
    }
  };

  if (!reviewData) {
    return null;
  }

  return (
    <div className="page-container review-page">
      <div className="header">
        <button className="btn-secondary" onClick={() => {
          onNavigate('home');
          clearForm();
        }}>
          <span className="btn-icon">←</span>
          Back
        </button>
        <button className="btn-logout" onClick={logout}>
          <span className="btn-icon">🚪</span>
          Logout
        </button>
      </div>

      <div className="form-card review-card">
        <div className="card-header">
          <div className="page-icon">👁️</div>
          <h1>Review & Edit</h1>
          <p className="subtitle">
            {reviewData.type === 'email' ? 'Review Email Draft' : 'Review Meeting Summary (PV)'}
          </p>
        </div>

        <div className="form">
          <div className="form-group">
            <label htmlFor="review-subject">
              <span className="label-icon">📌</span>
              Subject
            </label>
            <input
              type="text"
              id="review-subject"
              value={subject}
              onChange={(e: ChangeEvent<HTMLInputElement>) => setSubject(e.target.value)}
              placeholder="Subject"
              className="input-field"
              disabled={loading}
            />
          </div>

          {reviewData.type === 'email' ? (
            <div className="review-section">
              <div className="form-group">
                <label htmlFor="review-recipients">
                  <span className="label-icon">📧</span>
                  Recipients
                </label>
                <input
                  type="text"
                  id="review-recipients"
                  value={recipients}
                  onChange={(e: ChangeEvent<HTMLInputElement>) => setRecipients(e.target.value)}
                  placeholder="email@example.com"
                  className="input-field"
                  disabled={loading}
                />
              </div>
            </div>
          ) : (
            <div className="review-section">
              <div className="form-row">
                <div className="form-group">
                  <label htmlFor="review-people">
                    <span className="label-icon">👥</span>
                    Attendees
                  </label>
                  <input
                    type="text"
                    id="review-people"
                    value={people}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setPeople(e.target.value)}
                    placeholder="John, Jane"
                    className="input-field"
                    disabled={loading}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="review-location">
                    <span className="label-icon">📍</span>
                    Location
                  </label>
                  <input
                    type="text"
                    id="review-location"
                    value={location}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setLocation(e.target.value)}
                    placeholder="Conference Room"
                    className="input-field"
                    disabled={loading}
                  />
                </div>
                <div className="form-group">
                  <label htmlFor="review-date">
                    <span className="label-icon">📅</span>
                    Date
                  </label>
                  <input
                    type="date"
                    id="review-date"
                    value={date}
                    onChange={(e: ChangeEvent<HTMLInputElement>) => setDate(e.target.value)}
                    className="input-field"
                    disabled={loading}
                  />
                </div>
              </div>
            </div>
          )}

          <div className="form-group">
            <label htmlFor="review-content">
              <span className="label-icon">📄</span>
              Content
            </label>
            <textarea
              id="review-content"
              value={content}
              onChange={(e: ChangeEvent<HTMLTextAreaElement>) => setContent(e.target.value)}
              rows={10}
              placeholder="Generated content will appear here..."
              className="input-field"
              disabled={loading}
            />
          </div>

          {error && <div className="error-message">{error}</div>}
          {success && <div className="success-message">{success}</div>}

          <div className="button-group">
            <button
              type="button"
              className="btn-primary btn-send"
              onClick={handleSend}
              disabled={loading}
            >
              {loading ? (
                <span className="button-loading">
                  <span className="spinner-small"></span>
                  Sending...
                </span>
              ) : (
                <>
                  <span className="btn-icon">📤</span>
                  Send Final
                </>
              )}
            </button>
            <button
              type="button"
              className="btn-secondary"
              onClick={handleEdit}
              disabled={loading}
            >
              <span className="btn-icon">✏️</span>
              Edit Original
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Review;

