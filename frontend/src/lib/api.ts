import axios from 'axios'
import type { ApiResponse, MeetingFormData } from '../types'
import { supabase } from './supabase'

const api = axios.create({
    baseURL: '/api',
    headers: {
        'Content-Type': 'application/json',
    },
})

// Request interceptor to automatically add auth token from Supabase session
api.interceptors.request.use(
    async (config) => {
        try {
            const { data: { session } } = await supabase.auth.getSession()
            if (session?.access_token) {
                config.headers.Authorization = `Bearer ${session.access_token}`
            }
        } catch (error) {
            console.warn('Failed to get session for API request:', error)
        }
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
        const response = await api.post<ApiResponse>(`/${reviewType}/send-final`, data)
        return response.data
    } catch (error) {
        console.error('Send error:', error)
        return { status: 'error', message: 'Network error' }
    }
}

export default api
