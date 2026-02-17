import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import UserAvatar from '../components/UserAvatar'
import TechSupportButton from '../components/TechSupportButton'

export default function ProfilePage() {
    const navigate = useNavigate()
    const { user, updateProfile } = useAuth()
    const fileInputRef = useRef<HTMLInputElement>(null)

    const [displayName, setDisplayName] = useState(user?.username || '')
    const [avatarPreview, setAvatarPreview] = useState<string | null>(user?.avatarUrl || null)
    const [saving, setSaving] = useState(false)
    const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null)
    const [hasChanges, setHasChanges] = useState(false)

    const handleNameChange = (value: string) => {
        setDisplayName(value)
        setHasChanges(true)
        setFeedback(null)
    }

    const handlePhotoSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0]
        if (!file) return

        // Validate file type
        if (!file.type.startsWith('image/')) {
            setFeedback({ type: 'error', message: 'Please select an image file.' })
            return
        }

        // Validate file size (max 500KB for base64 storage in metadata)
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
            </motion.div>
        </div>
    )
}
