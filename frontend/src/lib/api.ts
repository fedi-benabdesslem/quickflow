import axios from 'axios'
import type { ApiResponse, MeetingFormData } from '../types'
import { getAccessToken, isTokenExpiring, refreshAccessToken, setAccessToken } from './tokenManager'

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || ''

const api = axios.create({
    baseURL: `${BACKEND_URL}/api`,
    headers: {
        'Content-Type': 'application/json',
        'ngrok-skip-browser-warning': 'true',
    },
    withCredentials: true, // Send httpOnly refresh token cookie
})

// Request interceptor — attach access token and auto-refresh if expiring
api.interceptors.request.use(
    async (config) => {
        // Try to refresh if token is expiring
        if (isTokenExpiring()) {
            const newToken = await refreshAccessToken(BACKEND_URL)
            if (newToken) {
                config.headers.Authorization = `Bearer ${newToken}`
                return config
            }
        }

        const token = getAccessToken()
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    (error) => Promise.reject(error)
)

// Response interceptor — retry on 401 after refreshing token
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true
            const newToken = await refreshAccessToken(BACKEND_URL)
            if (newToken) {
                originalRequest.headers.Authorization = `Bearer ${newToken}`
                return api(originalRequest)
            }
            // Refresh failed — let the caller handle the 401.
            // Don't clear tokens or redirect here; the access token may still be valid
            // for other endpoints, and AuthContext manages session lifecycle.
        }
        return Promise.reject(error)
    }
)

// Legacy compat — still used by AuthContext to set token after login
export const setAuthToken = (token: string | null) => {
    setAccessToken(token)
}

// API methods
export const generateMeeting = async (formData: MeetingFormData): Promise<ApiResponse> => {
    try {
        const requestData = {
            ...formData,
            people: typeof formData.people === 'string'
                ? formData.people.split(',').map(p => p.trim())
                : formData.people,
        }
        const response = await api.post<ApiResponse>('/meeting/generate', requestData)
        return response.data
    } catch (error) {
        console.error('Meeting error:', error)
        return { status: 'error', message: 'Network error' }
    }
}

export const sendEmail = async (
    recipients: string,
    content: string,
    userId?: string
): Promise<ApiResponse> => {
    try {
        const body = {
            recipients: recipients.split(',').map(email => email.trim()),
            userId: userId || 'anonymous',
            input: content,
            bulletPoints: [],
        }
        const response = await api.post<ApiResponse>('/email/send', body)
        return response.data
    } catch (error) {
        console.error('Email error:', error)
        return { status: 'error', message: 'Network error' }
    }
}

export const sendFinal = async (
    reviewType: 'email' | 'pv',
    data: Record<string, unknown>
): Promise<ApiResponse> => {
    try {
        const endpoint = reviewType === 'pv' ? '/meeting/send-final' : '/email/send-final'
        const response = await api.post<ApiResponse>(endpoint, data)
        return response.data
    } catch (error) {
        console.error('Send error:', error)
        return { status: 'error', message: 'Network error' }
    }
}

/**
 * Stores OAuth tokens after successful OAuth login.
 */
export const storeOAuthTokens = async (
    accessToken: string,
    refreshToken: string | null,
    provider: string,
    email: string,
    expiresIn: number = 3600
): Promise<ApiResponse> => {
    try {
        const response = await api.post<ApiResponse>('/auth/store-tokens', {
            accessToken,
            refreshToken,
            provider,
            email,
            expiresIn,
        })
        return response.data
    } catch (error) {
        console.error('Token storage error:', error)
        return { status: 'error', message: 'Failed to store tokens' }
    }
}

/**
 * Checks if the user can send emails via OAuth.
 */
export const getEmailCapability = async (): Promise<{
    canSendEmail: boolean
    provider: string
}> => {
    try {
        const response = await api.get('/auth/email-capability')
        return response.data
    } catch (error) {
        console.error('Email capability check error:', error)
        return { canSendEmail: false, provider: 'none' }
    }
}

/**
 * Generates a PDF preview from HTML content.
 */
export const previewPdf = async (
    content: string,
    title?: string,
    date?: string
): Promise<Blob | null> => {
    try {
        const response = await api.post('/pdf/preview',
            { content, title, date },
            { responseType: 'blob' }
        )
        return response.data
    } catch (error) {
        console.error('PDF preview error:', error)
        return null
    }
}

// ============ Meeting Minutes AI APIs (Step 2) ============

import type { ExtractedMeetingData, StructuredModeData } from '../types'

interface ExtractionResponse {
    status: 'success' | 'error'
    data?: ExtractedMeetingData
    message?: string
}

interface GenerationResponse {
    status: 'success' | 'error'
    content?: string
    message?: string
}

/**
 * Extract structured data from pasted meeting notes (Quick Mode).
 */
export const extractFromNotes = async (
    content: string,
    date?: string,
    time?: string,
    location?: string
): Promise<ExtractionResponse> => {
    try {
        const response = await api.post<ExtractionResponse>('/minutes/quick/extract', {
            content,
            date,
            time,
            location,
        })
        return response.data
    } catch (error) {
        console.error('Extraction error:', error)
        return { status: 'error', message: 'Failed to extract data. Please try again.' }
    }
}

/**
 * Extract structured data from an uploaded file (Quick Mode).
 */
export const extractFromFile = async (
    file: File,
    date?: string,
    time?: string,
    location?: string
): Promise<ExtractionResponse> => {
    try {
        const formData = new FormData()
        formData.append('file', file)
        if (date) formData.append('date', date)
        if (time) formData.append('time', time)
        if (location) formData.append('location', location)

        const response = await api.post<ExtractionResponse>(
            '/minutes/quick/extract-file',
            formData,
            {
                headers: {
                    'Content-Type': 'multipart/form-data',
                },
            }
        )
        return response.data
    } catch (error) {
        console.error('File extraction error:', error)
        return { status: 'error', message: 'Failed to process file. Please try again.' }
    }
}

/**
 * Generate minutes from reviewed extracted data (Quick Mode final step).
 */
export const generateFromExtracted = async (
    data: ExtractedMeetingData,
    tone: string = 'Formal',
    length: string = 'Standard'
): Promise<GenerationResponse> => {
    try {
        const response = await api.post<GenerationResponse>('/minutes/quick/generate', {
            data,
            tone,
            length,
        })
        return response.data
    } catch (error) {
        console.error('Generation error:', error)
        return { status: 'error', message: 'Failed to generate minutes. Please try again.' }
    }
}

/**
 * Generate professional meeting minutes from structured form data.
 */
export const generateMinutes = async (
    data: StructuredModeData
): Promise<GenerationResponse> => {
    try {
        const response = await api.post<GenerationResponse>('/minutes/structured/generate', data)
        return response.data
    } catch (error) {
        console.error('Generation error:', error)
        return { status: 'error', message: 'Failed to generate minutes. Please try again.' }
    }
}

/**
 * Regenerate minutes with same data (for retry functionality).
 */
export const regenerateMinutes = async (
    data: StructuredModeData
): Promise<GenerationResponse> => {
    try {
        const response = await api.post<GenerationResponse>('/minutes/structured/regenerate', data)
        return response.data
    } catch (error) {
        console.error('Regeneration error:', error)
        return { status: 'error', message: 'Failed to regenerate minutes. Please try again.' }
    }
}

// ============ PDF Generation APIs (Step 3) ============

export interface PdfGenerationRequest {
    htmlContent: string
    markdownContent?: string
    meetingMetadata: Record<string, string>
    outputPreferences: Record<string, any>
}

interface PdfResponse {
    status: 'success' | 'error'
    fileId?: string
    filename?: string
    message?: string
}

/**
 * Generate PDF from HTML content (Step 3).
 */
export const generatePdf = async (
    request: PdfGenerationRequest
): Promise<PdfResponse> => {
    try {
        const response = await api.post<PdfResponse>('/pdf/generate', request)
        return response.data
    } catch (error) {
        console.error('PDF generation error:', error)
        return { status: 'error', message: 'Failed to generate PDF. Please try again.' }
    }
}

/**
 * Get preview URL for generated PDF.
 */
export const getPdfPreviewUrl = (fileId: string): string => {
    return `/api/pdf/preview/${fileId}`
}

/**
 * Trigger PDF download.
 */
export const downloadPdf = async (fileId: string) => {
    try {
        const response = await api.get(`/pdf/download/${fileId}`, {
            responseType: 'blob',
        })

        // Extract filename from Content-Disposition header if possible
        const contentDisposition = response.headers['content-disposition']
        let filename = 'meeting_minutes.pdf'
        if (contentDisposition) {
            const filenameMatch = contentDisposition.match(/filename="?([^"]+)"?/)
            if (filenameMatch && filenameMatch.length === 2)
                filename = filenameMatch[1]
        }

        const url = window.URL.createObjectURL(new Blob([response.data]))
        const link = document.createElement('a')
        link.href = url
        link.setAttribute('download', filename)
        document.body.appendChild(link)
        link.click()
        link.parentNode?.removeChild(link)
        window.URL.revokeObjectURL(url)
    } catch (error) {
        console.error('Download error:', error)
        alert('Failed to download PDF. Please try again.')
    }
}

// ============ Minutes Email APIs (Step 4) ============

export interface MinutesEmailRequest {
    pdfFileId: string
    recipients: string[]
    subject?: string
    body?: string
    meetingMetadata?: Record<string, string>
}

export const sendMinutesEmail = async (request: MinutesEmailRequest): Promise<ApiResponse> => {
    try {
        const response = await api.post<ApiResponse>('/minutes/send', request)
        return response.data
    } catch (error) {
        console.error('Send minutes error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as ApiResponse
        }
        return { status: 'error', message: 'Network error or service unavailable' }
    }
}

// ============ Meeting Template APIs (Step 5) ============

import type {
    MeetingTemplate,
    CreateMeetingTemplateRequest,
    UpdateMeetingTemplateRequest
} from '../types/template'

interface TemplateResponse {
    success: boolean
    template?: MeetingTemplate
    templates?: MeetingTemplate[]
    message?: string
}

export const createTemplate = async (request: CreateMeetingTemplateRequest): Promise<TemplateResponse> => {
    try {
        const response = await api.post<TemplateResponse>('/meeting-templates', request)
        return response.data
    } catch (error) {
        console.error('Create template error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as TemplateResponse
        }
        return { success: false, message: 'Failed to create template' }
    }
}

export const getUserTemplates = async (): Promise<TemplateResponse> => {
    try {
        const response = await api.get<TemplateResponse>('/meeting-templates')
        return response.data
    } catch (error) {
        console.error('Get templates error:', error)
        return { success: false, message: 'Failed to fetch templates' }
    }
}

export const getTemplate = async (templateId: string): Promise<TemplateResponse> => {
    try {
        const response = await api.get<TemplateResponse>(`/meeting-templates/${templateId}`)
        return response.data
    } catch (error) {
        console.error('Get template error:', error)
        return { success: false, message: 'Failed to fetch template' }
    }
}

export const updateTemplate = async (
    templateId: string,
    request: UpdateMeetingTemplateRequest
): Promise<TemplateResponse> => {
    try {
        const response = await api.put<TemplateResponse>(`/meeting-templates/${templateId}`, request)
        return response.data
    } catch (error) {
        console.error('Update template error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as TemplateResponse
        }
        return { success: false, message: 'Failed to update template' }
    }
}

export const deleteTemplate = async (templateId: string): Promise<TemplateResponse> => {
    try {
        const response = await api.delete<TemplateResponse>(`/meeting-templates/${templateId}`)
        return response.data
    } catch (error) {
        console.error('Delete template error:', error)
        return { success: false, message: 'Failed to delete template' }
    }
}

export const trackTemplateUsage = async (templateId: string): Promise<TemplateResponse> => {
    try {
        const response = await api.post<TemplateResponse>(`/meeting-templates/${templateId}/track-usage`)
        return response.data
    } catch (error) {
        console.error('Track usage error:', error)
        return { success: false, message: 'Failed to track usage' }
    }
}

// ============ Contact APIs (Phase 2) ============

export interface Contact {
    id: string
    userId: string
    name: string
    email: string
    phone?: string
    photo?: string
    source: 'google' | 'microsoft' | 'manual'
    sourceId?: string
    usesQuickFlow: boolean
    quickflowUserId?: string
    groups: string[]
    isFavorite: boolean
    usageCount: number
    lastUsed?: string
    lastSynced?: string
    createdAt: string
    updatedAt: string
}

export interface ContactsResponse {
    contacts: Contact[]
    total: number
    lastSync?: string
}

export interface ImportResponse {
    success?: number
    imported: number
    updated: number
    deleted?: number
    total: number
    error?: string
}

export const getContacts = async (
    filter?: string,
    source?: string,
    sortBy?: string
): Promise<ContactsResponse> => {
    try {
        const params = new URLSearchParams()
        if (filter) params.append('filter', filter)
        if (source) params.append('source', source)
        if (sortBy) params.append('sortBy', sortBy)

        const response = await api.get<ContactsResponse>(`/contacts?${params.toString()}`)
        return response.data
    } catch (error) {
        console.error('Get contacts error:', error)
        return { contacts: [], total: 0 }
    }
}

export const searchContacts = async (query: string, limit: number = 10): Promise<Contact[]> => {
    try {
        const response = await api.get<Contact[]>(`/contacts/search?q=${encodeURIComponent(query)}&limit=${limit}`)
        return response.data
    } catch (error) {
        console.error('Search contacts error:', error)
        return []
    }
}

export const getRecentContacts = async (limit: number = 5): Promise<Contact[]> => {
    try {
        const response = await api.get<Contact[]>(`/contacts/recent?limit=${limit}`)
        return response.data
    } catch (error) {
        console.error('Get recent contacts error:', error)
        return []
    }
}

export const importContacts = async (provider?: string): Promise<ImportResponse> => {
    try {
        const url = provider ? `/contacts/import?provider=${provider}` : '/contacts/import'
        console.log('Importing contacts from:', url)
        const response = await api.post<ImportResponse>(url)
        console.log('Import response:', response.data)
        return response.data
    } catch (error) {
        console.error('Import contacts error:', error)
        if (axios.isAxiosError(error)) {
            console.error('Response status:', error.response?.status)
            console.error('Response data:', error.response?.data)
            const errorMsg = error.response?.data?.error || error.response?.data?.message || 'Failed to import contacts'
            return { imported: 0, updated: 0, total: 0, error: errorMsg }
        }
        return { imported: 0, updated: 0, total: 0, error: 'Failed to import contacts' }
    }
}

export const syncContacts = async (): Promise<ImportResponse> => {
    try {
        const response = await api.post<ImportResponse>('/contacts/sync')
        return response.data
    } catch (error) {
        console.error('Sync contacts error:', error)
        if (axios.isAxiosError(error)) {
            const errorMsg = error.response?.data?.error || error.response?.data?.message || 'Failed to sync contacts'
            return { imported: 0, updated: 0, total: 0, error: errorMsg }
        }
        return { imported: 0, updated: 0, total: 0, error: 'Failed to sync contacts' }
    }
}

export const createContact = async (contact: {
    name: string
    email: string
    phone?: string
    photo?: string
}): Promise<{ contact?: Contact; error?: string }> => {
    try {
        console.log('Creating contact:', contact)
        const response = await api.post<Contact>('/contacts', contact)
        console.log('Create contact response:', response.data)
        return { contact: response.data }
    } catch (error) {
        console.error('Create contact error:', error)
        if (axios.isAxiosError(error)) {
            console.error('Response status:', error.response?.status)
            console.error('Response data:', error.response?.data)
            const errorMsg = error.response?.data?.error || error.response?.data?.message || 'Failed to create contact'
            return { error: errorMsg }
        }
        return { error: 'Failed to create contact' }
    }
}

export const updateContact = async (
    id: string,
    updates: {
        name?: string
        email?: string
        phone?: string
        photo?: string
        groups?: string[]
        isFavorite?: boolean
    }
): Promise<{ contact?: Contact; error?: string }> => {
    try {
        const response = await api.put<Contact>(`/contacts/${id}`, updates)
        return { contact: response.data }
    } catch (error) {
        console.error('Update contact error:', error)
        if (axios.isAxiosError(error) && error.response?.data?.error) {
            return { error: error.response.data.error }
        }
        return { error: 'Failed to update contact' }
    }
}

export const deleteContact = async (id: string): Promise<{ success: boolean; error?: string }> => {
    try {
        await api.delete(`/contacts/${id}`)
        return { success: true }
    } catch (error) {
        console.error('Delete contact error:', error)
        return { success: false, error: 'Failed to delete contact' }
    }
}

export const toggleContactFavorite = async (id: string): Promise<{ contact?: Contact; error?: string }> => {
    try {
        const response = await api.post<Contact>(`/contacts/${id}/favorite`)
        return { contact: response.data }
    } catch (error) {
        console.error('Toggle favorite error:', error)
        return { error: 'Failed to toggle favorite' }
    }
}

export const incrementContactUsage = async (id: string): Promise<void> => {
    try {
        await api.post(`/contacts/${id}/use`)
    } catch (error) {
        // Silent fail - usage tracking is not critical
    }
}

export const getContactCount = async (): Promise<number> => {
    try {
        const response = await api.get<{ count: number }>('/contacts/count')
        return response.data.count
    } catch (error) {
        return 0
    }
}

// ============ Group APIs (Phase 2) ============

export interface Group {
    id: string
    userId: string
    name: string
    description?: string
    memberIds: string[]
    memberCount?: number
    memberPreview?: Array<{ id: string; name: string; email: string }>
    quickflowMemberCount?: number
    createdAt: string
    updatedAt: string
}

export interface GroupsResponse {
    groups: Group[]
    total: number
}

export const getGroups = async (): Promise<GroupsResponse> => {
    try {
        const response = await api.get<GroupsResponse>('/groups')
        return response.data
    } catch (error) {
        console.error('Get groups error:', error)
        return { groups: [], total: 0 }
    }
}

export const getGroup = async (id: string): Promise<{ group?: Group; members?: Contact[] }> => {
    try {
        const response = await api.get<{ id: string; name: string; members: Contact[] }>(`/groups/${id}`)
        return { group: response.data as unknown as Group, members: response.data.members }
    } catch (error) {
        console.error('Get group error:', error)
        return {}
    }
}

export const createGroup = async (group: {
    name: string
    description?: string
    memberIds?: string[]
}): Promise<{ group?: Group; error?: string }> => {
    try {
        const response = await api.post<Group>('/groups', group)
        return { group: response.data }
    } catch (error) {
        console.error('Create group error:', error)
        if (axios.isAxiosError(error) && error.response?.data?.error) {
            return { error: error.response.data.error }
        }
        return { error: 'Failed to create group' }
    }
}

export const updateGroup = async (
    id: string,
    updates: {
        name?: string
        description?: string
        memberIds?: string[]
    }
): Promise<{ group?: Group; error?: string }> => {
    try {
        const response = await api.put<Group>(`/groups/${id}`, updates)
        return { group: response.data }
    } catch (error) {
        console.error('Update group error:', error)
        if (axios.isAxiosError(error) && error.response?.data?.error) {
            return { error: error.response.data.error }
        }
        return { error: 'Failed to update group' }
    }
}

export const deleteGroup = async (id: string): Promise<{ success: boolean; error?: string }> => {
    try {
        await api.delete(`/groups/${id}`)
        return { success: true }
    } catch (error) {
        console.error('Delete group error:', error)
        return { success: false, error: 'Failed to delete group' }
    }
}

export const addGroupMembers = async (
    groupId: string,
    memberIds: string[]
): Promise<{ group?: Group; error?: string }> => {
    try {
        const response = await api.post<Group>(`/groups/${groupId}/members`, { memberIds })
        return { group: response.data }
    } catch (error) {
        console.error('Add members error:', error)
        return { error: 'Failed to add members' }
    }
}

export const removeGroupMember = async (
    groupId: string,
    memberId: string
): Promise<{ group?: Group; error?: string }> => {
    try {
        const response = await api.delete<Group>(`/groups/${groupId}/members/${memberId}`)
        return { group: response.data }
    } catch (error) {
        console.error('Remove member error:', error)
        return { error: 'Failed to remove member' }
    }
}

export interface GroupSearchResult {
    id: string
    name: string
    memberCount: number
    members: Contact[]
}

export const searchGroups = async (query: string, limit: number = 5): Promise<GroupSearchResult[]> => {
    try {
        const response = await api.get<GroupSearchResult[]>('/groups/search', {
            params: { q: query, limit }
        })
        return response.data
    } catch (error) {
        console.error('Search groups error:', error)
        return []
    }
}

export const getGroupCount = async (): Promise<number> => {
    try {
        const response = await api.get<{ count: number }>('/groups/count')
        return response.data.count
    } catch (error) {
        return 0
    }
}

// ============ Support Email API ============

/**
 * Send a support report email to QuickFlow support team.
 */
export const sendSupportEmail = async (message: string): Promise<ApiResponse> => {
    try {
        const response = await api.post<ApiResponse>('/support/report', { message })
        return response.data
    } catch (error) {
        console.error('Support email error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as ApiResponse
        }
        return { status: 'error', message: 'Failed to send support report' }
    }
}

// ============ SMTP Configuration APIs ============

export interface SmtpStatusResponse {
    smtpConfigured: boolean
    smtpSetupSkipped: boolean
    providerSupported: boolean
    providerBlocked: boolean
    providerName: string
    needsSetup: boolean
    isOAuth: boolean
    // New fields for hosting detection & OAuth linking
    hostingProvider: string
    hostingProviderName: string
    linkedProvider: string
    linkedProviderName: string
    linkedProviderEmail: string
    action: 'ready' | 'link_oauth' | 'setup_smtp' | 'unsupported' | ''
}

/**
 * Configure SMTP by validating and storing an app-specific password.
 */
export const configureSmtp = async (appPassword: string): Promise<ApiResponse> => {
    try {
        const response = await api.post<ApiResponse>('/user/smtp/configure', { appPassword })
        return response.data
    } catch (error) {
        console.error('SMTP configure error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as ApiResponse
        }
        return { status: 'error', message: 'Failed to configure email sending' }
    }
}

/**
 * Send a test email to the user's own address.
 */
export const testSmtp = async (): Promise<ApiResponse> => {
    try {
        const response = await api.post<ApiResponse>('/user/smtp/test')
        return response.data
    } catch (error) {
        console.error('SMTP test error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as ApiResponse
        }
        return { status: 'error', message: 'Failed to send test email' }
    }
}

/**
 * Remove SMTP configuration.
 */
export const removeSmtp = async (): Promise<ApiResponse> => {
    try {
        const response = await api.delete<ApiResponse>('/user/smtp/configure')
        return response.data
    } catch (error) {
        console.error('SMTP remove error:', error)
        if (axios.isAxiosError(error) && error.response) {
            return error.response.data as ApiResponse
        }
        return { status: 'error', message: 'Failed to remove email configuration' }
    }
}

/**
 * Get SMTP configuration status.
 */
export const getSmtpStatus = async (): Promise<SmtpStatusResponse> => {
    try {
        const response = await api.get<SmtpStatusResponse>('/user/smtp/status')
        return response.data
    } catch (error) {
        console.error('SMTP status error:', error)
        return {
            smtpConfigured: false,
            smtpSetupSkipped: false,
            providerSupported: false,
            providerBlocked: false,
            providerName: 'Unknown',
            needsSetup: false,
            isOAuth: false,
            hostingProvider: '',
            hostingProviderName: '',
            linkedProvider: '',
            linkedProviderName: '',
            linkedProviderEmail: '',
            action: '',
        }
    }
}

/**
 * Skip SMTP setup.
 */
export const skipSmtpSetup = async (): Promise<ApiResponse> => {
    try {
        const response = await api.post<ApiResponse>('/user/smtp/skip-setup')
        return response.data
    } catch (error) {
        console.error('SMTP skip error:', error)
        return { status: 'error', message: 'Failed to skip SMTP setup' }
    }
}

// ============ OAuth Linking APIs ============

/**
 * Initiate OAuth linking for a provider (google or microsoft).
 * Returns an authorization URL to redirect the user to.
 */
export const linkOAuthProvider = async (provider: 'google' | 'microsoft'): Promise<{ status: string; authorizationUrl?: string; message?: string }> => {
    try {
        const response = await api.get(`/auth/link/${provider}`)
        return response.data
    } catch (error) {
        console.error('OAuth linking error:', error)
        return { status: 'error', message: 'Failed to initiate account linking' }
    }
}

/**
 * Unlink an OAuth provider from the user's account.
 */
export const unlinkOAuthProvider = async (): Promise<ApiResponse> => {
    try {
        const response = await api.delete<ApiResponse>('/auth/link')
        return response.data
    } catch (error) {
        console.error('OAuth unlink error:', error)
        return { status: 'error', message: 'Failed to unlink provider' }
    }
}

export default api

