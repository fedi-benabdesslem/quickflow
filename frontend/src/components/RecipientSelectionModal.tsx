import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { sendMinutesEmail, getEmailCapability } from '../lib/api'

interface RecipientSelectionModalProps {
    isOpen: boolean
    onClose: () => void
    fileId: string
    filename: string
    meetingData?: {
        participants?: Array<{ name: string; email?: string } | string>
        title?: string
        date?: string
    }
}

export default function RecipientSelectionModal({
    isOpen,
    onClose,
    fileId,
    filename,
    meetingData
}: RecipientSelectionModalProps) {
    const [selectedParticipants, setSelectedParticipants] = useState<Set<string>>(new Set())
    const [participantDetails, setParticipantDetails] = useState<Array<{ name: string, email: string | null }>>([])
    const [additionalRecipients, setAdditionalRecipients] = useState<string[]>([])
    const [newRecipient, setNewRecipient] = useState('')
    const [subject, setSubject] = useState('')
    const [customBody, setCustomBody] = useState('')
    const [isSending, setIsSending] = useState(false)
    const [sendError, setSendError] = useState<string | null>(null)
    const [sendSuccess, setSendSuccess] = useState(false)
    const [providerWarning, setProviderWarning] = useState<string | null>(null)

    // Parse participants and check provider on open
    useEffect(() => {
        if (isOpen) {
            // Check provider capability
            getEmailCapability().then(({ canSendEmail, provider }) => {
                if (!canSendEmail && provider === 'none') {
                    // Not signed in? Should handle this, but AuthContext likely handles it
                } else if (!canSendEmail) {
                    setProviderWarning('Email sending is not available for your sign-in method. Please sign in with Google or Microsoft.')
                } else if (provider === 'email') {
                    setProviderWarning('Email sending is currently only supported for Google and Microsoft accounts.')
                }
            })

            // Init participants
            if (meetingData?.participants) {
                const parsed = meetingData.participants.map(p => {
                    if (typeof p === 'string') {
                        return { name: p, email: null }
                    }
                    return { name: p.name, email: p.email || null }
                })
                setParticipantDetails(parsed)

                // Pre-select those with emails
                const withEmails = parsed
                    .filter(p => p.email)
                    .map(p => p.email as string)
                setSelectedParticipants(new Set(withEmails))
            }

            // Init subject
            const title = meetingData?.title || 'Meeting Minutes'
            setSubject(`Meeting Minutes: ${title}`)
        }
    }, [isOpen, meetingData])

    const handleAddRecipient = () => {
        if (newRecipient && newRecipient.includes('@')) {
            setAdditionalRecipients(prev => [...prev, newRecipient.trim()])
            setNewRecipient('')
        }
    }

    const handleParticipantToggle = (email: string) => {
        const newSet = new Set(selectedParticipants)
        if (newSet.has(email)) {
            newSet.delete(email)
        } else {
            newSet.add(email)
        }
        setSelectedParticipants(newSet)
    }

    // Handle inline adding email for participant
    const handleUpdateParticipantEmail = (index: number, email: string) => {
        const updated = [...participantDetails]
        updated[index].email = email
        setParticipantDetails(updated)

        // Auto-select
        const newSet = new Set(selectedParticipants)
        newSet.add(email)
        setSelectedParticipants(newSet)
    }

    const getAllRecipients = () => {
        return [...Array.from(selectedParticipants), ...additionalRecipients]
    }

    const handleSend = async () => {
        const recipients = getAllRecipients()
        if (recipients.length === 0) {
            setSendError('Please select at least one recipient.')
            return
        }

        setIsSending(true)
        setSendError(null)

        const metadata: Record<string, string> = {}
        if (meetingData?.title) metadata.title = meetingData.title
        if (meetingData?.date) metadata.date = meetingData.date

        const result = await sendMinutesEmail({
            pdfFileId: fileId,
            recipients,
            subject,
            body: customBody,
            meetingMetadata: metadata
        })

        setIsSending(false)

        if (result.status === 'success') {
            setSendSuccess(true)
        } else if (result.status === 'unsupported') {
            setProviderWarning(result.message || 'Provider not supported')
        } else if ((result as any).code === 'reauth_required') {
            setSendError('Session expired. Please sign in again.')
        } else {
            setSendError(result.message || 'Failed to send email.')
        }
    }

    if (!isOpen) return null

    // Success View
    if (sendSuccess) {
        return (
            <AnimatePresence>
                <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4">
                    <motion.div
                        initial={{ scale: 0.9, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        className="glass-card max-w-md w-full p-8 text-center"
                    >
                        <div className="w-16 h-16 bg-green-500/20 rounded-full flex items-center justify-center mx-auto mb-4 text-green-400">
                            <svg className="w-10 h-10" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
                            </svg>
                        </div>
                        <h2 className="text-2xl font-bold text-white mb-2">Sent Successfully!</h2>
                        <p className="text-slate-300 mb-6">
                            Meeting minutes have been sent to {getAllRecipients().length} recipients.
                        </p>
                        <div className="flex gap-3 justify-center">
                            <button onClick={onClose} className="btn-primary">
                                Close
                            </button>
                        </div>
                    </motion.div>
                </div>
            </AnimatePresence>
        )
    }

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-[60] flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 sm:p-6"
            >
                <div className="w-full max-w-2xl flex flex-col glass-card max-h-[90vh]">
                    {/* Header */}
                    <div className="flex justify-between items-center p-5 border-b border-slate-700/50 bg-slate-900/50">
                        <div>
                            <h3 className="text-white text-lg font-semibold">Send Meeting Minutes</h3>
                            <p className="text-xs text-slate-400">
                                {filename}
                            </p>
                        </div>
                        <button onClick={onClose} className="text-slate-400 hover:text-white transition-colors">
                            ✕
                        </button>
                    </div>

                    {/* Content */}
                    <div className="flex-1 overflow-y-auto p-6 space-y-6">

                        {providerWarning && (
                            <div className="p-4 bg-yellow-500/10 border border-yellow-500/20 rounded-lg text-yellow-200 text-sm flex gap-3">
                                <span className="text-xl">⚠</span>
                                <div>
                                    <p className="font-semibold mb-1">Email Sending Not Available</p>
                                    <p>{providerWarning}</p>
                                    <p className="mt-2 text-xs opacity-70">
                                        You can try downloading the PDF instead.
                                    </p>
                                </div>
                            </div>
                        )}

                        {sendError && (
                            <div className="p-3 bg-red-500/10 border border-red-500/20 rounded-lg text-red-200 text-sm">
                                {sendError}
                            </div>
                        )}

                        {/* Participants Section */}
                        {participantDetails.length > 0 && (
                            <div>
                                <h4 className="text-sm font-medium text-slate-300 mb-3 uppercase tracking-wider">
                                    Meeting Participants
                                </h4>
                                <div className="space-y-2">
                                    {participantDetails.map((p, i) => (
                                        <div key={i} className="flex items-center justify-between p-3 bg-slate-800/30 rounded-lg border border-slate-700/30">
                                            <div className="flex items-center gap-3">
                                                <input
                                                    type="checkbox"
                                                    checked={!!p.email && selectedParticipants.has(p.email)}
                                                    disabled={!p.email || !!providerWarning}
                                                    onChange={() => p.email && handleParticipantToggle(p.email)}
                                                    className="w-4 h-4 rounded border-slate-600 bg-slate-700 text-blue-500 focus:ring-blue-500/50"
                                                />
                                                <div>
                                                    <p className="text-white font-medium">{p.name}</p>
                                                    {p.email ? (
                                                        <p className="text-xs text-slate-400">{p.email}</p>
                                                    ) : (
                                                        <div className="mt-1 flex gap-2">
                                                            <input
                                                                type="email"
                                                                placeholder="Add email address..."
                                                                className="text-xs bg-slate-800 border-b border-slate-600 focus:border-blue-500 outline-none w-48 text-white px-1 py-0.5"
                                                                onKeyDown={(e) => {
                                                                    if (e.key === 'Enter') {
                                                                        handleUpdateParticipantEmail(i, e.currentTarget.value)
                                                                    }
                                                                }}
                                                                onBlur={(e) => {
                                                                    if (e.target.value) handleUpdateParticipantEmail(i, e.target.value)
                                                                }}
                                                            />
                                                        </div>
                                                    )}
                                                </div>
                                            </div>
                                            {!p.email && (
                                                <span className="text-xs text-yellow-500/80">No email</span>
                                            )}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* Additional Recipients */}
                        <div>
                            <h4 className="text-sm font-medium text-slate-300 mb-2 uppercase tracking-wider">
                                Additional Recipients
                            </h4>
                            <div className="flex gap-2 mb-2">
                                <input
                                    type="email"
                                    value={newRecipient}
                                    onChange={(e) => setNewRecipient(e.target.value)}
                                    onKeyDown={(e) => e.key === 'Enter' && handleAddRecipient()}
                                    placeholder="Enter email address..."
                                    className="flex-1 bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:border-blue-500 outline-none"
                                    disabled={!!providerWarning}
                                />
                                <button
                                    onClick={handleAddRecipient}
                                    className="px-4 py-2 bg-slate-700 hover:bg-slate-600 rounded-lg text-white text-sm transition-colors"
                                    disabled={!!providerWarning}
                                >
                                    Add
                                </button>
                            </div>
                            {additionalRecipients.length > 0 && (
                                <div className="flex flex-wrap gap-2">
                                    {additionalRecipients.map((email, i) => (
                                        <span key={i} className="inline-flex items-center gap-1 px-2 py-1 bg-blue-500/20 text-blue-300 rounded text-xs border border-blue-500/30">
                                            {email}
                                            <button
                                                onClick={() => setAdditionalRecipients(prev => prev.filter((_, idx) => idx !== i))}
                                                className="hover:text-white"
                                            >
                                                ×
                                            </button>
                                        </span>
                                    ))}
                                </div>
                            )}
                        </div>

                        {/* Email Content */}
                        <div className="pt-4 border-t border-slate-700/50">
                            <div className="mb-4">
                                <label className="block text-xs text-slate-400 mb-1">Subject</label>
                                <input
                                    type="text"
                                    value={subject}
                                    onChange={(e) => setSubject(e.target.value)}
                                    className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:border-blue-500 outline-none"
                                    disabled={!!providerWarning}
                                />
                            </div>
                            <div>
                                <label className="block text-xs text-slate-400 mb-1">Message (Optional)</label>
                                <textarea
                                    value={customBody}
                                    onChange={(e) => setCustomBody(e.target.value)}
                                    placeholder="Add a personal message..."
                                    rows={3}
                                    className="w-full bg-slate-800/50 border border-slate-600 rounded-lg px-3 py-2 text-white text-sm focus:border-blue-500 outline-none resize-none"
                                    disabled={!!providerWarning}
                                />
                            </div>
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div className="p-5 border-t border-slate-700/50 bg-slate-900/50 flex justify-between items-center">
                        <div className="text-sm text-slate-400">
                            Sending to <strong className="text-white">{getAllRecipients().length}</strong> recipient(s)
                        </div>
                        <div className="flex gap-3">
                            <button onClick={onClose} className="btn-secondary px-6">
                                Cancel
                            </button>
                            <button
                                onClick={handleSend}
                                disabled={isSending || !!providerWarning || getAllRecipients().length === 0}
                                className="btn-primary px-6 flex items-center gap-2"
                            >
                                {isSending ? (
                                    <>
                                        <div className="spinner w-4 h-4 border-2" />
                                        Sending...
                                    </>
                                ) : (
                                    <>Send Email</>
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </motion.div>
        </AnimatePresence>
    )
}
