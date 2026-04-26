import { useState, FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || ''

/**
 * Forgot password page — user enters their email to receive a reset link.
 */
export default function ForgotPasswordPage() {
    const navigate = useNavigate()
    const [email, setEmail] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [sent, setSent] = useState(false)

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')

        if (!email.trim()) {
            setError('Please enter your email address')
            return
        }

        setLoading(true)
        try {
            const response = await fetch(`${BACKEND_URL}/api/auth/forgot-password`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', 'ngrok-skip-browser-warning': 'true' },
                body: JSON.stringify({ email: email.trim() }),
            })

            if (response.ok) {
                setSent(true)
            } else {
                // Don't reveal whether the email exists
                setSent(true)
            }
        } catch {
            setError('Could not connect to the server. Please try again later.')
        } finally {
            setLoading(false)
        }
    }

    if (sent) {
        return (
            <div className="min-h-screen flex items-center justify-center p-4 relative z-10">
                <motion.div
                    initial={{ opacity: 0, scale: 0.95 }}
                    animate={{ opacity: 1, scale: 1 }}
                    className="glass-card w-full max-w-md p-8 sm:p-10 text-center"
                >
                    <div className="text-5xl mb-4">📬</div>
                    <h1 className="text-2xl font-bold text-slate-200 mb-3">Check Your Email</h1>
                    <p className="text-slate-400 text-sm leading-relaxed">
                        If an account exists with <strong className="text-slate-300">{email}</strong>,
                        we've sent a password reset link. Check your inbox and spam folder.
                    </p>
                    <button
                        onClick={() => navigate('/auth')}
                        className="btn-primary mt-8"
                    >
                        Back to Login
                    </button>
                </motion.div>
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
                    <div className="text-5xl mb-4 inline-block">🔑</div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
                        Forgot Password
                    </h1>
                    <p className="text-slate-400 mt-2 text-sm">
                        Enter your email and we'll send you a reset link
                    </p>
                </div>

                <form onSubmit={handleSubmit} className="space-y-5">
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">Email Address</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            className="input-nebula"
                            disabled={loading}
                            autoFocus
                        />
                    </div>

                    {error && (
                        <div className="message-error">{error}</div>
                    )}

                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <span className="spinner" /> Sending...
                            </span>
                        ) : 'Send Reset Link'}
                    </button>

                    <div className="text-center pt-2">
                        <button
                            type="button"
                            onClick={() => navigate('/auth')}
                            className="text-sm text-slate-400 hover:text-blue-400 transition-colors"
                        >
                            ← Back to Login
                        </button>
                    </div>
                </form>
            </motion.div>
        </div>
    )
}
