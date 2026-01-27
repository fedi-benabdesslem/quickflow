import { useState, useEffect } from 'react'
import { useSearchParams } from 'react-router-dom'
import { motion } from 'framer-motion'
import api from '../lib/api'
import EmailPreviewModal from '../components/history/EmailPreviewModal'
import MinutesPreviewModal from '../components/history/MinutesPreviewModal'

// Types
type HistoryFilter = 'all' | 'email' | 'minute' | 'bookmarks'

interface HistoryItem {
    id: string
    type: 'email' | 'minute'
    title: string
    recipients: string[]
    recipientCount: number
    sentAt: string
    isBookmarked?: boolean
}

export default function HistoryPage() {
    const [searchParams, setSearchParams] = useSearchParams()
    const [filter, setFilter] = useState<HistoryFilter>('all')
    const [searchQuery, setSearchQuery] = useState('')
    const [items, setItems] = useState<HistoryItem[]>([])
    const [loading, setLoading] = useState(false)
    const [loadingMore, setLoadingMore] = useState(false)
    const [hasMore, setHasMore] = useState(false)
    const [offset, setOffset] = useState(0)
    const [total, setTotal] = useState(0)

    const [selectedEmailId, setSelectedEmailId] = useState<string | null>(null)
    const [selectedMinuteId, setSelectedMinuteId] = useState<string | null>(null)

    // Debounce search
    const [debouncedQuery, setDebouncedQuery] = useState(searchQuery)
    useEffect(() => {
        const handler = setTimeout(() => {
            setDebouncedQuery(searchQuery)
        }, 500)
        return () => clearTimeout(handler)
    }, [searchQuery])

    useEffect(() => {
        const filterParam = searchParams.get('filter') as HistoryFilter
        if (filterParam && ['all', 'email', 'minute', 'bookmarks'].includes(filterParam)) {
            setFilter(filterParam)
        }
    }, [searchParams])

    // Load initial data
    useEffect(() => {
        setOffset(0)
        fetchHistory(0, true)
    }, [filter, debouncedQuery])

    const fetchHistory = async (currentOffset: number, reset: boolean) => {
        if (reset) {
            setLoading(true)
        } else {
            setLoadingMore(true)
        }

        try {
            const isBookmark = filter === 'bookmarks'
            const typeParam = isBookmark ? 'all' : filter
            const response = await api.get(`/history?q=${debouncedQuery}&type=${typeParam}&onlyBookmarked=${isBookmark}&limit=20&offset=${currentOffset}`)
            const data = response.data

            if (reset) {
                setItems(data.items)
            } else {
                setItems(prev => [...prev, ...data.items])
            }

            setHasMore(data.hasMore)
            setTotal(data.total)
            setOffset(currentOffset + data.items.length)
        } catch (error) {
            console.error('Failed to fetch history:', error)
        } finally {
            setLoading(false)
            setLoadingMore(false)
        }
    }

    const loadMore = () => {
        if (!loadingMore && hasMore) {
            fetchHistory(offset, false)
        }
    }

    const handleItemClick = (item: HistoryItem) => {
        if (item.type === 'email') {
            setSelectedEmailId(item.id)
        } else {
            setSelectedMinuteId(item.id)
        }
    }

    const handleToggleBookmark = async (e: React.MouseEvent, item: HistoryItem) => {
        e.stopPropagation()
        try {
            if (item.isBookmarked) {
                await api.delete(`/bookmarks/${item.id}`)
            } else {
                await api.post('/bookmarks', { itemId: item.id, type: item.type })
            }
            setItems(prev => prev.map(i => i.id === item.id ? { ...i, isBookmarked: !i.isBookmarked } : i))
        } catch (err) {
            console.error('Failed to toggle bookmark', err)
        }
    }

    const handleDelete = async (e: React.MouseEvent, item: HistoryItem) => {
        e.stopPropagation()
        if (!window.confirm('Are you sure you want to delete this item?')) return

        try {
            await api.delete(`/history/${item.type}/${item.id}`)
            setItems(prev => prev.filter(i => i.id !== item.id))
            setTotal(prev => prev - 1)
        } catch (err) {
            console.error('Failed to delete item', err)
        }
    }

    const formatTime = (isoString: string) => {
        return new Date(isoString).toLocaleString('en-US', {
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        })
    }

    return (
        <div className="h-full flex flex-col bg-slate-900/50">
            {/* Header */}
            <div className="flex-none p-8 pb-4">
                <div className="flex items-center justify-between mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-white mb-2">History & Archives</h1>
                        <p className="text-slate-400">View and manage your past communications</p>
                    </div>
                    <div className="flex gap-4">
                        {/* Summary Stats or Action Buttons */}
                        <div className="px-4 py-2 bg-white/5 rounded-lg border border-white/5">
                            <span className="text-2xl font-bold text-white mr-2">{total}</span>
                            <span className="text-sm text-slate-400">Total Items</span>
                        </div>
                    </div>
                </div>

                {/* Controls */}
                <div className="flex flex-col md:flex-row gap-4 items-center justify-between bg-white/5 p-4 rounded-xl border border-white/5">
                    {/* Search */}
                    <div className="relative w-full md:w-96">
                        <SearchIcon className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-slate-400" />
                        <input
                            type="text"
                            placeholder="Search by subject, recipient, or content..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full bg-slate-900/50 border border-white/10 rounded-lg pl-10 pr-4 py-2 text-white placeholder-slate-500 focus:outline-none focus:border-purple-500 transition-colors"
                        />
                    </div>

                    {/* Filter Tabs */}
                    <div className="flex items-center gap-1 bg-slate-900/50 p-1 rounded-lg border border-white/10">
                        {(['all', 'email', 'minute', 'bookmarks'] as const).map((f) => (
                            <button
                                key={f}
                                onClick={() => {
                                    setFilter(f)
                                    setSearchParams({ filter: f })
                                }}
                                className={`px-4 py-1.5 rounded-md text-sm font-medium transition-all ${filter === f
                                    ? 'bg-purple-500 text-white shadow-lg shadow-purple-500/25'
                                    : 'text-slate-400 hover:text-white hover:bg-white/5'
                                    }`}
                            >

                                {f.charAt(0).toUpperCase() + f.slice(1) + (f === 'all' || f === 'minute' ? 's' : '')}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto px-8 pb-8">
                {loading ? (
                    <div className="flex flex-col items-center justify-center h-64">
                        <div className="animate-spin rounded-full h-10 w-10 border-t-2 border-b-2 border-purple-500 mb-4"></div>
                        <p className="text-slate-400">Loading history...</p>
                    </div>
                ) : items.length > 0 ? (
                    <div className="grid grid-cols-1 gap-3">
                        {items.map((item, index) => (
                            <motion.div
                                key={item.id}
                                initial={{ opacity: 0, y: 10 }}
                                animate={{ opacity: 1, y: 0 }}
                                transition={{ delay: index * 0.05 }}
                                onClick={() => handleItemClick(item)}
                                className="group bg-white/5 hover:bg-white/10 border border-white/5 hover:border-purple-500/30 rounded-xl p-4 transition-all cursor-pointer flex items-center gap-4"
                            >
                                {/* Icon */}
                                <div className={`w-10 h-10 rounded-full flex items-center justify-center shrink-0 ${item.type === 'email' ? 'bg-blue-500/20 text-blue-400' : 'bg-orange-500/20 text-orange-400'
                                    }`}>
                                    {item.type === 'email' ? <EmailIcon className="w-5 h-5" /> : <DocumentIcon className="w-5 h-5" />}
                                </div>

                                {/* Main Info */}
                                <div className="flex-1 min-w-0">
                                    <h3 className="text-white font-medium truncate group-hover:text-purple-300 transition-colors">
                                        {item.title}
                                    </h3>
                                    <div className="flex items-center gap-2 text-sm text-slate-400 mt-0.5">
                                        <span>{item.type === 'email' ? 'To:' : 'With:'}</span>
                                        <div className="flex items-center gap-1 truncate">
                                            {item.recipients.slice(0, 3).map((r, i) => (
                                                <span key={i} className="bg-white/5 px-1.5 rounded text-xs border border-white/5">
                                                    {r}
                                                </span>
                                            ))}
                                            {item.recipientCount > 3 && (
                                                <span className="text-xs">+{item.recipientCount - 3} more</span>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                {/* Meta */}
                                <div className="text-right shrink-0">
                                    <p className="text-sm text-slate-400 font-mono mb-1">
                                        {formatTime(item.sentAt)}
                                    </p>
                                    <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                                        <button
                                            className={`p-1 hover:bg-white/10 rounded transition-colors ${item.isBookmarked ? 'text-yellow-400' : 'text-slate-400 hover:text-white'}`}
                                            title={item.isBookmarked ? "Remove Bookmark" : "Bookmark"}
                                            onClick={(e) => handleToggleBookmark(e, item)}
                                        >
                                            <span>{item.isBookmarked ? '★' : '📌'}</span>
                                        </button>
                                        <button
                                            className="p-1 hover:bg-white/10 rounded text-slate-400 hover:text-red-400"
                                            title="Delete"
                                            onClick={(e) => handleDelete(e, item)}
                                        >
                                            <TrashIcon className="w-4 h-4" />
                                        </button>
                                    </div>
                                </div>
                            </motion.div>
                        ))}

                        {hasMore && (
                            <div className="text-center pt-4">
                                <button
                                    onClick={loadMore}
                                    disabled={loadingMore}
                                    className="px-6 py-2 bg-white/5 hover:bg-white/10 border border-white/10 rounded-lg text-slate-300 transition-colors disabled:opacity-50"
                                >
                                    {loadingMore ? 'Loading...' : 'Load More Items'}
                                </button>
                            </div>
                        )}
                    </div>
                ) : (
                    <div className="flex flex-col items-center justify-center h-64 text-slate-500">
                        <HistoryIcon className="w-16 h-16 mb-4 opacity-20" />
                        <p className="text-lg font-medium">No history items found</p>
                        <p className="text-sm">Try adjusting your search or filters</p>
                    </div>
                )}
            </div>

            {/* Modals */}
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
        </div>
    )
}

// Icons
function SearchIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className={className}>
            <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
        </svg>
    )
}

function HistoryIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path fillRule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zM12.75 6a.75.75 0 00-1.5 0v6c0 .414.336.75.75.75h4.5a.75.75 0 000-1.5h-3.75V6z" clipRule="evenodd" />
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

function TrashIcon({ className }: { className?: string }) {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={className}>
            <path fillRule="evenodd" d="M16.5 4.478v.227a48.816 48.816 0 013.878.512.75.75 0 11-.49 1.478l-.565 9.064a2.25 2.25 0 01-2.242 2.112H6.918a2.25 2.25 0 01-2.242-2.112l-.565-9.064a48.816 48.816 0 013.878-.512v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 013.369 0c1.603.051 2.815 1.387 2.815 2.951zm-6.136-1.452a51.196 51.196 0 013.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 00-6 0v-.113c0-.794.609-1.428 1.636-1.452zm-.99 8.272a.75.75 0 00-1.5 0v4.5a.75.75 0 001.5 0v-4.5zm4.5 0a.75.75 0 00-1.5 0v4.5a.75.75 0 001.5 0v-4.5z" clipRule="evenodd" />
        </svg>
    )
}
