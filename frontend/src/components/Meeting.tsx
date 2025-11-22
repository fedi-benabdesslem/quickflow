import React, { useState, useEffect, FormEvent, ChangeEvent } from 'react';
import { useAuth } from '../context/AuthContext';
import { api } from '../utils/api';
import { ReviewProps, ReviewData } from '../types';
import './Meeting.css';

interface MeetingFormData {
  people: string;
  location: string;
  timeBegin: string;
  timeEnd: string;
  date: string;
  subject: string;
  details: string;
}

const Meeting: React.FC<ReviewProps> = ({ onNavigate, onReview, reviewData }) => {
  const { token, logout } = useAuth();
  const [formData, setFormData] = useState<MeetingFormData>({
    people: '',
    location: '',
    timeBegin: '',
    timeEnd: '',
    date: '',
    subject: '',
    details: ''
  });
  const [error, setError] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);

  useEffect(() => {
    if (reviewData) {
      setFormData({
        people: reviewData.people || '',
        location: reviewData.location || '',
        timeBegin: reviewData.timeBegin || '',
        timeEnd: reviewData.timeEnd || '',
        date: reviewData.date || new Date().toISOString().split('T')[0],
        subject: reviewData.subject || '',
        details: reviewData.details || ''
      });
    } else {
      const today = new Date().toISOString().split('T')[0];
      setFormData(prev => ({ ...prev, date: today }));
    }
  }, [reviewData]);

  const handleChange = (e: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>): void => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleSubmit = async (e: FormEvent<HTMLFormElement>): Promise<void> => {
    e.preventDefault();

    const { people, location, timeBegin, timeEnd, date, subject } = formData;

    if (!people || !location || !timeBegin || !timeEnd || !date || !subject) {
      setError('Please fill in all fields');
      return;
    }

    if (new Date(timeEnd) <= new Date(timeBegin)) {
      setError('End time must be after start time');
      return;
    }

    if (!token) {
      setError('Authentication required. Please log in.');
      return;
    }

    setError('');
    setLoading(true);

    try {
      const requestData = {
        people: people.split(',').map(p => p.trim()),
        location: location,
        timeBegin: timeBegin,
        timeEnd: timeEnd,
        date: date,
        subject: subject,
        details: formData.details
      };

      const data = await api.generateMeeting(token, requestData);

      if (data.status === 'success' && onReview) {
        const reviewData: ReviewData = {
          type: 'pv',
          id: data.id || data.meetingId || '',
          subject: data.subject || subject,
          content: data.generatedContent || '',
          people: people,
          location: location,
          date: date,
          timeBegin: timeBegin,
          timeEnd: timeEnd,
          details: formData.details
        };
        onReview(reviewData);
        setFormData({
          people: '',
          location: '',
          timeBegin: '',
          timeEnd: '',
          date: new Date().toISOString().split('T')[0],
          subject: '',
          details: ''
        });
      } else {
        setError(data.message || 'Failed to generate summary');
      }
    } catch (error) {
      console.error('Meeting error:', error);
      setError('Network error. Try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-container meeting-page">
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
          <div className="page-icon">📝</div>
          <h1>Create Meeting Summary</h1>
          <p className="subtitle">Fill in the meeting details to generate a professional summary</p>
        </div>

        <form onSubmit={handleSubmit} className="form">
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="meeting-people">
                <span className="label-icon">👥</span>
                Attendees
              </label>
              <input
                type="text"
                id="meeting-people"
                name="people"
                value={formData.people}
                onChange={handleChange}
                placeholder="John, Jane, Team Lead"
                className="input-field"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="meeting-location">
                <span className="label-icon">📍</span>
                Location
              </label>
              <input
                type="text"
                id="meeting-location"
                name="location"
                value={formData.location}
                onChange={handleChange}
                placeholder="Conference Room A"
                className="input-field"
                disabled={loading}
              />
            </div>
          </div>

          <div className="form-row">
            <div className="form-group">
              <label htmlFor="meeting-time-begin">
                <span className="label-icon">🕐</span>
                Start Time
              </label>
              <input
                type="time"
                id="meeting-time-begin"
                name="timeBegin"
                value={formData.timeBegin}
                onChange={handleChange}
                className="input-field"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="meeting-time-end">
                <span className="label-icon">🕐</span>
                End Time
              </label>
              <input
                type="time"
                id="meeting-time-end"
                name="timeEnd"
                value={formData.timeEnd}
                onChange={handleChange}
                className="input-field"
                disabled={loading}
              />
            </div>

            <div className="form-group">
              <label htmlFor="meeting-date">
                <span className="label-icon">📅</span>
                Date
              </label>
              <input
                type="date"
                id="meeting-date"
                name="date"
                value={formData.date}
                onChange={handleChange}
                min={new Date().toISOString().split('T')[0]}
                className="input-field"
                disabled={loading}
              />
            </div>
          </div>

          <div className="form-group">
            <label htmlFor="meeting-subject">
              <span className="label-icon">📌</span>
              Subject
            </label>
            <input
              type="text"
              id="meeting-subject"
              name="subject"
              value={formData.subject}
              onChange={handleChange}
              placeholder="Meeting topic"
              className="input-field"
              disabled={loading}
            />
          </div>

          <div className="form-group">
            <label htmlFor="meeting-details">
              <span className="label-icon">📄</span>
              Details
            </label>
            <textarea
              id="meeting-details"
              name="details"
              value={formData.details}
              onChange={handleChange}
              rows={4}
              placeholder="Additional information..."
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
                Generating...
              </span>
            ) : (
              <>
                <span className="btn-icon">✨</span>
                Generate Summary
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
};

export default Meeting;

