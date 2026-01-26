import { motion } from 'framer-motion'
import UserAvatar from '../UserAvatar'
import type { Contact } from '../../lib/api'

interface ContactCardProps {
    contact: Contact
    onEdit: (contact: Contact) => void
    onDelete: (contact: Contact) => void
    onToggleFavorite: (contact: Contact) => void
}

export default function ContactCard({ contact, onEdit, onDelete, onToggleFavorite }: ContactCardProps) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="glass-card p-4 flex items-center gap-4 group hover:bg-white/5 transition-colors"
        >
            {/* Avatar */}
            <UserAvatar
                fullName={contact.name}
                photoUrl={contact.photo}
                size="md"
                showOnlineIndicator={contact.usesQuickFlow}
            />

            {/* Info */}
            <div className="flex-1 min-w-0">
                <div className="flex items-center gap-2">
                    <h3 className="text-white font-medium truncate">{contact.name}</h3>
                    {contact.usesQuickFlow && (
                        <span className="px-1.5 py-0.5 text-xs bg-emerald-500/20 text-emerald-400 rounded" title="Uses QuickFlow">
                            QF
                        </span>
                    )}
                </div>
                <p className="text-sm text-slate-400 truncate">{contact.email}</p>
                {contact.phone && (
                    <p className="text-xs text-slate-500 truncate">{contact.phone}</p>
                )}
            </div>

            {/* Source Badge */}
            <div className={`hidden sm:block px-2 py-1 text-xs rounded ${contact.source === 'google' ? 'bg-blue-500/20 text-blue-400' :
                    contact.source === 'microsoft' ? 'bg-orange-500/20 text-orange-400' :
                        'bg-slate-500/20 text-slate-400'
                }`}>
                {contact.source}
            </div>

            {/* Actions */}
            <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                {/* Favorite */}
                <button
                    onClick={() => onToggleFavorite(contact)}
                    className={`p-2 rounded-lg transition-colors ${contact.isFavorite
                            ? 'text-yellow-400 hover:bg-yellow-500/20'
                            : 'text-slate-400 hover:text-yellow-400 hover:bg-yellow-500/10'
                        }`}
                    title={contact.isFavorite ? 'Remove from favorites' : 'Add to favorites'}
                >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill={contact.isFavorite ? 'currentColor' : 'none'} stroke="currentColor" strokeWidth={1.5} className="w-5 h-5">
                        <path strokeLinecap="round" strokeLinejoin="round" d="M11.48 3.499a.562.562 0 011.04 0l2.125 5.111a.563.563 0 00.475.345l5.518.442c.499.04.701.663.321.988l-4.204 3.602a.563.563 0 00-.182.557l1.285 5.385a.562.562 0 01-.84.61l-4.725-2.885a.563.563 0 00-.586 0L6.982 20.54a.562.562 0 01-.84-.61l1.285-5.386a.562.562 0 00-.182-.557l-4.204-3.602a.563.563 0 01.321-.988l5.518-.442a.563.563 0 00.475-.345L11.48 3.5z" />
                    </svg>
                </button>

                {/* Edit */}
                <button
                    onClick={() => onEdit(contact)}
                    className="p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                    title="Edit contact"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                        <path d="M21.731 2.269a2.625 2.625 0 00-3.712 0l-1.157 1.157 3.712 3.712 1.157-1.157a2.625 2.625 0 000-3.712zM19.513 8.199l-3.712-3.712-12.15 12.15a5.25 5.25 0 00-1.32 2.214l-.8 2.685a.75.75 0 00.933.933l2.685-.8a5.25 5.25 0 002.214-1.32L19.513 8.2z" />
                    </svg>
                </button>

                {/* Delete */}
                <button
                    onClick={() => onDelete(contact)}
                    className="p-2 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                    title="Delete contact"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                        <path fillRule="evenodd" d="M16.5 4.478v.227a48.816 48.816 0 013.878.512.75.75 0 11-.256 1.478l-.209-.035-1.005 13.07a3 3 0 01-2.991 2.77H8.084a3 3 0 01-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 01-.256-1.478A48.567 48.567 0 017.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 013.369 0c1.603.051 2.815 1.387 2.815 2.951zm-6.136-1.452a51.196 51.196 0 013.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 00-6 0v-.113c0-.794.609-1.428 1.364-1.452zm-.355 5.945a.75.75 0 10-1.5.058l.347 9a.75.75 0 101.499-.058l-.346-9zm5.48.058a.75.75 0 10-1.498-.058l-.347 9a.75.75 0 001.5.058l.345-9z" clipRule="evenodd" />
                    </svg>
                </button>
            </div>
        </motion.div>
    )
}
