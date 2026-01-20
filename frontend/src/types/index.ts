export interface User {
    id: string;
    username: string;
    email?: string;
}

export interface AuthContextType {
    user: User | null;
    session: Session | null;
    loading: boolean;
    signIn: (email: string, password: string) => Promise<AuthResult>;
    signUp: (email: string, password: string, username: string) => Promise<AuthResult>;
    signInWithGoogle: () => Promise<AuthResult>;
    signInWithMicrosoft: () => Promise<AuthResult>;
    signOut: () => Promise<void>;
    resetPassword: (email: string) => Promise<AuthResult>;
}

export interface Session {
    access_token: string;
    refresh_token: string;
    expires_at?: number;
}

export interface AuthResult {
    success: boolean;
    error?: string;
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
    people: string[] | string;
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

export interface ReviewContextType {
    reviewData: ReviewData | null;
    setReviewData: (data: ReviewData | null) => void;
}
