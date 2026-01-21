import { useState, FormEvent, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useReview } from '../contexts/ReviewContext'
import { sendEmail } from '../lib/api'

export default function EmailPage() {
    const [recipients, setRecipients] = useState('')
    const [content, setContent] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    const { user, signOut } = useAuth()
    const { reviewData, setReviewData } = useReview()
    const navigate = useNavigate()

    // Load from review data if editing
    useEffect(() => {
        if (reviewData?.type === 'email') {
            setRecipients(reviewData.recipients || '')
            setContent(reviewData.details || '')
        }
    }, [reviewData])

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    const isValidEmail = (email: string): boolean => {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')
        setSuccess('')

        if (!recipients.trim() || !content.trim()) {
            setError('Please fill in all fields')
            return
        }

        const emailList = recipients.split(',').map((email) => email.trim())
        for (const email of emailList) {
            if (!isValidEmail(email)) {
                setError(`Invalid email: ${email}`)
                return
            }
        }

        setLoading(true)

        const result = await sendEmail(recipients, content, user?.id)

        if (result.status === 'success') {
            setSuccess('Email generated! Redirecting to review...')
            setTimeout(() => {
                setReviewData({
                    type: 'email',
                    id: result.emailId || '',
                    subject: result.subject || 'Draft Email',
                    content: result.generatedContent || '',
                    recipients,
                    details: content,
                })
                navigate('/review')
            }, 1500)
        } else {
            setError(result.message || 'Failed to generate email')
        }

        setLoading(false)
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <button onClick={() => navigate('/home')} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back</span>
                </button>
                <button onClick={handleLogout} className="btn-logout">
                    <span>🚪</span>
                    <span className="hidden sm:inline">Logout</span>
                </button>
            </motion.header>

            {/* Form Card */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="glass-card max-w-2xl mx-auto p-6 sm:p-8"
            >
                {/* Header */}
                <div className="text-center mb-8">
                    <motion.div
                        animate={{ y: [0, -5, 0] }}
                        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        className="text-4xl mb-4 inline-block"
                    >
                        ✉️
                    </motion.div>
                    <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-pink-400 to-purple-400 bg-clip-text text-transparent">
                        Draft Email
                    </h1>
                    <p className="text-slate-400 mt-2">
                        Compose your message and let AI polish it for you
                    </p>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Recipients */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">📧</span>Recipient Email(s)
                        </label>
                        <input
                            type="text"
                            value={recipients}
                            onChange={(e) => setRecipients(e.target.value)}
                            placeholder="email@example.com, another@example.com"
                            className="input-nebula"
                            disabled={loading}
                        />
                    </div>

                    {/* Content */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">💬</span>Message
                        </label>
                        <textarea
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            placeholder="Type your message here..."
                            rows={6}
                            className="input-nebula resize-y min-h-[150px]"
                            disabled={loading}
                        />
                    </div>

                    {/* Messages */}
                    {error && (
                        <motion.div
                            initial={{ opacity: 0, y: -10 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="message-error"
                        >
                            {error}
                        </motion.div>
                    )}
                    {success && (
                        <motion.div
                            initial={{ opacity: 0, y: -10 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="message-success"
                        >
                            {success}
                        </motion.div>
                    )}

                    {/* Submit Button */}
                    <button type="submit" className="btn-primary" disabled={loading}>
                        {loading ? (
                            <span className="flex items-center gap-2">
                                <span className="spinner" />
                                Generating...
                            </span>
                        ) : (
                            <span className="flex items-center gap-2">
                                <span>✨</span>
                                Generate & Review
                            </span>
                        )}
                    </button>
                </form>
            </motion.div>
        </div>
    )
}
