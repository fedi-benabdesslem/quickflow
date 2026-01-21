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

    const { signOut } = useAuth()
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
                        className="text-4xl text-blue-400 mb-4 inline-block"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16">
                            <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0016.5 9h-1.875a1.875 1.875 0 01-1.875-1.875V5.25A3.75 3.75 0 009 1.5H5.625zM7.5 15a.75.75 0 01.75-.75h7.5a.75.75 0 010 1.5h-7.5A.75.75 0 017.5 15zm.75 2.25a.75.75 0 000 1.5H12a.75.75 0 000-1.5H8.25z" clipRule="evenodd" />
                            <path d="M12.971 1.816A5.23 5.23 0 0114.25 5.25v1.875c0 .207.168.375.375.375H16.5a5.23 5.23 0 013.434 1.279 9.768 9.768 0 00-6.963-6.963z" />
                        </svg>
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
                            <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                    <path d="M4.5 6.375a4.125 4.125 0 118.25 0 4.125 4.125 0 01-8.25 0zM14.25 8.625a3.375 3.375 0 116.75 0 3.375 3.375 0 01-6.75 0zM1.5 19.125a7.125 7.125 0 0114.25 0v.003l-.081.569A8.354 8.354 0 0115 19.5a8.354 8.354 0 01-.73 3H.51a10.453 10.453 0 001.383-4.282A7.236 7.236 0 011.5 19.125zM14.25 19.125A7.125 7.125 0 0121.375 12a7.125 7.125 0 017.125 7.125v.75c0 1.036-.84 1.875-1.875 1.875h-10.5a1.875 1.875 0 01-1.875-1.875v-.75z" />
                                </svg>
                                Attendees
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
                            <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                    <path fillRule="evenodd" d="M11.54 22.351l.07.04.028.016a.76.76 0 00.723 0l.028-.015.071-.041a16.975 16.975 0 001.144-.742 19.58 19.58 0 002.683-2.282c1.944-1.99 3.963-4.98 3.963-8.827a8.25 8.25 0 00-16.5 0c0 3.846 2.02 6.837 3.963 8.827a19.58 19.58 0 002.682 2.282 16.975 16.975 0 001.145.742zM12 13.5a3 3 0 100-6 3 3 0 000 6z" clipRule="evenodd" />
                                </svg>
                                Location
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
                            <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                    <path fillRule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zM12.75 6a.75.75 0 00-1.5 0v6c0 .414.336.75.75.75h4.5a.75.75 0 000-1.5h-3.75V6z" clipRule="evenodd" />
                                </svg>
                                Start Time
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
                            <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                    <path fillRule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zM12.75 6a.75.75 0 00-1.5 0v6c0 .414.336.75.75.75h4.5a.75.75 0 000-1.5h-3.75V6z" clipRule="evenodd" />
                                </svg>
                                End Time
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
                            <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                    <path d="M12.75 12.75a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM7.5 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM8.25 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM9.75 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM10.5 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM12 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM12.75 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM14.25 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM15 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM16.5 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM15 12.75a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM16.5 13.5a.75.75 0 100-1.5.75.75 0 000 1.5z" />
                                    <path fillRule="evenodd" d="M6.75 2.25A.75.75 0 017.5 3v1.5h9V3A.75.75 0 0118 3v1.5h.75a3 3 0 013 3v11.25a3 3 0 01-3 3H5.25a3 3 0 01-3-3V7.5a3 3 0 013-3H6V3a.75.75 0 01.75-.75zm13.5 9a1.5 1.5 0 00-1.5-1.5H5.25a1.5 1.5 0 00-1.5 1.5v7.5a1.5 1.5 0 001.5 1.5h13.5a1.5 1.5 0 001.5-1.5v-7.5z" clipRule="evenodd" />
                                </svg>
                                Date
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
                        <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                <path fillRule="evenodd" d="M15.75 2.25H21a.75.75 0 01.75.75v5.25a.75.75 0 01-1.5 0V4.81L8.03 17.03a.75.75 0 01-1.06-1.06L19.19 3.75h-3.44a.75.75 0 010-1.5zm-10.5 4.5a1.5 1.5 0 00-1.5 1.5v10.5a1.5 1.5 0 001.5 1.5h10.5a1.5 1.5 0 001.5-1.5V10.5a.75.75 0 011.5 0v8.25a3 3 0 01-3 3H5.25a3 3 0 01-3-3V8.25a3 3 0 013-3h8.25a.75.75 0 010 1.5H5.25z" clipRule="evenodd" />
                            </svg>
                            Subject
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
                        <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-blue-400">
                                <path fillRule="evenodd" d="M3 3.75a.75.75 0 01.75-.75h12a.75.75 0 01.75.75v16.5a.75.75 0 01-.75.75h-12a.75.75 0 01-.75-.75V3.75zM4.5 4.5v15h11.25V4.5H4.5z" clipRule="evenodd" />
                                <path fillRule="evenodd" d="M15.75 1.5a.75.75 0 01.75.75v1.5a.75.75 0 01-1.5 0v-1.5a.75.75 0 01.75-.75zm-9 0a.75.75 0 01.75.75v1.5a.75.75 0 01-1.5 0v-1.5A.75.75 0 016.75 1.5zm.75 18a.75.75 0 010 1.5h9a.75.75 0 010-1.5h-9z" clipRule="evenodd" />
                            </svg>
                            Details
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
                            <span>
                                Generate Summary
                            </span>
                        )}
                    </button>
                </form>
            </motion.div>
        </div>
    )
}
