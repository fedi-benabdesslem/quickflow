import axios from 'axios'
import type { ApiResponse, MeetingFormData } from '../types'

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
})

// Request interceptor - token is set synchronously via setAuthToken() from AuthContext
// NOTE: We removed the async getSession() call here because it was causing race conditions
// during app initialization, leading to infinite loading states on page refresh.
api.interceptors.request.use(
    (config) => {
        // Token is already set in defaults.headers via setAuthToken()
        return config
    },
    (error) => {
        return Promise.reject(error)
    }
)

// Add auth token to requests (kept for backwards compatibility)
export const setAuthToken = (token: string | null) => {
    if (token) {
        api.defaults.headers.common['Authorization'] = `Bearer ${token}`
    } else {
        delete api.defaults.headers.common['Authorization']
    }
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
    time?: string
): Promise<ExtractionResponse> => {
    try {
        const response = await api.post<ExtractionResponse>('/minutes/quick/extract', {
            content,
            date,
            time,
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
    time?: string
): Promise<ExtractionResponse> => {
    try {
        const formData = new FormData()
        formData.append('file', file)
        if (date) formData.append('date', date)
        if (time) formData.append('time', time)

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

export default api

