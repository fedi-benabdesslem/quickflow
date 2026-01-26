import { motion } from 'framer-motion'

interface UserAvatarProps {
    photoUrl?: string | null
    fullName?: string
    size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
    showOnlineIndicator?: boolean
    onClick?: () => void
    className?: string
}

/**
 * Generate a consistent color based on a string (user ID or name)
 */
function getColorFromString(str: string): string {
    const colors = [
        'from-blue-500 to-purple-500',
        'from-pink-500 to-rose-500',
        'from-green-500 to-emerald-500',
        'from-amber-500 to-orange-500',
        'from-cyan-500 to-blue-500',
        'from-violet-500 to-purple-500',
        'from-teal-500 to-cyan-500',
        'from-fuchsia-500 to-pink-500',
    ]

    let hash = 0
    for (let i = 0; i < str.length; i++) {
        hash = str.charCodeAt(i) + ((hash << 5) - hash)
    }

    return colors[Math.abs(hash) % colors.length]
}

/**
 * Get initials from a full name
 */
function getInitials(fullName?: string): string {
    if (!fullName) return '?'

    const parts = fullName.trim().split(/\s+/)
    if (parts.length === 1) {
        return parts[0].charAt(0).toUpperCase()
    }

    return (parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase()
}

const sizeClasses = {
    xs: 'w-6 h-6 text-[10px]',
    sm: 'w-8 h-8 text-xs',
    md: 'w-10 h-10 text-sm',
    lg: 'w-16 h-16 text-xl',
    xl: 'w-20 h-20 text-2xl',
}

const indicatorSizeClasses = {
    xs: 'w-1.5 h-1.5 right-0 bottom-0',
    sm: 'w-2 h-2 right-0 bottom-0',
    md: 'w-2.5 h-2.5 right-0 bottom-0',
    lg: 'w-3 h-3 right-0.5 bottom-0.5',
    xl: 'w-4 h-4 right-1 bottom-1',
}

export default function UserAvatar({
    photoUrl,
    fullName,
    size = 'md',
    showOnlineIndicator = false,
    onClick,
    className = '',
}: UserAvatarProps) {
    const initials = getInitials(fullName)
    const gradientColor = getColorFromString(fullName || 'default')

    const AvatarContent = () => (
        <div className="relative">
            {photoUrl ? (
                <img
                    src={photoUrl}
                    alt={fullName || 'User avatar'}
                    className={`${sizeClasses[size]} rounded-full object-cover ring-2 ring-white/10`}
                />
            ) : (
                <div
                    className={`${sizeClasses[size]} rounded-full bg-gradient-to-br ${gradientColor} flex items-center justify-center font-semibold text-white ring-2 ring-white/10`}
                >
                    {initials}
                </div>
            )}

            {/* Online indicator */}
            {showOnlineIndicator && (
                <span
                    className={`absolute ${indicatorSizeClasses[size]} bg-emerald-500 rounded-full ring-2 ring-slate-900`}
                />
            )}
        </div>
    )

    if (onClick) {
        return (
            <motion.button
                onClick={onClick}
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
                className={`relative cursor-pointer rounded-full focus:outline-none focus:ring-2 focus:ring-purple-500/50 hover:shadow-lg hover:shadow-purple-500/20 transition-shadow ${className}`}
            >
                <AvatarContent />
            </motion.button>
        )
    }

    return (
        <div className={`relative ${className}`}>
            <AvatarContent />
        </div>
    )
}

export { getInitials, getColorFromString }
