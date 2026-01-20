import { useState, FormEvent } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { isSupabaseConfigured } from '../lib/supabase'

type AuthMode = 'signin' | 'signup' | 'forgot'

export default function AuthPage() {
    const [mode, setMode] = useState<AuthMode>('signin')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')
    const [username, setUsername] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    const { signIn, signUp, resetPassword } = useAuth()

    const validateEmail = (email: string): boolean => {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    }

    const getPasswordStrength = (password: string): { level: number; label: string; color: string } => {
        if (password.length === 0) return { level: 0, label: '', color: '' }
        if (password.length < 6) return { level: 1, label: 'Weak', color: 'bg-red-500' }
        if (password.length < 8) return { level: 2, label: 'Fair', color: 'bg-yellow-500' }
        if (/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)/.test(password)) {
            return { level: 4, label: 'Strong', color: 'bg-green-500' }
        }
        return { level: 3, label: 'Good', color: 'bg-blue-500' }
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')
        setSuccess('')

        if (!validateEmail(email)) {
            setError('Please enter a valid email address')
            return
        }

        if (mode === 'forgot') {
            setLoading(true)
            const result = await resetPassword(email)
            setLoading(false)
            if (result.success) {
                setSuccess('Password reset email sent! Check your inbox.')
            } else {
                setError(result.error || 'Failed to send reset email')
            }
            return
        }

        if (password.length < 6) {
            setError('Password must be at least 6 characters')
            return
        }

        if (mode === 'signup') {
            if (!username.trim()) {
                setError('Please enter a username')
                return
            }
            if (password !== confirmPassword) {
                setError('Passwords do not match')
                return
            }
        }

        setLoading(true)

        if (mode === 'signin') {
            const result = await signIn(email, password)
            if (!result.success) {
                setError(result.error || 'Sign in failed')
            }
        } else {
            const result = await signUp(email, password, username)
            if (result.success) {
                setSuccess('Account created! Please check your email to verify your account.')
            } else {
                setError(result.error || 'Sign up failed')
            }
        }

        setLoading(false)
    }

    const passwordStrength = getPasswordStrength(password)

    return (
        <div className="min-h-screen flex items-center justify-center p-4 relative z-10">
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.6, ease: 'easeOut' }}
                className="glass-card w-full max-w-md p-8 sm:p-10"
            >
                {/* Logo */}
                <div className="text-center mb-8">
                    <motion.div
                        animate={{ scale: [1, 1.05, 1] }}
                        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        className="text-5xl mb-4 inline-block drop-shadow-[0_0_15px_rgba(59,130,246,0.5)]"
                    >
                        ⚡
                    </motion.div>
                    <h1 className="text-3xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
                        QuickFlow
                    </h1>
                    <p className="text-slate-400 mt-2 text-sm">
                        {mode === 'signin' && 'Welcome back! Sign in to continue'}
                        {mode === 'signup' && 'Create your account to get started'}
                        {mode === 'forgot' && 'Enter your email to reset password'}
                    </p>

                    {/* Demo Mode Warning */}
                    {!isSupabaseConfigured && (
                        <div className="mt-4 p-3 rounded-lg bg-amber-500/10 border border-amber-500/30 text-amber-200 text-xs">
                            <p className="font-medium">⚠️ Demo Mode</p>
                            <p className="mt-1 text-amber-300/80">
                                Configure <code className="bg-amber-500/20 px-1 rounded">src/lib/supabase.ts</code> to enable auth.
                            </p>
                        </div>
                    )}
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-5">
                    <AnimatePresence mode="wait">
                        {mode === 'signup' && (
                            <motion.div
                                key="username"
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: 'auto' }}
                                exit={{ opacity: 0, height: 0 }}
                                transition={{ duration: 0.2 }}
                            >
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    <span className="mr-2">👤</span>Username
                                </label>
                                <input
                                    type="text"
                                    value={username}
                                    onChange={(e) => setUsername(e.target.value)}
                                    placeholder="Your username"
                                    className="input-nebula"
                                    disabled={loading}
                                />
                            </motion.div>
                        )}
                    </AnimatePresence>

                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">📧</span>Email
                        </label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="you@example.com"
                            className="input-nebula"
                            disabled={loading}
                        />
                    </div>

                    {mode !== 'forgot' && (
                        <>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    <span className="mr-2">🔒</span>Password
                                </label>
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

                                {/* Password Strength Indicator (signup only) */}
                                {mode === 'signup' && password.length > 0 && (
                                    <div className="mt-2">
                                        <div className="flex gap-1">
                                            {[1, 2, 3, 4].map((level) => (
                                                <div
                                                    key={level}
                                                    className={`h-1 flex-1 rounded-full transition-colors ${level <= passwordStrength.level ? passwordStrength.color : 'bg-slate-700'
                                                        }`}
                                                />
                                            ))}
                                        </div>
                                        <p className={`text-xs mt-1 ${passwordStrength.color.replace('bg-', 'text-')}`}>
                                            {passwordStrength.label}
                                        </p>
                                    </div>
                                )}
                            </div>

                            <AnimatePresence mode="wait">
                                {mode === 'signup' && (
                                    <motion.div
                                        key="confirm"
                                        initial={{ opacity: 0, height: 0 }}
                                        animate={{ opacity: 1, height: 'auto' }}
                                        exit={{ opacity: 0, height: 0 }}
                                        transition={{ duration: 0.2 }}
                                    >
                                        <label className="block text-sm font-medium text-slate-300 mb-2">
                                            <span className="mr-2">🔒</span>Confirm Password
                                        </label>
                                        <input
                                            type={showPassword ? 'text' : 'password'}
                                            value={confirmPassword}
                                            onChange={(e) => setConfirmPassword(e.target.value)}
                                            placeholder="••••••••"
                                            className="input-nebula"
                                            disabled={loading}
                                        />
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </>
                    )}

                    {/* Messages */}
                    <AnimatePresence>
                        {error && (
                            <motion.div
                                initial={{ opacity: 0, y: -10 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: -10 }}
                                className="message-error"
                            >
                                {error}
                            </motion.div>
                        )}
                        {success && (
                            <motion.div
                                initial={{ opacity: 0, y: -10 }}
                                animate={{ opacity: 1, y: 0 }}
                                exit={{ opacity: 0, y: -10 }}
                                className="message-success"
                            >
                                {success}
                            </motion.div>
                        )}
                    </AnimatePresence>

                    {/* Submit Button */}
                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <span className="spinner" />
                                {mode === 'signin' ? 'Signing in...' : mode === 'signup' ? 'Creating account...' : 'Sending...'}
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <span>{mode === 'signin' ? '🚀' : mode === 'signup' ? '✨' : '📧'}</span>
                                {mode === 'signin' ? 'Sign In' : mode === 'signup' ? 'Create Account' : 'Send Reset Link'}
                            </span>
                        )}
                    </button>
                </form>

                {/* Social Auth Buttons (prepared for OAuth) */}
                {mode !== 'forgot' && (
                    <div className="mt-6">
                        <div className="relative">
                            <div className="absolute inset-0 flex items-center">
                                <div className="w-full border-t border-slate-700" />
                            </div>
                            <div className="relative flex justify-center text-sm">
                                <span className="px-4 bg-[#12121a] text-slate-500">or continue with</span>
                            </div>
                        </div>

                        <div className="mt-4 grid grid-cols-2 gap-3">
                            <button
                                type="button"
                                className="flex items-center justify-center gap-2 px-4 py-3 rounded-xl border-2 border-slate-700 bg-slate-800/50 text-slate-300 hover:bg-slate-700/50 hover:border-slate-600 transition-all duration-200"
                                onClick={() => alert('Google OAuth coming soon!')}
                            >
                                <svg className="w-5 h-5" viewBox="0 0 24 24">
                                    <path
                                        fill="currentColor"
                                        d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"
                                    />
                                    <path
                                        fill="currentColor"
                                        d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"
                                    />
                                    <path
                                        fill="currentColor"
                                        d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"
                                    />
                                    <path
                                        fill="currentColor"
                                        d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"
                                    />
                                </svg>
                                Google
                            </button>
                            <button
                                type="button"
                                className="flex items-center justify-center gap-2 px-4 py-3 rounded-xl border-2 border-slate-700 bg-slate-800/50 text-slate-300 hover:bg-slate-700/50 hover:border-slate-600 transition-all duration-200"
                                onClick={() => alert('Microsoft OAuth coming soon!')}
                            >
                                <svg className="w-5 h-5" viewBox="0 0 23 23">
                                    <path fill="#f35325" d="M1 1h10v10H1z" />
                                    <path fill="#81bc06" d="M12 1h10v10H12z" />
                                    <path fill="#05a6f0" d="M1 12h10v10H1z" />
                                    <path fill="#ffba08" d="M12 12h10v10H12z" />
                                </svg>
                                Microsoft
                            </button>
                        </div>
                    </div>
                )}

                {/* Mode Toggles */}
                <div className="mt-6 text-center text-sm">
                    {mode === 'signin' && (
                        <>
                            <p className="text-slate-400">
                                Don't have an account?{' '}
                                <button
                                    type="button"
                                    onClick={() => {
                                        setMode('signup')
                                        setError('')
                                        setSuccess('')
                                    }}
                                    className="text-blue-400 hover:text-blue-300 font-medium transition-colors"
                                >
                                    Sign up
                                </button>
                            </p>
                            <button
                                type="button"
                                onClick={() => {
                                    setMode('forgot')
                                    setError('')
                                    setSuccess('')
                                }}
                                className="mt-2 text-slate-500 hover:text-slate-400 transition-colors"
                            >
                                Forgot password?
                            </button>
                        </>
                    )}
                    {mode === 'signup' && (
                        <p className="text-slate-400">
                            Already have an account?{' '}
                            <button
                                type="button"
                                onClick={() => {
                                    setMode('signin')
                                    setError('')
                                    setSuccess('')
                                }}
                                className="text-blue-400 hover:text-blue-300 font-medium transition-colors"
                            >
                                Sign in
                            </button>
                        </p>
                    )}
                    {mode === 'forgot' && (
                        <p className="text-slate-400">
                            Remember your password?{' '}
                            <button
                                type="button"
                                onClick={() => {
                                    setMode('signin')
                                    setError('')
                                    setSuccess('')
                                }}
                                className="text-blue-400 hover:text-blue-300 font-medium transition-colors"
                            >
                                Sign in
                            </button>
                        </p>
                    )}
                </div>
            </motion.div>
        </div>
    )
}
