import { useState, useRef, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import UserAvatar from '../components/UserAvatar'
import TechSupportButton from '../components/TechSupportButton'
import SmtpSetupModal from '../components/SmtpSetupModal'
import { getSmtpStatus, testSmtp, removeSmtp, type SmtpStatusResponse } from '../lib/api'

export default function ProfilePage() {
    const navigate = useNavigate()
    const { user, updateProfile } = useAuth()
    const fileInputRef = useRef<HTMLInputElement>(null)

    const [displayName, setDisplayName] = useState(user?.username || '')
    const [avatarPreview, setAvatarPreview] = useState<string | null>(user?.avatarUrl || null)
    const [saving, setSaving] = useState(false)
    const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
    const [hasChanges, setHasChanges] = useState(false)

    // SMTP state
    const [smtpStatus, setSmtpStatus] = useState<SmtpStatusResponse | null>(null)
    const [showSmtpModal, setShowSmtpModal] = useState(false)
    const [smtpModalMode, setSmtpModalMode] = useState<'setup' | 'update'>('setup')
    const [smtpLoading, setSmtpLoading] = useState(false)
    const [smtpFeedback, setSmtpFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)

    useEffect(() => {
        loadSmtpStatus()
    }, [])

    const loadSmtpStatus = async () => {
        const status = await getSmtpStatus()
        setSmtpStatus(status)
    }

    const handleNameChange = (value: string) => {
        setDisplayName(value)
        setHasChanges(true)
        setFeedback(null)
    }

    const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        if (!file.type.startsWith('image/')) {
            setFeedback({ type: 'error', message: 'Please select an image file.' })
            return
        }

        if (file.size > 500 * 1024) {
            setFeedback({ type: 'error', message: 'Image must be under 500KB. Please use a smaller image.' })
            return
        }

        const reader = new FileReader()
        reader.onload = (event) => {
            const dataUrl = event.target?.result as string
            setAvatarPreview(dataUrl)
            setHasChanges(true)
            setFeedback(null)
        }
        reader.readAsDataURL(file)
    }

    const handleRemovePhoto = () => {
        setAvatarPreview(null)
        setHasChanges(true)
        setFeedback(null)
        if (fileInputRef.current) {
            fileInputRef.current.value = ''
        }
    }

    const handleSave = async () => {
        if (!displayName.trim()) {
            setFeedback({ type: 'error', message: 'Display name cannot be empty.' })
            return
        }

        setSaving(true)
        setFeedback(null)

        const updates: { username?: string; avatarUrl?: string } = {}

        if (displayName !== user?.username) {
            updates.username = displayName.trim()
        }

        if (avatarPreview !== (user?.avatarUrl || null)) {
            updates.avatarUrl = avatarPreview || ''
        }

        if (Object.keys(updates).length === 0) {
            setFeedback({ type: 'success', message: 'No changes to save.' })
            setSaving(false)
            return
        }

        const result = await updateProfile(updates)

        if (result.success) {
            setFeedback({ type: 'success', message: 'Profile updated successfully!' })
            setHasChanges(false)
        } else {
            setFeedback({ type: 'error', message: result.error || 'Failed to update profile.' })
        }

        setSaving(false)
    }

    const handleTestSmtp = async () => {
        setSmtpLoading(true)
        setSmtpFeedback(null)
        const result = await testSmtp()
        if ((result as any).success) {
            setSmtpFeedback({ type: 'success', message: 'Test email sent! Check your inbox.' })
        } else {
            setSmtpFeedback({ type: 'error', message: (result as any).message || 'Failed to send test email.' })
        }
        setSmtpLoading(false)
    }

    const handleDisconnectSmtp = async () => {
        if (!confirm('Are you sure you want to disconnect email sending? You can set it up again later.')) return
        setSmtpLoading(true)
        setSmtpFeedback(null)
        const result = await removeSmtp()
        if ((result as any).success) {
            setSmtpFeedback({ type: 'success', message: 'Email configuration removed.' })
            await loadSmtpStatus()
        } else {
            setSmtpFeedback({ type: 'error', message: (result as any).message || 'Failed to remove configuration.' })
        }
        setSmtpLoading(false)
    }

    const handleSmtpConfigured = () => {
        loadSmtpStatus()
        setSmtpFeedback({ type: 'success', message: 'Email sending configured successfully!' })
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8"
            >
                <button onClick={() => navigate('/home')} className="btn-secondary">
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
                className="max-w-2xl mx-auto text-center mb-8"
            >
                <h1 className="text-2xl sm:text-3xl font-bold bg-gradient-to-r from-purple-400 to-pink-400 bg-clip-text text-transparent">
                    Edit Profile
                </h1>
                <p className="text-slate-400 mt-2">
                    Update your display name and profile photo
                </p>
            </motion.div>

            {/* Profile Form */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.2 }}
                className="max-w-lg mx-auto"
            >
                <div className="glass-card p-8">
                    {/* Avatar Section */}
                    <div className="flex flex-col items-center mb-8">
                        <UserAvatar
                            photoUrl={avatarPreview}
                            fullName={displayName || user?.email}
                            size="xl"
                            className="mb-4"
                        />

                        <div className="flex items-center gap-3">
                            <button
                                onClick={() => fileInputRef.current?.click()}
                                className="px-4 py-2 text-sm bg-purple-600 hover:bg-purple-500 text-white rounded-lg transition-colors"
                            >
                                {avatarPreview ? 'Change Photo' : 'Upload Photo'}
                            </button>

                            {avatarPreview && (
                                <button
                                    onClick={handleRemovePhoto}
                                    className="px-4 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg transition-colors"
                                >
                                    Remove
                                </button>
                            )}
                        </div>

                        <p className="text-xs text-slate-500 mt-2">
                            JPG, PNG or GIF • Max 500KB
                        </p>

                        <input
                            ref={fileInputRef}
                            type="file"
                            accept="image/*"
                            onChange={handlePhotoSelect}
                            className="hidden"
                        />
                    </div>

                    {/* Name Field */}
                    <div className="space-y-4">
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Display Name
                            </label>
                            <input
                                type="text"
                                value={displayName}
                                onChange={(e) => handleNameChange(e.target.value)}
                                placeholder="Enter your name"
                                className="input-nebula w-full"
                            />
                        </div>

                        {/* Email (read-only) */}
                        <div>
                            <label className="block text-sm font-medium text-slate-300 mb-2">
                                Email
                            </label>
                            <input
                                type="email"
                                value={user?.email || ''}
                                disabled
                                className="input-nebula w-full opacity-50 cursor-not-allowed"
                            />
                            <p className="text-xs text-slate-500 mt-1">
                                Email cannot be changed here
                            </p>
                        </div>
                    </div>

                    {/* Feedback Message */}
                    {feedback && (
                        <motion.div
                            initial={{ opacity: 0, y: -10 }}
                            animate={{ opacity: 1, y: 0 }}
                            className={`mt-6 p-3 rounded-lg text-sm ${feedback.type === 'success'
                                ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
                                : 'bg-red-500/10 border border-red-500/20 text-red-400'
                                }`}
                        >
                            {feedback.message}
                        </motion.div>
                    )}

                    {/* Save Button */}
                    <motion.button
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                        onClick={handleSave}
                        disabled={saving || !hasChanges}
                        className="mt-6 w-full btn-primary py-3 text-base disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                        {saving ? (
                            <>
                                <div className="spinner spinner-small" />
                                Saving...
                            </>
                        ) : (
                            'Save Changes'
                        )}
                    </motion.button>
                </div>

                {/* SMTP Email Configuration Section */}
                {smtpStatus && !smtpStatus.isOAuth && (
                    <motion.div
                        initial={{ opacity: 0, y: 20 }}
                        animate={{ opacity: 1, y: 0 }}
                        transition={{ delay: 0.3 }}
                        className="glass-card p-8 mt-6"
                    >
                        <h2 className="text-lg font-semibold text-white mb-4 flex items-center gap-2">
                            ✉️ Email Sending Configuration
                        </h2>

                        <div className="space-y-3">
                            {/* Provider Info */}
                            <div className="flex items-center justify-between">
                                <span className="text-sm text-slate-400">Provider</span>
                                <span className="text-sm text-white font-medium">{smtpStatus.providerName}</span>
                            </div>

                            <div className="flex items-center justify-between">
                                <span className="text-sm text-slate-400">Status</span>
                                <span className={`text-sm font-medium ${smtpStatus.smtpConfigured ? 'text-emerald-400' : 'text-slate-500'}`}>
                                    {smtpStatus.smtpConfigured ? '✅ Connected' : '❌ Not configured'}
                                </span>
                            </div>

                            {/* SMTP Feedback */}
                            {smtpFeedback && (
                                <motion.div
                                    initial={{ opacity: 0, y: -10 }}
                                    animate={{ opacity: 1, y: 0 }}
                                    className={`p-3 rounded-lg text-sm ${smtpFeedback.type === 'success'
                                        ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-400'
                                        : 'bg-red-500/10 border border-red-500/20 text-red-400'
                                        }`}
                                >
                                    {smtpFeedback.message}
                                </motion.div>
                            )}

                            {/* Actions */}
                            {smtpStatus.smtpConfigured ? (
                                <div className="flex flex-wrap gap-2 pt-2">
                                    <button
                                        onClick={handleTestSmtp}
                                        disabled={smtpLoading}
                                        className="px-4 py-2 text-sm bg-purple-600 hover:bg-purple-500 text-white rounded-lg transition-colors disabled:opacity-50"
                                    >
                                        {smtpLoading ? 'Sending...' : 'Send Test Email'}
                                    </button>
                                    <button
                                        onClick={() => { setSmtpModalMode('update'); setShowSmtpModal(true) }}
                                        className="px-4 py-2 text-sm bg-white/5 hover:bg-white/10 text-slate-300 rounded-lg border border-white/10 transition-colors"
                                    >
                                        Update Password
                                    </button>
                                    <button
                                        onClick={handleDisconnectSmtp}
                                        disabled={smtpLoading}
                                        className="px-4 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg transition-colors"
                                    >
                                        Disconnect
                                    </button>
                                </div>
                            ) : smtpStatus.providerSupported && !smtpStatus.providerBlocked ? (
                                <button
                                    onClick={() => { setSmtpModalMode('setup'); setShowSmtpModal(true) }}
                                    className="mt-2 w-full px-4 py-3 text-sm bg-purple-600 hover:bg-purple-500 text-white rounded-lg transition-colors font-medium"
                                >
                                    Set Up Email Sending
                                </button>
                            ) : smtpStatus.providerBlocked ? (
                                <p className="text-sm text-yellow-500/80 pt-1">
                                    ⚠️ Your email provider does not support third-party email sending.
                                </p>
                            ) : (
                                <p className="text-sm text-slate-500 pt-1">
                                    Email sending is not available for your provider.
                                </p>
                            )}
                        </div>
                    </motion.div>
                )}
            </motion.div>

            {/* SMTP Setup Modal */}
            <SmtpSetupModal
                email={user?.email || ''}
                isOpen={showSmtpModal}
                onClose={() => setShowSmtpModal(false)}
                onConfigured={handleSmtpConfigured}
                mode={smtpModalMode}
            />
        </div>
    )
}

