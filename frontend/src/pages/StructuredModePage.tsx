import { useState, KeyboardEvent, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { generateMinutes } from '../lib/api'
import CollapsibleSection from '../components/minutes/CollapsibleSection'
import ParticipantTag from '../components/minutes/ParticipantTag'
import FormProgress from '../components/minutes/FormProgress'
import type {
    StructuredModeData,
    Participant,
    AgendaItem,
    Decision,
    ActionItem,
    OutputPreferences
} from '../types'

const LOCATION_OPTIONS = ['Office Room 1', 'Zoom', 'Google Meet', 'Microsoft Teams', 'Other']
const OBJECTIVE_OPTIONS: AgendaItem['objective'][] = ['Discussion', 'Decision', 'Review', 'Information']
const DECISION_STATUS_OPTIONS: Decision['status'][] = ['Approved', 'Rejected', 'Deferred', 'No Decision']
const PRIORITY_OPTIONS: ActionItem['priority'][] = ['High', 'Medium', 'Low']

const getDefaultPreferences = (): OutputPreferences => {
    const saved = localStorage.getItem('minutesOutputPreferences')
    if (saved) {
        try { return JSON.parse(saved) } catch { }
    }
    return {
        tone: 'Formal',
        length: 'Standard',
        includeSections: {
            attendees: true,
            agenda: true,
            decisions: true,
            actionItems: true,
            additionalNotes: true,
        },
        pdfFooter: 'None',
    }
}

const generateId = () => Math.random().toString(36).substring(2, 9)

export default function StructuredModePage() {
    const [formData, setFormData] = useState<StructuredModeData>({
        meetingInfo: {
            title: '',
            date: new Date().toISOString().split('T')[0],
            startTime: '',
            endTime: '',
            location: '',
            organizer: '',
        },
        participants: [],
        absentParticipants: [],
        agenda: [],
        decisions: [],
        actionItems: [],
        additionalNotes: '',
        outputPreferences: getDefaultPreferences(),
    })

    const [participantInput, setParticipantInput] = useState('')
    const [absentInput, setAbsentInput] = useState('')
    const [showRoles, setShowRoles] = useState(false)
    const [customLocation, setCustomLocation] = useState('')

    const [agendaOpen, setAgendaOpen] = useState(false)
    const [notesOpen, setNotesOpen] = useState(false)
    const [prefsOpen, setPrefsOpen] = useState(false)
    const [absentOpen, setAbsentOpen] = useState(false)

    const [loading, setLoading] = useState(false)
    const [showClearModal, setShowClearModal] = useState(false)
    const [error, setError] = useState('')

    const { signOut } = useAuth()
    const navigate = useNavigate()

    // Save preferences to localStorage
    useEffect(() => {
        localStorage.setItem('minutesOutputPreferences', JSON.stringify(formData.outputPreferences))
    }, [formData.outputPreferences])

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    // Calculate required fields completion
    const calculateProgress = useCallback(() => {
        let completed = 0
        const total = 6 // title, date, startTime, endTime, location, 1+ participant

        if (formData.meetingInfo.title.trim()) completed++
        if (formData.meetingInfo.date) completed++
        if (formData.meetingInfo.startTime) completed++
        if (formData.meetingInfo.endTime) completed++
        if (formData.meetingInfo.location) completed++
        if (formData.participants.length > 0) completed++

        return { completed, total }
    }, [formData])

    const { completed, total } = calculateProgress()
    const canSubmit = completed === total

    // Participant handlers
    const addParticipant = (name: string, isAbsent: boolean = false) => {
        if (!name.trim()) return
        const newParticipant: Participant = {
            id: generateId(),
            name: name.trim(),
            present: !isAbsent,
        }

        if (isAbsent) {
            setFormData(prev => ({
                ...prev,
                absentParticipants: [...prev.absentParticipants, newParticipant]
            }))
            setAbsentInput('')
        } else {
            setFormData(prev => ({
                ...prev,
                participants: [...prev.participants, newParticipant]
            }))
            setParticipantInput('')
        }
    }

    const removeParticipant = (id: string, isAbsent: boolean = false) => {
        if (isAbsent) {
            setFormData(prev => ({
                ...prev,
                absentParticipants: prev.absentParticipants.filter(p => p.id !== id)
            }))
        } else {
            setFormData(prev => ({
                ...prev,
                participants: prev.participants.filter(p => p.id !== id)
            }))
        }
    }

    const updateParticipantRole = (id: string, role: string) => {
        setFormData(prev => ({
            ...prev,
            participants: prev.participants.map(p =>
                p.id === id ? { ...p, role: role || undefined } : p
            )
        }))
    }

    const handleParticipantKeyDown = (e: KeyboardEvent<HTMLInputElement>, isAbsent: boolean = false) => {
        if (e.key === 'Enter') {
            e.preventDefault()
            addParticipant(isAbsent ? absentInput : participantInput, isAbsent)
        }
    }

    // Agenda handlers
    const addAgendaItem = () => {
        const newItem: AgendaItem = {
            id: generateId(),
            title: '',
            objective: 'Discussion',
            keyPoints: '',
        }
        setFormData(prev => ({
            ...prev,
            agenda: [...prev.agenda, newItem]
        }))
    }

    const updateAgendaItem = (id: string, field: keyof AgendaItem, value: string) => {
        setFormData(prev => ({
            ...prev,
            agenda: prev.agenda.map(item =>
                item.id === id ? { ...item, [field]: value } : item
            )
        }))
    }

    const removeAgendaItem = (id: string) => {
        setFormData(prev => ({
            ...prev,
            agenda: prev.agenda.filter(item => item.id !== id)
        }))
    }

    // Decision handlers
    const addDecision = () => {
        const newDecision: Decision = {
            id: generateId(),
            statement: '',
            status: 'Approved',
        }
        setFormData(prev => ({
            ...prev,
            decisions: [...prev.decisions, newDecision]
        }))
    }

    const updateDecision = (id: string, field: keyof Decision, value: string) => {
        setFormData(prev => ({
            ...prev,
            decisions: prev.decisions.map(d =>
                d.id === id ? { ...d, [field]: value } : d
            )
        }))
    }

    const removeDecision = (id: string) => {
        setFormData(prev => ({
            ...prev,
            decisions: prev.decisions.filter(d => d.id !== id)
        }))
    }

    // Action Item handlers
    const addActionItem = () => {
        const newItem: ActionItem = {
            id: generateId(),
            task: '',
        }
        setFormData(prev => ({
            ...prev,
            actionItems: [...prev.actionItems, newItem]
        }))
    }

    const updateActionItem = (id: string, field: keyof ActionItem, value: string) => {
        setFormData(prev => ({
            ...prev,
            actionItems: prev.actionItems.map(item =>
                item.id === id ? { ...item, [field]: value } : item
            )
        }))
    }

    const removeActionItem = (id: string) => {
        setFormData(prev => ({
            ...prev,
            actionItems: prev.actionItems.filter(item => item.id !== id)
        }))
    }

    // Form handlers
    const clearForm = () => {
        setFormData({
            meetingInfo: {
                title: '',
                date: new Date().toISOString().split('T')[0],
                startTime: '',
                endTime: '',
                location: '',
                organizer: '',
            },
            participants: [],
            absentParticipants: [],
            agenda: [],
            decisions: [],
            actionItems: [],
            additionalNotes: '',
            outputPreferences: getDefaultPreferences(),
        })
        setShowClearModal(false)
    }

    const handleSubmit = async () => {
        if (!canSubmit || loading) return
        setLoading(true)
        setError('')

        try {
            const result = await generateMinutes(formData)

            if (result.status === 'success' && result.content) {
                navigate('/minutes/editor', {
                    state: {
                        content: result.content,
                        sourceData: formData,
                        mode: 'structured',
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

    const getStatusColor = (status: Decision['status']) => {
        switch (status) {
            case 'Approved': return 'bg-green-500/20 text-green-400 border-green-500/30'
            case 'Rejected': return 'bg-red-500/20 text-red-400 border-red-500/30'
            case 'Deferred': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30'
            default: return 'bg-slate-500/20 text-slate-400 border-slate-500/30'
        }
    }

    const getPriorityColor = (priority?: ActionItem['priority']) => {
        switch (priority) {
            case 'High': return 'bg-red-500/20 text-red-400'
            case 'Medium': return 'bg-yellow-500/20 text-yellow-400'
            case 'Low': return 'bg-green-500/20 text-green-400'
            default: return 'bg-slate-500/20 text-slate-400'
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

            {/* Error Toast */}
            {error && (
                <motion.div
                    initial={{ opacity: 0, y: -20 }}
                    animate={{ opacity: 1, y: 0 }}
                    className="fixed top-4 left-1/2 transform -translate-x-1/2 z-50 message-error max-w-md"
                >
                    {error}
                    <button onClick={() => setError('')} className="ml-2 text-red-300 hover:text-red-100">✕</button>
                </motion.div>
            )}

            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <button onClick={() => navigate('/minutes/new')} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back</span>
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
                    <span className="text-blue-400">Structured Mode</span>
                </nav>
            </motion.div>

            {/* Main Content */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="max-w-4xl mx-auto"
            >
                {/* Title */}
                <div className="glass-card p-6 sm:p-8 mb-6">
                    <div className="text-center mb-6">
                        <motion.div
                            animate={{ y: [0, -5, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                            className="text-blue-400 mb-4 inline-block"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                                <path fillRule="evenodd" d="M7.502 6h7.128A3.375 3.375 0 0 1 18 9.375v9.375a3 3 0 0 0 3-3V6.108c0-1.505-1.125-2.811-2.664-2.94a48.972 48.972 0 0 0-.673-.05A3 3 0 0 0 15 1.5h-1.5a3 3 0 0 0-2.663 1.618c-.225.015-.45.032-.673.05C8.662 3.295 7.554 4.542 7.502 6ZM13.5 3A1.5 1.5 0 0 0 12 4.5h4.5A1.5 1.5 0 0 0 15 3h-1.5Z" clipRule="evenodd" />
                                <path fillRule="evenodd" d="M3 9.375C3 8.339 3.84 7.5 4.875 7.5h9.75c1.036 0 1.875.84 1.875 1.875v11.25c0 1.035-.84 1.875-1.875 1.875h-9.75A1.875 1.875 0 0 1 3 20.625V9.375ZM6 12a.75.75 0 0 1 .75-.75h.008a.75.75 0 0 1 .75.75v.008a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V12Zm2.25 0a.75.75 0 0 1 .75-.75h3.75a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75ZM6 15a.75.75 0 0 1 .75-.75h.008a.75.75 0 0 1 .75.75v.008a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V15Zm2.25 0a.75.75 0 0 1 .75-.75h3.75a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75ZM6 18a.75.75 0 0 1 .75-.75h.008a.75.75 0 0 1 .75.75v.008a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V18Zm2.25 0a.75.75 0 0 1 .75-.75h3.75a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75Z" clipRule="evenodd" />
                            </svg>
                        </motion.div>
                        <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
                            Structured Mode - Professional Meeting Minutes
                        </h1>
                        <p className="text-slate-400 mt-2">
                            Fill in the details to generate formal meeting minutes (PV)
                        </p>
                    </div>

                    {/* Template Placeholder */}
                    <div className="mb-6 p-4 border border-slate-700/50 rounded-lg bg-slate-900/30">
                        <div className="flex items-center gap-3">
                            <span className="text-slate-400">Load Template:</span>
                            <select disabled className="input-nebula flex-1 opacity-50 cursor-not-allowed">
                                <option>Select template... (Coming soon)</option>
                            </select>
                        </div>
                    </div>

                    {/* Progress Indicator */}
                    <FormProgress completed={completed} total={total} />
                </div>

                {/* Section 1: Meeting Information */}
                <div className="glass-card p-6 mb-4">
                    <div className="flex items-center gap-3 mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-blue-400">
                            <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0 0 16.5 9h-1.875a1.875 1.875 0 0 1-1.875-1.875V5.25A3.75 3.75 0 0 0 9 1.5H5.625Z" clipRule="evenodd" />
                        </svg>
                        <h2 className="text-lg font-bold text-white">Meeting Information</h2>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Meeting Title <span className="text-red-400">*</span>
                            </label>
                            <input
                                type="text"
                                value={formData.meetingInfo.title}
                                onChange={(e) => setFormData(prev => ({
                                    ...prev,
                                    meetingInfo: { ...prev.meetingInfo, title: e.target.value }
                                }))}
                                placeholder="Enter meeting title"
                                className="input-nebula"
                                maxLength={200}
                            />
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Date <span className="text-red-400">*</span>
                                </label>
                                <input
                                    type="date"
                                    value={formData.meetingInfo.date}
                                    onChange={(e) => setFormData(prev => ({
                                        ...prev,
                                        meetingInfo: { ...prev.meetingInfo, date: e.target.value }
                                    }))}
                                    className="input-nebula"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Start Time <span className="text-red-400">*</span>
                                </label>
                                <input
                                    type="time"
                                    value={formData.meetingInfo.startTime}
                                    onChange={(e) => setFormData(prev => ({
                                        ...prev,
                                        meetingInfo: { ...prev.meetingInfo, startTime: e.target.value }
                                    }))}
                                    className="input-nebula"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    End Time <span className="text-red-400">*</span>
                                </label>
                                <input
                                    type="time"
                                    value={formData.meetingInfo.endTime}
                                    onChange={(e) => setFormData(prev => ({
                                        ...prev,
                                        meetingInfo: { ...prev.meetingInfo, endTime: e.target.value }
                                    }))}
                                    className="input-nebula"
                                />
                            </div>
                        </div>

                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Location / Platform <span className="text-red-400">*</span>
                                </label>
                                <select
                                    value={formData.meetingInfo.location}
                                    onChange={(e) => {
                                        setFormData(prev => ({
                                            ...prev,
                                            meetingInfo: { ...prev.meetingInfo, location: e.target.value }
                                        }))
                                        if (e.target.value !== 'Other') setCustomLocation('')
                                    }}
                                    className="input-nebula"
                                >
                                    <option value="">Select location...</option>
                                    {LOCATION_OPTIONS.map(loc => (
                                        <option key={loc} value={loc}>{loc}</option>
                                    ))}
                                </select>
                                {formData.meetingInfo.location === 'Other' && (
                                    <input
                                        type="text"
                                        value={customLocation}
                                        onChange={(e) => {
                                            setCustomLocation(e.target.value)
                                            setFormData(prev => ({
                                                ...prev,
                                                meetingInfo: { ...prev.meetingInfo, location: e.target.value || 'Other' }
                                            }))
                                        }}
                                        placeholder="Specify location..."
                                        className="input-nebula mt-2"
                                    />
                                )}
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Organizer / Chair (optional)
                                </label>
                                <input
                                    type="text"
                                    value={formData.meetingInfo.organizer}
                                    onChange={(e) => setFormData(prev => ({
                                        ...prev,
                                        meetingInfo: { ...prev.meetingInfo, organizer: e.target.value }
                                    }))}
                                    placeholder="Enter organizer name"
                                    className="input-nebula"
                                />
                            </div>
                        </div>
                    </div>
                </div>

                {/* Section 2: Participants */}
                <div className="glass-card p-6 mb-4">
                    <div className="flex items-center gap-3 mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-blue-400">
                            <path d="M4.5 6.375a4.125 4.125 0 1 1 8.25 0 4.125 4.125 0 0 1-8.25 0ZM14.25 8.625a3.375 3.375 0 1 1 6.75 0 3.375 3.375 0 0 1-6.75 0ZM1.5 19.125a7.125 7.125 0 0 1 14.25 0v.003l-.001.119a.75.75 0 0 1-.363.63 13.067 13.067 0 0 1-6.761 1.873c-2.472 0-4.786-.684-6.76-1.873a.75.75 0 0 1-.364-.63l-.001-.122ZM17.25 19.128l-.001.144a2.25 2.25 0 0 1-.233.96 10.088 10.088 0 0 0 5.06-1.01.75.75 0 0 0 .42-.643 4.875 4.875 0 0 0-6.957-4.611 8.586 8.586 0 0 1 1.71 5.157v.003Z" />
                        </svg>
                        <h2 className="text-lg font-bold text-white">
                            Participants <span className="text-red-400">*</span>
                        </h2>
                    </div>

                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Add Participants
                            </label>
                            <input
                                type="text"
                                value={participantInput}
                                onChange={(e) => setParticipantInput(e.target.value)}
                                onKeyDown={(e) => handleParticipantKeyDown(e)}
                                placeholder="Type name and press Enter..."
                                className="input-nebula"
                            />
                            <p className="text-xs text-slate-500 mt-1">Autocomplete will be added in Phase 2</p>
                        </div>

                        {formData.participants.length > 0 && (
                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">
                                    Added Participants ({formData.participants.length})
                                </label>
                                <div className="flex flex-wrap gap-2">
                                    {formData.participants.map(p => (
                                        <ParticipantTag
                                            key={p.id}
                                            name={p.name}
                                            role={p.role}
                                            showRole={showRoles}
                                            onRoleChange={(role) => updateParticipantRole(p.id, role)}
                                            onRemove={() => removeParticipant(p.id)}
                                        />
                                    ))}
                                </div>
                            </div>
                        )}

                        <label className="flex items-center gap-2 cursor-pointer">
                            <input
                                type="checkbox"
                                checked={showRoles}
                                onChange={(e) => setShowRoles(e.target.checked)}
                                className="rounded border-slate-600 bg-slate-800 text-blue-500"
                            />
                            <span className="text-sm text-slate-300">Add roles to participants</span>
                        </label>

                        {/* Absent Participants Accordion */}
                        <div className="border border-slate-700/50 rounded-lg overflow-hidden">
                            <button
                                onClick={() => setAbsentOpen(!absentOpen)}
                                className="w-full flex items-center justify-between p-3 hover:bg-slate-800/30 transition-colors"
                            >
                                <span className="text-sm text-slate-400">Mark Absent (optional)</span>
                                <motion.span
                                    animate={{ rotate: absentOpen ? 180 : 0 }}
                                    className="text-slate-400"
                                >
                                    ▼
                                </motion.span>
                            </button>
                            {absentOpen && (
                                <div className="p-3 pt-0 border-t border-slate-700/50">
                                    <input
                                        type="text"
                                        value={absentInput}
                                        onChange={(e) => setAbsentInput(e.target.value)}
                                        onKeyDown={(e) => handleParticipantKeyDown(e, true)}
                                        placeholder="Type name and press Enter..."
                                        className="input-nebula mb-2"
                                    />
                                    {formData.absentParticipants.length > 0 && (
                                        <div className="flex flex-wrap gap-2">
                                            {formData.absentParticipants.map(p => (
                                                <ParticipantTag
                                                    key={p.id}
                                                    name={p.name}
                                                    isAbsent
                                                    onRemove={() => removeParticipant(p.id, true)}
                                                />
                                            ))}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>
                    </div>
                </div>

                {/* Section 3: Agenda Items (Collapsible) */}
                <div className="mb-4">
                    <CollapsibleSection
                        title="Agenda Items"
                        icon={
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                <path fillRule="evenodd" d="M10.788 3.21c.448-1.077 1.976-1.077 2.424 0l2.082 5.006 5.404.434c1.164.093 1.636 1.545.749 2.305l-4.117 3.527 1.257 5.273c.271 1.136-.964 2.033-1.96 1.425L12 18.354 7.373 21.18c-.996.608-2.231-.29-1.96-1.425l1.257-5.273-4.117-3.527c-.887-.76-.415-2.212.749-2.305l5.404-.434 2.082-5.005Z" clipRule="evenodd" />
                            </svg>
                        }
                        count={formData.agenda.length}
                        isOpen={agendaOpen}
                        onToggle={() => setAgendaOpen(!agendaOpen)}
                    >
                        <div className="space-y-4">
                            {formData.agenda.map((item, index) => (
                                <div key={item.id} className="border border-slate-700/50 rounded-lg p-4 bg-slate-900/30">
                                    <div className="flex justify-between items-center mb-3">
                                        <span className="text-sm font-medium text-slate-300">
                                            Agenda Item {index + 1}
                                        </span>
                                        <button
                                            onClick={() => removeAgendaItem(item.id)}
                                            className="text-red-400 hover:text-red-300 text-sm"
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                    <div className="space-y-3">
                                        <input
                                            type="text"
                                            value={item.title}
                                            onChange={(e) => updateAgendaItem(item.id, 'title', e.target.value)}
                                            placeholder="Agenda item title"
                                            className="input-nebula"
                                        />
                                        <select
                                            value={item.objective}
                                            onChange={(e) => updateAgendaItem(item.id, 'objective', e.target.value)}
                                            className="input-nebula"
                                        >
                                            {OBJECTIVE_OPTIONS.map(obj => (
                                                <option key={obj} value={obj}>{obj}</option>
                                            ))}
                                        </select>
                                        <textarea
                                            value={item.keyPoints || ''}
                                            onChange={(e) => updateAgendaItem(item.id, 'keyPoints', e.target.value)}
                                            placeholder="• Key point 1&#10;• Key point 2"
                                            className="input-nebula min-h-[80px] resize-y"
                                        />
                                    </div>
                                </div>
                            ))}
                            <button
                                onClick={addAgendaItem}
                                className="w-full py-3 border-2 border-dashed border-slate-600 rounded-lg text-slate-400 hover:text-white hover:border-blue-500 transition-colors"
                            >
                                + Add Agenda Item
                            </button>
                        </div>
                    </CollapsibleSection>
                </div>

                {/* Section 4: Decisions */}
                <div className="glass-card p-6 mb-4">
                    <div className="flex items-center gap-3 mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-green-400">
                            <path fillRule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12Zm13.36-1.814a.75.75 0 1 0-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 0 0-1.06 1.06l2.25 2.25a.75.75 0 0 0 1.14-.094l3.75-5.25Z" clipRule="evenodd" />
                        </svg>
                        <h2 className="text-lg font-bold text-white">Decisions</h2>
                    </div>

                    <div className="space-y-4">
                        {formData.decisions.map((decision, index) => (
                            <div key={decision.id} className="border border-slate-700/50 rounded-lg p-4 bg-slate-900/30">
                                <div className="flex justify-between items-center mb-3">
                                    <span className="text-sm font-medium text-slate-300">
                                        Decision {index + 1}
                                    </span>
                                    <button
                                        onClick={() => removeDecision(decision.id)}
                                        className="text-red-400 hover:text-red-300 text-sm"
                                    >
                                        🗑️
                                    </button>
                                </div>
                                <div className="space-y-3">
                                    <input
                                        type="text"
                                        value={decision.statement}
                                        onChange={(e) => updateDecision(decision.id, 'statement', e.target.value)}
                                        placeholder="Decision statement"
                                        className="input-nebula"
                                        maxLength={500}
                                    />
                                    <div className="flex gap-3 items-center">
                                        <select
                                            value={decision.status}
                                            onChange={(e) => updateDecision(decision.id, 'status', e.target.value)}
                                            className="input-nebula flex-1"
                                        >
                                            {DECISION_STATUS_OPTIONS.map(status => (
                                                <option key={status} value={status}>{status}</option>
                                            ))}
                                        </select>
                                        <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getStatusColor(decision.status)}`}>
                                            {decision.status}
                                        </span>
                                    </div>
                                    {formData.agenda.length > 0 && (
                                        <select
                                            value={decision.relatedAgendaId || ''}
                                            onChange={(e) => updateDecision(decision.id, 'relatedAgendaId', e.target.value)}
                                            className="input-nebula"
                                        >
                                            <option value="">General Decision</option>
                                            {formData.agenda.map(item => (
                                                <option key={item.id} value={item.id}>
                                                    {item.title || `Agenda Item ${formData.agenda.indexOf(item) + 1}`}
                                                </option>
                                            ))}
                                        </select>
                                    )}
                                    <textarea
                                        value={decision.rationale || ''}
                                        onChange={(e) => updateDecision(decision.id, 'rationale', e.target.value)}
                                        placeholder="Rationale (optional)"
                                        className="input-nebula min-h-[60px] resize-y"
                                        maxLength={1000}
                                    />
                                </div>
                            </div>
                        ))}
                        <button
                            onClick={addDecision}
                            className="w-full py-3 border-2 border-dashed border-slate-600 rounded-lg text-slate-400 hover:text-white hover:border-green-500 transition-colors"
                        >
                            + Add Decision
                        </button>
                        {formData.decisions.length === 0 && (
                            <p className="text-yellow-400/70 text-sm text-center">
                                Tip: Add at least one decision for a complete meeting record
                            </p>
                        )}
                    </div>
                </div>

                {/* Section 5: Action Items */}
                <div className="glass-card p-6 mb-4">
                    <div className="flex items-center gap-3 mb-4">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-purple-400">
                            <path fillRule="evenodd" d="M7.502 6h7.128A3.375 3.375 0 0 1 18 9.375v9.375a3 3 0 0 0 3-3V6.108c0-1.505-1.125-2.811-2.664-2.94a48.972 48.972 0 0 0-.673-.05A3 3 0 0 0 15 1.5h-1.5a3 3 0 0 0-2.663 1.618c-.225.015-.45.032-.673.05C8.662 3.295 7.554 4.542 7.502 6ZM13.5 3A1.5 1.5 0 0 0 12 4.5h4.5A1.5 1.5 0 0 0 15 3h-1.5Z" clipRule="evenodd" />
                            <path fillRule="evenodd" d="M3 9.375C3 8.339 3.84 7.5 4.875 7.5h9.75c1.036 0 1.875.84 1.875 1.875v11.25c0 1.035-.84 1.875-1.875 1.875h-9.75A1.875 1.875 0 0 1 3 20.625V9.375Zm9.586 4.594a.75.75 0 0 0-1.172-.938l-2.476 3.096-.908-.907a.75.75 0 0 0-1.06 1.06l1.5 1.5a.75.75 0 0 0 1.116-.062l3-3.75Z" clipRule="evenodd" />
                        </svg>
                        <h2 className="text-lg font-bold text-white">Action Items</h2>
                    </div>

                    <div className="space-y-4">
                        {formData.actionItems.map((item, index) => (
                            <div key={item.id} className="border border-slate-700/50 rounded-lg p-4 bg-slate-900/30">
                                <div className="flex justify-between items-center mb-3">
                                    <span className="text-sm font-medium text-slate-300">
                                        Action Item {index + 1}
                                    </span>
                                    <button
                                        onClick={() => removeActionItem(item.id)}
                                        className="text-red-400 hover:text-red-300 text-sm"
                                    >
                                        🗑️
                                    </button>
                                </div>
                                <div className="space-y-3">
                                    <input
                                        type="text"
                                        value={item.task}
                                        onChange={(e) => updateActionItem(item.id, 'task', e.target.value)}
                                        placeholder="Task description"
                                        className="input-nebula"
                                        maxLength={500}
                                    />
                                    <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
                                        <select
                                            value={item.owner || ''}
                                            onChange={(e) => updateActionItem(item.id, 'owner', e.target.value)}
                                            className="input-nebula"
                                        >
                                            <option value="">Select owner...</option>
                                            {formData.participants.map(p => (
                                                <option key={p.id} value={p.name}>{p.name}</option>
                                            ))}
                                        </select>
                                        <input
                                            type="date"
                                            value={item.deadline || ''}
                                            onChange={(e) => updateActionItem(item.id, 'deadline', e.target.value)}
                                            className="input-nebula"
                                        />
                                        <div className="flex items-center gap-2">
                                            <select
                                                value={item.priority || ''}
                                                onChange={(e) => updateActionItem(item.id, 'priority', e.target.value)}
                                                className="input-nebula flex-1"
                                            >
                                                <option value="">Priority...</option>
                                                {PRIORITY_OPTIONS.map(p => (
                                                    <option key={p} value={p}>{p}</option>
                                                ))}
                                            </select>
                                            {item.priority && (
                                                <span className={`px-2 py-1 rounded text-xs font-medium ${getPriorityColor(item.priority)}`}>
                                                    {item.priority}
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                </div>
                            </div>
                        ))}
                        <button
                            onClick={addActionItem}
                            className="w-full py-3 border-2 border-dashed border-slate-600 rounded-lg text-slate-400 hover:text-white hover:border-purple-500 transition-colors"
                        >
                            + Add Action Item
                        </button>
                        {formData.actionItems.length === 0 && (
                            <p className="text-yellow-400/70 text-sm text-center">
                                Tip: Add action items to track follow-ups
                            </p>
                        )}
                    </div>
                </div>

                {/* Section 6: Additional Notes (Collapsible) */}
                <div className="mb-4">
                    <CollapsibleSection
                        title="Additional Notes"
                        icon={
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                <path d="M21.731 2.269a2.625 2.625 0 0 0-3.712 0l-1.157 1.157 3.712 3.712 1.157-1.157a2.625 2.625 0 0 0 0-3.712ZM19.513 8.199l-3.712-3.712-8.4 8.4a5.25 5.25 0 0 0-1.32 2.214l-.8 2.685a.75.75 0 0 0 .933.933l2.685-.8a5.25 5.25 0 0 0 2.214-1.32l8.4-8.4Z" />
                            </svg>
                        }
                        isOpen={notesOpen}
                        onToggle={() => setNotesOpen(!notesOpen)}
                    >
                        <div>
                            <textarea
                                value={formData.additionalNotes || ''}
                                onChange={(e) => setFormData(prev => ({
                                    ...prev,
                                    additionalNotes: e.target.value.slice(0, 5000)
                                }))}
                                placeholder="Optional: Add context, risks, blockers, or parking lot items..."
                                className="input-nebula min-h-[150px] resize-y"
                            />
                            <div className="text-right text-sm text-slate-500 mt-1">
                                {(formData.additionalNotes || '').length.toLocaleString()} / 5,000 characters
                            </div>
                        </div>
                    </CollapsibleSection>
                </div>

                {/* Section 7: Output Preferences (Collapsible) */}
                <div className="mb-6">
                    <CollapsibleSection
                        title="Output Preferences"
                        icon={
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
                                <path fillRule="evenodd" d="M11.078 2.25c-.917 0-1.699.663-1.85 1.567L9.05 4.889c-.02.12-.115.26-.297.348a7.493 7.493 0 0 0-.986.57c-.166.115-.334.126-.45.083L6.3 5.508a1.875 1.875 0 0 0-2.282.819l-.922 1.597a1.875 1.875 0 0 0 .432 2.385l.84.692c.095.078.17.229.154.43a7.598 7.598 0 0 0 0 1.139c.015.2-.059.352-.153.43l-.841.692a1.875 1.875 0 0 0-.432 2.385l.922 1.597a1.875 1.875 0 0 0 2.282.818l1.019-.382c.115-.043.283-.031.45.082.312.214.641.405.985.57.182.088.277.228.297.35l.178 1.071c.151.904.933 1.567 1.85 1.567h1.844c.916 0 1.699-.663 1.85-1.567l.178-1.072c.02-.12.114-.26.297-.349.344-.165.673-.356.985-.57.167-.114.335-.125.45-.082l1.02.382a1.875 1.875 0 0 0 2.28-.819l.923-1.597a1.875 1.875 0 0 0-.432-2.385l-.84-.692c-.095-.078-.17-.229-.154-.43a7.614 7.614 0 0 0 0-1.139c-.016-.2.059-.352.153-.43l.84-.692c.708-.582.891-1.59.433-2.385l-.922-1.597a1.875 1.875 0 0 0-2.282-.818l-1.02.382c-.114.043-.282.031-.449-.083a7.49 7.49 0 0 0-.985-.57c-.183-.087-.277-.227-.297-.348l-.179-1.072a1.875 1.875 0 0 0-1.85-1.567h-1.843ZM12 15.75a3.75 3.75 0 1 0 0-7.5 3.75 3.75 0 0 0 0 7.5Z" clipRule="evenodd" />
                            </svg>
                        }
                        isOpen={prefsOpen}
                        onToggle={() => setPrefsOpen(!prefsOpen)}
                    >
                        <div className="space-y-4">
                            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">Tone</label>
                                    <select
                                        value={formData.outputPreferences.tone}
                                        onChange={(e) => setFormData(prev => ({
                                            ...prev,
                                            outputPreferences: { ...prev.outputPreferences, tone: e.target.value as OutputPreferences['tone'] }
                                        }))}
                                        className="input-nebula"
                                    >
                                        <option value="Formal">Formal</option>
                                        <option value="Executive">Executive</option>
                                        <option value="Technical">Technical</option>
                                    </select>
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-slate-300 mb-2">Length</label>
                                    <select
                                        value={formData.outputPreferences.length}
                                        onChange={(e) => setFormData(prev => ({
                                            ...prev,
                                            outputPreferences: { ...prev.outputPreferences, length: e.target.value as OutputPreferences['length'] }
                                        }))}
                                        className="input-nebula"
                                    >
                                        <option value="Standard">Standard</option>
                                        <option value="Detailed">Detailed</option>
                                        <option value="Summary">Summary</option>
                                    </select>
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">Include Sections</label>
                                <div className="space-y-2">
                                    {Object.entries(formData.outputPreferences.includeSections).map(([key, value]) => (
                                        <label key={key} className="flex items-center gap-2 cursor-pointer">
                                            <input
                                                type="checkbox"
                                                checked={value}
                                                onChange={(e) => setFormData(prev => ({
                                                    ...prev,
                                                    outputPreferences: {
                                                        ...prev.outputPreferences,
                                                        includeSections: {
                                                            ...prev.outputPreferences.includeSections,
                                                            [key]: e.target.checked
                                                        }
                                                    }
                                                }))}
                                                className="rounded border-slate-600 bg-slate-800 text-blue-500"
                                            />
                                            <span className="text-sm text-slate-300 capitalize">
                                                {key.replace(/([A-Z])/g, ' $1').trim()}
                                            </span>
                                        </label>
                                    ))}
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-slate-300 mb-2">PDF Footer</label>
                                <select
                                    value={formData.outputPreferences.pdfFooter}
                                    onChange={(e) => setFormData(prev => ({
                                        ...prev,
                                        outputPreferences: { ...prev.outputPreferences, pdfFooter: e.target.value as OutputPreferences['pdfFooter'] }
                                    }))}
                                    className="input-nebula"
                                >
                                    <option value="None">None</option>
                                    <option value="Confidential">Confidential</option>
                                    <option value="Internal Use Only">Internal Use Only</option>
                                </select>
                            </div>
                        </div>
                    </CollapsibleSection>
                </div>

                {/* Action Buttons */}
                <div className="glass-card p-6">
                    <div className="flex flex-wrap gap-4 justify-between">
                        <div className="flex gap-3">
                            <button
                                disabled
                                className="btn-secondary opacity-50 cursor-not-allowed"
                                title="Coming in Step 5"
                            >
                                Save Draft
                            </button>
                            <button
                                onClick={() => setShowClearModal(true)}
                                className="btn-secondary text-red-400 border-red-400/30 hover:bg-red-500/10"
                            >
                                Clear Form
                            </button>
                        </div>
                        <button
                            onClick={handleSubmit}
                            disabled={!canSubmit || loading}
                            className="btn-primary px-8"
                        >
                            {loading ? (
                                <span className="flex items-center gap-2">
                                    <span className="spinner" />
                                    Processing...
                                </span>
                            ) : (
                                'Continue →'
                            )}
                        </button>
                    </div>
                </div>
            </motion.div>

            {/* Clear Form Modal */}
            {showClearModal && (
                <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
                    <motion.div
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="glass-card p-6 max-w-md w-full"
                    >
                        <h3 className="text-xl font-bold text-white mb-2">Clear All Fields?</h3>
                        <p className="text-slate-400 mb-6">
                            This will remove all entered data. This action cannot be undone.
                        </p>
                        <div className="flex gap-3 justify-end">
                            <button
                                onClick={() => setShowClearModal(false)}
                                className="btn-secondary"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={clearForm}
                                className="btn-primary bg-red-600 hover:bg-red-500"
                            >
                                Clear Form
                            </button>
                        </div>
                    </motion.div>
                </div>
            )}
        </div>
    )
}
