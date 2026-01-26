import { useState, useEffect, useCallback } from 'react'
import { motion } from 'framer-motion'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { useSidebar } from '../contexts/SidebarContext'
import UserAvatar from '../components/UserAvatar'
import UserProfileSidebar from '../components/UserProfileSidebar'
import GroupCard from '../components/groups/GroupCard'
import CreateGroupModal from '../components/groups/CreateGroupModal'
import DeleteConfirmModal from '../components/contacts/DeleteConfirmModal'
import {
    getGroups,
    getGroup,
    deleteGroup,
    type Group,
    type GroupsResponse,
    type Contact,
} from '../lib/api'

export default function GroupsPage() {
    const { user } = useAuth()
    const { openSidebar } = useSidebar()
    const navigate = useNavigate()

    // State
    const [groups, setGroups] = useState<Group[]>([])
    const [loading, setLoading] = useState(true)

    // Modal state
    const [showCreateModal, setShowCreateModal] = useState(false)
    const [deletingGroup, setDeletingGroup] = useState<Group | null>(null)
    const [viewingGroup, setViewingGroup] = useState<{ group: Group; members: Contact[] } | null>(null)

    // Load groups
    const loadGroups = useCallback(async () => {
        setLoading(true)
        try {
            const response: GroupsResponse = await getGroups()
            setGroups(response.groups || [])
        } catch (error) {
            console.error('Failed to load groups:', error)
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        loadGroups()
    }, [loadGroups])

    // View group details
    const handleViewGroup = async (group: Group) => {
        try {
            const result = await getGroup(group.id)
            if (result.group) {
                setViewingGroup({ group: result.group, members: result.members || [] })
            }
        } catch (error) {
            console.error('Failed to load group details:', error)
        }
    }

    // Delete group
    const handleDelete = async () => {
        if (!deletingGroup) return

        const result = await deleteGroup(deletingGroup.id)
        if (result.success) {
            setGroups(prev => prev.filter(g => g.id !== deletingGroup.id))
        } else {
            alert(result.error || 'Failed to delete group')
        }
        setDeletingGroup(null)
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Profile Sidebar */}
            <UserProfileSidebar />

            {/* Modals */}
            <CreateGroupModal
                isOpen={showCreateModal}
                onClose={() => setShowCreateModal(false)}
                onGroupCreated={(group) => {
                    setGroups(prev => [group, ...prev])
                }}
            />

            <DeleteConfirmModal
                isOpen={!!deletingGroup}
                title="Delete Group"
                message="Are you sure you want to delete this group?"
                itemName={deletingGroup?.name || ''}
                onConfirm={handleDelete}
                onCancel={() => setDeletingGroup(null)}
            />

            {/* Group Details Modal */}
            {viewingGroup && (
                <>
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        onClick={() => setViewingGroup(null)}
                        className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50"
                    />
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95, y: 20 }}
                        animate={{ opacity: 1, scale: 1, y: 0 }}
                        className="fixed inset-0 z-50 flex items-center justify-center p-4"
                    >
                        <div className="bg-slate-900 border border-white/10 rounded-xl shadow-2xl w-full max-w-md max-h-[80vh] overflow-hidden flex flex-col">
                            <div className="flex items-center justify-between p-6 border-b border-white/10">
                                <h2 className="text-xl font-semibold text-white">{viewingGroup.group.name}</h2>
                                <button
                                    onClick={() => setViewingGroup(null)}
                                    className="p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                                >
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                        <path fillRule="evenodd" d="M5.47 5.47a.75.75 0 011.06 0L12 10.94l5.47-5.47a.75.75 0 111.06 1.06L13.06 12l5.47 5.47a.75.75 0 11-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 01-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 010-1.06z" clipRule="evenodd" />
                                    </svg>
                                </button>
                            </div>

                            <div className="flex-1 overflow-y-auto p-6">
                                {viewingGroup.group.description && (
                                    <p className="text-slate-400 text-sm mb-4">{viewingGroup.group.description}</p>
                                )}

                                <h3 className="text-sm font-medium text-slate-300 mb-3">
                                    Members ({viewingGroup.members.length})
                                </h3>

                                {viewingGroup.members.length > 0 ? (
                                    <div className="space-y-2">
                                        {viewingGroup.members.map(member => (
                                            <div key={member.id} className="flex items-center gap-3 p-2 rounded-lg hover:bg-white/5">
                                                <UserAvatar
                                                    fullName={member.name}
                                                    photoUrl={member.photo}
                                                    size="sm"
                                                    showOnlineIndicator={member.usesQuickFlow}
                                                />
                                                <div className="flex-1 min-w-0">
                                                    <p className="text-white text-sm truncate">{member.name}</p>
                                                    <p className="text-slate-400 text-xs truncate">{member.email}</p>
                                                </div>
                                                {member.usesQuickFlow && (
                                                    <span className="px-1.5 py-0.5 text-xs bg-emerald-500/20 text-emerald-400 rounded">
                                                        QF
                                                    </span>
                                                )}
                                            </div>
                                        ))}
                                    </div>
                                ) : (
                                    <p className="text-slate-500 text-sm">No members in this group yet.</p>
                                )}
                            </div>

                            <div className="flex gap-3 p-6 border-t border-white/10">
                                <button
                                    onClick={() => setViewingGroup(null)}
                                    className="flex-1 px-4 py-2 bg-slate-700 hover:bg-slate-600 text-white rounded-lg font-medium transition-colors"
                                >
                                    Close
                                </button>
                            </div>
                        </div>
                    </motion.div>
                </>
            )}

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
                            My Groups
                        </h1>
                        <p className="text-slate-400">
                            {groups.length} groups • Organize your contacts
                        </p>
                    </div>
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-lg font-medium transition-colors flex items-center gap-2"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                            <path fillRule="evenodd" d="M12 3.75a.75.75 0 01.75.75v6.75h6.75a.75.75 0 010 1.5h-6.75v6.75a.75.75 0 01-1.5 0v-6.75H4.5a.75.75 0 010-1.5h6.75V4.5a.75.75 0 01.75-.75z" clipRule="evenodd" />
                        </svg>
                        New Group
                    </button>
                </div>

                {/* Group List or Empty State */}
                {loading ? (
                    <div className="glass-card p-12 text-center">
                        <div className="w-10 h-10 mx-auto border-2 border-purple-500/30 border-t-purple-500 rounded-full animate-spin" />
                        <p className="mt-4 text-slate-400">Loading groups...</p>
                    </div>
                ) : groups.length > 0 ? (
                    <div className="grid gap-4 md:grid-cols-2">
                        {groups.map((group) => (
                            <GroupCard
                                key={group.id}
                                group={group}
                                onView={handleViewGroup}
                                onEdit={() => {/* TODO: Edit group */ }}
                                onDelete={setDeletingGroup}
                            />
                        ))}
                    </div>
                ) : (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="glass-card p-12 text-center"
                    >
                        <div className="w-20 h-20 mx-auto mb-6 rounded-full bg-purple-500/10 flex items-center justify-center">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-10 h-10 text-purple-400">
                                <path fillRule="evenodd" d="M8.25 6.75a3.75 3.75 0 117.5 0 3.75 3.75 0 01-7.5 0zM15.75 9.75a3 3 0 116 0 3 3 0 01-6 0zM2.25 9.75a3 3 0 116 0 3 3 0 01-6 0zM6.31 15.117A6.745 6.745 0 0112 12a6.745 6.745 0 016.709 7.498.75.75 0 01-.372.568A12.696 12.696 0 0112 21.75c-2.305 0-4.47-.612-6.337-1.684a.75.75 0 01-.372-.568 6.787 6.787 0 011.019-4.38z" clipRule="evenodd" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-semibold text-white mb-2">No groups yet</h3>
                        <p className="text-slate-400 mb-6 max-w-md mx-auto">
                            Create groups to organize your contacts by team, project, or any other criteria.
                        </p>
                        <button
                            onClick={() => setShowCreateModal(true)}
                            className="px-6 py-3 bg-purple-600 hover:bg-purple-500 text-white rounded-lg font-medium transition-colors"
                        >
                            Create Your First Group
                        </button>
                    </motion.div>
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
