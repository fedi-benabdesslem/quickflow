import { useState, useEffect, KeyboardEvent } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { generateFromExtracted } from '../lib/api'
import type { ExtractedMeetingData, ExtractedDecision, ExtractedActionItem } from '../types'

interface LocationState {
    extractedData: ExtractedMeetingData
    originalContent: string
    originalDate?: string
    originalTime?: string
}

export default function QuickModeReviewPage() {
    const navigate = useNavigate()
    const location = useLocation()
    const { signOut } = useAuth()

    const state = location.state as LocationState | null

    const [data, setData] = useState<ExtractedMeetingData>({
        meetingTitle: '',
        date: '',
        time: '',
        participants: [],
        discussionPoints: [],
        decisions: [],
        actionItems: [],
        confidence: 'medium',
    })
    const [newParticipant, setNewParticipant] = useState('')
    const [newDiscussionPoint, setNewDiscussionPoint] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')

    useEffect(() => {
        if (state?.extractedData) {
            setData(state.extractedData)
        } else {
            // No data, redirect back
            navigate('/minutes/quick')
        }
    }, [state, navigate])

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    // Participant handlers
    const addParticipant = () => {
        if (newParticipant.trim()) {
            setData(prev => ({
                ...prev,
                participants: [...prev.participants, newParticipant.trim()],
            }))
            setNewParticipant('')
        }
    }

    const removeParticipant = (index: number) => {
        setData(prev => ({
            ...prev,
            participants: prev.participants.filter((_, i) => i !== index),
        }))
    }

    const handleParticipantKeyDown = (e: KeyboardEvent<HTMLInputElement>) => {
        if (e.key === 'Enter') {
            e.preventDefault()
            addParticipant()
        }
    }

    // Discussion point handlers
    const addDiscussionPoint = () => {
        if (newDiscussionPoint.trim()) {
            setData(prev => ({
                ...prev,
                discussionPoints: [...prev.discussionPoints, newDiscussionPoint.trim()],
            }))
            setNewDiscussionPoint('')
        }
    }

    const removeDiscussionPoint = (index: number) => {
        setData(prev => ({
            ...prev,
            discussionPoints: prev.discussionPoints.filter((_, i) => i !== index),
        }))
    }

    const updateDiscussionPoint = (index: number, value: string) => {
        setData(prev => ({
            ...prev,
            discussionPoints: prev.discussionPoints.map((p, i) => (i === index ? value : p)),
        }))
    }

    // Decision handlers
    const addDecision = () => {
        setData(prev => ({
            ...prev,
            decisions: [...prev.decisions, { statement: '', status: 'Approved' }],
        }))
    }

    const updateDecision = (index: number, field: keyof ExtractedDecision, value: string) => {
        setData(prev => ({
            ...prev,
            decisions: prev.decisions.map((d, i) =>
                i === index ? { ...d, [field]: value } : d
            ),
        }))
    }

    const removeDecision = (index: number) => {
        setData(prev => ({
            ...prev,
            decisions: prev.decisions.filter((_, i) => i !== index),
        }))
    }

    // Action item handlers
    const addActionItem = () => {
        setData(prev => ({
            ...prev,
            actionItems: [...prev.actionItems, { task: '', owner: '', deadline: '' }],
        }))
    }

    const updateActionItem = (index: number, field: keyof ExtractedActionItem, value: string) => {
        setData(prev => ({
            ...prev,
            actionItems: prev.actionItems.map((a, i) =>
                i === index ? { ...a, [field]: value } : a
            ),
        }))
    }

    const removeActionItem = (index: number) => {
        setData(prev => ({
            ...prev,
            actionItems: prev.actionItems.filter((_, i) => i !== index),
        }))
    }

    const handleGenerate = async () => {
        if (loading) return
        setLoading(true)
        setError('')

        try {
            const result = await generateFromExtracted(data, 'Formal', 'Standard')

            if (result.status === 'success' && result.content) {
                navigate('/minutes/editor', {
                    state: {
                        content: result.content,
                        sourceData: data,
                        mode: 'quick',
                    },
                })
            } else {
                setError(result.message || 'Generation failed. Please try again.')
            }
        } catch (err) {
            console.error('Generation error:', err)
            setError('AI service temporarily unavailable. Please try again later.')
        } finally {
            setLoading(false)
        }
    }

    const getConfidenceColor = () => {
        switch (data.confidence) {
            case 'high': return 'text-green-400 bg-green-500/10 border-green-500/30'
            case 'medium': return 'text-yellow-400 bg-yellow-500/10 border-yellow-500/30'
            case 'low': return 'text-red-400 bg-red-500/10 border-red-500/30'
            default: return 'text-slate-400 bg-slate-500/10 border-slate-500/30'
        }
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Loading Overlay */}
            {loading && (
                <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center">
                    <motion.div
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="glass-card p-8 text-center max-w-md"
                    >
                        <div className="spinner spinner-large mx-auto mb-4" />
                        <h3 className="text-xl font-semibold text-white mb-2">
                            Generating Meeting Minutes...
                        </h3>
                        <p className="text-slate-400 text-sm">
                            AI is creating professional minutes. This may take 15-30 seconds.
                        </p>
                    </motion.div>
                </div>
            )}

            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <button onClick={() => navigate('/minutes/quick')} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back to Notes</span>
                </button>
                <button onClick={handleLogout} className="btn-logout">
                    <span className="hidden sm:inline">Logout</span>
                </button>
            </motion.header>

            {/* Breadcrumb */}
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="max-w-4xl mx-auto mb-4"
            >
                <nav className="text-sm text-slate-500">
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/home')}>Home</span>
                    <span className="mx-2">›</span>
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/minutes/new')}>Create Minutes</span>
                    <span className="mx-2">›</span>
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/minutes/quick')}>Quick Mode</span>
                    <span className="mx-2">›</span>
                    <span className="text-blue-400">Review Extracted Data</span>
                </nav>
            </motion.div>

            {/* Main Content */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="glass-card max-w-4xl mx-auto p-6 sm:p-8"
            >
                {/* Title */}
                <div className="text-center mb-8">
                    <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
                        Review Extracted Information
                    </h1>
                    <p className="text-slate-400 mt-2">
                        AI has extracted structure from your notes. Review and edit before generating final minutes.
                    </p>
                </div>

                {/* Confidence Indicator */}
                <div className={`border rounded-lg p-4 mb-6 ${getConfidenceColor()}`}>
                    <div className="flex items-center gap-2">
                        <span className="font-medium">AI Confidence:</span>
                        <span className="capitalize">{data.confidence}</span>
                        {data.confidence === 'low' && (
                            <span className="text-sm ml-2">
                                — Please review carefully and make corrections
                            </span>
                        )}
                    </div>
                </div>

                {/* Error Message */}
                {error && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="message-error mb-6"
                    >
                        {error}
                        <button onClick={() => setError('')} className="ml-2 text-red-300 hover:text-red-100">✕</button>
                    </motion.div>
                )}

                {/* Meeting Information */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <span className="text-blue-400">📋</span> Meeting Information
                    </h2>
                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                        <div className="sm:col-span-3">
                            <label className="block text-sm font-medium text-slate-300 mb-2">Meeting Title</label>
                            <input
                                type="text"
                                value={data.meetingTitle || ''}
                                onChange={(e) => setData(prev => ({ ...prev, meetingTitle: e.target.value }))}
                                className="input-nebula"
                                placeholder="Enter meeting title"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">Date</label>
                            <input
                                type="date"
                                value={data.date || ''}
                                onChange={(e) => setData(prev => ({ ...prev, date: e.target.value }))}
                                className="input-nebula"
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">Time</label>
                            <input
                                type="time"
                                value={data.time || ''}
                                onChange={(e) => setData(prev => ({ ...prev, time: e.target.value }))}
                                className="input-nebula"
                            />
                        </div>
                    </div>
                </section>

                {/* Participants */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <span className="text-green-400">👥</span> Participants
                    </h2>
                    <div className="flex flex-wrap gap-2 mb-3">
                        {data.participants.map((p, i) => (
                            <span
                                key={i}
                                className="inline-flex items-center gap-1 px-3 py-1 bg-slate-700/50 rounded-full text-sm"
                            >
                                {p}
                                <button
                                    onClick={() => removeParticipant(i)}
                                    className="text-slate-400 hover:text-red-400 ml-1"
                                >
                                    ×
                                </button>
                            </span>
                        ))}
                    </div>
                    <div className="flex gap-2">
                        <input
                            type="text"
                            value={newParticipant}
                            onChange={(e) => setNewParticipant(e.target.value)}
                            onKeyDown={handleParticipantKeyDown}
                            className="input-nebula flex-1"
                            placeholder="Add participant name..."
                        />
                        <button onClick={addParticipant} className="btn-secondary px-4">Add</button>
                    </div>
                </section>

                {/* Discussion Points */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <span className="text-yellow-400">💬</span> Discussion Points
                    </h2>
                    <div className="space-y-3">
                        {data.discussionPoints.map((point, i) => (
                            <div key={i} className="flex gap-2">
                                <input
                                    type="text"
                                    value={point}
                                    onChange={(e) => updateDiscussionPoint(i, e.target.value)}
                                    className="input-nebula flex-1"
                                />
                                <button
                                    onClick={() => removeDiscussionPoint(i)}
                                    className="btn-secondary text-red-400 hover:text-red-300 px-3"
                                >
                                    ×
                                </button>
                            </div>
                        ))}
                    </div>
                    <div className="flex gap-2 mt-3">
                        <input
                            type="text"
                            value={newDiscussionPoint}
                            onChange={(e) => setNewDiscussionPoint(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addDiscussionPoint())}
                            className="input-nebula flex-1"
                            placeholder="Add discussion point..."
                        />
                        <button onClick={addDiscussionPoint} className="btn-secondary px-4">Add</button>
                    </div>
                </section>

                {/* Decisions */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <span className="text-purple-400">⚖️</span> Decisions
                    </h2>
                    <div className="space-y-4">
                        {data.decisions.map((d, i) => (
                            <div key={i} className="border border-slate-700/50 rounded-lg p-4 bg-slate-900/30">
                                <div className="flex gap-2 mb-2">
                                    <input
                                        type="text"
                                        value={d.statement}
                                        onChange={(e) => updateDecision(i, 'statement', e.target.value)}
                                        className="input-nebula flex-1"
                                        placeholder="Decision statement"
                                    />
                                    <button
                                        onClick={() => removeDecision(i)}
                                        className="text-red-400 hover:text-red-300 px-2"
                                    >
                                        ×
                                    </button>
                                </div>
                                <select
                                    value={d.status}
                                    onChange={(e) => updateDecision(i, 'status', e.target.value)}
                                    className="input-nebula w-48"
                                >
                                    <option value="Approved">Approved</option>
                                    <option value="Rejected">Rejected</option>
                                    <option value="Deferred">Deferred</option>
                                    <option value="No Decision">No Decision</option>
                                </select>
                            </div>
                        ))}
                    </div>
                    <button onClick={addDecision} className="btn-secondary mt-3">
                        + Add Decision
                    </button>
                </section>

                {/* Action Items */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <span className="text-orange-400">✅</span> Action Items
                    </h2>
                    <div className="space-y-4">
                        {data.actionItems.map((a, i) => (
                            <div key={i} className="border border-slate-700/50 rounded-lg p-4 bg-slate-900/30">
                                <div className="flex gap-2 mb-2">
                                    <input
                                        type="text"
                                        value={a.task}
                                        onChange={(e) => updateActionItem(i, 'task', e.target.value)}
                                        className="input-nebula flex-1"
                                        placeholder="Task description"
                                    />
                                    <button
                                        onClick={() => removeActionItem(i)}
                                        className="text-red-400 hover:text-red-300 px-2"
                                    >
                                        ×
                                    </button>
                                </div>
                                <div className="grid grid-cols-2 gap-2">
                                    <input
                                        type="text"
                                        value={a.owner || ''}
                                        onChange={(e) => updateActionItem(i, 'owner', e.target.value)}
                                        className="input-nebula"
                                        placeholder="Owner"
                                    />
                                    <input
                                        type="date"
                                        value={a.deadline || ''}
                                        onChange={(e) => updateActionItem(i, 'deadline', e.target.value)}
                                        className="input-nebula"
                                        placeholder="Deadline"
                                    />
                                </div>
                            </div>
                        ))}
                    </div>
                    <button onClick={addActionItem} className="btn-secondary mt-3">
                        + Add Action Item
                    </button>
                </section>

                {/* Action Buttons */}
                <div className="flex gap-4 mt-8 pt-6 border-t border-slate-700/50">
                    <button
                        onClick={() => navigate('/minutes/quick')}
                        className="btn-secondary flex-1"
                        disabled={loading}
                    >
                        Back to Notes
                    </button>
                    <button
                        onClick={handleGenerate}
                        className="btn-primary flex-1"
                        disabled={loading}
                    >
                        {loading ? (
                            <span className="flex items-center justify-center gap-2">
                                <span className="spinner" />
                                Generating...
                            </span>
                        ) : (
                            'Generate Minutes →'
                        )}
                    </button>
                </div>
            </motion.div>
        </div>
    )
}
