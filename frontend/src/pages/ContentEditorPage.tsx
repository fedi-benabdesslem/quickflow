import { useState, useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion } from 'framer-motion'
import TechSupportButton from '../components/TechSupportButton'

import RichTextEditor from '../components/RichTextEditor'
import PdfPreviewModal from '../components/PdfPreviewModal'
import { generateFromExtracted, generateMinutes, generatePdf } from '../lib/api'

import type { ExtractedMeetingData, StructuredModeData } from '../types'
import { marked } from 'marked'

interface LocationState {
    content: string
    sourceData: ExtractedMeetingData | StructuredModeData
    mode: 'quick' | 'structured'
}

export default function ContentEditorPage() {
    const navigate = useNavigate()
    const location = useLocation()


    const state = location.state as LocationState | null

    const [content, setContent] = useState('')
    const [loading, setLoading] = useState(false)
    const [showRegenerateConfirm, setShowRegenerateConfirm] = useState(false)
    const [showSidebar, setShowSidebar] = useState(true)
    const [error, setError] = useState('')
    const [lastSaved, setLastSaved] = useState<Date | null>(null)
    const [showPdfPreview, setShowPdfPreview] = useState(false)
    const [pdfFileId, setPdfFileId] = useState<string | null>(null)
    const [pdfFilename, setPdfFilename] = useState('')

    // Helper to process content (handle markdown if present)
    const processContent = async (rawContent: string) => {
        try {
            // Check if content looks like markdown (has ** or ##) and doesn't look like HTML
            if ((rawContent.includes('**') || rawContent.includes('##')) && !rawContent.includes('<div')) {
                const html = await marked.parse(rawContent)
                return html
            }
            return rawContent
        } catch (e) {
            console.error('Markdown parsing error:', e)
            return rawContent
        }
    }

    useEffect(() => {
        const initContent = async () => {
            if (state?.content) {
                const html = await processContent(state.content)
                setContent(html)
                // Save to localStorage
                localStorage.setItem('minutesEditorContent', html)
                setLastSaved(new Date())
            } else {
                // Try to restore from localStorage
                const saved = localStorage.getItem('minutesEditorContent')
                if (saved) {
                    setContent(saved)
                } else {
                    navigate('/minutes/new')
                }
            }
        }
        initContent()
    }, [state, navigate])

    // Auto-save every 30 seconds
    useEffect(() => {
        const interval = setInterval(() => {
            if (content) {
                localStorage.setItem('minutesEditorContent', content)
                setLastSaved(new Date())
            }
        }, 30000)
        return () => clearInterval(interval)
    }, [content])



    const handleRegenerate = async () => {
        if (!state?.sourceData) {
            setError('Cannot regenerate - source data not available')
            return
        }

        setShowRegenerateConfirm(false)
        setLoading(true)
        setError('')

        try {
            let result
            if (state.mode === 'quick') {
                result = await generateFromExtracted(
                    state.sourceData as ExtractedMeetingData,
                    'Formal',
                    'Standard'
                )
            } else {
                result = await generateMinutes(state.sourceData as StructuredModeData)
            }

            if (result.status === 'success' && result.content) {
                const html = await processContent(result.content)
                setContent(html)
                localStorage.setItem('minutesEditorContent', html)
                setLastSaved(new Date())
            } else {
                setError(result.message || 'Regeneration failed. Please try again.')
            }
        } catch (err) {
            console.error('Regeneration error:', err)
            setError('AI service temporarily unavailable. Please try again later.')
        } finally {
            setLoading(false)
        }
    }

    const handleBackToForm = () => {
        if (state?.mode === 'quick') {
            navigate('/minutes/quick/review', { state: { extractedData: state.sourceData } })
        } else {
            navigate('/minutes/structured')
        }
    }

    const handleClearAndStartOver = () => {
        if (confirm('Are you sure you want to clear all content and start over?')) {
            localStorage.removeItem('minutesEditorContent')
            navigate('/minutes/new')
        }
    }

    const handlePreviewPdf = async () => {
        if (!state?.sourceData || !content) return

        setLoading(true)
        setError('')

        try {
            // Prepare metadata from source data
            const metadata: Record<string, string> = {}
            if (state.mode === 'quick') {
                const data = state.sourceData as ExtractedMeetingData
                metadata.title = data.meetingTitle || 'Meeting Minutes'
                metadata.date = data.date || new Date().toISOString().split('T')[0]
            } else {
                const data = state.sourceData as StructuredModeData
                metadata.title = data.meetingInfo?.title || 'Meeting Minutes'
                metadata.date = data.meetingInfo?.date || new Date().toISOString().split('T')[0]
                if (data.meetingInfo?.location) metadata.location = data.meetingInfo.location
                if (data.meetingInfo?.startTime) metadata.startTime = data.meetingInfo.startTime
                if (data.meetingInfo?.endTime) metadata.endTime = data.meetingInfo.endTime
                if (data.meetingInfo?.organizer) metadata.organizer = data.meetingInfo.organizer
            }

            // Get preferences (for now using defaults/mock)
            const preferences = {
                pdfFooter: 'Generated by QuickFlow AI'
            }

            const result = await generatePdf({
                htmlContent: content,
                meetingMetadata: metadata,
                outputPreferences: preferences
            })

            if (result.status === 'success' && result.fileId) {
                setPdfFileId(result.fileId)
                setPdfFilename(result.filename || 'document.pdf')
                setShowPdfPreview(true)
            } else {
                setError(result.message || 'PDF generation failed')
            }
        } catch (err) {
            console.error('PDF error:', err)
            setError('Failed to generate PDF. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    const getWordCount = () => {
        const text = content.replace(/<[^>]*>/g, '').trim()
        return text ? text.split(/\s+/).length : 0
    }

    const formatTime = (date: Date) => {
        return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
    }

    // Format source data for sidebar display
    const getSourceDataSummary = () => {
        if (!state?.sourceData) return null

        if (state.mode === 'quick') {
            const data = state.sourceData as ExtractedMeetingData
            // Map participants - they could be strings or objects with name/email
            const participantsList = data.participants?.map(p => {
                if (typeof p === 'string') {
                    return { name: p, email: undefined }
                }
                return { name: p.name, email: p.email }
            }) || []
            return {
                title: data.meetingTitle || 'Untitled',
                date: data.date || 'Not specified',
                participants: participantsList,
                participantNames: participantsList.map(p => p.name),
                decisions: data.decisions?.length || 0,
                actionItems: data.actionItems?.length || 0,
            }
        } else {
            const data = state.sourceData as StructuredModeData
            return {
                title: data.meetingInfo?.title || 'Untitled',
                date: data.meetingInfo?.date || 'Not specified',
                participants: data.participants?.map(p => ({ name: p.name, email: p.email })) || [],
                participantNames: data.participants?.map(p => p.name) || [],
                decisions: data.decisions?.length || 0,
                actionItems: data.actionItems?.length || 0,
            }
        }
    }

    const summary = getSourceDataSummary()

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
                            Regenerating Content...
                        </h3>
                        <p className="text-slate-400 text-sm">
                            AI is creating new content. This may take 15-30 seconds.
                        </p>
                    </motion.div>
                </div>
            )}

            {/* Regenerate Confirmation Modal */}
            {showRegenerateConfirm && (
                <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center">
                    <motion.div
                        initial={{ opacity: 0, scale: 0.9 }}
                        animate={{ opacity: 1, scale: 1 }}
                        className="glass-card p-6 max-w-md"
                    >
                        <h3 className="text-xl font-semibold text-white mb-4">
                            Regenerate Content?
                        </h3>
                        <p className="text-slate-400 mb-6">
                            This will replace the current content with a new AI-generated version.
                            Your edits will be lost.
                        </p>
                        <div className="flex gap-3">
                            <button
                                onClick={() => setShowRegenerateConfirm(false)}
                                className="btn-secondary flex-1"
                            >
                                Cancel
                            </button>
                            <button
                                onClick={handleRegenerate}
                                className="btn-primary flex-1"
                            >
                                Regenerate
                            </button>
                        </div>
                    </motion.div>
                </div>
            )}

            <PdfPreviewModal
                isOpen={showPdfPreview}
                onClose={() => setShowPdfPreview(false)}
                fileId={pdfFileId}
                filename={pdfFilename}
                meetingData={summary || undefined}
            />

            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-6"
            >
                <button onClick={handleBackToForm} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back to Form</span>
                </button>
                <div className="flex items-center gap-4">
                    {lastSaved && (
                        <span className="text-sm text-slate-500">
                            Saved at {formatTime(lastSaved)}
                        </span>
                    )}
                    <TechSupportButton />
                </div>
            </motion.header>

            {/* Breadcrumb */}
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="max-w-6xl mx-auto mb-4"
            >
                <nav className="text-sm text-slate-500">
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/home')}>Home</span>
                    <span className="mx-2">›</span>
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/minutes/new')}>Create Minutes</span>
                    <span className="mx-2">›</span>
                    <span className="text-blue-400">Review & Edit</span>
                </nav>
            </motion.div>

            {/* Main Content */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="max-w-6xl mx-auto"
            >
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

                <div className="flex gap-6">
                    {/* Editor Panel */}
                    <div className={`flex-1 ${showSidebar ? '' : 'w-full'}`}>
                        <div className="glass-card p-6">
                            {/* Editor Header */}
                            <div className="flex justify-between items-center mb-4">
                                <h1 className="text-xl font-bold text-white">
                                    Review & Edit Meeting Minutes
                                </h1>
                                <span className="text-sm text-slate-500">
                                    {getWordCount()} words
                                </span>
                            </div>

                            {/* Rich Text Editor */}
                            <div className="min-h-[500px]">
                                <RichTextEditor
                                    value={content}
                                    onChange={setContent}
                                    placeholder="Your meeting minutes will appear here..."
                                    disabled={loading}
                                />
                            </div>

                            {/* Editor Actions */}
                            <div className="flex flex-wrap gap-3 mt-6 pt-4 border-t border-slate-700/50">
                                <button
                                    onClick={() => setShowRegenerateConfirm(true)}
                                    className="btn-secondary"
                                    disabled={loading || !state?.sourceData}
                                >
                                    🔄 Regenerate
                                </button>
                                <button
                                    onClick={() => setShowSidebar(!showSidebar)}
                                    className="btn-secondary"
                                >
                                    {showSidebar ? '◀ Hide Sidebar' : '▶ Show Sidebar'}
                                </button>
                                <button
                                    onClick={handleClearAndStartOver}
                                    className="btn-secondary text-red-400 hover:text-red-300"
                                    disabled={loading}
                                >
                                    Clear & Start Over
                                </button>
                                <div className="flex-1" />
                                <button
                                    className="btn-secondary opacity-50 cursor-not-allowed"
                                    title="Coming in Step 3"
                                    disabled
                                >
                                    📄 Save Draft
                                </button>
                                <button
                                    onClick={handlePreviewPdf}
                                    className="btn-primary"
                                    disabled={loading || !content}
                                >
                                    Continue to PDF Preview →
                                </button>
                            </div>
                        </div>
                    </div>

                    {/* Sidebar - Source Data */}
                    {showSidebar && summary && (
                        <motion.div
                            initial={{ opacity: 0, x: 20 }}
                            animate={{ opacity: 1, x: 0 }}
                            className="w-80 flex-shrink-0"
                        >
                            <div className="glass-card p-5 sticky top-6">
                                <h2 className="text-lg font-semibold text-white mb-4">
                                    📋 Source Data
                                </h2>

                                <div className="space-y-4 text-sm">
                                    <div>
                                        <span className="text-slate-400">Title:</span>
                                        <p className="text-white">{summary.title}</p>
                                    </div>

                                    <div>
                                        <span className="text-slate-400">Date:</span>
                                        <p className="text-white">{summary.date}</p>
                                    </div>

                                    <div>
                                        <span className="text-slate-400">Participants:</span>
                                        <div className="flex flex-wrap gap-1 mt-1">
                                            {summary.participantNames.slice(0, 5).map((p, i) => (
                                                <span key={i} className="px-2 py-0.5 bg-slate-700/50 rounded text-xs">
                                                    {p}
                                                </span>
                                            ))}
                                            {summary.participantNames.length > 5 && (
                                                <span className="text-slate-500 text-xs">
                                                    +{summary.participantNames.length - 5} more
                                                </span>
                                            )}
                                        </div>
                                    </div>

                                    <div className="flex gap-4">
                                        <div>
                                            <span className="text-slate-400">Decisions:</span>
                                            <p className="text-white">{summary.decisions}</p>
                                        </div>
                                        <div>
                                            <span className="text-slate-400">Actions:</span>
                                            <p className="text-white">{summary.actionItems}</p>
                                        </div>
                                    </div>
                                </div>

                                <div className="mt-6 pt-4 border-t border-slate-700/50">
                                    <p className="text-xs text-slate-500">
                                        This sidebar shows the original data used to generate
                                        the minutes. Use it as reference while editing.
                                    </p>
                                </div>
                            </div>
                        </motion.div>
                    )}
                </div>
            </motion.div>
        </div>
    )
}
