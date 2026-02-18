/**
 * Token Manager — manages JWT access tokens and refresh flow.
 * Replaces Supabase client token management.
 */

const ACCESS_TOKEN_KEY = 'qf_access_token';

let accessToken: string | null = null;

/**
 * Store the access token in memory and localStorage.
 */
export function setAccessToken(token: string | null): void {
    accessToken = token;
    if (token) {
        localStorage.setItem(ACCESS_TOKEN_KEY, token);
    } else {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
    }
}

/**
 * Get the current access token.
 */
export function getAccessToken(): string | null {
    if (accessToken) return accessToken;
    // Recover from localStorage on page reload
    accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
    return accessToken;
}

/**
 * Clear all tokens (logout).
 */
export function clearTokens(): void {
    accessToken = null;
    localStorage.removeItem(ACCESS_TOKEN_KEY);
}

/**
 * Check if the token is expired or about to expire (within 60 seconds).
 */
export function isTokenExpiring(): boolean {
    const token = getAccessToken();
    if (!token) return true;

    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        const exp = payload.exp * 1000; // Convert to ms
        const now = Date.now();
        return now >= exp - 60_000; // 60s buffer
    } catch {
        return true;
    }
}

/**
 * Extract user info from the JWT payload.
 */
export function extractUserFromToken(token: string): {
    userId: string;
    email: string;
    name: string;
    role: string;
} | null {
    try {
        const payload = JSON.parse(atob(token.split('.')[1]));
        return {
            userId: payload.sub,
            email: payload.email,
            name: payload.name,
            role: payload.role,
        };
    } catch {
        return null;
    }
}

/**
 * Attempt to refresh the access token using the httpOnly refresh token cookie.
 * Returns the new access token or null on failure.
 */
export async function refreshAccessToken(backendUrl: string): Promise<string | null> {
    try {
        const response = await fetch(`${backendUrl}/api/auth/refresh`, {
            method: 'POST',
            credentials: 'include', // Include httpOnly cookies
            headers: {
                'Content-Type': 'application/json',
                'ngrok-skip-browser-warning': 'true',
            },
        });

        if (!response.ok) {
            // Don't clear tokens here — the existing access token may still be valid.
            // The caller (AuthContext) decides what to do when refresh fails.
            return null;
        }

        const data = await response.json();
        if (data.accessToken) {
            setAccessToken(data.accessToken);
            return data.accessToken;
        }

        return null;
    } catch (error) {
        console.error('Token refresh failed:', error);
        // Don't clear tokens on network errors — the access token may still be valid.
        return null;
    }
}
