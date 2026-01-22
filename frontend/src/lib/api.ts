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

export default api
