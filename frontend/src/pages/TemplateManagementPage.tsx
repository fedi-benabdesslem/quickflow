import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { getUserTemplates, deleteTemplate, updateTemplate } from '../lib/api'
import type { MeetingTemplate, UpdateMeetingTemplateRequest } from '../types/template'
import EditTemplateModal from '../components/templates/EditTemplateModal'

export default function TemplateManagementPage() {
    const [templates, setTemplates] = useState<MeetingTemplate[]>([])
    const [loading, setLoading] = useState(true)
    const [error, setError] = useState('')
    const [editingTemplate, setEditingTemplate] = useState<MeetingTemplate | null>(null)

    const { signOut } = useAuth()
    const navigate = useNavigate()

    useEffect(() => {
        loadTemplates()
    }, [])

    const loadTemplates = async () => {
        setLoading(true)
        const result = await getUserTemplates()
        if (result.success && result.templates) {
            setTemplates(result.templates)
        } else {
            setError(result.message || 'Failed to load templates')
        }
        setLoading(false)
    }

    const handleUseTemplate = (templateId: string) => {
        navigate('/minutes/structured', { state: { templateId } })
    }

    const handleDelete = async (id: string, name: string) => {
        if (!window.confirm(`Are you sure you want to delete template "${name}"?`)) return

        const result = await deleteTemplate(id)
        if (result.success) {
            setTemplates(prev => prev.filter(t => t.id !== id))
        } else {
            alert(result.message || 'Failed to delete template')
        }
    }

    const handleUpdate = async (id: string, request: UpdateMeetingTemplateRequest) => {
        const result = await updateTemplate(id, request)
        if (result.success) {
            await loadTemplates()
        } else {
            throw new Error(result.message || 'Failed to update template')
        }
    }

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    // Format date helper
    const formatDate = (dateString?: string) => {
        if (!dateString) return 'Never'
        return new Date(dateString).toLocaleDateString(undefined, {
            year: 'numeric', month: 'short', day: 'numeric'
        })
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <div className="flex items-center gap-4">
                    <button onClick={() => navigate('/home')} className="btn-secondary">
                        <span>←</span>
                        <span className="hidden sm:inline">Back</span>
                    </button>
                    <h1 className="text-xl font-bold text-white">My Templates</h1>
                </div>
                <button onClick={handleLogout} className="btn-logout">
                    <span className="hidden sm:inline">Logout</span>
                </button>
            </motion.header>

            {/* Error Message */}
            {error && (
                <div className="max-w-4xl mx-auto mb-6 p-4 bg-red-500/20 border border-red-500/50 rounded-lg text-red-200 flex justify-between items-center">
                    <span>{error}</span>
                    <button onClick={() => setError('')} className="hover:text-white">✕</button>
                </div>
            )}

            {/* Main Content */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                className="max-w-4xl mx-auto"
            >
                {/* Create New Button */}
                <div className="flex justify-between items-center mb-6">
                    <p className="text-slate-400">Manage your meeting templates</p>
                    <button
                        onClick={() => navigate('/minutes/structured')}
                        className="btn-primary text-sm px-4 py-2"
                    >
                        + Create New (from Form)
                    </button>
                </div>

                {loading ? (
                    <div className="justify-center flex py-20">
                        <div className="spinner spinner-large" />
                    </div>
                ) : templates.length === 0 ? (
                    <div className="glass-card p-12 text-center">
                        <div className="text-4xl mb-4 opacity-50 flex justify-center text-purple-400">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16">
                                <path d="M11.644 1.59a.75.75 0 0 1 .712 0l9.75 5.25a.75.75 0 0 1 0 1.32l-9.75 5.25a.75.75 0 0 1-.712 0l-9.75-5.25a.75.75 0 0 1 0-1.32l9.75-5.25Z" />
                                <path d="M3.265 10.602l7.668 4.129a2.25 2.25 0 0 0 2.134 0l7.668-4.13 1.37.739a.75.75 0 0 1 0 1.32l-9.75 5.25a.75.75 0 0 1-.712 0l-9.75-5.25a.75.75 0 0 1 0-1.32l1.37-.738Z" />
                                <path d="M10.933 19.231l-7.668-4.13-1.37.739a.75.75 0 0 0 0 1.32l9.75 5.25c.221.12.489.12.71 0l9.75-5.25a.75.75 0 0 0 0-1.32l-1.37-.738-7.668 4.13a2.25 2.25 0 0 1-2.134 0Z" />
                            </svg>
                        </div>
                        <h3 className="text-xl font-bold text-white mb-2">No Templates Yet</h3>
                        <p className="text-slate-400 mb-6 max-w-md mx-auto">
                            Create templates from your meeting structures to save time on recurring meetings.
                            Go to Structured Mode to create one.
                        </p>
                        <button
                            onClick={() => navigate('/minutes/structured')}
                            className="btn-primary"
                        >
                            Create First Template
                        </button>
                    </div>
                ) : (
                    <div className="grid gap-4">
                        {templates.map((template) => (
                            <motion.div
                                key={template.id}
                                layout
                                className="glass-card p-6 flex flex-col sm:flex-row gap-6 justify-between items-start sm:items-center"
                            >
                                <div>
                                    <div className="flex items-center gap-3 mb-1">
                                        <h3 className="text-lg font-semibold text-white">{template.name}</h3>
                                        <span className="bg-slate-700/50 text-slate-300 text-xs px-2 py-0.5 rounded-full border border-slate-600">
                                            Used {template.usageCount} times
                                        </span>
                                    </div>
                                    <p className="text-slate-400 text-sm mb-3">
                                        {template.description || 'No description'}
                                    </p>
                                    <div className="text-xs text-slate-500 flex gap-4">
                                        <span>Created: {formatDate(template.createdAt)}</span>
                                        <span>Last used: {formatDate(template.lastUsed)}</span>
                                    </div>
                                </div>
                                <div className="flex flex-wrap gap-2 w-full sm:w-auto min-w-0">
                                    <button
                                        onClick={() => handleUseTemplate(template.id)}
                                        className="btn-primary flex-1 sm:flex-none text-sm px-4 py-2 whitespace-nowrap"
                                    >
                                        Use
                                    </button>
                                    <button
                                        onClick={() => setEditingTemplate(template)}
                                        className="btn-secondary flex-1 sm:flex-none text-sm px-4 py-2 whitespace-nowrap"
                                    >
                                        Edit
                                    </button>
                                    <button
                                        onClick={() => handleDelete(template.id, template.name)}
                                        className="btn-secondary flex-1 sm:flex-none text-sm px-4 py-2 border-red-500/30 text-red-400 hover:bg-red-500/10 whitespace-nowrap"
                                    >
                                        Delete
                                    </button>
                                </div>
                            </motion.div>
                        ))}
                    </div>
                )}
            </motion.div>

            {editingTemplate && (
                <EditTemplateModal
                    isOpen={!!editingTemplate}
                    onClose={() => setEditingTemplate(null)}
                    template={editingTemplate}
                    onUpdate={handleUpdate}
                />
            )}
        </div>
    )
}
