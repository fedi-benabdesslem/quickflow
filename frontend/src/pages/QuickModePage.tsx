import { useState, useRef, DragEvent, ChangeEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import TechSupportButton from '../components/TechSupportButton'

import { extractFromNotes, extractFromFile } from '../lib/api'
import type { QuickModeData } from '../types'

type TabType = 'paste' | 'upload'

const SUPPORTED_FORMATS = ['.txt', '.docx', '.pdf', '.md']
const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB
const MAX_CHARS = 10000
const MIN_CHARS = 50

export default function QuickModePage() {
    const [activeTab, setActiveTab] = useState<TabType>('paste')
    const [formData, setFormData] = useState<QuickModeData>({
        content: '',
        file: undefined,
        date: '',
        time: '',
    })
    const [isDragOver, setIsDragOver] = useState(false)
    const [fileError, setFileError] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState('')
    const fileInputRef = useRef<HTMLInputElement>(null)


    const navigate = useNavigate()



    const isValidContent = formData.content.length >= MIN_CHARS
    const isValidFile = formData.file !== undefined
    const canSubmit = activeTab === 'paste' ? isValidContent : isValidFile

    const validateFile = (file: File): string | null => {
        const extension = '.' + file.name.split('.').pop()?.toLowerCase()
        if (!SUPPORTED_FORMATS.includes(extension)) {
            return `Invalid file type. Supported: ${SUPPORTED_FORMATS.join(', ')}`
        }
        if (file.size > MAX_FILE_SIZE) {
            return 'File too large. Maximum size: 10 MB'
        }
        return null
    }

    const handleFileDrop = (e: DragEvent<HTMLDivElement>) => {
        e.preventDefault()
        setIsDragOver(false)
        const file = e.dataTransfer.files[0]
        if (file) {
            const error = validateFile(file)
            if (error) {
                setFileError(error)
            } else {
                setFileError('')
                setFormData(prev => ({ ...prev, file }))
            }
        }
    }

    const handleFileChange = (e: ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (file) {
            const error = validateFile(file)
            if (error) {
                setFileError(error)
            } else {
                setFileError('')
                setFormData(prev => ({ ...prev, file }))
            }
        }
    }

    const handleRemoveFile = () => {
        setFormData(prev => ({ ...prev, file: undefined }))
        setFileError('')
        if (fileInputRef.current) {
            fileInputRef.current.value = ''
        }
    }

    const handleSubmit = async () => {
        if (!canSubmit || loading) return
        setLoading(true)
        setError('')

        try {
            let result
            if (activeTab === 'paste') {
                result = await extractFromNotes(
                    formData.content,
                    formData.date || undefined,
                    formData.time || undefined
                )
            } else if (formData.file) {
                result = await extractFromFile(
                    formData.file,
                    formData.date || undefined,
                    formData.time || undefined
                )
            } else {
                throw new Error('No content to extract')
            }

            if (result.status === 'success' && result.data) {
                // Navigate to review page with extracted data
                navigate('/minutes/quick/review', {
                    state: {
                        extractedData: result.data,
                        originalContent: activeTab === 'paste' ? formData.content : formData.file?.name,
                        originalDate: formData.date,
                        originalTime: formData.time,
                    },
                })
            } else {
                setError(result.message || 'Extraction failed. Please try again.')
            }
        } catch (err) {
            console.error('Extraction error:', err)
            setError('AI service temporarily unavailable. Please try again later.')
        } finally {
            setLoading(false)
        }
    }

    const formatFileSize = (bytes: number): string => {
        if (bytes < 1024) return bytes + ' B'
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
        return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
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
                            Extracting Structure...
                        </h3>
                        <p className="text-slate-400 text-sm">
                            AI is analyzing your notes. This may take 10-20 seconds.
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
                <button onClick={() => navigate('/minutes/new')} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back</span>
                </button>
                <TechSupportButton />
            </motion.header>

            {/* Breadcrumb */}
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="max-w-3xl mx-auto mb-4"
            >
                <nav className="text-sm text-slate-500">
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/home')}>Home</span>
                    <span className="mx-2">›</span>
                    <span className="hover:text-slate-300 cursor-pointer" onClick={() => navigate('/minutes/new')}>Create Minutes</span>
                    <span className="mx-2">›</span>
                    <span className="text-blue-400">Quick Mode</span>
                </nav>
            </motion.div>

            {/* Main Content */}
            <motion.div
                initial={{ opacity: 0, y: 30 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ duration: 0.5 }}
                className="glass-card max-w-3xl mx-auto p-6 sm:p-8"
            >
                {/* Title */}
                <div className="text-center mb-8">
                    <motion.div
                        animate={{ rotate: [0, 10, -10, 0] }}
                        transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        className="text-yellow-400 mb-4 inline-block"
                    >
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                            <path fillRule="evenodd" d="M14.615 1.595a.75.75 0 0 1 .359.852L12.982 9.75h7.268a.75.75 0 0 1 .548 1.262l-10.5 11.25a.75.75 0 0 1-1.272-.71l1.992-7.302H3.75a.75.75 0 0 1-.548-1.262l10.5-11.25a.75.75 0 0 1 .913-.143Z" clipRule="evenodd" />
                        </svg>
                    </motion.div>
                    <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-yellow-400 to-orange-400 bg-clip-text text-transparent">
                        Quick Mode - Upload or Paste Notes
                    </h1>
                    <p className="text-slate-400 mt-2">
                        AI will extract structure from your meeting notes
                    </p>
                </div>

                {/* Error Message */}
                {error && (
                    <motion.div
                        initial={{ opacity: 0, y: -10 }}
                        animate={{ opacity: 1, y: 0 }}
                        className="message-error mb-6"
                    >
                        {error}
                        <button
                            onClick={() => setError('')}
                            className="ml-2 text-red-300 hover:text-red-100"
                        >
                            ✕
                        </button>
                    </motion.div>
                )}

                {/* Tab Toggle */}
                <div className="flex bg-slate-900/50 rounded-lg p-1 mb-6">
                    <button
                        onClick={() => setActiveTab('paste')}
                        className={`flex-1 py-3 px-4 rounded-md text-sm font-medium transition-all ${activeTab === 'paste'
                            ? 'bg-blue-600 text-white'
                            : 'text-slate-400 hover:text-white'
                            }`}
                    >
                        Paste Notes
                    </button>
                    <button
                        onClick={() => setActiveTab('upload')}
                        className={`flex-1 py-3 px-4 rounded-md text-sm font-medium transition-all ${activeTab === 'upload'
                            ? 'bg-blue-600 text-white'
                            : 'text-slate-400 hover:text-white'
                            }`}
                    >
                        Upload File
                    </button>
                </div>

                {/* Paste Tab Content */}
                {activeTab === 'paste' && (
                    <motion.div
                        initial={{ opacity: 0, x: -20 }}
                        animate={{ opacity: 1, x: 0 }}
                        className="space-y-4"
                    >
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Paste your meeting notes here
                            </label>
                            <div className="relative">
                                <textarea
                                    value={formData.content}
                                    onChange={(e) => setFormData(prev => ({
                                        ...prev,
                                        content: e.target.value.slice(0, MAX_CHARS)
                                    }))}
                                    placeholder={`You can include:
• Meeting details (date, time, location)
• Participant names
• Topics discussed
• Decisions made
• Action items`}
                                    className="input-nebula min-h-[400px] resize-y font-mono text-sm"
                                    disabled={loading}
                                />
                                <div className="absolute bottom-3 right-3 text-sm text-slate-500">
                                    <span className={formData.content.length >= MAX_CHARS * 0.9 ? 'text-yellow-400' : ''}>
                                        {formData.content.length.toLocaleString()}
                                    </span>
                                    {' / '}
                                    {MAX_CHARS.toLocaleString()} characters
                                </div>
                            </div>
                            {formData.content.length > 0 && formData.content.length < MIN_CHARS && (
                                <p className="text-yellow-400 text-sm mt-2">
                                    Minimum {MIN_CHARS} characters required ({MIN_CHARS - formData.content.length} more needed)
                                </p>
                            )}
                        </div>
                    </motion.div>
                )}

                {/* Upload Tab Content */}
                {activeTab === 'upload' && (
                    <motion.div
                        initial={{ opacity: 0, x: 20 }}
                        animate={{ opacity: 1, x: 0 }}
                        className="space-y-4"
                    >
                        {!formData.file ? (
                            <div
                                onDragOver={(e) => { e.preventDefault(); setIsDragOver(true) }}
                                onDragLeave={() => setIsDragOver(false)}
                                onDrop={handleFileDrop}
                                onClick={() => fileInputRef.current?.click()}
                                className={`border-2 border-dashed rounded-xl p-12 text-center cursor-pointer transition-all ${isDragOver
                                    ? 'border-blue-400 bg-blue-500/10'
                                    : 'border-slate-600 hover:border-slate-500'
                                    }`}
                            >
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept={SUPPORTED_FORMATS.join(',')}
                                    onChange={handleFileChange}
                                    className="hidden"
                                />
                                <div className="text-4xl mb-4">📁</div>
                                <p className="text-white font-medium mb-2">
                                    Drag and drop your file here
                                </p>
                                <p className="text-slate-400 text-sm mb-4">
                                    or click to browse
                                </p>
                                <p className="text-slate-500 text-xs">
                                    Supported formats: {SUPPORTED_FORMATS.join(', ')}
                                </p>
                                <p className="text-slate-500 text-xs">
                                    Max file size: 10 MB
                                </p>
                            </div>
                        ) : (
                            <div className="border border-slate-600 rounded-xl p-6 bg-slate-900/30">
                                <div className="flex items-center gap-4">
                                    <div className="text-3xl text-green-400">✓</div>
                                    <div className="flex-1">
                                        <p className="text-white font-medium">{formData.file.name}</p>
                                        <p className="text-slate-400 text-sm">
                                            {formatFileSize(formData.file.size)} • Ready to process
                                        </p>
                                    </div>
                                </div>
                                <div className="flex gap-3 mt-4">
                                    <button
                                        onClick={handleRemoveFile}
                                        className="text-red-400 hover:text-red-300 text-sm"
                                    >
                                        Remove
                                    </button>
                                    <button
                                        onClick={() => fileInputRef.current?.click()}
                                        className="text-blue-400 hover:text-blue-300 text-sm"
                                    >
                                        Upload Different File
                                    </button>
                                </div>
                            </div>
                        )}
                        {fileError && (
                            <div className="message-error">{fileError}</div>
                        )}
                    </motion.div>
                )}

                {/* Optional Fields */}
                <div className="mt-8 pt-6 border-t border-slate-700/50">
                    <p className="text-sm text-slate-400 mb-4">
                        Optional: Provide date/time if not in your notes
                    </p>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Meeting Date (optional)
                            </label>
                            <input
                                type="date"
                                value={formData.date}
                                onChange={(e) => setFormData(prev => ({ ...prev, date: e.target.value }))}
                                className="input-nebula"
                                disabled={loading}
                            />
                        </div>
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Meeting Time (optional)
                            </label>
                            <input
                                type="time"
                                value={formData.time}
                                onChange={(e) => setFormData(prev => ({ ...prev, time: e.target.value }))}
                                className="input-nebula"
                                disabled={loading}
                            />
                        </div>
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="flex gap-4 mt-8">
                    <button
                        onClick={() => navigate('/minutes/new')}
                        className="btn-secondary flex-1"
                        disabled={loading}
                    >
                        Cancel
                    </button>
                    <button
                        onClick={handleSubmit}
                        className="btn-primary flex-1"
                        disabled={!canSubmit || loading}
                    >
                        {loading ? (
                            <span className="flex items-center justify-center gap-2">
                                <span className="spinner" />
                                Extracting...
                            </span>
                        ) : (
                            'Extract Structure →'
                        )}
                    </button>
                </div>
            </motion.div>
        </div>
    )
}
