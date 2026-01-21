import { createClient, SupabaseClient } from '@supabase/supabase-js'

// ===========================================
// SUPABASE CONFIGURATION - PLACEHOLDERS
// ===========================================
// Replace these values with your actual Supabase project credentials
// Get them from: https://supabase.com/dashboard/project/_/settings/api

const supabaseUrl: string = 'https://qemqauonfieivgqzhwiz.supabase.co'
const supabaseAnonKey: string = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InFlbXFhdW9uZmllaXZncXpod2l6Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Njg5MjU2ODcsImV4cCI6MjA4NDUwMTY4N30._drP_O9gzRsdtJJqByyiekHCbppH3ac5C5Uiq-6bE0A'

// ===========================================

// Check if credentials are configured (validate URL format and key presence)
const isConfigured =
    supabaseUrl.startsWith('https://') &&
    supabaseUrl.includes('.supabase.co') &&
    supabaseAnonKey.length > 0

// Create a mock Supabase client for development when credentials aren't set
const createMockClient = (): SupabaseClient => {
    console.warn(
        '⚠️ Supabase credentials not configured. Running in demo mode.\n' +
        'To enable authentication, update src/lib/supabase.ts with your Supabase credentials.'
    )

    // Return a mock client that provides stub methods
    const mockAuth = {
        getSession: async () => ({ data: { session: null }, error: null }),
        onAuthStateChange: (_callback: unknown) => ({
            data: { subscription: { unsubscribe: () => { } } }
        }),
        signInWithPassword: async () => ({
            data: { user: null, session: null },
            error: { message: 'Supabase not configured. Please add your credentials to src/lib/supabase.ts' }
        }),
        signUp: async () => ({
            data: { user: null, session: null },
            error: { message: 'Supabase not configured. Please add your credentials to src/lib/supabase.ts' }
        }),
        signOut: async () => ({ error: null }),
        resetPasswordForEmail: async () => ({
            data: null,
            error: { message: 'Supabase not configured. Please add your credentials to src/lib/supabase.ts' }
        }),
    }

    return { auth: mockAuth } as unknown as SupabaseClient
}

// Export the appropriate client
export const supabase: SupabaseClient = isConfigured
    ? createClient(supabaseUrl, supabaseAnonKey, {
        auth: {
            autoRefreshToken: true,
            persistSession: true,
            detectSessionInUrl: true,
        },
    })
    : createMockClient()

// Export a flag so components can show appropriate UI
export const isSupabaseConfigured = isConfigured
