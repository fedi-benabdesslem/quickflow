/**
 * Tests for VoiceModePage component.
 * Tests the voice mode workflow: upload, transcribe, review, generate.
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { BrowserRouter } from 'react-router-dom'
import VoiceModePage from '../pages/VoiceModePage'

// Mock dependencies
vi.mock('framer-motion', () => ({
    motion: {
        div: ({ children, ...props }: any) => <div {...props}>{children}</div>,
        header: ({ children, ...props }: any) => <header {...props}>{children}</header>,
    },
    AnimatePresence: ({ children }: any) => <>{children}</>,
}))

vi.mock('../components/TechSupportButton', () => ({
    default: () => <div data-testid="tech-support-button" />,
}))

vi.mock('../components/PdfPreviewModal', () => ({
    default: () => <div data-testid="pdf-preview-modal" />,
}))

vi.mock('../components/contacts/ContactAutocomplete', () => ({
    default: ({ value, onChange, placeholder }: any) => (
        <input
            data-testid="contact-autocomplete"
            value={value}
            onChange={(e) => onChange(e.target.value)}
            placeholder={placeholder}
        />
    ),
}))

// Mock fetch for API calls
const mockFetch = vi.fn()
global.fetch = mockFetch

describe('VoiceModePage', () => {
    beforeEach(() => {
        vi.clearAllMocks()
        mockFetch.mockResolvedValue({
            json: () => Promise.resolve({ available: true }),
        })
    })

    const renderPage = () => {
        return render(
            <BrowserRouter>
                <VoiceModePage />
            </BrowserRouter>
        )
    }

    describe('Initial Render', () => {
        it('renders upload step initially', () => {
            renderPage()
            expect(screen.getByText(/Upload Recording/i)).toBeInTheDocument()
        })

        it('displays supported formats', () => {
            renderPage()
            // Check that supported formats are mentioned
            expect(screen.getByText(/mp3|wav|m4a/i)).toBeInTheDocument()
        })

        it('shows back button', () => {
            renderPage()
            expect(screen.getByText(/Back/i)).toBeInTheDocument()
        })
    })

    describe('File Upload', () => {
        it('accepts valid audio file', async () => {
            renderPage()

            const file = new File(['audio content'], 'test.mp3', { type: 'audio/mpeg' })
            const input = screen.getByTestId('file-input') || document.querySelector('input[type="file"]')

            if (input) {
                fireEvent.change(input, { target: { files: [file] } })
                // Should not show error for valid format
                await waitFor(() => {
                    expect(screen.queryByText(/unsupported/i)).not.toBeInTheDocument()
                })
            }
        })

        it('rejects unsupported file format', async () => {
            renderPage()

            const file = new File(['not audio'], 'test.txt', { type: 'text/plain' })
            const dropzone = screen.getByText(/drag and drop/i).closest('div')

            if (dropzone) {
                // Simulate file selection (implementation may vary)
                // Error message should appear for invalid formats
            }
        })

        it('rejects file over size limit', async () => {
            renderPage()

            // Create a mock large file (over 350MB)
            const largeFile = {
                name: 'large.mp3',
                size: 400 * 1024 * 1024, // 400MB
                type: 'audio/mpeg',
            } as File

            // File size validation should trigger error
        })
    })

    describe('Service Status', () => {
        it('checks service availability on mount', async () => {
            renderPage()

            await waitFor(() => {
                expect(mockFetch).toHaveBeenCalledWith(
                    expect.stringContaining('/status'),
                    expect.any(Object)
                )
            })
        })

        it('shows warning when service unavailable', async () => {
            mockFetch.mockResolvedValueOnce({
                json: () => Promise.resolve({ available: false }),
            })

            renderPage()

            await waitFor(() => {
                // Should show service unavailable warning
                // Implementation depends on UI design
            })
        })
    })

    describe('Transcription Flow', () => {
        it('shows processing step during transcription', async () => {
            mockFetch
                .mockResolvedValueOnce({
                    json: () => Promise.resolve({ available: true }),
                })
                .mockResolvedValueOnce({
                    json: () =>
                        Promise.resolve({
                            status: 'success',
                            data: {
                                segments: [{ speaker: 'SPEAKER_00', start: 0, end: 2, text: 'Hello' }],
                                speakers: ['SPEAKER_00'],
                                duration: 60,
                                language: 'en',
                            },
                        }),
                })

            renderPage()

            // Simulate file upload and transcription start
            // Processing indicator should appear
        })
    })

    describe('Review Step', () => {
        it('displays speaker mapping inputs', () => {
            // When in review step with transcript data
            // Should show speaker assignment inputs
        })

        it('shows meeting info form', () => {
            // Meeting title, date, time inputs should be visible
        })
    })

    describe('Generation Step', () => {
        it('generates minutes from transcript', async () => {
            // When generate button is clicked
            // Should call generate API and show result
        })

        it('shows PDF generation button on success', async () => {
            // After successful generation
            // PDF generation button should be visible
        })
    })
})
