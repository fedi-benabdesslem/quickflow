import { useState, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import TechSupportButton from '../components/TechSupportButton'

// Types
interface TranscriptSegment {
    speaker: string
    start: number
    end: number
    text: string
}

interface TranscriptResult {
    jobId: string
    status: string
    segments: TranscriptSegment[]
    speakers: string[]
    duration: number
    language: string
    fullText: string
    processingTimeSeconds: number
    error?: string
}

interface GenerateRequest {
    transcriptData: TranscriptResult
    speakerMapping: Record<string, string>
    tone: string
    length: string
    meetingTitle: string
    meetingDate: string
    meetingTime: string
    meetingLocation: string
}

// API calls
const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api'

async function checkVoiceServiceStatus(): Promise<{ available: boolean }> {
    const response = await fetch(`${API_BASE}/minutes/voice/status`)
    return response.json()
}

async function uploadForTranscription(file: File): Promise<{ status: string; data?: TranscriptResult; message?: string }> {
    const formData = new FormData()
    formData.append('file', file)

    const response = await fetch(`${API_BASE}/minutes/voice/transcribe`, {
        method: 'POST',
        body: formData,
    })
    return response.json()
}

async function generateFromTranscript(request: GenerateRequest): Promise<{ status: string; content?: string; message?: string }> {
    const response = await fetch(`${API_BASE}/minutes/voice/generate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(request),
    })
    return response.json()
}

// Helper functions
function formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60)
    const secs = Math.floor(seconds % 60)
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
}

function formatDuration(seconds: number): string {
    const hours = Math.floor(seconds / 3600)
    const mins = Math.floor((seconds % 3600) / 60)
    const secs = Math.floor(seconds % 60)
    if (hours > 0) {
        return `${hours}:${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`
    }
    return `${mins}:${secs.toString().padStart(2, '0')}`
}

// Supported formats
const SUPPORTED_FORMATS = ['.mp3', '.wav', '.m4a', '.flac', '.ogg', '.wma', '.aac', '.webm']
const MAX_FILE_SIZE_MB = 350

export default function VoiceModePage() {
    const navigate = useNavigate()
    const fileInputRef = useRef<HTMLInputElement>(null)

    // State
    const [step, setStep] = useState<'upload' | 'processing' | 'review' | 'generating' | 'done'>('upload')
    const [file, setFile] = useState<File | null>(null)
    const [transcript, setTranscript] = useState<TranscriptResult | null>(null)
    const [speakerMapping, setSpeakerMapping] = useState<Record<string, string>>({})
    const [meetingInfo, setMeetingInfo] = useState({
        title: '',
        date: new Date().toISOString().split('T')[0],
        time: '',
        location: '',
    })
    const [outputPrefs, setOutputPrefs] = useState({ tone: 'Formal', length: 'Standard' })
    const [generatedMinutes, setGeneratedMinutes] = useState<string>('')
    const [error, setError] = useState<string>('')
    const [serviceAvailable, setServiceAvailable] = useState<boolean | null>(null)

    // Check service availability on mount
    useState(() => {
        checkVoiceServiceStatus()
            .then(res => setServiceAvailable(res.available))
            .catch(() => setServiceAvailable(false))
    })

    // File selection handler
    const handleFileSelect = useCallback((selectedFile: File) => {
        // Validate format
        const ext = '.' + selectedFile.name.split('.').pop()?.toLowerCase()
        if (!SUPPORTED_FORMATS.includes(ext)) {
            setError(`Unsupported format. Supported: ${SUPPORTED_FORMATS.join(', ')}`)
            return
        }

        // Validate size
        if (selectedFile.size > MAX_FILE_SIZE_MB * 1024 * 1024) {
            setError(`File too large. Maximum size: ${MAX_FILE_SIZE_MB}MB`)
            return
        }

        setFile(selectedFile)
        setError('')
    }, [])

    // Drag and drop handlers
    const handleDrop = useCallback((e: React.DragEvent) => {
        e.preventDefault()
        const droppedFile = e.dataTransfer.files[0]
        if (droppedFile) handleFileSelect(droppedFile)
    }, [handleFileSelect])

    // Upload and transcribe
    const handleUpload = useCallback(async () => {
        if (!file) return

        setStep('processing')
        setError('')

        try {
            const result = await uploadForTranscription(file)

            if (result.status === 'success' && result.data) {
                setTranscript(result.data)
                // Initialize speaker mapping
                const mapping: Record<string, string> = {}
                result.data.speakers?.forEach((speaker, idx) => {
                    mapping[speaker] = `Speaker ${idx + 1}`
                })
                setSpeakerMapping(mapping)
                setStep('review')
            } else {
                setError(result.message || 'Transcription failed')
                setStep('upload')
            }
        } catch (err) {
            setError('Failed to connect to transcription service. Please try again.')
            setStep('upload')
        }
    }, [file])

    // Update speaker name
    const updateSpeakerName = useCallback((speakerId: string, name: string) => {
        setSpeakerMapping(prev => ({ ...prev, [speakerId]: name }))
    }, [])

    // Generate minutes
    const handleGenerate = useCallback(async () => {
        if (!transcript) return

        setStep('generating')
        setError('')

        try {
            const result = await generateFromTranscript({
                transcriptData: transcript,
                speakerMapping,
                tone: outputPrefs.tone,
                length: outputPrefs.length,
                meetingTitle: meetingInfo.title,
                meetingDate: meetingInfo.date,
                meetingTime: meetingInfo.time,
                meetingLocation: meetingInfo.location,
            })

            if (result.status === 'success' && result.content) {
                setGeneratedMinutes(result.content)
                setStep('done')
            } else {
                setError(result.message || 'Generation failed')
                setStep('review')
            }
        } catch (err) {
            setError('Failed to generate minutes. Please try again.')
            setStep('review')
        }
    }, [transcript, speakerMapping, outputPrefs, meetingInfo])

    // Open in editor
    const handleOpenEditor = useCallback(() => {
        navigate('/minutes/editor', { state: { content: generatedMinutes } })
    }, [navigate, generatedMinutes])

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
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
                <TechSupportButton />
            </motion.header>

            {/* Page Title */}
            <motion.div
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="max-w-6xl mx-auto text-center mb-8"
            >
                <motion.div
                    animate={{ scale: [1, 1.1, 1] }}
                    transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                    className="text-purple-400 mb-4 inline-block text-5xl"
                >
                    🎙️
                </motion.div>
                <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                    Voice Mode
                </h1>
                <p className="text-slate-400 mt-2">
                    Upload a recording and let AI transcribe and generate minutes
                </p>
            </motion.div>

            <main className="max-w-6xl mx-auto">

                {/* Service unavailable warning */}
                {serviceAvailable === false && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="glass-card p-4 mb-6 border-l-4 border-amber-500"
                    >
                        <div className="flex gap-3">
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5 text-amber-500 flex-shrink-0 mt-0.5">
                                <path fillRule="evenodd" d="M8.485 2.495c.673-1.167 2.357-1.167 3.03 0l6.28 10.875c.673 1.167-.17 2.625-1.516 2.625H3.72c-1.347 0-2.189-1.458-1.515-2.625L8.485 2.495ZM10 5a.75.75 0 0 1 .75.75v3.5a.75.75 0 0 1-1.5 0v-3.5A.75.75 0 0 1 10 5Zm0 9a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z" clipRule="evenodd" />
                            </svg>
                            <div>
                                <p className="text-amber-200 font-medium">Transcription Service Unavailable</p>
                                <p className="text-slate-400 text-sm mt-1">
                                    The transcription service is not currently running. Please ensure it's started and try again.
                                </p>
                            </div>
                        </div>
                    </motion.div>
                )}

                {/* Progress Steps */}
                <div className="flex items-center justify-center gap-4 mb-8">
                    {['Upload', 'Processing', 'Review', 'Generate'].map((label, idx) => {
                        const stepOrder = ['upload', 'processing', 'review', 'generating']
                        const isActive = stepOrder.indexOf(step) >= idx || step === 'done'
                        const isCurrent = stepOrder[idx] === step || (step === 'done' && idx === 3)
                        return (
                            <div key={label} className="flex items-center gap-3">
                                <div
                                    className={`w-8 h-8 rounded-full flex items-center justify-center text-sm font-bold transition-all ${isCurrent
                                        ? 'bg-purple-500 text-white scale-110'
                                        : isActive
                                            ? 'bg-purple-500/30 text-purple-300'
                                            : 'bg-slate-700 text-slate-500'
                                        }`}
                                >
                                    {step === 'done' && idx === 3 ? '✓' : idx + 1}
                                </div>
                                <span className={`hidden sm:block text-sm ${isCurrent ? 'text-purple-300' : 'text-slate-500'}`}>
                                    {label}
                                </span>
                                {idx < 3 && <div className="w-12 h-0.5 bg-slate-700" />}
                            </div>
                        )
                    })}
                </div>

                {/* Error Display */}
                <AnimatePresence>
                    {error && (
                        <motion.div
                            initial={{ opacity: 0, y: -10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0 }}
                            className="glass-card p-4 mb-6 border-l-4 border-red-500"
                        >
                            <p className="text-red-400">{error}</p>
                        </motion.div>
                    )}
                </AnimatePresence>

                {/* Upload Step */}
                {step === 'upload' && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="glass-card p-8"
                    >
                        <div
                            className={`border-2 border-dashed rounded-xl p-12 text-center transition-colors ${file ? 'border-purple-500/50 bg-purple-500/5' : 'border-slate-600 hover:border-purple-500/30'
                                }`}
                            onDrop={handleDrop}
                            onDragOver={e => e.preventDefault()}
                        >
                            {file ? (
                                <div className="space-y-4">
                                    <div className="mx-auto w-16 h-16 rounded-full bg-purple-500/20 flex items-center justify-center">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 text-purple-400">
                                            <path fillRule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12Zm13.36-1.814a.75.75 0 1 0-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 0 0-1.06 1.06l2.25 2.25a.75.75 0 0 0 1.14-.094l3.75-5.25Z" clipRule="evenodd" />
                                        </svg>
                                    </div>
                                    <div>
                                        <p className="text-white font-medium text-lg">{file.name}</p>
                                        <p className="text-slate-400 text-sm mt-1">
                                            {(file.size / 1024 / 1024).toFixed(1)} MB
                                        </p>
                                    </div>
                                    <button
                                        onClick={() => setFile(null)}
                                        className="text-slate-400 hover:text-red-400 transition-colors text-sm"
                                    >
                                        Remove and choose another
                                    </button>
                                </div>
                            ) : (
                                <div className="space-y-4">
                                    <div className="mx-auto w-16 h-16 rounded-full bg-slate-700 flex items-center justify-center">
                                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-8 h-8 text-slate-400">
                                            <path fillRule="evenodd" d="M10.5 3.75a6 6 0 0 0-5.98 6.496A5.25 5.25 0 0 0 6.75 20.25H18a4.5 4.5 0 0 0 2.206-8.423 3.75 3.75 0 0 0-4.133-4.303A6.001 6.001 0 0 0 10.5 3.75Zm2.25 6a.75.75 0 0 0-1.5 0v4.94l-1.72-1.72a.75.75 0 0 0-1.06 1.06l3 3a.75.75 0 0 0 1.06 0l3-3a.75.75 0 1 0-1.06-1.06l-1.72 1.72V9.75Z" clipRule="evenodd" />
                                        </svg>
                                    </div>
                                    <div>
                                        <p className="text-white font-medium text-lg">Drop your audio file here</p>
                                        <p className="text-slate-400 text-sm mt-1">or click to browse</p>
                                    </div>
                                    <p className="text-slate-500 text-xs">
                                        Supports: MP3, WAV, M4A, FLAC, OGG • Max {MAX_FILE_SIZE_MB}MB • Up to 2 hours
                                    </p>
                                </div>
                            )}
                            <input
                                ref={fileInputRef}
                                type="file"
                                accept={SUPPORTED_FORMATS.join(',')}
                                onChange={e => e.target.files?.[0] && handleFileSelect(e.target.files[0])}
                                className="absolute inset-0 opacity-0 cursor-pointer"
                                style={{ position: 'absolute', top: 0, left: 0, right: 0, bottom: 0 }}
                            />
                        </div>

                        {file && (
                            <motion.button
                                initial={{ opacity: 0 }}
                                animate={{ opacity: 1 }}
                                onClick={handleUpload}
                                disabled={serviceAvailable === false}
                                className="mt-6 btn-primary w-full text-lg py-4 disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                Start Transcription →
                            </motion.button>
                        )}
                    </motion.div>
                )}

                {/* Processing Step */}
                {step === 'processing' && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="glass-card p-12 text-center"
                    >
                        <motion.div
                            animate={{ rotate: 360 }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
                            className="mx-auto w-16 h-16 rounded-full border-4 border-purple-500/30 border-t-purple-500"
                        />
                        <h2 className="text-xl font-bold text-white mt-6">Transcribing Your Recording</h2>
                        <p className="text-slate-400 mt-2">
                            This may take a few minutes depending on the recording length...
                        </p>
                        <div className="mt-6 space-y-2 text-sm text-slate-500">
                            <p>🎧 Analyzing audio...</p>
                            <p>🗣️ Detecting speakers...</p>
                            <p>✍️ Generating transcript...</p>
                        </div>
                    </motion.div>
                )}

                {/* Review Step */}
                {step === 'review' && transcript && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="space-y-6"
                    >
                        {/* Transcript Info */}
                        <div className="glass-card p-6">
                            <div className="flex flex-wrap gap-6 text-sm">
                                <div>
                                    <span className="text-slate-500">Duration:</span>
                                    <span className="text-white ml-2">{formatDuration(transcript.duration)}</span>
                                </div>
                                <div>
                                    <span className="text-slate-500">Speakers:</span>
                                    <span className="text-white ml-2">{transcript.speakers?.length || 0}</span>
                                </div>
                                <div>
                                    <span className="text-slate-500">Language:</span>
                                    <span className="text-white ml-2 uppercase">{transcript.language}</span>
                                </div>
                                <div>
                                    <span className="text-slate-500">Processing Time:</span>
                                    <span className="text-white ml-2">{transcript.processingTimeSeconds?.toFixed(1)}s</span>
                                </div>
                            </div>
                        </div>

                        <div className="grid lg:grid-cols-3 gap-6">
                            {/* Speaker Mapping */}
                            <div className="glass-card p-6">
                                <h3 className="text-lg font-bold text-white mb-4">Name the Speakers</h3>
                                <div className="space-y-3">
                                    {transcript.speakers?.map((speaker, idx) => (
                                        <div key={speaker}>
                                            <label className="text-sm text-slate-400">{speaker}</label>
                                            <input
                                                type="text"
                                                value={speakerMapping[speaker] || ''}
                                                onChange={e => updateSpeakerName(speaker, e.target.value)}
                                                placeholder={`Speaker ${idx + 1}`}
                                                className="input-field mt-1"
                                            />
                                        </div>
                                    ))}
                                </div>
                            </div>

                            {/* Meeting Info */}
                            <div className="glass-card p-6">
                                <h3 className="text-lg font-bold text-white mb-4">Meeting Details</h3>
                                <div className="space-y-3">
                                    <div>
                                        <label className="text-sm text-slate-400">Title</label>
                                        <input
                                            type="text"
                                            value={meetingInfo.title}
                                            onChange={e => setMeetingInfo(prev => ({ ...prev, title: e.target.value }))}
                                            placeholder="Team Standup"
                                            className="input-field mt-1"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-sm text-slate-400">Date</label>
                                        <input
                                            type="date"
                                            value={meetingInfo.date}
                                            onChange={e => setMeetingInfo(prev => ({ ...prev, date: e.target.value }))}
                                            className="input-field mt-1"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-sm text-slate-400">Time</label>
                                        <input
                                            type="time"
                                            value={meetingInfo.time}
                                            onChange={e => setMeetingInfo(prev => ({ ...prev, time: e.target.value }))}
                                            className="input-field mt-1"
                                        />
                                    </div>
                                    <div>
                                        <label className="text-sm text-slate-400">Location</label>
                                        <input
                                            type="text"
                                            value={meetingInfo.location}
                                            onChange={e => setMeetingInfo(prev => ({ ...prev, location: e.target.value }))}
                                            placeholder="Conference Room A"
                                            className="input-field mt-1"
                                        />
                                    </div>
                                </div>
                            </div>

                            {/* Output Preferences */}
                            <div className="glass-card p-6">
                                <h3 className="text-lg font-bold text-white mb-4">Output Style</h3>
                                <div className="space-y-3">
                                    <div>
                                        <label className="text-sm text-slate-400">Tone</label>
                                        <select
                                            value={outputPrefs.tone}
                                            onChange={e => setOutputPrefs(prev => ({ ...prev, tone: e.target.value }))}
                                            className="input-field mt-1"
                                        >
                                            <option value="Formal">Formal</option>
                                            <option value="Executive">Executive</option>
                                            <option value="Technical">Technical</option>
                                        </select>
                                    </div>
                                    <div>
                                        <label className="text-sm text-slate-400">Length</label>
                                        <select
                                            value={outputPrefs.length}
                                            onChange={e => setOutputPrefs(prev => ({ ...prev, length: e.target.value }))}
                                            className="input-field mt-1"
                                        >
                                            <option value="Summary">Summary</option>
                                            <option value="Standard">Standard</option>
                                            <option value="Detailed">Detailed</option>
                                        </select>
                                    </div>
                                </div>
                            </div>
                        </div>

                        {/* Transcript Preview */}
                        <div className="glass-card p-6">
                            <h3 className="text-lg font-bold text-white mb-4">Transcript Preview</h3>
                            <div className="max-h-96 overflow-y-auto space-y-3 pr-2">
                                {transcript.segments?.map((seg, idx) => (
                                    <div key={idx} className="flex gap-3">
                                        <span className="text-xs text-slate-500 font-mono w-12 flex-shrink-0">
                                            {formatTime(seg.start)}
                                        </span>
                                        <span className="text-sm font-semibold text-purple-400 w-28 flex-shrink-0">
                                            {speakerMapping[seg.speaker] || seg.speaker}
                                        </span>
                                        <p className="text-slate-300 text-sm">{seg.text}</p>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Generate Button */}
                        <button onClick={handleGenerate} className="btn-primary w-full text-lg py-4">
                            Generate Meeting Minutes →
                        </button>
                    </motion.div>
                )}

                {/* Generating Step */}
                {step === 'generating' && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="glass-card p-12 text-center"
                    >
                        <motion.div
                            animate={{ rotate: 360 }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'linear' }}
                            className="mx-auto w-16 h-16 rounded-full border-4 border-purple-500/30 border-t-purple-500"
                        />
                        <h2 className="text-xl font-bold text-white mt-6">Generating Meeting Minutes</h2>
                        <p className="text-slate-400 mt-2">
                            AI is analyzing the transcript and creating professional minutes...
                        </p>
                    </motion.div>
                )}

                {/* Done Step */}
                {step === 'done' && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="space-y-6"
                    >
                        <div className="glass-card p-6">
                            <div className="flex items-center gap-3 text-green-400 mb-4">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
                                    <path fillRule="evenodd" d="M2.25 12c0-5.385 4.365-9.75 9.75-9.75s9.75 4.365 9.75 9.75-4.365 9.75-9.75 9.75S2.25 17.385 2.25 12Zm13.36-1.814a.75.75 0 1 0-1.22-.872l-3.236 4.53L9.53 12.22a.75.75 0 0 0-1.06 1.06l2.25 2.25a.75.75 0 0 0 1.14-.094l3.75-5.25Z" clipRule="evenodd" />
                                </svg>
                                <h3 className="text-lg font-bold">Minutes Generated Successfully!</h3>
                            </div>
                            <div className="bg-slate-900/50 rounded-lg p-4 max-h-96 overflow-y-auto">
                                <pre className="text-slate-300 text-sm whitespace-pre-wrap font-mono">
                                    {generatedMinutes}
                                </pre>
                            </div>
                        </div>

                        <div className="flex gap-4">
                            <button
                                onClick={handleOpenEditor}
                                className="btn-primary flex-1 py-4"
                            >
                                Open in Editor
                            </button>
                            <button
                                onClick={() => navigate('/minutes/new')}
                                className="btn-secondary flex-1 py-4"
                            >
                                Create Another
                            </button>
                        </div>
                    </motion.div>
                )}
            </main>
        </div>
    )
}
