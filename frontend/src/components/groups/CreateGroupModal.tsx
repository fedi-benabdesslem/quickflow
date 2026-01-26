import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { createGroup, searchContacts, type Group, type Contact } from '../../lib/api'
import UserAvatar from '../UserAvatar'

interface CreateGroupModalProps {
    isOpen: boolean
    onClose: () => void
    onGroupCreated: (group: Group) => void
}

export default function CreateGroupModal({ isOpen, onClose, onGroupCreated }: CreateGroupModalProps) {
    const [name, setName] = useState('')
    const [description, setDescription] = useState('')
    const [selectedMembers, setSelectedMembers] = useState<Contact[]>([])
    const [searchQuery, setSearchQuery] = useState('')
    const [searchResults, setSearchResults] = useState<Contact[]>([])
    const [searching, setSearching] = useState(false)
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    // Search contacts when query changes
    useEffect(() => {
        const search = async () => {
            if (searchQuery.length < 2) {
                setSearchResults([])
                return
            }

            setSearching(true)
            try {
                const results = await searchContacts(searchQuery, 10)
                // Filter out already selected members
                const filtered = results.filter(
                    r => !selectedMembers.some(m => m.id === r.id)
                )
                setSearchResults(filtered)
            } catch {
                setSearchResults([])
            } finally {
                setSearching(false)
            }
        }

        const debounce = setTimeout(search, 300)
        return () => clearTimeout(debounce)
    }, [searchQuery, selectedMembers])

    const handleAddMember = (contact: Contact) => {
        setSelectedMembers(prev => [...prev, contact])
        setSearchQuery('')
        setSearchResults([])
    }

    const handleRemoveMember = (contactId: string) => {
        setSelectedMembers(prev => prev.filter(m => m.id !== contactId))
    }

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setError('')

        if (!name.trim()) {
            setError('Group name is required')
            return
        }

        setLoading(true)

        try {
            const result = await createGroup({
                name: name.trim(),
                description: description.trim() || undefined,
                memberIds: selectedMembers.map(m => m.id),
            })

            if (result.error) {
                setError(result.error)
            } else if (result.group) {
                onGroupCreated(result.group)
                handleClose()
            }
        } catch {
            setError('Failed to create group. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    const handleClose = () => {
        setName('')
        setDescription('')
        setSelectedMembers([])
        setSearchQuery('')
        setSearchResults([])
        setError('')
        onClose()
    }

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* Backdrop */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        onClick={handleClose}
                        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
                    />

                    {/* Modal */}
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        exit={{ opacity: 0, scale: 0.95, y: 20 }}
                        className="fixed inset-0 z-50 flex items-center justify-center p-4"
                    >
                        <div className="bg-slate-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-lg max-h-[90vh] overflow-hidden flex flex-col">
                            {/* Header */}
                            <div className="flex items-center justify-between p-6 border-b border-white/10">
                                <h2 className="text-xl font-semibold text-white">Create Group</h2>
                                <button
                                    onClick={handleClose}
                                    className="p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                        <path fillRule="evenodd" d="M5.47 5.47a.75.75 0 011.06 0L12 10.94l5.47-5.47a.75.75 0 111.06 1.06L13.06 12l5.47 5.47a.75.75 0 11-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 01-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 010-1.06z" clipRule="evenodd" />
                                    </svg>
                                </button>
                            </div>

                            {/* Form */}
                            <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
                                {error && (
                                    <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
                                        {error}
                                    </div>
                                )}

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Group Name <span className="text-red-400">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        placeholder="e.g., Engineering Team"
                                        className="w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Description <span className="text-slate-500">(optional)</span>
                                    </label>
                                    <textarea
                                        value={description}
                                        onChange={(e) => setDescription(e.target.value)}
                                        placeholder="What is this group for?"
                                        rows={2}
                                        className="w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50 resize-none"
                                    />
                                </div>

                                {/* Member Search */}
                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Add Members
                                    </label>
                                    <div className="relative">
                                        <input
                                            type="text"
                                            value={searchQuery}
                                            onChange={(e) => setSearchQuery(e.target.value)}
                                            placeholder="Search contacts to add..."
                                            className="w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                                        />

                                        {/* Search Results Dropdown */}
                                        {(searchResults.length > 0 || searching) && (
                                            <div className="absolute top-full left-0 right-0 mt-1 bg-slate-800 border border-white/10 rounded-lg shadow-xl max-h-48 overflow-y-auto z-10">
                                                {searching ? (
                                                    <div className="p-3 text-center text-slate-400">
                                                        <div className="w-4 h-4 mx-auto border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin" />
                                                    </div>
                                                ) : (
                                                    searchResults.map(contact => (
                                                        <button
                                                            key={contact.id}
                                                            type="button"
                                                            onClick={() => handleAddMember(contact)}
                                                            className="w-full flex items-center gap-3 p-3 hover:bg-white/5 transition-colors text-left"
                                                        >
                                                            <UserAvatar fullName={contact.name} size="sm" />
                                                            <div className="flex-1 min-w-0">
                                                                <p className="text-white text-sm truncate">{contact.name}</p>
                                                                <p className="text-slate-400 text-xs truncate">{contact.email}</p>
                                                            </div>
                                                        </button>
                                                    ))
                                                )}
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {/* Selected Members */}
                                {selectedMembers.length > 0 && (
                                    <div>
                                        <label className="block text-sm font-medium text-slate-300 mb-2">
                                            Members ({selectedMembers.length})
                                        </label>
                                        <div className="flex flex-wrap gap-2">
                                            {selectedMembers.map(member => (
                                                <div
                                                    key={member.id}
                                                    className="flex items-center gap-2 px-3 py-1.5 bg-purple-500/20 border border-purple-500/30 rounded-full"
                                                >
                                                    <UserAvatar fullName={member.name} size="sm" />
                                                    <span className="text-sm text-white">{member.name}</span>
                                                    <button
                                                        type="button"
                                                        onClick={() => handleRemoveMember(member.id)}
                                                        className="text-purple-300 hover:text-white transition-colors"
                                                    >
                                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                                                            <path d="M6.28 5.22a.75.75 0 00-1.06 1.06L8.94 10l-3.72 3.72a.75.75 0 101.06 1.06L10 11.06l3.72 3.72a.75.75 0 101.06-1.06L11.06 10l3.72-3.72a.75.75 0 00-1.06-1.06L10 8.94 6.28 5.22z" />
                                                        </svg>
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </form>

                            {/* Actions */}
                            <div className="flex gap-3 p-6 border-t border-white/10">
                                <button
                                    type="button"
                                    onClick={handleClose}
                                    className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-medium transition-colors"
                                >
                                    Cancel
                                </button>
                                <button
                                    onClick={handleSubmit}
                                    disabled={loading}
                                    className="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2"
                                >
                                    {loading ? (
                                        <>
                                            <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                            Creating...
                                        </>
                                    ) : (
                                        'Create Group'
                                    )}
                                </button>
                            </div>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    )
}
