import { useState, useEffect, useRef, useCallback } from 'react'
import { searchContacts, getRecentContacts, incrementContactUsage, searchGroups, type Contact, type GroupSearchResult } from '../../lib/api'
import UserAvatar from '../UserAvatar'

interface ContactAutocompleteProps {
    value: string
    onChange: (value: string) => void
    onSelect?: (contact: Contact) => void
    onSelectGroup?: (group: GroupSearchResult) => void // Called when a group is selected
    onEnterManual?: () => void // Called when Enter is pressed with no dropdown selection
    placeholder?: string
    className?: string
    disabled?: boolean
}

// Union type for dropdown items
type DropdownItem =
    | { type: 'contact'; data: Contact }
    | { type: 'group'; data: GroupSearchResult }

export default function ContactAutocomplete({
    value,
    onChange,
    onSelect,
    onSelectGroup,
    onEnterManual,
    placeholder = 'Search contacts...',
    className = '',
    disabled = false,
}: ContactAutocompleteProps) {
    const [isOpen, setIsOpen] = useState(false)
    const [results, setResults] = useState<DropdownItem[]>([])
    const [recentContacts, setRecentContacts] = useState<Contact[]>([])
    const [loading, setLoading] = useState(false)
    const [highlightedIndex, setHighlightedIndex] = useState(-1)
    const inputRef = useRef<HTMLInputElement>(null)
    const dropdownRef = useRef<HTMLDivElement>(null)

    // Load recent contacts on focus
    const loadRecent = useCallback(async () => {
        try {
            const recent = await getRecentContacts(5)
            setRecentContacts(recent)
        } catch {
            setRecentContacts([])
        }
    }, [])

    // Search contacts and groups when query changes
    useEffect(() => {
        const search = async () => {
            if (value.length < 2) {
                setResults([])
                return
            }

            setLoading(true)
            try {
                // Search both contacts and groups in parallel
                const [contactResults, groupResults] = await Promise.all([
                    searchContacts(value, 6),
                    searchGroups(value, 3)
                ])

                // Combine into dropdown items - groups first, then contacts
                const items: DropdownItem[] = [
                    ...groupResults.map(g => ({ type: 'group' as const, data: g })),
                    ...contactResults.map(c => ({ type: 'contact' as const, data: c }))
                ]

                setResults(items)
            } catch {
                setResults([])
            } finally {
                setLoading(false)
            }
        }

        const debounce = setTimeout(search, 200)
        return () => clearTimeout(debounce)
    }, [value])

    // Handle click outside to close dropdown
    useEffect(() => {
        const handleClickOutside = (e: MouseEvent) => {
            if (
                dropdownRef.current &&
                !dropdownRef.current.contains(e.target as Node) &&
                inputRef.current &&
                !inputRef.current.contains(e.target as Node)
            ) {
                setIsOpen(false)
            }
        }

        document.addEventListener('mousedown', handleClickOutside)
        return () => document.removeEventListener('mousedown', handleClickOutside)
    }, [])

    // Handle contact selection
    const handleSelectContact = async (contact: Contact) => {
        onChange('')
        if (onSelect) {
            onSelect(contact)
        }
        setIsOpen(false)
        setHighlightedIndex(-1)

        // Track usage
        await incrementContactUsage(contact.id)
    }

    // Handle group selection
    const handleSelectGroup = async (group: GroupSearchResult) => {
        onChange('')
        if (onSelectGroup) {
            onSelectGroup(group)
        }
        setIsOpen(false)
        setHighlightedIndex(-1)

        // Track usage for all members
        for (const member of group.members) {
            await incrementContactUsage(member.id)
        }
    }

    // Handle item selection (contact or group)
    const handleSelectItem = (item: DropdownItem) => {
        if (item.type === 'contact') {
            handleSelectContact(item.data)
        } else {
            handleSelectGroup(item.data)
        }
    }

    // Get display items based on query
    const getDisplayItems = (): DropdownItem[] => {
        if (value.length >= 2) {
            return results
        }
        // Show recent contacts when no query
        return recentContacts.map(c => ({ type: 'contact' as const, data: c }))
    }

    // Keyboard navigation
    const handleKeyDown = (e: React.KeyboardEvent) => {
        const displayItems = getDisplayItems()

        if (!isOpen || displayItems.length === 0) {
            if (e.key === 'ArrowDown' && !isOpen) {
                setIsOpen(true)
                loadRecent()
            }
            // Allow Enter for manual entry even if list is empty/closed
            if (e.key === 'Enter' && onEnterManual && value.trim()) {
                e.preventDefault()
                onEnterManual()
                setIsOpen(false)
            }
            return
        }

        switch (e.key) {
            case 'ArrowDown':
                e.preventDefault()
                setHighlightedIndex(prev =>
                    prev < displayItems.length - 1 ? prev + 1 : 0
                )
                break
            case 'ArrowUp':
                e.preventDefault()
                setHighlightedIndex(prev =>
                    prev > 0 ? prev - 1 : displayItems.length - 1
                )
                break
            case 'Enter':
                e.preventDefault()
                if (highlightedIndex >= 0 && highlightedIndex < displayItems.length) {
                    handleSelectItem(displayItems[highlightedIndex])
                } else if (onEnterManual && value.trim()) {
                    // No selection, trigger manual entry
                    onEnterManual()
                    setIsOpen(false)
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

    const displayItems = getDisplayItems()
    const showDropdown = isOpen && (displayItems.length > 0 || loading || value.length < 2)

    return (
        <div className="relative">
            <input
                ref={inputRef}
                type="text"
                value={value}
                onChange={(e) => onChange(e.target.value)}
                onFocus={handleFocus}
                onKeyDown={handleKeyDown}
                placeholder={placeholder}
                disabled={disabled}
                className={`w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 ${disabled ? 'opacity-50 cursor-not-allowed' : ''} ${className}`}
            />

            {showDropdown && (
                <div
                    ref={dropdownRef}
                    className="absolute top-full left-0 right-0 mt-1 bg-slate-800 border border-white/10 rounded-lg shadow-xl max-h-64 overflow-y-auto z-50"
                >
                    {loading ? (
                        <div className="p-3 text-center">
                            <div className="w-5 h-5 mx-auto border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin" />
                        </div>
                    ) : displayItems.length > 0 ? (
                        <>
                            {value.length < 2 && recentContacts.length > 0 && (
                                <div className="px-3 py-2 text-xs text-slate-500 border-b border-white/5">
                                    Recently used
                                </div>
                            )}
                            {displayItems.map((item, index) => (
                                item.type === 'group' ? (
                                    // Group item
                                    <button
                                        key={`group-${item.data.id}`}
                                        type="button"
                                        onClick={() => handleSelectItem(item)}
                                        className={`w-full flex items-center gap-3 px-3 py-2 text-left transition-colors ${highlightedIndex === index
                                            ? 'bg-purple-500/20'
                                            : 'hover:bg-white/5'
                                            }`}
                                    >
                                        {/* Group icon */}
                                        <div className="w-8 h-8 rounded-full bg-blue-500/20 flex items-center justify-center">
                                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4 text-blue-400">
                                                <path d="M10 9a3 3 0 100-6 3 3 0 000 6zM6 8a2 2 0 11-4 0 2 2 0 014 0zM1.49 15.326a.78.78 0 01-.358-.442 3 3 0 014.308-3.516 6.484 6.484 0 00-1.905 3.959c-.023.222-.014.442.025.654a4.97 4.97 0 01-2.07-.655zM16.44 15.98a4.97 4.97 0 002.07-.654.78.78 0 00.357-.442 3 3 0 00-4.308-3.517 6.484 6.484 0 011.907 3.96 2.32 2.32 0 01-.026.654zM18 8a2 2 0 11-4 0 2 2 0 014 0zM5.304 16.19a.844.844 0 01-.277-.71 5 5 0 019.947 0 .843.843 0 01-.277.71A6.975 6.975 0 0110 18a6.974 6.974 0 01-4.696-1.81z" />
                                            </svg>
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2">
                                                <span className="text-white text-sm truncate">{item.data.name}</span>
                                                <span className="px-1.5 py-0.5 text-xs bg-blue-500/20 text-blue-400 rounded">
                                                    Group
                                                </span>
                                            </div>
                                            <span className="text-slate-400 text-xs truncate block">
                                                {item.data.memberCount} member{item.data.memberCount !== 1 ? 's' : ''}
                                            </span>
                                        </div>
                                    </button>
                                ) : (
                                    // Contact item
                                    <button
                                        key={`contact-${item.data.id}`}
                                        type="button"
                                        onClick={() => handleSelectItem(item)}
                                        className={`w-full flex items-center gap-3 px-3 py-2 text-left transition-colors ${highlightedIndex === index
                                            ? 'bg-purple-500/20'
                                            : 'hover:bg-white/5'
                                            }`}
                                    >
                                        <UserAvatar
                                            fullName={item.data.name}
                                            photoUrl={item.data.photo}
                                            size="sm"
                                            showOnlineIndicator={item.data.usesQuickFlow}
                                        />
                                        <div className="flex-1 min-w-0">
                                            <div className="flex items-center gap-2">
                                                <span className="text-white text-sm truncate">{item.data.name}</span>
                                                {item.data.isFavorite && (
                                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-3.5 h-3.5 text-yellow-400">
                                                        <path fillRule="evenodd" d="M10.868 2.884c-.321-.772-1.415-.772-1.736 0l-1.83 4.401-4.753.381c-.833.067-1.171 1.107-.536 1.651l3.62 3.102-1.106 4.637c-.194.813.691 1.456 1.405 1.02L10 15.591l4.069 2.485c.713.436 1.598-.207 1.404-1.02l-1.106-4.637 3.62-3.102c.635-.544.297-1.584-.536-1.65l-4.752-.382-1.831-4.401z" clipRule="evenodd" />
                                                    </svg>
                                                )}
                                            </div>
                                            <span className="text-slate-400 text-xs truncate block">{item.data.email}</span>
                                        </div>
                                        {item.data.usesQuickFlow && (
                                            <span className="px-1.5 py-0.5 text-xs bg-emerald-500/20 text-emerald-400 rounded">
                                                QF
                                            </span>
                                        )}
                                    </button>
                                )
                            ))}
                        </>
                    ) : value.length >= 2 ? (
                        <div className="px-3 py-4 text-center text-slate-500 text-sm">
                            No contacts or groups found
                        </div>
                    ) : (
                        <div className="px-3 py-4 text-center text-slate-500 text-sm">
                            Type to search contacts and groups
                        </div>
                    )}
                </div>
            )}
        </div>
    )
}

