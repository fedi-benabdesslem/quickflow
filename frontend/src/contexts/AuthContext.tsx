import { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import type { User, AuthContextType, AuthResult, Session } from '../types'
import { setAuthToken } from '../lib/api'
import {
    getAccessToken,
    setAccessToken,
    clearTokens,
    extractUserFromToken,
    refreshAccessToken,
} from '../lib/tokenManager'

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || ''

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [session, setSession] = useState<Session | null>(null)
    const [loading, setLoading] = useState(true)

    /**
     * Set authenticated state from an access token.
     */
    const setAuthState = (accessToken: string) => {
        setAccessToken(accessToken)
        setAuthToken(accessToken)
        setSession({ accessToken: accessToken })

        const info = extractUserFromToken(accessToken)
        if (info) {
            setUser({
                id: info.userId,
                username: info.name || info.email?.split('@')[0] || 'User',
                email: info.email,
                role: info.role,
            })
        }
    }

    /**
     * Clear all authenticated state.
     */
    const clearAuthState = () => {
        clearTokens()
        setAuthToken(null)
        setSession(null)
        setUser(null)
    }

    useEffect(() => {
        let cancelled = false

        const initializeAuth = async () => {
            try {
                // 1. Check for token in URL (OAuth callback)
                const params = new URLSearchParams(window.location.search)
                const urlToken = params.get('token')
                if (urlToken) {
                    // Clean URL
                    window.history.replaceState({}, '', window.location.pathname)
                    if (!cancelled) {
                        setAuthState(urlToken)
                        setLoading(false)
                    }
                    return
                }

                // 2. Check for existing token in localStorage
                const existingToken = getAccessToken()
                if (existingToken) {
                    if (!cancelled) {
                        setAuthState(existingToken)
                    }
                    // Try to silently refresh for a fresh token
                    const newToken = await refreshAccessToken(BACKEND_URL)
                    if (newToken && !cancelled) {
                        setAuthState(newToken)
                    } else if (!newToken && !cancelled) {
                        // Refresh failed but we have a stored token — keep it until it expires
                    }
                    if (!cancelled) setLoading(false)
                    return
                }

                // 3. Try silent refresh (maybe we have a valid refresh token cookie)
                const refreshed = await refreshAccessToken(BACKEND_URL)
                if (refreshed && !cancelled) {
                    setAuthState(refreshed)
                }
            } catch (error) {
                console.error('Auth initialization error:', error)
            } finally {
                if (!cancelled) setLoading(false)
            }
        }

        initializeAuth()

        // Safety timeout
        const safetyTimeout = setTimeout(() => {
            if (!cancelled) setLoading(false)
        }, 5000)

        return () => {
            cancelled = true
            clearTimeout(safetyTimeout)
        }
    }, [])

    const signIn = async (email: string, password: string): Promise<AuthResult> => {
        try {
            const response = await fetch(`${BACKEND_URL}/api/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
                credentials: 'include',
                body: JSON.stringify({ email, password }),
            })

            const data = await response.json()

            if (!response.ok) {
                return { success: false, error: data.error || 'Login failed' }
            }

            if (data.accessToken) {
                setAuthState(data.accessToken)
                return { success: true }
            }

            // MFA required
            if (data.mfaRequired) {
                return { success: false, error: 'MFA_REQUIRED' }
            }

            return { success: false, error: 'Unexpected response' }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const signUp = async (email: string, password: string, username: string): Promise<AuthResult> => {
        try {
            const response = await fetch(`${BACKEND_URL}/api/auth/signup`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
                credentials: 'include',
                body: JSON.stringify({ email, password, name: username }),
            })

            const data = await response.json()

            if (!response.ok) {
                return { success: false, error: data.error || 'Signup failed' }
            }

            if (data.accessToken) {
                setAuthState(data.accessToken)
                return { success: true }
            }

            // Email verification required
            if (data.message?.includes('verify')) {
                return { success: true }
            }

            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const signInWithGoogle = async (): Promise<AuthResult> => {
        try {
            // Redirect to backend OAuth2 authorization endpoint
            window.location.href = `${BACKEND_URL}/oauth2/authorization/google`
            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const signInWithMicrosoft = async (): Promise<AuthResult> => {
        try {
            window.location.href = `${BACKEND_URL}/oauth2/authorization/microsoft`
            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const updateProfile = async (updates: { username?: string; avatarUrl?: string }): Promise<AuthResult> => {
        try {
            const response = await fetch(`${BACKEND_URL}/api/auth/me`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${getAccessToken()}`,
                    'ngrok-skip-browser-warning': 'true',
                },
                credentials: 'include',
                body: JSON.stringify({
                    name: updates.username,
                    profilePhotoUrl: updates.avatarUrl,
                }),
            })

            if (!response.ok) {
                const data = await response.json()
                return { success: false, error: data.error || 'Update failed' }
            }

            // Update local state immediately
            setUser(prev => prev ? {
                ...prev,
                username: updates.username ?? prev.username,
                avatarUrl: updates.avatarUrl ?? prev.avatarUrl,
            } : null)

            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const signOut = async (): Promise<void> => {
        try {
            await fetch(`${BACKEND_URL}/api/auth/logout`, {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${getAccessToken()}`,
                    'ngrok-skip-browser-warning': 'true',
                },
                credentials: 'include',
            })
        } catch (error) {
            console.error('Logout request failed:', error)
        }
        clearAuthState()
    }

    const resetPassword = async (email: string): Promise<AuthResult> => {
        try {
            const response = await fetch(`${BACKEND_URL}/api/auth/forgot-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
                body: JSON.stringify({ email }),
            })

            const data = await response.json()

            if (!response.ok) {
                return { success: false, error: data.error || 'Password reset failed' }
            }

            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    return (
        <AuthContext.Provider value={{ user, session, loading, signIn, signUp, signInWithGoogle, signInWithMicrosoft, signOut, resetPassword, updateProfile }}>
            {children}
        </AuthContext.Provider>
    )
}

export function useAuth(): AuthContextType {
    const context = useContext(AuthContext)
    if (context === undefined) {
        throw new Error('useAuth must be used within an AuthProvider')
    }
    return context
}
