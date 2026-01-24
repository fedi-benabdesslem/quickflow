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
    status: 'success' | 'error' | 'unsupported' | 'reauth_required';
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

// ===== Meeting Minutes Types (Phase 1 Step 1) =====

export interface Participant {
    id: string;
    name: string;
    email?: string;
    role?: 'Chair' | 'Secretary' | 'Member' | 'Guest' | string;
    present: boolean;
}

export interface AgendaItem {
    id: string;
    title: string;
    objective: 'Discussion' | 'Decision' | 'Review' | 'Information';
    keyPoints?: string;
}

export interface Decision {
    id: string;
    statement: string;
    status: 'Approved' | 'Rejected' | 'Deferred' | 'No Decision';
    relatedAgendaId?: string;
    rationale?: string;
}

export interface ActionItem {
    id: string;
    task: string;
    owner?: string;
    deadline?: string;
    priority?: 'High' | 'Medium' | 'Low';
}

export interface OutputPreferences {
    tone: 'Formal' | 'Executive' | 'Technical';
    length: 'Standard' | 'Detailed' | 'Summary';
    includeSections: {
        attendees: boolean;
        agenda: boolean;
        decisions: boolean;
        actionItems: boolean;
        additionalNotes: boolean;
    };
    pdfFooter: 'None' | 'Confidential' | 'Internal Use Only';
}

export interface QuickModeData {
    content: string;
    file?: File;
    date?: string;
    time?: string;
}

export interface StructuredModeData {
    meetingInfo: {
        title: string;
        date: string;
        startTime: string;
        endTime: string;
        location: string;
        organizer?: string;
    };
    participants: Participant[];
    absentParticipants: Participant[];
    agenda: AgendaItem[];
    decisions: Decision[];
    actionItems: ActionItem[];
    additionalNotes?: string;
    outputPreferences: OutputPreferences;
}

// ===== AI Extraction Types (Phase 1 Step 2) =====

export interface ExtractedDecision {
    statement: string;
    status: string;
}

export interface ExtractedActionItem {
    task: string;
    owner?: string;
    deadline?: string;
}

export interface ExtractedMeetingData {
    meetingTitle?: string;
    date?: string;
    time?: string;
    participants: string[];
    discussionPoints: string[];
    decisions: ExtractedDecision[];
    actionItems: ExtractedActionItem[];
    confidence: 'high' | 'medium' | 'low';
}

