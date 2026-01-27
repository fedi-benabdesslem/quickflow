import { useState, FormEvent, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useReview } from '../contexts/ReviewContext'
import { sendEmail, type Contact } from '../lib/api'
import ContactAutocomplete from '../components/contacts/ContactAutocomplete'
import TechSupportButton from '../components/TechSupportButton'

interface Recipient {
    id: string
    name: string
    email: string
    isContact: boolean // true if from contacts, false if manually entered
}

const generateId = () => Math.random().toString(36).substring(2, 9)

export default function EmailPage() {
    const [recipientList, setRecipientList] = useState<Recipient[]>([])
    const [inputValue, setInputValue] = useState('')
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
            // Parse emails back into recipients
            if (reviewData.recipients) {
                const emails = reviewData.recipients.split(',').map(e => e.trim()).filter(Boolean)
                setRecipientList(emails.map(email => ({
                    id: generateId(),
                    name: email,
                    email: email,
                    isContact: false
                })))
            }
            setContent(reviewData.details || '')
        }
    }, [reviewData])



    const isValidEmail = (email: string): boolean => {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    }

    // Add recipient from contact
    const handleContactSelect = (contact: Contact) => {
        // Check if already added
        if (recipientList.some(r => r.email.toLowerCase() === contact.email.toLowerCase())) {
            return
        }

        setRecipientList(prev => [...prev, {
            id: generateId(),
            name: contact.name,
            email: contact.email,
            isContact: true
        }])
        setInputValue('')
    }

    // Add recipient from manual email entry (called when Enter is pressed)
    const handleManualAdd = () => {
        const email = inputValue.trim()
        if (!email) return

        if (!isValidEmail(email)) {
            setError('Please enter a valid email address')
            return
        }

        if (recipientList.some(r => r.email.toLowerCase() === email.toLowerCase())) {
            setInputValue('')
            return
        }

        setRecipientList(prev => [...prev, {
            id: generateId(),
            name: email,
            email: email,
            isContact: false
        }])
        setInputValue('')
        setError('')
    }

    const removeRecipient = (id: string) => {
        setRecipientList(prev => prev.filter(r => r.id !== id))
    }

    const handleSubmit = async (e: FormEvent) => {
        e.preventDefault()
        setError('')
        setSuccess('')

        if (recipientList.length === 0 || !content.trim()) {
            setError('Please add at least one recipient and enter a message')
            return
        }

        const recipients = recipientList.map(r => r.email).join(', ')

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
                <TechSupportButton />
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
                        className="text-4xl text-pink-400 mb-4 inline-block"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16">
                            <path d="M1.5 8.67v8.58a3 3 0 003 3h15a3 3 0 003-3V8.67l-8.928 5.493a3 3 0 01-3.144 0L1.5 8.67z" />
                            <path d="M22.5 6.908V6.75a3 3 0 00-3-3h-15a3 3 0 00-3 3v.158l9.714 5.978a1.5 1.5 0 001.572 0L22.5 6.908z" />
                        </svg>
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
                        <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-pink-400">
                                <path d="M1.5 8.67v8.58a3 3 0 003 3h15a3 3 0 003-3V8.67l-8.928 5.493a3 3 0 01-3.144 0L1.5 8.67z" />
                                <path d="M22.5 6.908V6.75a3 3 0 00-3-3h-15a3 3 0 00-3 3v.158l9.714 5.978a1.5 1.5 0 001.572 0L22.5 6.908z" />
                            </svg>
                            Recipients
                        </label>

                        {/* Selected Recipients as Tags */}
                        {recipientList.length > 0 && (
                            <div className="flex flex-wrap gap-2 mb-2">
                                {recipientList.map(recipient => (
                                    <div
                                        key={recipient.id}
                                        className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm ${recipient.isContact
                                            ? 'bg-pink-500/20 border border-pink-500/30 text-pink-300'
                                            : 'bg-slate-700/50 border border-slate-600 text-slate-300'
                                            }`}
                                    >
                                        {recipient.isContact && (
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-3.5 h-3.5 text-pink-400">
                                                <path d="M10 8a3 3 0 100-6 3 3 0 000 6zM3.465 14.493a1.23 1.23 0 00.41 1.412A9.957 9.957 0 0010 18c2.31 0 4.438-.784 6.131-2.1.43-.333.604-.903.408-1.41a7.002 7.002 0 00-13.074.003z" />
                                            </svg>
                                        )}
                                        <span className="max-w-[200px] truncate">{recipient.name}</span>
                                        <button
                                            type="button"
                                            onClick={() => removeRecipient(recipient.id)}
                                            className="text-current opacity-60 hover:opacity-100 transition-opacity"
                                            disabled={loading}
                                        >
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                                                <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
                                            </svg>
                                        </button>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* Autocomplete Input */}
                        <ContactAutocomplete
                            value={inputValue}
                            onChange={setInputValue}
                            onSelect={handleContactSelect}
                            onEnterManual={handleManualAdd}
                            placeholder={recipientList.length > 0 ? "Add more recipients..." : "Search contacts or type email..."}
                            disabled={loading}
                        />
                        <p className="text-xs text-slate-500 mt-1">
                            Search contacts or type an email and press Enter
                        </p>
                    </div>

                    {/* Content */}
                    <div>
                        <label className="block text-sm font-medium text-slate-300 mb-2 flex items-center gap-2">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-4 h-4 text-pink-400">
                                <path fillRule="evenodd" d="M4.804 21.644A6.707 6.707 0 006 21.75a6.721 6.721 0 003.583-1.029c.774.182 1.584.279 2.417.279 5.322 0 9.75-3.97 9.75-9 0-5.03-4.428-9-9.75-9s-9.75 3.97-9.75 9c0 2.409 1.025 4.587 2.674 6.192.232.226.277.428.254.543a3.73 3.73 0 01-.814 1.686.75.75 0 00.44 1.223zM8.25 10.875a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25zM10.875 12a1.125 1.125 0 112.25 0 1.125 1.125 0 01-2.25 0zm4.875-1.125a1.125 1.125 0 100 2.25 1.125 1.125 0 000-2.25z" clipRule="evenodd" />
                            </svg>
                            Message
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
                            <span>
                                Generate & Review
                            </span>
                        )}
                    </button>
                </form>
            </motion.div>
        </div>
    )
}
