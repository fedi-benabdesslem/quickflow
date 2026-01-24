import { StructuredModeData } from './index';

export interface MeetingTemplateData {
    meetingInfo: {
        title: string;
        defaultDuration: number; // minutes
        defaultStartTime: string;
        location: string;
        organizer?: string;
    };
    participants: {
        name: string;
        email?: string;
        role?: string;
    }[];
    agendaStructure: {
        title: string;
        objective: string;
    }[];
    outputPreferences: StructuredModeData['outputPreferences'];
}

export interface MeetingTemplate {
    id: string;
    userId: string;
    name: string;
    description?: string;
    templateData: MeetingTemplateData;
    usageCount: number;
    lastUsed?: string;
    createdAt: string;
    updatedAt: string;
}

export interface CreateMeetingTemplateRequest {
    name: string;
    description?: string;
    templateData: MeetingTemplateData;
}

export interface UpdateMeetingTemplateRequest {
    name?: string;
    description?: string;
    templateData?: MeetingTemplateData;
}
