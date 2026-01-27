import { useState, useEffect, useRef, useCallback } from 'react'
import { searchContacts, getRecentContacts, incrementContactUsage, type Contact } from '../../lib/api'
import UserAvatar from '../UserAvatar'

interface MultiContactSelectProps {
    value: Contact[]
    onChange: (contacts: Contact[]) => void
    placeholder?: string
    className?: string
    disabled?: boolean
    label?: string
}

export default function MultiContactSelect({
    value,
    onChange,
    placeholder = 'Add participants...',
    className = '',
    disabled = false,
    label,
}: MultiContactSelectProps) {
    const [query, setQuery] = useState('')
    const [isOpen, setIsOpen] = useState(false)
    const [results, setResults] = useState<Contact[]>([])
    const [recentContacts, setRecentContacts] = useState<Contact[]>([])
    const [loading, setLoading] = useState(false)
    const [highlightedIndex, setHighlightedIndex] = useState(-1)
    const inputRef = useRef<HTMLInputElement>(null)
    const containerRef = useRef<HTMLDivElement>(null)

    // Load recent contacts on focus
    const loadRecent = useCallback(async () => {
        try {
            const recent = await getRecentContacts(5)
            // Filter out already selected
            const filtered = recent.filter(r => !value.some(v => v.id === r.id))
            setRecentContacts(filtered)
        } catch {
            setRecentContacts([])
        }
    }, [value])

    // Search contacts when query changes
    useEffect(() => {
        const search = async () => {
            if (query.length < 2) {
                setResults([])
                return
            }

            setLoading(true)
            try {
                const searchResults = await searchContacts(query, 8)
                // Filter out already selected
                const filtered = searchResults.filter(r => !value.some(v => v.id === r.id))
                setResults(filtered)
            } catch {
                setResults([])
            } finally {
                setLoading(false)
            }
        }

        const debounce = setTimeout(search, 200)
        return () => clearTimeout(debounce)
    }, [query, value])

    // Handle click outside to close dropdown
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
                setIsOpen(false)
            }
        }

        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    // Handle selection
    const handleSelect = async (contact: Contact) => {
        onChange([...value, contact])
        setQuery('')
        setHighlightedIndex(-1)
        inputRef.current?.focus()

        // Track usage
        await incrementContactUsage(contact.id)
    }

    // Handle removal
    const handleRemove = (contactId: string) => {
        onChange(value.filter(c => c.id !== contactId))
    }

    // Keyboard navigation
    const handleKeyDown = (e: React.KeyboardEvent) => {
        const displayItems = query.length >= 2 ? results : recentContacts

        switch (e.key) {
            case 'ArrowDown':
                if (!isOpen) {
                    setIsOpen(true)
                    loadRecent()
                } else if (displayItems.length > 0) {
                    e.preventDefault()
                    setHighlightedIndex(prev =>
                        prev < displayItems.length - 1 ? prev + 1 : 0
                    )
                }
                break
            case 'ArrowUp':
                if (displayItems.length > 0) {
                    e.preventDefault()
                    setHighlightedIndex(prev =>
                        prev > 0 ? prev - 1 : displayItems.length - 1
                    )
                }
                break
            case 'Enter':
                e.preventDefault()
                if (highlightedIndex >= 0 && highlightedIndex < displayItems.length) {
                    handleSelect(displayItems[highlightedIndex])
                }
                break
            case 'Backspace':
                if (query === '' && value.length > 0) {
                    handleRemove(value[value.length - 1].id)
                }
                break
            case 'Escape':
                setIsOpen(false)
                setHighlightedIndex(-1)
                break
        }
    }

    const handleFocus = () => {
        setIsOpen(true)
        loadRecent()
    }

    const displayItems = query.length >= 2 ? results : recentContacts
    const showDropdown = isOpen && (displayItems.length > 0 || loading || query.length < 2)

    return (
        <div className={className} ref={containerRef}>
            {label && (
                <label className="block text-sm font-medium text-slate-300 mb-2">
                    {label}
                </label>
            )}

            <div
                className={`min-h-[42px] px-2 py-1.5 bg-slate-800/50 border border-white/10 rounded-lg flex flex-wrap gap-1.5 items-center focus-within:ring-2 focus-within:ring-purple-500/50 ${disabled ? 'opacity-50 cursor-not-allowed' : ''}`}
                onClick={() => !disabled && inputRef.current?.focus()}
            >
                {/* Selected tags */}
                {value.map(contact => (
                    <div
                        key={contact.id}
                        className="flex items-center gap-1.5 px-2 py-1 bg-purple-500/20 border border-purple-500/30 rounded-full"
                    >
                        <UserAvatar fullName={contact.name} photoUrl={contact.photo} size="xs" />
                        <span className="text-sm text-white max-w-[100px] truncate">{contact.name}</span>
                        <button
                            type="button"
                            onClick={(e) => { e.stopPropagation(); handleRemove(contact.id); }}
                            className="text-purple-300 hover:text-white transition-colors"
                            disabled={disabled}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-3.5 h-3.5">
                                <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
                            </svg>
                        </button>
                    </div>
                ))}

                {/* Input */}
                <input
                    ref={inputRef}
                    type="text"
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onFocus={handleFocus}
                    onKeyDown={handleKeyDown}
                    placeholder={value.length === 0 ? placeholder : ''}
                    disabled={disabled}
                    className="flex-1 min-w-[100px] bg-transparent text-white placeholder-slate-400 focus:outline-none text-sm py-1"
                />
            </div>

            {/* Dropdown */}
            {showDropdown && (
                <div className="relative">
                    <div className="absolute top-1 left-0 right-0 bg-slate-800 border border-white/10 rounded-lg shadow-xl max-h-64 overflow-y-auto z-50">
                        {loading ? (
                            <div className="p-3 text-center">
                                <div className="w-5 h-5 mx-auto border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin" />
                            </div>
                        ) : displayItems.length > 0 ? (
                            <>
                                {query.length < 2 && recentContacts.length > 0 && (
                                    <div className="px-3 py-2 text-xs text-slate-500 border-b border-white/5">
                                        Recently used
                                    </div>
                                )}
                                {displayItems.map((contact, index) => (
                                    <button
                                        key={contact.id}
                                        type="button"
                                        onClick={() => handleSelect(contact)}
                                        className={`w-full flex items-center gap-3 px-3 py-2 text-left transition-colors ${highlightedIndex === index
                                                ? 'bg-purple-500/20'
                                                : 'hover:bg-white/5'
                                            }`}
                                    >
                                        <UserAvatar
                                            fullName={contact.name}
                                            photoUrl={contact.photo}
                                            size="sm"
                                            showOnlineIndicator={contact.usesQuickFlow}
                                        />
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2">
                                                <span className="text-white text-sm truncate">{contact.name}</span>
                                                {contact.isFavorite && (
                                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-3.5 h-3.5 text-yellow-400">
                                                        <path fillRule="evenodd" d="M10.868 2.884c-.321-.772-1.415-.772-1.736 0l-1.83 4.401-4.753.381c-.833.067-1.171 1.107-.536 1.651l3.62 3.102-1.106 4.637c-.194.813.691 1.456 1.405 1.02L10 15.591l4.069 2.485c.713.436 1.598-.207 1.404-1.02l-1.106-4.637 3.62-3.102c.635-.544.297-1.584-.536-1.65l-4.752-.382-1.831-4.401z" clipRule="evenodd" />
                                                    </svg>
                                                )}
                                            </div>
                                            <span className="text-slate-400 text-xs truncate block">{contact.email}</span>
                                        </div>
                                        {contact.usesQuickFlow && (
                                            <span className="px-1.5 py-0.5 text-xs bg-emerald-500/20 text-emerald-400 rounded">
                                                QF
                                            </span>
                                        )}
                                    </button>
                                ))}
                            </>
                        ) : query.length >= 2 ? (
                            <div className="px-3 py-4 text-center text-slate-500 text-sm">
                                No contacts found
                            </div>
                        ) : (
                            <div className="px-3 py-4 text-center text-slate-500 text-sm">
                                Type to search contacts
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    )
}
