import { createContext, useContext, useEffect, useState, ReactNode } from 'react'
import { supabase } from '../lib/supabase'
import type { User, AuthContextType, AuthResult, Session } from '../types'
import { storeOAuthTokens, setAuthToken } from '../lib/api'

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: ReactNode }) {
    const [user, setUser] = useState<User | null>(null)
    const [session, setSession] = useState<Session | null>(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        let isCleanedUp = false;

        // Initialize auth - get existing session
        const initializeAuth = async () => {
            try {
                const { data: { session: currentSession }, error } = await supabase.auth.getSession();

                if (isCleanedUp) return;

                if (error) {
                    console.error('Error getting session:', error);
                    return;
                }

                if (currentSession) {
                    setSession({
                        access_token: currentSession.access_token,
                        refresh_token: currentSession.refresh_token,
                        expires_at: currentSession.expires_at,
                    });
                    setUser({
                        id: currentSession.user.id,
                        username: currentSession.user.user_metadata?.full_name || currentSession.user.user_metadata?.username || currentSession.user.email?.split('@')[0] || 'User',
                        email: currentSession.user.email,
                        avatarUrl: currentSession.user.user_metadata?.avatar_url || currentSession.user.user_metadata?.picture || undefined,
                    });
                    // Set auth token synchronously for API calls
                    setAuthToken(currentSession.access_token);
                }
            } catch (error) {
                console.error('Failed to get session:', error);
            } finally {
                if (!isCleanedUp) {
                    setLoading(false);
                }
            }
        };

        initializeAuth();

        // Safety timeout - ensure loading NEVER stays true forever
        const safetyTimeout = setTimeout(() => {
            if (!isCleanedUp) {
                console.warn('Auth initialization safety timeout reached');
                setLoading(false);
            }
        }, 5000);

        // Listen for auth changes
        const { data: { subscription } } = supabase.auth.onAuthStateChange(async (event, currentSession) => {
            if (isCleanedUp) return;

            if (currentSession) {
                setSession({
                    access_token: currentSession.access_token,
                    refresh_token: currentSession.refresh_token,
                    expires_at: currentSession.expires_at,
                });
                setUser({
                    id: currentSession.user.id,
                    username: currentSession.user.user_metadata?.full_name || currentSession.user.user_metadata?.username || currentSession.user.email?.split('@')[0] || 'User',
                    email: currentSession.user.email,
                    avatarUrl: currentSession.user.user_metadata?.avatar_url || currentSession.user.user_metadata?.picture || undefined,
                });
                // Set auth token synchronously
                setAuthToken(currentSession.access_token);

                // Extract and store OAuth provider tokens for email sending
                // IMPORTANT: Don't await this - it would block the auth flow and cause hangs
                if (event === 'SIGNED_IN' && currentSession.provider_token) {
                    const provider = currentSession.user.app_metadata?.provider || 'email';
                    if (provider === 'google' || provider === 'azure') {
                        // Fire and forget - don't block auth flow
                        storeOAuthTokens(
                            currentSession.provider_token,
                            currentSession.provider_refresh_token || null,
                            provider,
                            currentSession.user.email || '',
                            3600 // Default 1 hour expiry
                        ).then(() => {
                            console.log(`OAuth tokens stored for ${provider} provider`);
                        }).catch(error => {
                            console.error('Failed to store OAuth tokens:', error);
                        });
                    }
                }
            } else {
                setSession(null);
                setUser(null);
                setAuthToken(null);
            }
            setLoading(false);
        });

        return () => {
            isCleanedUp = true;
            clearTimeout(safetyTimeout);
            subscription.unsubscribe();
        };
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

    const signInWithGoogle = async (): Promise<AuthResult> => {
        try {
            const { error } = await supabase.auth.signInWithOAuth({
                provider: 'google',
                options: {
                    redirectTo: `${window.location.origin}/`,
                    scopes: 'https://www.googleapis.com/auth/gmail.send https://www.googleapis.com/auth/contacts.readonly',
                    queryParams: {
                        access_type: 'offline',
                        prompt: 'consent',
                    },
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



    const signInWithMicrosoft = async (): Promise<AuthResult> => {
        try {
            const { error } = await supabase.auth.signInWithOAuth({
                provider: 'azure',
                options: {
                    redirectTo: `${window.location.origin}/`,
                    scopes: 'email Mail.Send Contacts.Read offline_access',
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

    const updateProfile = async (updates: { username?: string; avatarUrl?: string }): Promise<AuthResult> => {
        try {
            const data: Record<string, string> = {}
            if (updates.username !== undefined) {
                data.full_name = updates.username
                data.username = updates.username
            }
            if (updates.avatarUrl !== undefined) {
                data.avatar_url = updates.avatarUrl
            }

            const { error } = await supabase.auth.updateUser({ data })
            if (error) {
                return { success: false, error: error.message }
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
