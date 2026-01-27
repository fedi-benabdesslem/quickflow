import { motion, AnimatePresence } from 'framer-motion'
import { useEffect, useState } from 'react'
import api from '../../lib/api'

interface EmailPreviewModalProps {
    emailId: string
    isOpen: boolean
    onClose: () => void
}

interface EmailDetails {
    id: string
    subject: string
    recipients: string[]
    sentAt: string
    body: string // Content/Generated Content
    senderEmail: string
    status: string
    isBookmarked: boolean
}

export default function EmailPreviewModal({ emailId, isOpen, onClose }: EmailPreviewModalProps) {
    const [email, setEmail] = useState<EmailDetails | null>(null)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (isOpen && emailId) {
            fetchEmailDetails()
        } else {
            setEmail(null)
        }
    }, [isOpen, emailId])

    const fetchEmailDetails = async () => {
        setLoading(true)
        setError(null)
        try {
            const response = await api.get(`/history/email/${emailId}`)
            setEmail(response.data)
        } catch (err) {
            console.error('Failed to fetch email details:', err)
            setError('Failed to load email details')
        } finally {
            setLoading(false)
        }
    }

    const handleToggleBookmark = async () => {
        if (!email) return
        try {
            if (email.isBookmarked) {
                await api.delete(`/bookmarks/${email.id}`)
                setEmail(prev => prev ? { ...prev, isBookmarked: false } : null)
            } else {
                await api.post('/bookmarks', { itemId: email.id, type: 'email' })
                setEmail(prev => prev ? { ...prev, isBookmarked: true } : null)
            }
        } catch (err) {
            console.error('Failed to toggle bookmark:', err)
        }
    }

    const formatTime = (isoString?: string) => {
        if (!isoString) return ''
        return new Date(isoString).toLocaleString('en-US', {
            weekday: 'short',
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: 'numeric',
            minute: 'numeric',
        })
    }

    if (!isOpen) return null

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* Backdrop */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={onClose}
                        className="fixed inset-0 bg-black/70 backdrop-blur-sm z-[80]"
                    />

                    {/* Modal */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: 20 }}
                        className="fixed inset-0 z-[90] flex items-center justify-center p-4 pointer-events-none"
                    >
                        <div className="bg-slate-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-2xl max-h-[85vh] flex flex-col pointer-events-auto">
                            {/* Header */}
                            <div className="flex items-center justify-between p-4 border-b border-white/10 bg-white/5 rounded-t-xl">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full bg-blue-500/20 flex items-center justify-center text-blue-400">
                                        <EmailIcon className="w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-lg font-bold text-white">Email Preview</h3>
                                        {email && (
                                            <p className="text-xs text-slate-400">
                                                Sent via {email.senderEmail || 'QuickFlow'}
                                            </p>
                                        )}
                                    </div>
                                </div>
                                <button
                                    onClick={onClose}
                                    className="p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                                >
                                    <CloseIcon className="w-5 h-5" />
                                </button>
                            </div>

                            {/* Content */}
                            <div className="flex-1 overflow-y-auto p-6">
                                {loading ? (
                                    <div className="flex justify-center py-12">
                                        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purple-500"></div>
                                    </div>
                                ) : error ? (
                                    <div className="text-center py-12 text-red-400">
                                        <p>{error}</p>
                                        <button
                                            onClick={fetchEmailDetails}
                                            className="mt-4 px-4 py-2 bg-white/10 hover:bg-white/20 rounded-lg text-sm text-white transition-colors"
                                        >
                                            Retry
                                        </button>
                                    </div>
                                ) : email && (
                                    <div className="space-y-6">
                                        {/* Metadata */}
                                        <div className="space-y-3">
                                            <div>
                                                <h1 className="text-xl font-bold text-white mb-2">
                                                    {email.subject || '(No Subject)'}
                                                </h1>
                                                <p className="text-sm text-slate-400">
                                                    {formatTime(email.sentAt)}
                                                </p>
                                            </div>

                                            <div className="flex items-start gap-2 text-sm">
                                                <span className="text-slate-500 min-w-[3rem]">To:</span>
                                                <div className="flex flex-wrap gap-2">
                                                    {email.recipients.map((recipient, i) => (
                                                        <span key={i} className="px-2 py-0.5 bg-white/5 rounded-full text-slate-300 border border-white/5">
                                                            {recipient}
                                                        </span>
                                                    ))}
                                                </div>
                                            </div>
                                        </div>

                                        <div className="h-px bg-white/10" />

                                        {/* Body */}
                                        <div className="prose prose-invert max-w-none">
                                            <div className="whitespace-pre-wrap text-slate-300 leading-relaxed font-sans">
                                                {email.body}
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>

                            {/* Footer */}
                            <div className="p-4 border-t border-white/10 bg-white/5 rounded-b-xl flex justify-between items-center">
                                <button
                                    onClick={handleToggleBookmark}
                                    className={`px-4 py-2 text-sm rounded-lg transition-colors flex items-center gap-2 ${email?.isBookmarked
                                            ? 'text-yellow-400 bg-yellow-400/10 hover:bg-yellow-400/20'
                                            : 'text-slate-400 hover:text-white hover:bg-white/10'
                                        }`}
                                >
                                    <span>{email?.isBookmarked ? '★' : '📌'}</span>
                                    {email?.isBookmarked ? 'Bookmarked' : 'Bookmark'}
                                </button>
                                <div className="flex gap-2">
                                    <button
                                        className="px-4 py-2 text-sm text-slate-300 hover:text-white hover:bg-white/10 rounded-lg transition-colors border border-white/10"
                                        onClick={() => {
                                            if (email?.body) {
                                                navigator.clipboard.writeText(email.body)
                                                // TODO: Show toast
                                            }
                                        }}
                                    >
                                        Copy Text
                                    </button>
                                    <button
                                        onClick={onClose}
                                        className="px-4 py-2 bg-white text-slate-900 font-medium rounded-lg hover:bg-slate-200 transition-colors"
                                    >
                                        Close
                                    </button>
                                </div>
                            </div>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    )
}

function EmailIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path d="M1.5 8.67v8.58a3 3 0 003 3h15a3 3 0 003-3V8.67l-8.928 5.493a3 3 0 01-3.144 0L1.5 8.67z" />
            <path d="M22.5 6.908V6.75a3 3 0 00-3-3h-15a3 3 0 00-3 3v.158l9.714 5.978a1.5 1.5 0 001.572 0L22.5 6.908z" />
        </svg>
    )
}

function CloseIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path fillRule="evenodd" d="M5.47 5.47a.75.75 0 011.06 0L12 10.94l5.47-5.47a.75.75 0 111.06 1.06L13.06 12l5.47 5.47a.75.75 0 11-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 01-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 010-1.06z" clipRule="evenodd" />
        </svg>
    )
}
