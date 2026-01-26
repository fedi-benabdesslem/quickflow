import { useState, useEffect, useCallback } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useSidebar } from '../contexts/SidebarContext'
import UserAvatar from '../components/UserAvatar'
import UserProfileSidebar from '../components/UserProfileSidebar'
import ContactCard from '../components/contacts/ContactCard'
import AddContactModal from '../components/contacts/AddContactModal'
import EditContactModal from '../components/contacts/EditContactModal'
import DeleteConfirmModal from '../components/contacts/DeleteConfirmModal'
import {
    getContacts,
    importContacts,
    syncContacts,
    deleteContact,
    toggleContactFavorite,
    type Contact,
    type ContactsResponse,
} from '../lib/api'

type FilterType = 'all' | 'favorites' | 'quickflow' | 'google' | 'microsoft' | 'manual'
type SortType = 'name' | 'name_desc' | 'recent'

export default function ContactsPage() {
    const { user } = useAuth()
    const { openSidebar } = useSidebar()
    const navigate = useNavigate()

    // State
    const [contacts, setContacts] = useState<Contact[]>([])
    const [loading, setLoading] = useState(true)
    const [importing, setImporting] = useState(false)
    const [lastSync, setLastSync] = useState<string | null>(null)
    const [searchQuery, setSearchQuery] = useState('')
    const [filter, setFilter] = useState<FilterType>('all')
    const [sortBy, setSortBy] = useState<SortType>('name')

    // Modal state
    const [showAddModal, setShowAddModal] = useState(false)
    const [editingContact, setEditingContact] = useState<Contact | null>(null)
    const [deletingContact, setDeletingContact] = useState<Contact | null>(null)

    // Load contacts
    const loadContacts = useCallback(async () => {
        setLoading(true)
        try {
            const filterParam = filter === 'all' ? undefined :
                filter === 'favorites' ? 'favorites' :
                    filter === 'quickflow' ? 'quickflow' : undefined
            const sourceParam = ['google', 'microsoft', 'manual'].includes(filter) ? filter : undefined

            const response: ContactsResponse = await getContacts(filterParam, sourceParam, sortBy)
            setContacts(response.contacts || [])
            if (response.lastSync) {
                setLastSync(response.lastSync)
            }
        } catch (error) {
            console.error('Failed to load contacts:', error)
        } finally {
            setLoading(false)
        }
    }, [filter, sortBy])

    useEffect(() => {
        loadContacts()
    }, [loadContacts])

    // Filter by search query
    const filteredContacts = contacts.filter(contact => {
        if (!searchQuery) return true
        const query = searchQuery.toLowerCase()
        return (
            contact.name.toLowerCase().includes(query) ||
            contact.email.toLowerCase().includes(query) ||
            (contact.phone && contact.phone.includes(query))
        )
    })

    // Import contacts
    const handleImport = async (provider?: string) => {
        setImporting(true)
        try {
            const result = await importContacts(provider)
            if (result.error) {
                alert(result.error)
            } else {
                await loadContacts()
                alert(`Imported ${result.imported} new contacts, updated ${result.updated}`)
            }
        } catch (error) {
            alert('Failed to import contacts')
        } finally {
            setImporting(false)
        }
    }

    // Sync contacts
    const handleSync = async () => {
        setImporting(true)
        try {
            const result = await syncContacts()
            if (result.error) {
                alert(result.error)
            } else {
                await loadContacts()
            }
        } finally {
            setImporting(false)
        }
    }

    // Delete contact
    const handleDelete = async () => {
        if (!deletingContact) return

        const result = await deleteContact(deletingContact.id)
        if (result.success) {
            setContacts(prev => prev.filter(c => c.id !== deletingContact.id))
        } else {
            alert(result.error || 'Failed to delete contact')
        }
        setDeletingContact(null)
    }

    // Toggle favorite
    const handleToggleFavorite = async (contact: Contact) => {
        const result = await toggleContactFavorite(contact.id)
        if (result.contact) {
            setContacts(prev => prev.map(c => c.id === contact.id ? result.contact! : c))
        }
    }

    // Format last sync time
    const formatLastSync = () => {
        if (!lastSync) return 'Never'
        const date = new Date(lastSync)
        return date.toLocaleString()
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Profile Sidebar */}
            <UserProfileSidebar />

            {/* Modals */}
            <AddContactModal
                isOpen={showAddModal}
                onClose={() => setShowAddModal(false)}
                onContactAdded={(contact) => {
                    setContacts(prev => [contact, ...prev])
                }}
            />

            <EditContactModal
                isOpen={!!editingContact}
                contact={editingContact}
                onClose={() => setEditingContact(null)}
                onContactUpdated={(contact) => {
                    setContacts(prev => prev.map(c => c.id === contact.id ? contact : c))
                }}
            />

            <DeleteConfirmModal
                isOpen={!!deletingContact}
                title="Delete Contact"
                message="Are you sure you want to delete this contact?"
                itemName={deletingContact?.name || ''}
                onConfirm={handleDelete}
                onCancel={() => setDeletingContact(null)}
            />

            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <div className="flex items-center gap-3">
                    <UserAvatar
                        fullName={user?.username || user?.email}
                        size="md"
                        showOnlineIndicator
                        onClick={openSidebar}
                    />
                </div>
                <div className="flex items-center gap-3">
                    <img src="/logo.png" alt="QuickFlow" className="h-8 w-auto object-contain opacity-70" />
                </div>
            </motion.header>

            {/* Page Content */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="max-w-6xl mx-auto"
            >
                {/* Page Header */}
                <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
                    <div>
                        <h1 className="text-2xl sm:text-3xl font-bold text-white mb-2">
                            My Contacts
                        </h1>
                        <p className="text-slate-400">
                            {contacts.length} contacts • Manage and import from Google or Microsoft
                        </p>
                    </div>
                    <div className="flex gap-3">
                        <button
                            onClick={() => handleImport()}
                            disabled={importing}
                            className="px-4 py-2 bg-purple-600 hover:bg-purple-500 disabled:opacity-50 text-white rounded-lg font-medium transition-colors flex items-center gap-2"
                        >
                            {importing ? (
                                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                            ) : (
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                    <path fillRule="evenodd" d="M12 2.25a.75.75 0 01.75.75v11.69l3.22-3.22a.75.75 0 111.06 1.06l-4.5 4.5a.75.75 0 01-1.06 0l-4.5-4.5a.75.75 0 111.06-1.06l3.22 3.22V3a.75.75 0 01.75-.75zm-9 13.5a.75.75 0 01.75.75v2.25a1.5 1.5 0 001.5 1.5h13.5a1.5 1.5 0 001.5-1.5V16.5a.75.75 0 011.5 0v2.25a3 3 0 01-3 3H5.25a3 3 0 01-3-3V16.5a.75.75 0 01.75-.75z" clipRule="evenodd" />
                                </svg>
                            )}
                            Import
                        </button>
                        <button
                            onClick={() => setShowAddModal(true)}
                            className="px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-medium transition-colors flex items-center gap-2"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                <path fillRule="evenodd" d="M12 3.75a.75.75 0 01.75.75v6.75h6.75a.75.75 0 010 1.5h-6.75v6.75a.75.75 0 01-1.5 0v-6.75H4.5a.75.75 0 010-1.5h6.75V4.5a.75.75 0 01.75-.75z" clipRule="evenodd" />
                            </svg>
                            Add
                        </button>
                    </div>
                </div>

                {/* Sync Status */}
                <div className="flex items-center gap-4 mb-6 text-sm text-slate-400">
                    <span>Last synced: {formatLastSync()}</span>
                    <button
                        onClick={handleSync}
                        disabled={importing}
                        className="text-purple-400 hover:text-purple-300 disabled:opacity-50 transition-colors flex items-center gap-1"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className={`w-4 h-4 ${importing ? 'animate-spin' : ''}`}>
                            <path fillRule="evenodd" d="M4.755 10.059a7.5 7.5 0 0112.548-3.364l1.903 1.903h-3.183a.75.75 0 100 1.5h4.992a.75.75 0 00.75-.75V4.356a.75.75 0 00-1.5 0v3.18l-1.9-1.9A9 9 0 003.306 9.67a.75.75 0 101.45.388zm15.408 3.352a.75.75 0 00-.919.53 7.5 7.5 0 01-12.548 3.364l-1.902-1.903h3.183a.75.75 0 000-1.5H2.984a.75.75 0 00-.75.75v4.992a.75.75 0 001.5 0v-3.18l1.9 1.9a9 9 0 0015.059-4.035.75.75 0 00-.53-.918z" clipRule="evenodd" />
                        </svg>
                        Sync Now
                    </button>
                </div>

                {/* Search and Filters */}
                <div className="glass-card p-4 mb-6">
                    <div className="flex flex-col sm:flex-row gap-4">
                        <div className="flex-1 relative">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400">
                                <path fillRule="evenodd" d="M10.5 3.75a6.75 6.75 0 100 13.5 6.75 6.75 0 000-13.5zM2.25 10.5a8.25 8.25 0 1114.59 5.28l4.69 4.69a.75.75 0 11-1.06 1.06l-4.69-4.69A8.25 8.25 0 012.25 10.5z" clipRule="evenodd" />
                            </svg>
                            <input
                                type="text"
                                value={searchQuery}
                                onChange={(e) => setSearchQuery(e.target.value)}
                                placeholder="Search contacts..."
                                className="w-full pl-10 pr-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                            />
                        </div>
                        <div className="flex gap-2">
                            <select
                                value={filter}
                                onChange={(e) => setFilter(e.target.value as FilterType)}
                                className="px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                            >
                                <option value="all">All Contacts</option>
                                <option value="favorites">Favorites</option>
                                <option value="quickflow">Uses QuickFlow</option>
                                <option value="google">From Google</option>
                                <option value="microsoft">From Microsoft</option>
                                <option value="manual">Manual</option>
                            </select>
                            <select
                                value={sortBy}
                                onChange={(e) => setSortBy(e.target.value as SortType)}
                                className="px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                            >
                                <option value="name">Name A-Z</option>
                                <option value="name_desc">Name Z-A</option>
                                <option value="recent">Recently Added</option>
                            </select>
                        </div>
                    </div>
                </div>

                {/* Contact List or Empty State */}
                {loading ? (
                    <div className="glass-card p-12 text-center">
                        <div className="w-10 h-10 mx-auto border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin" />
                        <p className="mt-4 text-slate-400">Loading contacts...</p>
                    </div>
                ) : filteredContacts.length > 0 ? (
                    <div className="space-y-2">
                        {filteredContacts.map((contact) => (
                            <ContactCard
                                key={contact.id}
                                contact={contact}
                                onEdit={setEditingContact}
                                onDelete={setDeletingContact}
                                onToggleFavorite={handleToggleFavorite}
                            />
                        ))}
                    </div>
                ) : contacts.length === 0 ? (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="glass-card p-12 text-center"
                    >
                        <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-purple-500/10 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-purple-400">
                                <path fillRule="evenodd" d="M7.5 6a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM3.751 20.105a8.25 8.25 0 0116.498 0 .75.75 0 01-.437.695A18.683 18.683 0 0112 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 01-.437-.695z" clipRule="evenodd" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-semibold text-white mb-2">No contacts yet</h3>
                        <p className="text-slate-400 mb-6 max-w-md mx-auto">
                            Import your contacts from Google or Microsoft, or add them manually.
                        </p>
                        <div className="flex flex-col sm:flex-row gap-3 justify-center">
                            <button
                                onClick={() => handleImport('google')}
                                disabled={importing}
                                className="px-6 py-3 bg-blue-600 hover:bg-blue-500 disabled:opacity-50 text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2"
                            >
                                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" />
                                    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" />
                                    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" />
                                    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" />
                                </svg>
                                Import from Google
                            </button>
                            <button
                                onClick={() => handleImport('microsoft')}
                                disabled={importing}
                                className="px-6 py-3 bg-orange-600 hover:bg-orange-500 disabled:opacity-50 text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2"
                            >
                                <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M0 0h11.377v11.372H0zm12.623 0H24v11.372H12.623zM0 12.623h11.377V24H0zm12.623 0H24V24H12.623z" />
                                </svg>
                                Import from Microsoft
                            </button>
                        </div>
                    </motion.div>
                ) : (
                    <div className="glass-card p-8 text-center">
                        <p className="text-slate-400">No contacts match your search.</p>
                    </div>
                )}
            </motion.div>

            {/* Back Button */}
            <motion.button
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                transition={{ delay: 0.2 }}
                onClick={() => navigate('/home')}
                className="fixed bottom-6 left-6 px-4 py-2 bg-slate-800/80 backdrop-blur border border-white/10 rounded-lg text-slate-300 hover:text-white hover:bg-slate-700 transition-colors flex items-center gap-2"
            >
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                    <path fillRule="evenodd" d="M7.72 12.53a.75.75 0 010-1.06l7.5-7.5a.75.75 0 111.06 1.06L9.31 12l6.97 6.97a.75.75 0 11-1.06 1.06l-7.5-7.5z" clipRule="evenodd" />
                </svg>
                Back
            </motion.button>
        </div>
    )
}
