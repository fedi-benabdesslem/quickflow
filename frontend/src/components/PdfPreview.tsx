import { motion, AnimatePresence } from 'framer-motion'

interface PdfPreviewProps {
    isOpen: boolean
    onClose: () => void
    pdfUrl: string | null
    title?: string
    onSend?: () => void
    loading?: boolean
}

/**
 * Modal component for previewing PDF documents.
 */
export default function PdfPreview({
    isOpen,
    onClose,
    pdfUrl,
    title = 'PDF Preview',
    onSend,
    loading = false,
}: PdfPreviewProps) {
    if (!isOpen) return null

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm"
                onClick={onClose}
            >
                <motion.div
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    onClick={(e) => e.stopPropagation()}
                    className="relative w-full max-w-4xl h-[80vh] glass-card p-0 overflow-hidden"
                >
                    {/* Header */}
                    <div className="flex items-center justify-between px-6 py-4 border-b border-slate-700/50">
                        <h2 className="text-xl font-semibold text-slate-200">{title}</h2>
                        <button
                            onClick={onClose}
                            className="p-2 text-slate-400 hover:text-slate-200 transition-colors"
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
                                <path fillRule="evenodd" d="M5.47 5.47a.75.75 0 011.06 0L12 10.94l5.47-5.47a.75.75 0 111.06 1.06L13.06 12l5.47 5.47a.75.75 0 11-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 01-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 010-1.06z" clipRule="evenodd" />
                            </svg>
                        </button>
                    </div>

                    {/* PDF Content */}
                    <div className="h-[calc(100%-8rem)] overflow-hidden">
                        {pdfUrl ? (
                            <iframe
                                src={pdfUrl}
                                className="w-full h-full border-0"
                                title="PDF Preview"
                            />
                        ) : (
                            <div className="flex items-center justify-center h-full">
                                <div className="text-center text-slate-400">
                                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-16 h-16 mx-auto mb-4 opacity-50">
                                        <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0016.5 9h-1.875a1.875 1.875 0 01-1.875-1.875V5.25A3.75 3.75 0 009 1.5H5.625zM7.5 15a.75.75 0 01.75-.75h7.5a.75.75 0 010 1.5h-7.5A.75.75 0 017.5 15zm.75 2.25a.75.75 0 000 1.5H12a.75.75 0 000-1.5H8.25z" clipRule="evenodd" />
                                        <path d="M12.971 1.816A5.23 5.23 0 0114.25 5.25v1.875c0 .207.168.375.375.375H16.5a5.23 5.23 0 013.434 1.279 9.768 9.768 0 00-6.963-6.963z" />
                                    </svg>
                                    <p>No PDF to preview</p>
                                </div>
                            </div>
                        )}
                    </div>

                    {/* Footer */}
                    <div className="absolute bottom-0 left-0 right-0 flex items-center justify-end gap-4 px-6 py-4 bg-slate-900/80 border-t border-slate-700/50">
                        <button
                            onClick={onClose}
                            className="btn-secondary"
                            disabled={loading}
                        >
                            Close
                        </button>
                        {onSend && (
                            <button
                                onClick={onSend}
                                className="btn-primary"
                                disabled={loading}
                            >
                                {loading ? (
                                    <span className="flex items-center gap-2">
                                        <span className="spinner" />
                                        Sending...
                                    </span>
                                ) : (
                                    'Send Email'
                                )}
                            </button>
                        )}
                    </div>
                </motion.div>
            </motion.div>
        </AnimatePresence>
    )
}
