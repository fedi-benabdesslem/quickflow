import { useState, FormEvent, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useReview } from '../contexts/ReviewContext'
import { generateMeeting } from '../lib/api'
import type { MeetingFormData } from '../types'

export default function MeetingPage() {
    const [formData, setFormData] = useState<MeetingFormData>({
        people: '',
        location: '',
        timeBegin: '',
        timeEnd: '',
        date: new Date().toISOString().split('T')[0],
        subject: '',
        details: '',
    })
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const { user, signOut } = useAuth()
    const { reviewData, setReviewData } = useReview()
    const navigate = useNavigate()

    // Load from review data if editing
    useEffect(() => {
        if (reviewData?.type === 'pv') {
            setFormData({
                people: reviewData.people || '',
                location: reviewData.location || '',
                timeBegin: reviewData.timeBegin || '',
                timeEnd: reviewData.timeEnd || '',
                date: reviewData.date || new Date().toISOString().split('T')[0],
                subject: reviewData.subject || '',
                details: reviewData.details || '',
            })
        }
    }, [reviewData])

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')

        const { people, location, timeBegin, timeEnd, date, subject } = formData

        if (!people || !location || !timeBegin || !timeEnd || !date || !subject) {
            setError('Please fill in all required fields')
            return
        }

        if (new Date(`2000-01-01T${timeEnd}`) <= new Date(`2000-01-01T${timeBegin}`)) {
            setError('End time must be after start time')
            return
        }

        setLoading(true)

        const result = await generateMeeting(formData)

        if (result.status === 'success') {
            setReviewData({
                type: 'pv',
                id: result.id || result.meetingId || '',
                subject: result.subject || subject,
                content: result.generatedContent || '',
                people: typeof people === 'string' ? people : people.join(', '),
                location,
                date,
                timeBegin,
                timeEnd,
                details: formData.details,
            })
            navigate('/review')
        } else {
            setError(result.message || 'Failed to generate summary')
        }

        setLoading(false)
    }

    const updateField = (field: keyof MeetingFormData, value: string) => {
        setFormData((prev) => ({ ...prev, [field]: value }))
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
                        animate={{ rotate: [0, 5, -5, 0] }}
                        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        className="text-4xl mb-4 inline-block"
                    >
                        📝
                    </motion.div>
                    <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
                        Create Meeting Summary
                    </h1>
                    <p className="text-slate-400 mt-2">
                        Fill in the meeting details to generate a professional summary
                    </p>
                </div>

                {/* Form */}
                <form onSubmit={handleSubmit} className="space-y-6">
                    {/* Row 1: Attendees & Location */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                <span className="mr-2">👥</span>Attendees
                            </label>
                            <input
                                type="text"
                                value={formData.people}
                                onChange={(e) => updateField('people', e.target.value)}
                                placeholder="John, Jane, Team Lead"
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
                                value={formData.location}
                                onChange={(e) => updateField('location', e.target.value)}
                                placeholder="Conference Room A"
                                className="input-nebula"
                                disabled={loading}
                            />
                        </div>
                    </div>

                    {/* Row 2: Time & Date */}
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                <span className="mr-2">🕐</span>Start Time
                            </label>
                            <input
                                type="time"
                                value={formData.timeBegin}
                                onChange={(e) => updateField('timeBegin', e.target.value)}
                                className="input-nebula"
                                disabled={loading}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                <span className="mr-2">🕐</span>End Time
                            </label>
                            <input
                                type="time"
                                value={formData.timeEnd}
                                onChange={(e) => updateField('timeEnd', e.target.value)}
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
                                value={formData.date}
                                onChange={(e) => updateField('date', e.target.value)}
                                className="input-nebula"
                                disabled={loading}
                            />
                        </div>
                    </div>

                    {/* Subject */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">📌</span>Subject
                        </label>
                        <input
                            type="text"
                            value={formData.subject}
                            onChange={(e) => updateField('subject', e.target.value)}
                            placeholder="Meeting topic"
                            className="input-nebula"
                            disabled={loading}
                        />
                    </div>

                    {/* Details */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2">
                            <span className="mr-2">📄</span>Details
                        </label>
                        <textarea
                            value={formData.details}
                            onChange={(e) => updateField('details', e.target.value)}
                            placeholder="Additional information..."
                            rows={4}
                            className="input-nebula resize-y min-h-[100px]"
                            disabled={loading}
                        />
                    </div>

                    {/* Error Message */}
                    {error && (
                        <motion.div
                            initial={{ opacity: 0, y: -10 }}
                            animate={{ opacity: 1, y: 0 }}
                            className="message-error"
                        >
                            {error}
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
                                Generate Summary
                            </span>
                        )}
                    </button>
                </form>
            </motion.div>
        </div>
    )
}
