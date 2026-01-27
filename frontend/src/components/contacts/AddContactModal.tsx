import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { createContact, type Contact } from '../../lib/api'

interface AddContactModalProps {
    isOpen: boolean
    onClose: () => void
    onContactAdded: (contact: Contact) => void
}

export default function AddContactModal({ isOpen, onClose, onContactAdded }: AddContactModalProps) {
    const [name, setName] = useState('')
    const [email, setEmail] = useState('')
    const [phone, setPhone] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault()
        setError('')

        if (!name.trim() || !email.trim()) {
            setError('Name and email are required')
            return
        }

        // Basic email validation
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
        if (!emailRegex.test(email.trim())) {
            setError('Please enter a valid email address')
            return
        }

        setLoading(true)

        try {
            const result = await createContact({
                name: name.trim(),
                email: email.trim(),
                phone: phone.trim() || undefined,
            })

            if (result.error) {
                setError(result.error)
            } else if (result.contact) {
                onContactAdded(result.contact)
                handleClose()
            }
        } catch {
            setError('Failed to create contact. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    const handleClose = () => {
        setName('')
        setEmail('')
        setPhone('')
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
                        <div className="bg-slate-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-md">
                            {/* Header */}
                            <div className="flex items-center justify-between p-6 border-b border-white/10">
                                <h2 className="text-xl font-semibold text-white">Add Contact</h2>
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
                            <form onSubmit={handleSubmit} className="p-6 space-y-4">
                                {error && (
                                    <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-400 text-sm">
                                        {error}
                                    </div>
                                )}

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Name <span className="text-red-400">*</span>
                                    </label>
                                    <input
                                        type="text"
                                        value={name}
                                        onChange={(e) => setName(e.target.value)}
                                        placeholder="John Doe"
                                        className="w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Email <span className="text-red-400">*</span>
                                    </label>
                                    <input
                                        type="email"
                                        value={email}
                                        onChange={(e) => setEmail(e.target.value)}
                                        placeholder="john@example.com"
                                        className="w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                                    />
                                </div>

                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">
                                        Phone <span className="text-slate-500">(optional)</span>
                                    </label>
                                    <input
                                        type="tel"
                                        value={phone}
                                        onChange={(e) => setPhone(e.target.value)}
                                        placeholder="+1 234 567 890"
                                        className="w-full px-4 py-2 bg-slate-800/50 border border-white/10 rounded-lg text-white placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-purple-500/50"
                                    />
                                </div>

                                {/* Actions */}
                                <div className="flex gap-3 pt-4">
                                    <button
                                        type="button"
                                        onClick={handleClose}
                                        className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-medium transition-colors"
                                    >
                                        Cancel
                                    </button>
                                    <button
                                        type="submit"
                                        disabled={loading}
                                        className="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-500 disabled:opacity-50 disabled:cursor-not-allowed text-white rounded-lg font-medium transition-colors flex items-center justify-center gap-2"
                                    >
                                        {loading ? (
                                            <>
                                                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                                                Adding...
                                            </>
                                        ) : (
                                            'Add Contact'
                                        )}
                                    </button>
                                </div>
                            </form>
                        </div>
                    </motion.div>
                </>
            )}
        </AnimatePresence>
    )
}
