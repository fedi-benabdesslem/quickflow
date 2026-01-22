import { Toaster, toast } from 'react-hot-toast'

/**
 * Toast notification provider component.
 * Add this to your app root to enable toast notifications.
 */
export function ToastProvider() {
    return (
        <Toaster
            position="bottom-right"
            toastOptions={{
                duration: 4000,
                style: {
                    background: 'rgba(30, 41, 59, 0.95)',
                    color: '#e2e8f0',
                    border: '1px solid rgba(148, 163, 184, 0.2)',
                    boxShadow: '0 0 20px rgba(168, 85, 247, 0.3)',
                    borderRadius: '0.75rem',
                    padding: '12px 16px',
                },
                success: {
                    iconTheme: {
                        primary: '#10b981',
                        secondary: '#e2e8f0',
                    },
                },
                error: {
                    iconTheme: {
                        primary: '#ef4444',
                        secondary: '#e2e8f0',
                    },
                },
            }}
        />
    )
}

/**
 * Toast notification utilities with nebula theme styling.
 */
export const showToast = {
    success: (message: string) => {
        toast.success(message)
    },

    error: (message: string) => {
        toast.error(message)
    },

    info: (message: string) => {
        toast(message, {
            icon: '💡',
        })
    },

    warning: (message: string) => {
        toast(message, {
            icon: '⚠️',
            style: {
                borderColor: 'rgba(234, 179, 8, 0.3)',
            },
        })
    },

    /**
     * Shows a toast for unsupported email provider.
     * Used when email/password users try to send emails.
     */
    unsupportedProvider: () => {
        toast("Email sending not supported for your domain yet. Coming soon!", {
            icon: '📧',
            duration: 5000,
            style: {
                borderColor: 'rgba(168, 85, 247, 0.3)',
            },
        })
    },

    /**
     * Shows a toast for reauthorization required.
     */
    reauthRequired: () => {
        toast.error("Please sign in again to send emails.")
    },

    /**
     * Shows loading toast that can be updated.
     */
    loading: (message: string) => {
        return toast.loading(message, {
            style: {
                borderColor: 'rgba(59, 130, 246, 0.3)',
            },
        })
    },

    /**
     * Dismisses a specific toast.
     */
    dismiss: (toastId: string) => {
        toast.dismiss(toastId)
    },
}

export { toast }
