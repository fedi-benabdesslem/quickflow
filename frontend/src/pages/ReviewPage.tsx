import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import TechSupportButton from '../components/TechSupportButton'
import { useReview } from '../contexts/ReviewContext'
import { sendFinal } from '../lib/api'
import { showToast } from '../components/Toast'

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
            showToast.error('Subject and content required')
            return
        }

        setError('')
        setSuccess('')
        setLoading(true)

        const loadingToast = showToast.loading('Sending...')

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
            // For meeting minutes, also send recipients for email
            if (recipients.trim()) {
                requestData.recipients = recipients.split(',').map((r) => r.trim())
            }
        }

        const result = await sendFinal(reviewData?.type || 'email', requestData)
        showToast.dismiss(loadingToast)

        if (result.status === 'success') {
            showToast.success(result.message || 'Sent successfully!')
            setTimeout(() => {
                setReviewData(null)
                navigate('/home')
            }, 2000)
        } else if (result.status === 'unsupported') {
            // Email/password user - show info toast
            showToast.unsupportedProvider()
        } else if (result.status === 'reauth_required') {
            showToast.reauthRequired()
        } else {
            showToast.error(result.message || 'Failed to send')
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
                <TechSupportButton />
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
                        className="text-4xl text-purple-400 mb-4 inline-block"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16">
                            <path d="M12 15a3 3 0 100-6 3 3 0 000 6z" />
                            <path fillRule="evenodd" d="M1.323 11.447C2.811 6.976 7.028 3.75 12.001 3.75c4.97 0 9.185 3.223 10.675 7.69.12.362.12.752 0 1.113-1.487 4.471-5.705 7.697-10.677 7.697-4.97 0-9.186-3.223-10.675-7.69a1.762 1.762 0 010-1.113zM17.25 12a5.25 5.25 0 11-10.5 0 5.25 5.25 0 0110.5 0z" clipRule="evenodd" />
                        </svg>
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
                        <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-purple-400">
                                <path fillRule="evenodd" d="M15.75 2.25H21a.75.75 0 01.75.75v5.25a.75.75 0 01-1.5 0V4.81L8.03 17.03a.75.75 0 01-1.06-1.06L19.19 3.75h-3.44a.75.75 0 010-1.5zm-10.5 4.5a1.5 1.5 0 00-1.5 1.5v10.5a1.5 1.5 0 001.5 1.5h10.5a1.5 1.5 0 001.5-1.5V10.5a.75.75 0 011.5 0v8.25a3 3 0 01-3 3H5.25a3 3 0 01-3-3V8.25a3 3 0 013-3h8.25a.75.75 0 010 1.5H5.25z" clipRule="evenodd" />
                            </svg>
                            Subject
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
                            <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-purple-400">
                                    <path d="M1.5 8.67v8.58a3 3 0 003 3h15a3 3 0 003-3V8.67l-8.928 5.493a3 3 0 01-3.144 0L1.5 8.67z" />
                                    <path d="M22.5 6.908V6.75a3 3 0 00-3-3h-15a3 3 0 00-3 3v.158l9.714 5.978a1.5 1.5 0 001.572 0L22.5 6.908z" />
                                </svg>
                                Recipients
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
                                <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-purple-400">
                                        <path d="M4.5 6.375a4.125 4.125 0 118.25 0 4.125 4.125 0 01-8.25 0zM14.25 8.625a3.375 3.375 0 116.75 0 3.375 3.375 0 01-6.75 0zM1.5 19.125a7.125 7.125 0 0114.25 0v.003l-.081.569A8.354 8.354 0 0115 19.5a8.354 8.354 0 01-.73 3H.51a10.453 10.453 0 001.383-4.282A7.236 7.236 0 011.5 19.125zM14.25 19.125A7.125 7.125 0 0121.375 12a7.125 7.125 0 017.125 7.125v.75c0 1.036-.84 1.875-1.875 1.875h-10.5a1.875 1.875 0 01-1.875-1.875v-.75z" />
                                    </svg>
                                    Attendees
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
                                <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-purple-400">
                                        <path fillRule="evenodd" d="M11.54 22.351l.07.04.028.016a.76.76 0 00.723 0l.028-.015.071-.041a16.975 16.975 0 001.144-.742 19.58 19.58 0 002.683-2.282c1.944-1.99 3.963-4.98 3.963-8.827a8.25 8.25 0 00-16.5 0c0 3.846 2.02 6.837 3.963 8.827a19.58 19.58 0 002.682 2.282 16.975 16.975 0 001.145.742zM12 13.5a3 3 0 100-6 3 3 0 000 6z" clipRule="evenodd" />
                                    </svg>
                                    Location
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
                                <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-purple-400">
                                        <path d="M12.75 12.75a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM7.5 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM8.25 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM9.75 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM10.5 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM12 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM12.75 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM14.25 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM15 17.25a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM16.5 15.75a.75.75 0 100-1.5.75.75 0 000 1.5zM15 12.75a.75.75 0 11-1.5 0 .75.75 0 011.5 0zM16.5 13.5a.75.75 0 100-1.5.75.75 0 000 1.5z" />
                                        <path fillRule="evenodd" d="M6.75 2.25A.75.75 0 017.5 3v1.5h9V3A.75.75 0 0118 3v1.5h.75a3 3 0 013 3v11.25a3 3 0 01-3 3H5.25a3 3 0 01-3-3V7.5a3 3 0 013-3H6V3a.75.75 0 01.75-.75zm13.5 9a1.5 1.5 0 00-1.5-1.5H5.25a1.5 1.5 0 00-1.5 1.5v7.5a1.5 1.5 0 001.5 1.5h13.5a1.5 1.5 0 001.5-1.5v-7.5z" clipRule="evenodd" />
                                    </svg>
                                    Date
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
                        <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-purple-400">
                                <path fillRule="evenodd" d="M3 3.75a.75.75 0 01.75-.75h12a.75.75 0 01.75.75v16.5a.75.75 0 01-.75.75h-12a.75.75 0 01-.75-.75V3.75zM4.5 4.5v15h11.25V4.5H4.5z" clipRule="evenodd" />
                                <path fillRule="evenodd" d="M15.75 1.5a.75.75 0 01.75.75v1.5a.75.75 0 01-1.5 0v-1.5a.75.75 0 01.75-.75zm-9 0a.75.75 0 01.75.75v1.5a.75.75 0 01-1.5 0v-1.5A.75.75 0 016.75 1.5zm.75 18a.75.75 0 010 1.5h9a.75.75 0 010-1.5h-9z" clipRule="evenodd" />
                            </svg>
                            Content
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
                                    <span>Send Final</span>
                                </span>
                            )}
                        </button>
                        <button
                            type="button"
                            onClick={handleEdit}
                            className="btn-secondary"
                            disabled={loading}
                        >
                            <span>Edit Original</span>
                        </button>
                    </div>
                </div>
            </motion.div>
        </div>
    )
}
