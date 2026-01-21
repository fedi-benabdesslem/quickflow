import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useReview } from '../contexts/ReviewContext'
import { sendFinal } from '../lib/api'

export default function ReviewPage() {
    const [subject, setSubject] = useState('')
    const [content, setContent] = useState('')
    const [recipients, setRecipients] = useState('')
    const [people, setPeople] = useState('')
    const [location, setLocation] = useState('')
    const [date, setDate] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const [success, setSuccess] = useState('')

    const { signOut } = useAuth()
    const { reviewData, setReviewData } = useReview()
    const navigate = useNavigate()

    useEffect(() => {
        if (!reviewData) {
            navigate('/home')
            return
        }

        setSubject(reviewData.subject || '')
        setContent(reviewData.content || '')

        if (reviewData.type === 'email') {
            setRecipients(reviewData.recipients || '')
        } else {
            setPeople(reviewData.people || '')
            setLocation(reviewData.location || '')
            setDate(reviewData.date || '')
        }
    }, [reviewData, navigate])

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    const handleBack = () => {
        setReviewData(null)
        navigate('/home')
    }

    const handleEdit = () => {
        if (reviewData?.type === 'email') {
            navigate('/email')
        } else {
            navigate('/meeting')
        }
    }

    const handleSend = async () => {
        if (!content.trim() || !subject.trim()) {
            setError('Subject and content required')
            return
        }

        setError('')
        setSuccess('')
        setLoading(true)

        const requestData: Record<string, unknown> = {
            id: reviewData?.id,
            subject,
            content,
        }

        if (reviewData?.type === 'email') {
            requestData.recipients = recipients.split(',').map((r) => r.trim())
        } else {
            requestData.people = people.split(',').map((p) => p.trim())
            requestData.location = location
            requestData.date = date
        }

        const result = await sendFinal(reviewData?.type || 'email', requestData)

        if (result.status === 'success') {
            setSuccess(result.message || 'Sent successfully!')
            setTimeout(() => {
                setReviewData(null)
                navigate('/home')
            }, 2000)
        } else {
            setError(result.message || 'Failed to send')
        }

        setLoading(false)
    }

    if (!reviewData) {
        return null
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <button onClick={handleBack} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back</span>
                </button>
                <button onClick={handleLogout} className="btn-logout">
                    <span>🚪</span>
                    <span className="hidden sm:inline">Logout</span>
                </button>
            </motion.header>

            {/* Review Card */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="glass-card max-w-3xl mx-auto p-6 sm:p-8"
            >
                {/* Header */}
                <div className="text-center mb-8">
                    <motion.div
                        animate={{ scale: [1, 1.1, 1] }}
                        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        className="text-4xl mb-4 inline-block"
                    >
                        👁️
                    </motion.div>
                    <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
                        Review & Edit
                    </h1>
                    <p className="text-slate-400 mt-2">
                        {reviewData.type === 'email' ? 'Review Email Draft' : 'Review Meeting Summary (PV)'}
                    </p>
                </div>

                {/* Form */}
                <div className="space-y-6">
                    {/* Subject */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">📌</span>Subject
                        </label>
                        <input
                            type="text"
                            value={subject}
                            onChange={(e) => setSubject(e.target.value)}
                            placeholder="Subject"
                            className="input-nebula"
                            disabled={loading}
                        />
                    </div>

                    {/* Email-specific: Recipients */}
                    {reviewData.type === 'email' && (
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                <span className="mr-2">📧</span>Recipients
                            </label>
                            <input
                                type="text"
                                value={recipients}
                                onChange={(e) => setRecipients(e.target.value)}
                                placeholder="email@example.com"
                                className="input-nebula"
                                disabled={loading}
                            />
                        </div>
                    )}

                    {/* Meeting-specific: People, Location, Date */}
                    {reviewData.type === 'pv' && (
                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    <span className="mr-2">👥</span>Attendees
                                </label>
                                <input
                                    type="text"
                                    value={people}
                                    onChange={(e) => setPeople(e.target.value)}
                                    placeholder="John, Jane"
                                    className="input-nebula"
                                    disabled={loading}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    <span className="mr-2">📍</span>Location
                                </label>
                                <input
                                    type="text"
                                    value={location}
                                    onChange={(e) => setLocation(e.target.value)}
                                    placeholder="Conference Room"
                                    className="input-nebula"
                                    disabled={loading}
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    <span className="mr-2">📅</span>Date
                                </label>
                                <input
                                    type="date"
                                    value={date}
                                    onChange={(e) => setDate(e.target.value)}
                                    className="input-nebula"
                                    disabled={loading}
                                />
                            </div>
                        </div>
                    )}

                    {/* Content */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">📄</span>Content
                        </label>
                        <textarea
                            value={content}
                            onChange={(e) => setContent(e.target.value)}
                            placeholder="Generated content will appear here..."
                            rows={10}
                            className="input-nebula resize-y min-h-[200px] font-mono text-sm"
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

                    {/* Buttons */}
                    <div className="flex flex-col sm:flex-row gap-4">
                        <button
                            type="button"
                            onClick={handleSend}
                            className="btn-primary flex-1"
                            disabled={loading}
                        >
                            {loading ? (
                                <span className="flex items-center gap-2">
                                    <span className="spinner" />
                                    Sending...
                                </span>
                            ) : (
                                <span className="flex items-center gap-2">
                                    <span>📤</span>
                                    Send Final
                                </span>
                            )}
                        </button>
                        <button
                            type="button"
                            onClick={handleEdit}
                            className="btn-secondary"
                            disabled={loading}
                        >
                            <span>✏️</span>
                            Edit Original
                        </button>
                    </div>
                </div>
            </motion.div>
        </div>
    )
}
