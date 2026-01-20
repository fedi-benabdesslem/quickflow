import { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import { supabase } from '../lib/supabase'
import type { User, AuthContextType, AuthResult, Session } from '../types'

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [session, setSession] = useState<Session | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        // Get initial session
        supabase.auth.getSession().then(({ data: { session: currentSession } }) => {
            if (currentSession) {
                setSession({
                    access_token: currentSession.access_token,
                    refresh_token: currentSession.refresh_token,
                    expires_at: currentSession.expires_at,
                })
                setUser({
                    id: currentSession.user.id,
                    username: currentSession.user.user_metadata?.username || currentSession.user.email?.split('@')[0] || 'User',
                    email: currentSession.user.email,
                })
            }
            setLoading(false)
        })

        // Listen for auth changes
        const { data: { subscription } } = supabase.auth.onAuthStateChange((_event, currentSession) => {
            if (currentSession) {
                setSession({
                    access_token: currentSession.access_token,
                    refresh_token: currentSession.refresh_token,
                    expires_at: currentSession.expires_at,
                })
                setUser({
                    id: currentSession.user.id,
                    username: currentSession.user.user_metadata?.username || currentSession.user.email?.split('@')[0] || 'User',
                    email: currentSession.user.email,
                })
            } else {
                setSession(null)
                setUser(null)
            }
            setLoading(false)
        })

        return () => {
            subscription.unsubscribe()
        }
    }, [])

    const signIn = async (email: string, password: string): Promise<AuthResult> => {
        try {
            const { error } = await supabase.auth.signInWithPassword({
                email,
                password,
            })

            if (error) {
                return { success: false, error: error.message }
            }

            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const signUp = async (email: string, password: string, username: string): Promise<AuthResult> => {
        try {
            const { error } = await supabase.auth.signUp({
                email,
                password,
                options: {
                    data: { username },
                },
            })

            if (error) {
                return { success: false, error: error.message }
            }

            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    const signOut = async (): Promise<void> => {
        await supabase.auth.signOut()
        setUser(null)
        setSession(null)
    }

    const resetPassword = async (email: string): Promise<AuthResult> => {
        try {
            const { error } = await supabase.auth.resetPasswordForEmail(email, {
                redirectTo: `${window.location.origin}/reset-password`,
            })

            if (error) {
                return { success: false, error: error.message }
            }

            return { success: true }
        } catch (err) {
            return { success: false, error: 'An unexpected error occurred' }
        }
    }

    return (
        <AuthContext.Provider value={{ user, session, loading, signIn, signUp, signOut, resetPassword }}>
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
