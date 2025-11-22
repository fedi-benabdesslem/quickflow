import { ApiResponse, MeetingFormData } from '../types';

// Use proxy (relative URLs) by default, or set REACT_APP_API_URL for direct connection
const API_BASE_URL = process.env.REACT_APP_API_URL || '';

export const api = {
  login: async (username: string, password: string): Promise<ApiResponse> => {
    const url = API_BASE_URL ? `${API_BASE_URL}/api/login` : `/api/login`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, password })
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ status: 'error', message: 'Network error' }));
      return errorData;
    }
    
    return response.json();
  },

  signup: async (username: string, email: string, password: string): Promise<ApiResponse> => {
    const url = API_BASE_URL ? `${API_BASE_URL}/api/signup` : `/api/signup`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({ username, email, password })
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ status: 'error', message: 'Network error' }));
      return errorData;
    }
    
    return response.json();
  },

  generateMeeting: async (token: string, formData: MeetingFormData): Promise<ApiResponse> => {
    const url = API_BASE_URL ? `${API_BASE_URL}/api/meeting/generate` : `/api/meeting/generate`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(formData)
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ status: 'error', message: 'Network error' }));
      return errorData;
    }
    
    return response.json();
  },

  sendEmail: async (token: string, recipients: string, content: string, userId?: string): Promise<ApiResponse> => {
    const url = API_BASE_URL ? `${API_BASE_URL}/api/email/send` : `/api/email/send`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify({
        recipients: recipients.split(',').map(email => email.trim()),
        userId: userId || 'anonymous',
        input: content,
        bulletPoints: []
      })
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ status: 'error', message: 'Network error' }));
      return errorData;
    }
    
    return response.json();
  },

  sendFinal: async (token: string, reviewType: 'email' | 'pv', data: Record<string, unknown>): Promise<ApiResponse> => {
    const url = API_BASE_URL ? `${API_BASE_URL}/api/${reviewType}/send-final` : `/api/${reviewType}/send-final`;
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`
      },
      body: JSON.stringify(data)
    });
    
    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ status: 'error', message: 'Network error' }));
      return errorData;
    }
    
    return response.json();
  }
};

export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

