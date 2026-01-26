import { motion } from 'framer-motion'
import type { Group } from '../../lib/api'

interface GroupCardProps {
    group: Group
    onView: (group: Group) => void
    onEdit: (group: Group) => void
    onDelete: (group: Group) => void
}

export default function GroupCard({ group, onView, onEdit, onDelete }: GroupCardProps) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            className="glass-card p-4 group hover:bg-white/5 transition-colors cursor-pointer"
            onClick={() => onView(group)}
        >
            <div className="flex items-start justify-between">
                <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                        <div className="w-10 h-10 rounded-lg bg-purple-500/20 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-purple-400">
                                <path fillRule="evenodd" d="M8.25 6.75a3.75 3.75 0 117.5 0 3.75 3.75 0 01-7.5 0zM15.75 9.75a3 3 0 116 0 3 3 0 01-6 0zM2.25 9.75a3 3 0 116 0 3 3 0 01-6 0zM6.31 15.117A6.745 6.745 0 0112 12a6.745 6.745 0 016.709 7.498.75.75 0 01-.372.568A12.696 12.696 0 0112 21.75c-2.305 0-4.47-.612-6.337-1.684a.75.75 0 01-.372-.568 6.787 6.787 0 011.019-4.38z" clipRule="evenodd" />
                            </svg>
                        </div>
                        <div>
                            <h3 className="text-white font-medium">{group.name}</h3>
                            <p className="text-sm text-slate-400">
                                {group.memberCount || group.memberIds?.length || 0} members
                                {group.quickflowMemberCount && group.quickflowMemberCount > 0 && (
                                    <span className="ml-2 text-emerald-400">
                                        • {group.quickflowMemberCount} on QuickFlow
                                    </span>
                                )}
                            </p>
                        </div>
                    </div>

                    {group.description && (
                        <p className="text-sm text-slate-500 mt-2 line-clamp-2">{group.description}</p>
                    )}

                    {/* Member Preview */}
                    {group.memberPreview && group.memberPreview.length > 0 && (
                        <div className="flex items-center gap-2 mt-3">
                            <div className="flex -space-x-2">
                                {group.memberPreview.slice(0, 3).map((member) => (
                                    <div
                                        key={member.id}
                                        className="w-6 h-6 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-xs text-white font-medium ring-2 ring-slate-900"
                                        title={member.name}
                                    >
                                        {member.name.charAt(0).toUpperCase()}
                                    </div>
                                ))}
                            </div>
                            {(group.memberCount || group.memberIds?.length || 0) > 3 && (
                                <span className="text-xs text-slate-500">
                                    +{(group.memberCount || group.memberIds?.length || 0) - 3} more
                                </span>
                            )}
                        </div>
                    )}
                </div>

                {/* Actions */}
                <div className="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity">
                    <button
                        onClick={(e) => { e.stopPropagation(); onEdit(group); }}
                        className="p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                        title="Edit group"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                            <path d="M21.731 2.269a2.625 2.625 0 00-3.712 0l-1.157 1.157 3.712 3.712 1.157-1.157a2.625 2.625 0 000-3.712zM19.513 8.199l-3.712-3.712-12.15 12.15a5.25 5.25 0 00-1.32 2.214l-.8 2.685a.75.75 0 00.933.933l2.685-.8a5.25 5.25 0 002.214-1.32L19.513 8.2z" />
                        </svg>
                    </button>
                    <button
                        onClick={(e) => { e.stopPropagation(); onDelete(group); }}
                        className="p-2 text-slate-400 hover:text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                        title="Delete group"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                            <path fillRule="evenodd" d="M16.5 4.478v.227a48.816 48.816 0 013.878.512.75.75 0 11-.256 1.478l-.209-.035-1.005 13.07a3 3 0 01-2.991 2.77H8.084a3 3 0 01-2.991-2.77L4.087 6.66l-.209.035a.75.75 0 01-.256-1.478A48.567 48.567 0 017.5 4.705v-.227c0-1.564 1.213-2.9 2.816-2.951a52.662 52.662 0 013.369 0c1.603.051 2.815 1.387 2.815 2.951zm-6.136-1.452a51.196 51.196 0 013.273 0C14.39 3.05 15 3.684 15 4.478v.113a49.488 49.488 0 00-6 0v-.113c0-.794.609-1.428 1.364-1.452zm-.355 5.945a.75.75 0 10-1.5.058l.347 9a.75.75 0 101.499-.058l-.346-9zm5.48.058a.75.75 0 10-1.498-.058l-.347 9a.75.75 0 001.5.058l.345-9z" clipRule="evenodd" />
                        </svg>
                    </button>
                </div>
            </div>
        </motion.div>
    )
}
