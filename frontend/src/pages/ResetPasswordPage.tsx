import { useState, FormEvent } from 'react'
import { useSearchParams, useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || ''

/**
 * Password reset page. User arrives here from the reset email link.
 * URL format: /reset-password?token=<reset_token>
 */
export default function ResetPasswordPage() {
    const [searchParams] = useSearchParams()
    const navigate = useNavigate()
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState(false)

    const token = searchParams.get('token')

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')

        if (!token) {
            setError('Invalid or missing reset token.')
            return
        }

        if (password.length < 8) {
            setError('Password must be at least 8 characters')
            return
        }

        if (password !== confirmPassword) {
            setError('Passwords do not match')
            return
        }

        setLoading(true)
        try {
            const response = await fetch(`${BACKEND_URL}/api/auth/reset-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
                body: JSON.stringify({ token, newPassword: password }),
            })

            if (response.ok) {
                setSuccess(true)
            } else {
                const data = await response.json()
                setError(data.error || 'Password reset failed. The link may have expired.')
            }
        } catch {
            setError('Could not connect to the server.')
        } finally {
            setLoading(false)
        }
    }

    if (!token) {
        return (
            <div className="min-h-screen flex items-center justify-center p-4">
                <div className="glass-card p-8 text-center max-w-md">
                    <div className="text-4xl mb-4">❌</div>
                    <h2 className="text-xl font-semibold text-red-400 mb-2">Invalid Link</h2>
                    <p className="text-slate-400">This password reset link is invalid or has expired.</p>
                    <button onClick={() => navigate('/auth')} className="btn-primary mt-6">
                        Back to Login
                    </button>
                </div>
            </div>
        )
    }

    if (success) {
        return (
            <div className="min-h-screen flex items-center justify-center p-4">
                <div className="glass-card p-8 text-center max-w-md">
                    <div className="text-4xl mb-4">✅</div>
                    <h2 className="text-xl font-semibold text-green-400 mb-2">Password Updated!</h2>
                    <p className="text-slate-400">Your password has been reset. You can now sign in.</p>
                    <button onClick={() => navigate('/auth')} className="btn-primary mt-6">
                        Sign In
                    </button>
                </div>
            </div>
        )
    }

    return (
        <div className="min-h-screen flex items-center justify-center p-4 relative z-10">
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
                className="glass-card w-full max-w-md p-8 sm:p-10"
            >
                <div className="text-center mb-8">
                    <div className="text-5xl mb-4 inline-block">🔒</div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
                        Reset Password
                    </h1>
                    <p className="text-slate-400 mt-2 text-sm">Enter your new password</p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">New Password</label>
                        <div className="relative">
                            <input
                                type={showPassword ? 'text' : 'password'}
                                value={password}
                                onChange={(e) => setPassword(e.target.value)}
                                placeholder="••••••••"
                                className="input-nebula pr-12"
                                disabled={loading}
                            />
                            <button
                                type="button"
                                onClick={() => setShowPassword(!showPassword)}
                                className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-200 transition-colors"
                            >
                                {showPassword ? '🙈' : '👁️'}
                            </button>
                        </div>
                    </div>

                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">Confirm Password</label>
                        <input
                            type={showPassword ? 'text' : 'password'}
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            placeholder="••••••••"
                            className="input-nebula"
                            disabled={loading}
                        />
                    </div>

                    {error && (
                        <div className="message-error">{error}</div>
                    )}

                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <span className="spinner" /> Resetting...
                            </span>
                        ) : 'Reset Password'}
                    </button>
                </form>
            </motion.div>
        </div>
    )
}
