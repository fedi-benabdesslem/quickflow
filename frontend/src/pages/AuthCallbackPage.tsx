import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { setAccessToken } from '../lib/tokenManager'
import { setAuthToken } from '../lib/api'

/**
 * Handles OAuth redirect callback.
 * The backend redirects here with ?token=<JWT> after successful OAuth login.
 */
export default function AuthCallbackPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        const token = searchParams.get('token')
        const errorParam = searchParams.get('error')

        if (errorParam) {
            setError(errorParam)
            setTimeout(() => navigate('/auth'), 3000)
            return
        }

        if (token) {
            setAccessToken(token)
            setAuthToken(token)
            navigate('/', { replace: true })
        } else {
            setError('No authentication token received.')
            setTimeout(() => navigate('/auth'), 3000)
        }
    }, [searchParams, navigate])

    if (error) {
        return (
            <div className="min-h-screen flex items-center justify-center p-4">
                <div className="glass-card p-8 text-center max-w-md">
                    <div className="text-4xl mb-4">❌</div>
                    <h2 className="text-xl font-semibold text-red-400 mb-2">Authentication Failed</h2>
                    <p className="text-slate-400">{error}</p>
                    <p className="text-slate-500 text-sm mt-4">Redirecting to login...</p>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="glass-card p-8 text-center max-w-md">
                <div className="text-4xl mb-4 animate-pulse">⚡</div>
                <h2 className="text-xl font-semibold text-slate-200">Completing sign in...</h2>
                <div className="mt-4 flex justify-center">
                    <span className="spinner" />
                </div>
            </div>
        </div>
    )
}
