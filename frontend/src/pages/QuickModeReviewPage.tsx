import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import TechSupportButton from '../components/TechSupportButton'

import { generateFromExtracted, getContacts, type Contact } from '../lib/api'
import type { ExtractedMeetingData, ExtractedDecision, ExtractedActionItem, ExtractedParticipant } from '../types'
import ContactAutocomplete from '../components/contacts/ContactAutocomplete'

interface LocationState {
    extractedData: ExtractedMeetingData
    originalContent: string
    originalDate?: string
    originalTime?: string
}

export default function QuickModeReviewPage() {
    const navigate = useNavigate()
    const location = useLocation()


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

    // Match participant names to contacts when data loads
    useEffect(() => {
        const loadAndMatchContacts = async () => {
            if (!state?.extractedData) {
                navigate('/minutes/quick')
                return
            }

            // Set the data first
            setData(state.extractedData)

            // Then try to match participants to contacts
            try {
                const result = await getContacts()
                const contacts = result.contacts || []
                if (contacts.length > 0 && state.extractedData.participants.length > 0) {
                    // Match extracted participant names to contacts
                    const matchedParticipants: ExtractedParticipant[] = state.extractedData.participants.map(p => {
                        const name = typeof p === 'string' ? p : p.name
                        const existingEmail = typeof p === 'object' ? p.email : undefined

                        // If already has email, keep it
                        if (existingEmail) {
                            return { name, email: existingEmail }
                        }

                        // Try to find a matching contact (case-insensitive)
                        const matchedContact = contacts.find((c: Contact) =>
                            c.name.toLowerCase() === name.toLowerCase() ||
                            c.name.toLowerCase().includes(name.toLowerCase()) ||
                            name.toLowerCase().includes(c.name.toLowerCase())
                        )

                        if (matchedContact) {
                            return { name: matchedContact.name, email: matchedContact.email }
                        }

                        return { name, email: undefined }
                    })

                    setData(prev => ({ ...prev, participants: matchedParticipants }))
                }
            } catch (err) {
                console.log('Could not match contacts:', err)
                // Continue without matching - not critical
            }
        }

        loadAndMatchContacts()
    }, [state, navigate])



    // Participant handlers
    const addParticipant = () => {
        if (newParticipant.trim()) {
            setData(prev => ({
                ...prev,
                participants: [...prev.participants, { name: newParticipant.trim(), email: undefined }],
            }))
            setNewParticipant('')
        }
    }

    const handleContactSelect = (contact: Contact) => {
        // Check if already added
        const exists = data.participants.some(p => {
            const name = typeof p === 'string' ? p : p.name
            return name.toLowerCase() === contact.name.toLowerCase()
        })

        if (!exists) {
            setData(prev => ({
                ...prev,
                participants: [...prev.participants, { name: contact.name, email: contact.email }],
            }))
        }
        setNewParticipant('')
    }

    const removeParticipant = (index: number) => {
        setData(prev => ({
            ...prev,
            participants: prev.participants.filter((_, i) => i !== index),
        }))
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
                <TechSupportButton />
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
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-blue-400">
                            <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0 0 16.5 9h-1.875a1.875 1.875 0 0 1-1.875-1.875V5.25A3.75 3.75 0 0 0 9 1.5H5.625Z" clipRule="evenodd" />
                        </svg> Meeting Information
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
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">Location</label>
                            <input
                                type="text"
                                value={data.location || ''}
                                onChange={(e) => setData(prev => ({ ...prev, location: e.target.value }))}
                                className="input-nebula"
                                placeholder="Enter location"
                            />
                        </div>
                    </div>
                </section>

                {/* Participants */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-green-400">
                            <path d="M4.5 6.375a4.125 4.125 0 1 1 8.25 0 4.125 4.125 0 0 1-8.25 0ZM14.25 8.625a3.375 3.375 0 1 1 6.75 0 3.375 3.375 0 0 1-6.75 0ZM1.5 19.125a7.125 7.125 0 0 1 14.25 0v.003l-.001.119a.75.75 0 0 1-.363.63 13.067 13.067 0 0 1-6.761 1.873c-2.472 0-4.786-.684-6.76-1.873a.75.75 0 0 1-.364-.63l-.001-.122ZM17.25 19.128l-.001.144a2.25 2.25 0 0 1-.233.96 10.088 10.088 0 0 0 5.06-1.01.75.75 0 0 0 .42-.643 4.875 4.875 0 0 0-6.957-4.611 8.586 8.586 0 0 1 1.71 5.157v.003Z" />
                        </svg> Participants
                    </h2>
                    <div className="flex flex-wrap gap-2 mb-3">
                        {data.participants.map((p, i) => {
                            const name = typeof p === 'string' ? p : p.name
                            const email = typeof p === 'object' ? p.email : undefined
                            return (
                                <span
                                    key={i}
                                    className={`inline-flex items-center gap-1 px-3 py-1 rounded-full text-sm ${email ? 'bg-green-700/30 border border-green-500/30' : 'bg-slate-700/50'}`}
                                    title={email || 'No email linked'}
                                >
                                    {email && (
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-3.5 h-3.5 text-green-400">
                                            <path d="M10 8a3 3 0 100-6 3 3 0 000 6zM3.465 14.493a1.23 1.23 0 00.41 1.412A9.957 9.957 0 0010 18c2.31 0 4.438-.784 6.131-2.1.43-.333.604-.903.408-1.41a7.002 7.002 0 00-13.074.003z" />
                                        </svg>
                                    )}
                                    {name}
                                    <button
                                        onClick={() => removeParticipant(i)}
                                        className="text-slate-400 hover:text-red-400 ml-1"
                                    >
                                        ×
                                    </button>
                                </span>
                            )
                        })}
                    </div>
                    <div className="flex gap-2">
                        <div className="flex-1">
                            <ContactAutocomplete
                                value={newParticipant}
                                onChange={setNewParticipant}
                                onSelect={handleContactSelect}
                                onEnterManual={addParticipant}
                                placeholder="Search contacts or type name..."
                            />
                        </div>
                        <button onClick={addParticipant} className="btn-secondary px-4">Add</button>
                    </div>
                </section>

                {/* Discussion Points */}
                <section className="mb-8">
                    <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-yellow-400">
                            <path fillRule="evenodd" d="M4.848 2.771A49.144 49.144 0 0 1 12 2.25c2.43 0 4.817.178 7.152.52 1.978.292 3.348 2.024 3.348 3.97v6.02c0 1.946-1.37 3.678-3.348 3.97a48.901 48.901 0 0 1-3.476.383.39.39 0 0 0-.297.17l-2.755 4.133a.75.75 0 0 1-1.248 0l-2.755-4.133a.39.39 0 0 0-.297-.17 48.9 48.9 0 0 1-3.476-.384c-1.978-.29-3.348-2.024-3.348-3.97V6.741c0-1.946 1.37-3.678 3.348-3.97Z" clipRule="evenodd" />
                        </svg> Discussion Points
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
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-green-400">
                            <path fillRule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12Zm13.36-1.814a.75.75 0 1 0-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 0 0-1.06 1.06l2.25 2.25a.75.75 0 0 0 1.14-.094l3.75-5.25Z" clipRule="evenodd" />
                        </svg> Decisions
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
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-purple-400">
                            <path fillRule="evenodd" d="M7.502 6h7.128A3.375 3.375 0 0 1 18 9.375v9.375a3 3 0 0 0 3-3V6.108c0-1.505-1.125-2.811-2.664-2.94a48.972 48.972 0 0 0-.673-.05A3 3 0 0 0 15 1.5h-1.5a3 3 0 0 0-2.663 1.618c-.225.015-.45.032-.673.05C8.662 3.295 7.554 4.542 7.502 6ZM13.5 3A1.5 1.5 0 0 0 12 4.5h4.5A1.5 1.5 0 0 0 15 3h-1.5Z" clipRule="evenodd" />
                            <path fillRule="evenodd" d="M3 9.375C3 8.339 3.84 7.5 4.875 7.5h9.75c1.036 0 1.875.84 1.875 1.875v11.25c0 1.035-.84 1.875-1.875 1.875h-9.75A1.875 1.875 0 0 1 3 20.625V9.375Zm9.586 4.594a.75.75 0 0 0-1.172-.938l-2.476 3.096-.908-.907a.75.75 0 0 0-1.06 1.06l1.5 1.5a.75.75 0 0 0 1.116-.062l3-3.75Z" clipRule="evenodd" />
                        </svg> Action Items
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
