import { motion, AnimatePresence } from 'framer-motion'
import { useEffect, useState } from 'react'
import api from '../../lib/api'

interface MinutesPreviewModalProps {
    minuteId: string
    isOpen: boolean
    onClose: () => void
}

interface MinuteDetails {
    id: string
    subject: string
    people: string[]
    sentAt: string
    pdfFileId: string
    date: string
    time: string
    isBookmarked: boolean
}

export default function MinutesPreviewModal({ minuteId, isOpen, onClose }: MinutesPreviewModalProps) {
    const [minute, setMinute] = useState<MinuteDetails | null>(null)
    const [loading, setLoading] = useState(false)
    const [pdfLoading, setPdfLoading] = useState(false)
    const [pdfUrl, setPdfUrl] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)

    useEffect(() => {
        if (isOpen && minuteId) {
            fetchMinuteDetails()
        } else {
            setMinute(null)
            if (pdfUrl) {
                URL.revokeObjectURL(pdfUrl)
                setPdfUrl(null)
            }
        }
        return () => {
            if (pdfUrl) URL.revokeObjectURL(pdfUrl)
        }
    }, [isOpen, minuteId])

    const fetchMinuteDetails = async () => {
        setLoading(true)
        setError(null)
        setPdfUrl(null)

        try {
            const response = await api.get(`/history/minute/${minuteId}`)
            const details = response.data
            setMinute(details)

            // If PDF exists, fetch it securely
            if (details.pdfFileId) {
                fetchPdfBlob(details.pdfFileId)
            }
        } catch (err) {
            console.error('Failed to fetch minute details:', err)
            setError('Failed to load meeting minutes')
        } finally {
            setLoading(false)
        }
    }

    const fetchPdfBlob = async (fileId: string) => {
        setPdfLoading(true)
        try {
            const response = await api.get(`/pdf/preview/${fileId}`, {
                responseType: 'blob'
            })
            const blob = new Blob([response.data], { type: 'application/pdf' })
            const url = URL.createObjectURL(blob)
            setPdfUrl(url)
        } catch (err) {
            console.error('Failed to fetch PDF blob:', err)
            // Don't set main error, just log. Preview will show fallback.
        } finally {
            setPdfLoading(false)
        }
    }

    const handleToggleBookmark = async () => {
        if (!minute) return
        try {
            if (minute.isBookmarked) {
                await api.delete(`/bookmarks/${minute.id}`)
                setMinute(prev => prev ? { ...prev, isBookmarked: false } : null)
            } else {
                await api.post('/bookmarks', { itemId: minute.id, type: 'minute' })
                setMinute(prev => prev ? { ...prev, isBookmarked: true } : null)
            }
        } catch (err) {
            console.error('Failed to toggle bookmark:', err)
        }
    }

    const handleDownload = () => {
        if (!minute?.pdfFileId) return

        // If we already have the blob, use it
        if (pdfUrl) {
            const a = document.createElement('a')
            a.href = pdfUrl
            a.download = `${minute.subject || 'minutes'}.pdf`
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
        } else {
            // Fallback to api helper if blob missing (shouldn't happen if preview loaded)
            import('../../lib/api').then(({ downloadPdf }) => {
                downloadPdf(minute.pdfFileId)
            })
        }
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
                        <div className="bg-slate-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-5xl h-[85vh] flex flex-col pointer-events-auto">
                            {/* Header */}
                            <div className="flex items-center justify-between p-4 border-b border-white/10 bg-white/5 rounded-t-xl">
                                <div className="flex items-center gap-3">
                                    <div className="w-10 h-10 rounded-full bg-orange-500/20 flex items-center justify-center text-orange-400">
                                        <DocumentIcon className="w-5 h-5" />
                                    </div>
                                    <div>
                                        <h3 className="text-lg font-bold text-white">Meeting Minutes</h3>
                                        {minute && (
                                            <p className="text-xs text-slate-400">
                                                {minute.date} at {minute.time}
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
                            <div className="flex-1 overflow-hidden bg-slate-800 relative">
                                {loading ? (
                                    <div className="absolute inset-0 flex items-center justify-center">
                                        <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purple-500"></div>
                                    </div>
                                ) : error ? (
                                    <div className="absolute inset-0 flex items-center justify-center">
                                        <div className="text-center text-red-400">
                                            <p>{error}</p>
                                            <button
                                                onClick={fetchMinuteDetails}
                                                className="mt-4 px-4 py-2 bg-white/10 hover:bg-white/20 rounded-lg text-sm text-white transition-colors"
                                            >
                                                Retry
                                            </button>
                                        </div>
                                    </div>
                                ) : pdfLoading ? (
                                    <div className="absolute inset-0 flex items-center justify-center">
                                        <div className="flex flex-col items-center gap-3">
                                            <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-blue-500"></div>
                                            <p className="text-slate-400 text-sm">Loading PDF safely...</p>
                                        </div>
                                    </div>
                                ) : pdfUrl ? (
                                    <iframe
                                        src={pdfUrl}
                                        className="w-full h-full border-none"
                                        title="PDF Preview"
                                    />
                                ) : (
                                    <div className="absolute inset-0 flex items-center justify-center text-slate-400">
                                        <p>No PDF available</p>
                                    </div>
                                )}
                            </div>

                            {/* Footer */}
                            <div className="p-4 border-t border-white/10 bg-white/5 rounded-b-xl flex justify-between items-center">
                                <button
                                    onClick={handleToggleBookmark}
                                    className={`px-4 py-2 text-sm rounded-lg transition-colors flex items-center gap-2 ${minute?.isBookmarked
                                        ? 'text-yellow-400 bg-yellow-400/10 hover:bg-yellow-400/20'
                                        : 'text-slate-400 hover:text-white hover:bg-white/10'
                                        }`}
                                >
                                    <span>{minute?.isBookmarked ? '★' : '📌'}</span>
                                    {minute?.isBookmarked ? 'Bookmarked' : 'Bookmark'}
                                </button>
                                <div className="flex gap-2">
                                    <button
                                        className={`px-4 py-2 text-sm rounded-lg transition-colors border border-white/10 ${minute?.pdfFileId
                                            ? 'text-slate-300 hover:text-white hover:bg-white/10 cursor-pointer'
                                            : 'text-slate-600 cursor-not-allowed opacity-50'
                                            }`}
                                        disabled={!minute?.pdfFileId}
                                        onClick={handleDownload}
                                    >
                                        Download PDF
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

function DocumentIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0016.5 9h-1.875a1.875 1.875 0 01-1.875-1.875V5.25A3.75 3.75 0 009 1.5H5.625zM7.5 15a.75.75 0 01.75-.75h7.5a.75.75 0 010 1.5h-7.5A.75.75 0 017.5 15zm.75 2.25a.75.75 0 000 1.5H12a.75.75 0 000-1.5H8.25z" clipRule="evenodd" />
            <path d="M12.971 1.816A5.23 5.23 0 0114.25 5.25v1.875c0 .207.168.375.375.375h1.875c1.148 0 2.22.419 3.058 1.125a5.25 5.25 0 00-6.587-6.808z" />
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
