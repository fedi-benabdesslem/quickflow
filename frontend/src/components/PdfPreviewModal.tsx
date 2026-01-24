import { useState, useEffect } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { Document, Page, pdfjs } from 'react-pdf'
import 'react-pdf/dist/Page/AnnotationLayer.css'
import 'react-pdf/dist/Page/TextLayer.css'
import api, { downloadPdf } from '../lib/api'

// Set worker source - must match pdfjs-dist version bundled with react-pdf
// Using unpkg CDN which is more reliable for ES modules
pdfjs.GlobalWorkerOptions.workerSrc = `https://unpkg.com/pdfjs-dist@${pdfjs.version}/build/pdf.worker.min.mjs`;

interface PdfPreviewModalProps {
    fileId: string | null
    filename: string
    isOpen: boolean
    onClose: () => void
}

export default function PdfPreviewModal({
    fileId,
    filename,
    isOpen,
    onClose,
}: PdfPreviewModalProps) {
    const [numPages, setNumPages] = useState<number | null>(null)
    const [pageNumber, setPageNumber] = useState(1)
    const [scale, setScale] = useState(1.0)
    const [loading, setLoading] = useState(true)
    const [pdfUrl, setPdfUrl] = useState<string | null>(null)
    const [error, setError] = useState<string | null>(null)

    // Fetch PDF when modal opens with fileId
    useEffect(() => {
        if (isOpen && fileId) {
            setPageNumber(1)
            setScale(1.0)
            setLoading(true)
            setError(null)

            // Clean up previous URL
            if (pdfUrl) {
                URL.revokeObjectURL(pdfUrl)
                setPdfUrl(null)
            }

            // Fetch PDF using authenticated api instance
            api.get(`/pdf/preview/${fileId}`, { responseType: 'blob' })
                .then((response) => {
                    // Create a Blob URL for react-pdf
                    const blob = new Blob([response.data], { type: 'application/pdf' })
                    const url = URL.createObjectURL(blob)
                    setPdfUrl(url)
                    setLoading(false)
                })
                .catch((err) => {
                    console.error('Failed to fetch PDF:', err)
                    setError('Failed to load PDF. Please try again.')
                    setLoading(false)
                })
        }

        // Cleanup on unmount
        return () => {
            if (pdfUrl) {
                URL.revokeObjectURL(pdfUrl)
            }
        }
    }, [isOpen, fileId])

    function onDocumentLoadSuccess({ numPages }: { numPages: number }) {
        setNumPages(numPages)
    }

    const changePage = (offset: number) => {
        setPageNumber(prev => Math.min(Math.max(1, prev + offset), numPages || 1))
    }

    const zoomIn = () => setScale(prev => Math.min(prev + 0.25, 2.5))
    const zoomOut = () => setScale(prev => Math.max(prev - 0.25, 0.5))

    const handleDownload = () => {
        if (fileId) {
            downloadPdf(fileId)
        }
    }

    if (!isOpen || !fileId) return null

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 backdrop-blur-sm p-4 sm:p-6"
            >
                <div className="w-full h-full max-w-6xl flex flex-col glass-card overflow-hidden">
                    {/* Header */}
                    <div className="flex justify-between items-center p-4 border-b border-slate-700/50 bg-slate-900/50">
                        <div className="flex items-center gap-3">
                            <div className="p-2 bg-red-500/20 rounded-lg text-red-400">
                                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
                                    <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0 0 16.5 9h-1.875a1.875 1.875 0 0 1-1.875-1.875V5.25A3.75 3.75 0 0 0 9 1.5H5.625Z" clipRule="evenodd" />
                                    <path d="M12.971 1.816A5.23 5.23 0 0 1 14.25 5.25v1.875c0 .207.168.375.375.375H16.5a5.23 5.23 0 0 1 3.434 1.279 9.768 9.768 0 0 0-6.963-6.963Z" />
                                </svg>
                            </div>
                            <div>
                                <h3 className="text-white font-semibold">PDF Preview</h3>
                                <p className="text-xs text-slate-400 max-w-[300px] truncate">{filename}</p>
                            </div>
                        </div>
                        <div className="flex items-center gap-3">
                            <button
                                onClick={handleDownload}
                                className="btn-primary py-2 px-4 text-sm"
                            >
                                ⬇ Download
                            </button>
                            <button
                                onClick={onClose}
                                className="p-2 hover:bg-slate-700/50 rounded-lg text-slate-400 hover:text-white transition-colors"
                            >
                                ✕
                            </button>
                        </div>
                    </div>

                    {/* Toolbar */}
                    <div className="flex justify-between items-center p-3 bg-slate-800/50 border-b border-slate-700/50 text-sm">
                        <div className="flex items-center gap-2">
                            <button
                                onClick={() => changePage(-1)}
                                disabled={pageNumber <= 1}
                                className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300 disabled:opacity-50"
                            >
                                ◀
                            </button>
                            <span className="text-slate-300">
                                Page {pageNumber} of {numPages || '--'}
                            </span>
                            <button
                                onClick={() => changePage(1)}
                                disabled={numPages === null || pageNumber >= numPages}
                                className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300 disabled:opacity-50"
                            >
                                ▶
                            </button>
                        </div>
                        <div className="flex items-center gap-2">
                            <button onClick={zoomOut} className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300">
                                −
                            </button>
                            <span className="text-slate-300 min-w-[3ch] text-center">
                                {Math.round(scale * 100)}%
                            </span>
                            <button onClick={zoomIn} className="p-1.5 hover:bg-slate-700 rounded-md text-slate-300">
                                +
                            </button>
                        </div>
                    </div>

                    {/* PDF Viewer */}
                    <div className="flex-1 overflow-auto bg-slate-900/80 relative">
                        {loading && (
                            <div className="absolute inset-0 flex items-center justify-center z-10">
                                <div className="spinner spinner-large" />
                            </div>
                        )}
                        {error && (
                            <div className="absolute inset-0 flex items-center justify-center z-10">
                                <div className="text-red-400 text-center">
                                    <p>{error}</p>
                                    <button
                                        onClick={() => window.location.reload()}
                                        className="mt-4 btn-secondary text-sm"
                                    >
                                        Retry
                                    </button>
                                </div>
                            </div>
                        )}
                        <div className="flex justify-center p-8 min-h-full">
                            {pdfUrl && (
                                <Document
                                    file={pdfUrl}
                                    onLoadSuccess={onDocumentLoadSuccess}
                                    onLoadError={(err) => {
                                        console.error('PDF load error:', err)
                                        setError('Failed to render PDF.')
                                    }}
                                    loading={<div className="text-slate-400">Rendering PDF...</div>}
                                    className="pdf-document"
                                >
                                    <Page
                                        pageNumber={pageNumber}
                                        scale={scale}
                                        renderTextLayer={false}
                                        renderAnnotationLayer={false}
                                        className="shadow-2xl"
                                    />
                                </Document>
                            )}
                        </div>
                    </div>

                    {/* Footer Actions */}
                    <div className="p-4 border-t border-slate-700/50 bg-slate-900/50 flex justify-between items-center">
                        <button onClick={onClose} className="btn-secondary py-2 px-6">
                            Back to Edit
                        </button>
                        <div className="flex gap-3">
                            <div className="relative group">
                                <button className="btn-secondary py-2 px-6 opacity-60 cursor-not-allowed">
                                    Send via Email →
                                </button>
                                <div className="absolute bottom-full left-1/2 transform -translate-x-1/2 mb-2 px-3 py-1 bg-black text-white text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                                    Coming in Step 4
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </motion.div>
        </AnimatePresence>
    )
}
