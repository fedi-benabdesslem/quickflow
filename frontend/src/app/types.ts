export interface User {
  id: string;
  username: string;
  email?: string;
}

export interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (userData: User, authToken: string) => void;
  logout: () => void;
  loading: boolean;
}

export interface ApiResponse {
  status: 'success' | 'error';
  message?: string;
  token?: string;
  user?: User;
  id?: string;
  meetingId?: string;
  emailId?: string;
  subject?: string;
  generatedContent?: string;
}

export interface MeetingFormData {
  people: string[];
  location: string;
  timeBegin: string;
  timeEnd: string;
  date: string;
  subject: string;
  details: string;
}

export interface ReviewData {
  type: 'email' | 'pv';
  id: string;
  subject: string;
  content: string;
  recipients?: string;
  people?: string;
  location?: string;
  date?: string;
  timeBegin?: string;
  timeEnd?: string;
  details?: string;
}

export type Page = 'login' | 'signup' | 'home' | 'meeting' | 'email' | 'review';
