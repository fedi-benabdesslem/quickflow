import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useSidebar } from '../../contexts/SidebarContext'
import api from '../../lib/api'
import EmailPreviewModal from './EmailPreviewModal'
import MinutesPreviewModal from './MinutesPreviewModal'

// Types
type HistoryFilter = 'all' | 'email' | 'minute'

interface HistoryItem {
    id: string
    type: 'email' | 'minute'
    title: string
    recipients: string[]
    recipientCount: number
    sentAt: string
    isBookmarked?: boolean
}



export default function HistorySidebar() {
    // State
    const { isHistoryOpen, closeHistorySidebar } = useSidebar()
    const navigate = useNavigate()
    const [filter, setFilter] = useState<HistoryFilter>('all')
    const [items, setItems] = useState<HistoryItem[]>([])
    const [loading, setLoading] = useState(false)
    const [, setError] = useState<string | null>(null)
    const [selectedEmailId, setSelectedEmailId] = useState<string | null>(null)
    const [selectedMinuteId, setSelectedMinuteId] = useState<string | null>(null)

    // Fetch history
    useEffect(() => {
        if (isHistoryOpen) {
            fetchHistory()
        }
    }, [isHistoryOpen, filter])

    const fetchHistory = async () => {
        setLoading(true)
        setError(null)
        try {
            const response = await api.get(`/history/recent?type=${filter}&limit=15`)
            // Backend returns { items: [], hasMore: bool, total: number }
            setItems(response.data.items)
        } catch (err) {
            console.error('Failed to fetch history:', err)
            setError('Failed to load history')
        } finally {
            setLoading(false)
        }
    }

    // Close on ESC
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape' && isHistoryOpen) {
                closeHistorySidebar()
            }
        }
        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [isHistoryOpen, closeHistorySidebar])

    const filteredItems = items.filter(item => {
        if (filter === 'all') return true
        return item.type === filter
    })

    const handleViewFullHistory = () => {
        closeHistorySidebar()
        navigate('/history')
    }

    const handleItemClick = (item: HistoryItem) => {
        if (item.type === 'email') {
            setSelectedEmailId(item.id)
        } else {
            setSelectedMinuteId(item.id)
        }
    }

    const formatTime = (isoString: string) => {
        const date = new Date(isoString)
        const now = new Date()
        const diffMs = now.getTime() - date.getTime()
        const diffHours = diffMs / (1000 * 60 * 60)

        if (diffHours < 24) {
            return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
        }
        return date.toLocaleDateString([], { month: 'short', day: 'numeric' })
    }

    return (
        <AnimatePresence>
            {isHistoryOpen && (
                <>
                    <EmailPreviewModal
                        emailId={selectedEmailId || ''}
                        isOpen={!!selectedEmailId}
                        onClose={() => setSelectedEmailId(null)}
                    />
                    <MinutesPreviewModal
                        minuteId={selectedMinuteId || ''}
                        isOpen={!!selectedMinuteId}
                        onClose={() => setSelectedMinuteId(null)}
                    />
                    {/* Backdrop */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={closeHistorySidebar}
                        className="fixed inset-0 bg-black/50 backdrop-blur-sm z-[60]"
                    />

                    {/* Sidebar */}
                    <motion.div
                        initial={{ x: '100%' }}
                        animate={{ x: 0 }}
                        exit={{ x: '100%' }}
                        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                        className="fixed right-0 top-0 h-full w-[400px] bg-slate-900/95 backdrop-blur-xl border-l border-white/10 z-[70] flex flex-col shadow-2xl"
                    >
                        {/* Header */}
                        <div className="p-4 border-b border-white/10 flex items-center justify-between">
                            <h2 className="text-lg font-bold text-white flex items-center gap-2">
                                <HistoryIcon className="w-5 h-5 text-purple-400" />
                                History
                            </h2>
                            <div className="flex items-center gap-2">
                                <button
                                    onClick={handleViewFullHistory}
                                    className="text-xs text-purple-400 hover:text-purple-300 px-2 py-1 rounded hover:bg-purple-500/10 transition-colors"
                                >
                                    See Full →
                                </button>
                                <button
                                    onClick={closeHistorySidebar}
                                    className="p-1 text-slate-400 hover:text-white hover:bg-white/10 rounded transition-colors"
                                >
                                    <CloseIcon className="w-5 h-5" />
                                </button>
                            </div>
                        </div>

                        {/* Filter Tabs */}
                        <div className="p-4 pb-0 grid grid-cols-3 gap-1 border-b border-white/10">
                            {(['all', 'email', 'minute'] as const).map((f) => (
                                <button
                                    key={f}
                                    onClick={() => setFilter(f)}
                                    className={`pb-3 text-sm font-medium transition-colors relative ${filter === f ? 'text-white' : 'text-slate-500 hover:text-slate-300'
                                        }`}
                                >
                                    {f.charAt(0).toUpperCase() + f.slice(1) + 's'}
                                    {filter === f && (
                                        <motion.div
                                            layoutId="activeTab"
                                            className="absolute bottom-0 left-0 right-0 h-0.5 bg-purple-500"
                                        />
                                    )}
                                </button>
                            ))}
                        </div>

                        {/* Content */}
                        <div className="flex-1 overflow-y-auto p-4 space-y-3">
                            {loading ? (
                                <div className="flex justify-center py-8">
                                    <div className="animate-spin rounded-full h-8 w-8 border-t-2 border-b-2 border-purple-500"></div>
                                </div>
                            ) : filteredItems.length > 0 ? (
                                filteredItems.map((item) => (
                                    <div
                                        key={item.id}
                                        className="bg-white/5 hover:bg-white/10 border border-white/5 hover:border-white/20 rounded-lg p-3 transition-colors cursor-pointer group"
                                        onClick={() => handleItemClick(item)}
                                    >
                                        <div className="flex justify-between items-start mb-1">
                                            <div className="flex items-center gap-2">
                                                {item.type === 'email' ? (
                                                    <EmailIcon className="w-4 h-4 text-blue-400" />
                                                ) : (
                                                    <DocumentIcon className="w-4 h-4 text-orange-400" />
                                                )}
                                                <span className="text-xs text-slate-400 font-mono">
                                                    {formatTime(item.sentAt)}
                                                </span>
                                            </div>
                                            {item.isBookmarked && (
                                                <span className="text-xs">📌</span>
                                            )}
                                        </div>

                                        <h3 className="text-sm font-semibold text-white mb-1 truncate pr-2">
                                            {item.title}
                                        </h3>

                                        <p className="text-xs text-slate-400 truncate">
                                            To: {item.recipients[0]}
                                            {item.recipientCount > 1 && ` +${item.recipientCount - 1}`}
                                        </p>

                                        <div className="mt-2 opacity-0 group-hover:opacity-100 transition-opacity flex justify-end">
                                            <button className="text-xs text-purple-400 hover:text-purple-300">
                                                View Details
                                            </button>
                                        </div>
                                    </div>
                                ))
                            ) : (
                                <div className="text-center py-10 text-slate-500">
                                    <p>No items found</p>
                                </div>
                            )}

                            <button className="w-full py-2 text-xs text-slate-500 hover:text-slate-300 transition-colors border-t border-white/5 mt-2">
                                Load More...
                            </button>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    )
}

// Icons
function HistoryIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path fillRule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zM12.75 6a.75.75 0 00-1.5 0v6c0 .414.336.75.75.75h4.5a.75.75 0 000-1.5h-3.75V6z" clipRule="evenodd" />
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

function EmailIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path d="M1.5 8.67v8.58a3 3 0 003 3h15a3 3 0 003-3V8.67l-8.928 5.493a3 3 0 01-3.144 0L1.5 8.67z" />
            <path d="M22.5 6.908V6.75a3 3 0 00-3-3h-15a3 3 0 00-3 3v.158l9.714 5.978a1.5 1.5 0 001.572 0L22.5 6.908z" />
        </svg>
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
