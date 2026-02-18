import { useState } from 'react'
import { motion, AnimatePresence } from 'framer-motion'
import { configureSmtp, skipSmtpSetup } from '../lib/api'
import { getProviderByDomain, extractDomain } from '../lib/smtpProviders'

interface SmtpSetupModalProps {
    email: string
    isOpen: boolean
    onClose: () => void
    onConfigured: () => void
    mode?: 'setup' | 'update'
}

export default function SmtpSetupModal({ email, isOpen, onClose, onConfigured, mode = 'setup' }: SmtpSetupModalProps) {
    const [appPassword, setAppPassword] = useState('')
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)
    const [success, setSuccess] = useState(false)

    const domain = extractDomain(email)
    const provider = domain ? getProviderByDomain(domain) : undefined

    const handleConfigure = async () => {
        if (!appPassword.trim()) {
            setError('Please enter your app-specific password.')
            return
        }

        setLoading(true)
        setError(null)

        try {
            const result = await configureSmtp(appPassword.trim())
            if (result.status === 'error' || (result as any).success === false) {
                setError((result as any).message || 'Failed to configure email sending.')
            } else {
                setSuccess(true)
                setTimeout(() => {
                    onConfigured()
                    onClose()
                }, 1500)
            }
        } catch (err) {
            setError('An unexpected error occurred. Please try again.')
        } finally {
            setLoading(false)
        }
    }

    const handleSkip = async () => {
        await skipSmtpSetup()
        onClose()
    }

    if (!isOpen) return null

    // ProtonMail blocked view
    if (provider?.isBlocked) {
        return (
            <AnimatePresence>
                <motion.div
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    className="fixed inset-0 z-50 flex items-center justify-center p-4"
                    style={{ backgroundColor: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(8px)' }}
                >
                    <motion.div
                        initial={{ scale: 0.9, opacity: 0 }}
                        animate={{ scale: 1, opacity: 1 }}
                        exit={{ scale: 0.9, opacity: 0 }}
                        style={{
                            backgroundColor: 'rgba(30, 30, 50, 0.95)',
                            border: '1px solid rgba(255, 255, 255, 0.1)',
                            borderRadius: '16px',
                            padding: '32px',
                            maxWidth: '480px',
                            width: '100%',
                        }}
                    >
                        <div style={{ textAlign: 'center', marginBottom: '20px' }}>
                            <div style={{ fontSize: '48px', marginBottom: '12px' }}>⚠️</div>
                            <h2 style={{ color: '#fff', fontSize: '20px', fontWeight: 600, margin: 0 }}>
                                ProtonMail Is Not Supported
                            </h2>
                        </div>
                        <p style={{ color: 'rgba(255,255,255,0.7)', fontSize: '14px', lineHeight: 1.7, margin: '0 0 24px' }}>
                            {provider.blockedMessage}
                        </p>
                        <button
                            onClick={onClose}
                            style={{
                                width: '100%',
                                padding: '12px',
                                backgroundColor: 'rgba(139, 92, 246, 0.8)',
                                color: '#fff',
                                border: 'none',
                                borderRadius: '10px',
                                fontSize: '14px',
                                fontWeight: 600,
                                cursor: 'pointer',
                            }}
                        >
                            Continue to Dashboard
                        </button>
                    </motion.div>
                </motion.div>
            </AnimatePresence>
        )
    }

    return (
        <AnimatePresence>
            <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                className="fixed inset-0 z-50 flex items-center justify-center p-4"
                style={{ backgroundColor: 'rgba(0, 0, 0, 0.6)', backdropFilter: 'blur(8px)' }}
                onClick={(e) => e.target === e.currentTarget && mode !== 'setup' && onClose()}
            >
                <motion.div
                    initial={{ scale: 0.9, opacity: 0, y: 20 }}
                    animate={{ scale: 1, opacity: 1, y: 0 }}
                    exit={{ scale: 0.9, opacity: 0, y: 20 }}
                    style={{
                        backgroundColor: 'rgba(30, 30, 50, 0.95)',
                        border: '1px solid rgba(255, 255, 255, 0.1)',
                        borderRadius: '16px',
                        padding: '32px',
                        maxWidth: '560px',
                        width: '100%',
                        maxHeight: '85vh',
                        overflowY: 'auto',
                    }}
                >
                    {/* Header */}
                    <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                        <div style={{ fontSize: '40px', marginBottom: '12px' }}>✉️</div>
                        <h2 style={{ color: '#fff', fontSize: '20px', fontWeight: 600, margin: '0 0 8px' }}>
                            {mode === 'update' ? 'Update App Password' : 'Set Up Email Sending'}
                        </h2>
                        <p style={{ color: 'rgba(255,255,255,0.6)', fontSize: '14px', margin: 0 }}>
                            {provider
                                ? `To send emails from your ${provider.name} account, you need an App-Specific Password.`
                                : 'To send emails, you need to create an App-Specific Password.'}
                        </p>
                        <p style={{ color: 'rgba(255,255,255,0.5)', fontSize: '12px', margin: '8px 0 0' }}>
                            This is a special password — NOT your regular login password. It keeps your main password safe.
                        </p>
                    </div>

                    {/* Instructions */}
                    {provider && provider.instructions.length > 0 && (
                        <div
                            style={{
                                backgroundColor: 'rgba(255, 255, 255, 0.05)',
                                border: '1px solid rgba(255, 255, 255, 0.08)',
                                borderRadius: '12px',
                                padding: '20px',
                                marginBottom: '20px',
                            }}
                        >
                            <h3 style={{ color: 'rgba(255,255,255,0.9)', fontSize: '14px', fontWeight: 600, margin: '0 0 14px' }}>
                                How to get your {provider.name} App Password:
                            </h3>
                            <ol style={{ margin: 0, padding: '0 0 0 20px', listStyle: 'decimal' }}>
                                {provider.instructions.map((step, i) => (
                                    <li
                                        key={i}
                                        style={{
                                            color: 'rgba(255,255,255,0.7)',
                                            fontSize: '13px',
                                            lineHeight: 1.7,
                                            marginBottom: '4px',
                                        }}
                                    >
                                        {step}
                                    </li>
                                ))}
                            </ol>
                            {provider.directLink && (
                                <a
                                    href={provider.directLink}
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    style={{
                                        display: 'inline-block',
                                        marginTop: '12px',
                                        color: '#8b5cf6',
                                        fontSize: '13px',
                                        textDecoration: 'none',
                                    }}
                                >
                                    🔗 Direct link →
                                </a>
                            )}
                            {provider.note && (
                                <p style={{
                                    color: 'rgba(255, 200, 50, 0.8)',
                                    fontSize: '12px',
                                    margin: '12px 0 0',
                                    lineHeight: 1.5,
                                }}>
                                    ⚠️ {provider.note}
                                </p>
                            )}
                        </div>
                    )}

                    {/* Password Input */}
                    <div style={{ marginBottom: '16px' }}>
                        <label style={{ display: 'block', color: 'rgba(255,255,255,0.7)', fontSize: '13px', marginBottom: '8px' }}>
                            Paste your App-Specific Password here:
                        </label>
                        <input
                            type="password"
                            value={appPassword}
                            onChange={(e) => { setAppPassword(e.target.value); setError(null) }}
                            placeholder="xxxx-xxxx-xxxx-xxxx"
                            disabled={loading || success}
                            style={{
                                width: '100%',
                                padding: '12px 16px',
                                backgroundColor: 'rgba(255, 255, 255, 0.06)',
                                border: error ? '1px solid rgba(239, 68, 68, 0.5)' : '1px solid rgba(255, 255, 255, 0.12)',
                                borderRadius: '10px',
                                color: '#fff',
                                fontSize: '15px',
                                outline: 'none',
                                fontFamily: 'monospace',
                                letterSpacing: '1px',
                                boxSizing: 'border-box',
                            }}
                            onKeyDown={(e) => e.key === 'Enter' && handleConfigure()}
                        />
                    </div>

                    {/* Error */}
                    {error && (
                        <motion.div
                            initial={{ opacity: 0, y: -4 }}
                            animate={{ opacity: 1, y: 0 }}
                            style={{
                                color: '#ef4444',
                                fontSize: '13px',
                                marginBottom: '16px',
                                padding: '10px 14px',
                                backgroundColor: 'rgba(239, 68, 68, 0.1)',
                                borderRadius: '8px',
                                border: '1px solid rgba(239, 68, 68, 0.2)',
                            }}
                        >
                            {error}
                        </motion.div>
                    )}

                    {/* Success */}
                    {success && (
                        <motion.div
                            initial={{ opacity: 0, scale: 0.95 }}
                            animate={{ opacity: 1, scale: 1 }}
                            style={{
                                color: '#22c55e',
                                fontSize: '14px',
                                marginBottom: '16px',
                                padding: '12px 14px',
                                backgroundColor: 'rgba(34, 197, 94, 0.1)',
                                borderRadius: '8px',
                                border: '1px solid rgba(34, 197, 94, 0.2)',
                                textAlign: 'center',
                                fontWeight: 600,
                            }}
                        >
                            ✅ Email sending configured successfully!
                        </motion.div>
                    )}

                    {/* Security Note */}
                    <p style={{
                        color: 'rgba(255,255,255,0.4)',
                        fontSize: '12px',
                        marginBottom: '20px',
                        lineHeight: 1.5,
                    }}>
                        🔒 Your password is encrypted and stored securely. You can revoke it anytime from your email provider's settings.
                    </p>

                    {/* Actions */}
                    <div style={{ display: 'flex', gap: '12px' }}>
                        <button
                            onClick={handleConfigure}
                            disabled={loading || success || !appPassword.trim()}
                            style={{
                                flex: 1,
                                padding: '12px',
                                background: loading || success || !appPassword.trim()
                                    ? 'rgba(139, 92, 246, 0.3)'
                                    : 'linear-gradient(135deg, #8b5cf6, #6d28d9)',
                                color: '#fff',
                                border: 'none',
                                borderRadius: '10px',
                                fontSize: '14px',
                                fontWeight: 600,
                                cursor: loading || success ? 'not-allowed' : 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                gap: '8px',
                            }}
                        >
                            {loading ? (
                                <>
                                    <span className="animate-spin" style={{ display: 'inline-block', width: '16px', height: '16px', border: '2px solid rgba(255,255,255,0.3)', borderTopColor: '#fff', borderRadius: '50%' }} />
                                    Validating...
                                </>
                            ) : success ? (
                                '✓ Connected'
                            ) : mode === 'update' ? (
                                'Update Password'
                            ) : (
                                'Connect Email'
                            )}
                        </button>
                        {mode === 'setup' && !success && (
                            <button
                                onClick={handleSkip}
                                disabled={loading}
                                style={{
                                    flex: 1,
                                    padding: '12px',
                                    backgroundColor: 'rgba(255, 255, 255, 0.06)',
                                    color: 'rgba(255,255,255,0.6)',
                                    border: '1px solid rgba(255, 255, 255, 0.1)',
                                    borderRadius: '10px',
                                    fontSize: '14px',
                                    fontWeight: 500,
                                    cursor: loading ? 'not-allowed' : 'pointer',
                                }}
                            >
                                Skip for Now
                            </button>
                        )}
                        {mode === 'update' && !success && (
                            <button
                                onClick={onClose}
                                disabled={loading}
                                style={{
                                    padding: '12px 20px',
                                    backgroundColor: 'transparent',
                                    color: 'rgba(255,255,255,0.6)',
                                    border: '1px solid rgba(255, 255, 255, 0.1)',
                                    borderRadius: '10px',
                                    fontSize: '14px',
                                    cursor: 'pointer',
                                }}
                            >
                                Cancel
                            </button>
                        )}
                    </div>

                    {mode === 'setup' && (
                        <p style={{ color: 'rgba(255,255,255,0.35)', fontSize: '11px', textAlign: 'center', margin: '14px 0 0' }}>
                            "Skip for Now" → You can set this up later in your profile settings
                        </p>
                    )}
                </motion.div>
            </motion.div>
        </AnimatePresence>
    )
}
