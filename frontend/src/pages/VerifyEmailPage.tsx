import { useEffect, useState } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || ''

/**
 * Verifies email when user clicks the verification link.
 * URL format: /verify-email?token=<verification_token>
 */
export default function VerifyEmailPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const [status, setStatus] = useState<'verifying' | 'success' | 'error'>('verifying')
    const [message, setMessage] = useState('')

    useEffect(() => {
        const token = searchParams.get('token')
        if (!token) {
            setStatus('error')
            setMessage('Missing verification token.')
            return
        }

        const verify = async () => {
            try {
                const response = await fetch(`${BACKEND_URL}/api/auth/verify-email`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
                    body: JSON.stringify({ token }),
                })

                if (response.ok) {
                    setStatus('success')
                    setMessage('Your email has been verified! You can now sign in.')
                } else {
                    const data = await response.json()
                    setStatus('error')
                    setMessage(data.error || 'Verification failed. The link may have expired.')
                }
            } catch {
                setStatus('error')
                setMessage('Could not connect to the server. Please try again later.')
            }
        }

        verify()
    }, [searchParams])

    return (
        <div className="min-h-screen flex items-center justify-center p-4">
            <div className="glass-card p-8 text-center max-w-md">
                {status === 'verifying' && (
                    <>
                        <div className="text-4xl mb-4 animate-pulse">📧</div>
                        <h2 className="text-xl font-semibold text-slate-200">Verifying your email...</h2>
                        <div className="mt-4 flex justify-center"><span className="spinner" /></div>
                    </>
                )}
                {status === 'success' && (
                    <>
                        <div className="text-4xl mb-4">✅</div>
                        <h2 className="text-xl font-semibold text-green-400 mb-2">Email Verified!</h2>
                        <p className="text-slate-400">{message}</p>
                        <button
                            onClick={() => navigate('/auth')}
                            className="btn-primary mt-6"
                        >
                            Sign In
                        </button>
                    </>
                )}
                {status === 'error' && (
                    <>
                        <div className="text-4xl mb-4">❌</div>
                        <h2 className="text-xl font-semibold text-red-400 mb-2">Verification Failed</h2>
                        <p className="text-slate-400">{message}</p>
                        <button
                            onClick={() => navigate('/auth')}
                            className="btn-primary mt-6"
                        >
                            Back to Login
                        </button>
                    </>
                )}
            </div>
        </div>
    )
}
