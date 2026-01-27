import { useEffect } from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useSidebar } from '../contexts/SidebarContext'
import UserAvatar from './UserAvatar'

interface MenuItem {
    id: string
    label: string
    icon: React.ReactNode
    path?: string
    disabled?: boolean
    disabledLabel?: string
    badge?: number | string
    onClick?: () => void
}

export default function UserProfileSidebar() {
    const navigate = useNavigate()
    const location = useLocation()
    const { user, session, signOut } = useAuth()
    const { isOpen, closeSidebar, openHistorySidebar } = useSidebar()

    // Get provider from session metadata
    const provider = session ? getProviderFromSession() : 'email'

    function getProviderFromSession(): string {
        // Provider info might be stored in user metadata
        // For now, we'll check if it's a Google or Microsoft email pattern
        const email = user?.email || ''
        if (email.endsWith('@gmail.com') || email.endsWith('@googlemail.com')) {
            return 'google'
        }
        if (email.endsWith('@outlook.com') || email.endsWith('@hotmail.com') || email.endsWith('@live.com')) {
            return 'microsoft'
        }
        return 'email'
    }

    // Get user photo from Supabase session metadata
    const photoUrl = null // Will be populated from session.user.user_metadata.avatar_url
    const fullName = user?.username || user?.email?.split('@')[0] || 'User'

    // Close sidebar on ESC key
    useEffect(() => {
        const handleKeyDown = (e: KeyboardEvent) => {
            if (e.key === 'Escape' && isOpen) {
                closeSidebar()
            }
        }

        window.addEventListener('keydown', handleKeyDown)
        return () => window.removeEventListener('keydown', handleKeyDown)
    }, [isOpen, closeSidebar])

    // Close sidebar on route change
    useEffect(() => {
        closeSidebar()
    }, [location.pathname])

    const handleLogout = async () => {
        await signOut()
        closeSidebar()
        navigate('/auth')
    }

    const handleNavigation = (path: string) => {
        closeSidebar()
        navigate(path)
    }

    const menuItems: MenuItem[] = [
        {
            id: 'contacts',
            label: 'Contacts',
            icon: <ContactsIcon />,
            path: '/contacts',
            badge: undefined, // Will be populated with contact count
        },
        {
            id: 'groups',
            label: 'Groups',
            icon: <GroupsIcon />,
            path: '/groups',
        },
        {
            id: 'history',
            label: 'History',
            icon: <HistoryIcon />,
            onClick: () => {
                closeSidebar()
                openHistorySidebar()
            },
        },
        {
            id: 'bookmarks',
            label: 'Bookmarks',
            icon: <BookmarksIcon />,
            onClick: () => {
                closeSidebar()
                // Navigate to history with filter
                navigate('/history?filter=bookmarks')
            },
        },
        {
            id: 'inbox',
            label: 'Inbox',
            icon: <InboxIcon />,
            path: '/inbox',
            disabled: true,
            disabledLabel: 'Coming Soon',
        },
        {
            id: 'templates',
            label: 'Templates',
            icon: <TemplatesIcon />,
            path: '/templates',
        },
        {
            id: 'settings',
            label: 'Settings',
            icon: <SettingsIcon />,
            path: '/settings',
            disabled: true,
            disabledLabel: 'Coming Soon',
        },
    ]

    const providerLabels: Record<string, string> = {
        google: 'Google Account',
        microsoft: 'Microsoft Account',
        email: 'Email Account',
    }

    return (
        <AnimatePresence>
            {isOpen && (
                <>
                    {/* Backdrop */}
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        onClick={closeSidebar}
                        className="fixed inset-0 bg-black/50 backdrop-blur-sm z-40"
                    />

                    {/* Sidebar */}
                    <motion.aside
                        initial={{ x: '-100%' }}
                        animate={{ x: 0 }}
                        exit={{ x: '-100%' }}
                        transition={{ type: 'spring', damping: 25, stiffness: 300 }}
                        className="fixed left-0 top-0 h-full w-80 bg-slate-900/95 backdrop-blur-xl border-r border-white/10 z-50 flex flex-col"
                    >
                        {/* Close button */}
                        <button
                            onClick={closeSidebar}
                            className="absolute top-4 right-4 p-2 text-slate-400 hover:text-white hover:bg-white/10 rounded-lg transition-colors"
                        >
                            <CloseIcon />
                        </button>

                        {/* Header Section */}
                        <div className="p-6 pb-4 border-b border-white/10">
                            <div className="flex flex-col items-center text-center">
                                <UserAvatar
                                    photoUrl={photoUrl}
                                    fullName={fullName}
                                    size="xl"
                                    showOnlineIndicator
                                    className="mb-4"
                                />

                                <h2 className="text-xl font-bold text-white mb-1">
                                    {fullName}
                                </h2>

                                <p className="text-sm text-slate-400 mb-2">
                                    {user?.email}
                                </p>

                                <div className="flex items-center gap-2 text-xs">
                                    <ProviderBadge provider={provider} />
                                    <span className="text-slate-500">
                                        {providerLabels[provider]}
                                    </span>
                                </div>

                                <button
                                    onClick={() => {/* TODO: Open edit profile modal */ }}
                                    className="mt-4 px-4 py-2 text-sm text-purple-400 hover:text-purple-300 hover:bg-purple-500/10 rounded-lg transition-colors"
                                >
                                    Edit Profile
                                </button>
                            </div>
                        </div>

                        {/* Navigation Menu */}
                        <nav className="flex-1 overflow-y-auto p-4">
                            <ul className="space-y-1">
                                {menuItems.map((item) => (
                                    <li key={item.id}>
                                        <button
                                            onClick={() => {
                                                if (!item.disabled) {
                                                    if (item.onClick) {
                                                        item.onClick()
                                                    } else if (item.path) {
                                                        handleNavigation(item.path)
                                                    }
                                                }
                                            }}
                                            disabled={item.disabled}
                                            className={`w-full flex items-center gap-3 px-4 py-3 rounded-lg transition-all
                                                ${item.disabled
                                                    ? 'text-slate-600 cursor-not-allowed'
                                                    : 'text-slate-300 hover:text-white hover:bg-white/5'
                                                }
                                                ${item.path && location.pathname === item.path ? 'bg-purple-500/20 text-purple-300' : ''}
                                            `}
                                        >
                                            <span className="w-5 h-5">
                                                {item.icon}
                                            </span>
                                            <span className="flex-1 text-left">
                                                {item.label}
                                            </span>
                                            {item.badge !== undefined && (
                                                <span className="px-2 py-0.5 text-xs bg-purple-500/20 text-purple-300 rounded-full">
                                                    {item.badge}
                                                </span>
                                            )}
                                            {item.disabled && item.disabledLabel && (
                                                <span className="px-2 py-0.5 text-xs bg-slate-700 text-slate-500 rounded-full">
                                                    {item.disabledLabel}
                                                </span>
                                            )}
                                        </button>
                                    </li>
                                ))}
                            </ul>
                        </nav>

                        {/* Logout Button */}
                        <div className="p-4 border-t border-white/10">
                            <button
                                onClick={handleLogout}
                                className="w-full flex items-center gap-3 px-4 py-3 text-red-400 hover:text-red-300 hover:bg-red-500/10 rounded-lg transition-colors"
                            >
                                <LogoutIcon />
                                <span>Logout</span>
                            </button>
                        </div>
                    </motion.aside>
                </>
            )}
        </AnimatePresence>
    )
}

// Provider badge component
function ProviderBadge({ provider }: { provider: string }) {
    const colors: Record<string, string> = {
        google: 'bg-blue-500',
        microsoft: 'bg-orange-500',
        email: 'bg-slate-500',
    }

    return (
        <span className={`w-2 h-2 rounded-full ${colors[provider] || colors.email}`} />
    )
}

// Icon components
function CloseIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M5.47 5.47a.75.75 0 011.06 0L12 10.94l5.47-5.47a.75.75 0 111.06 1.06L13.06 12l5.47 5.47a.75.75 0 11-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 01-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 010-1.06z" clipRule="evenodd" />
        </svg>
    )
}

function ContactsIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M7.5 6a4.5 4.5 0 119 0 4.5 4.5 0 01-9 0zM3.751 20.105a8.25 8.25 0 0116.498 0 .75.75 0 01-.437.695A18.683 18.683 0 0112 22.5c-2.786 0-5.433-.608-7.812-1.7a.75.75 0 01-.437-.695z" clipRule="evenodd" />
        </svg>
    )
}

function GroupsIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M8.25 6.75a3.75 3.75 0 117.5 0 3.75 3.75 0 01-7.5 0zM15.75 9.75a3 3 0 116 0 3 3 0 01-6 0zM2.25 9.75a3 3 0 116 0 3 3 0 01-6 0zM6.31 15.117A6.745 6.745 0 0112 12a6.745 6.745 0 016.709 7.498.75.75 0 01-.372.568A12.696 12.696 0 0112 21.75c-2.305 0-4.47-.612-6.337-1.684a.75.75 0 01-.372-.568 6.787 6.787 0 011.019-4.38z" clipRule="evenodd" />
            <path d="M5.082 14.254a8.287 8.287 0 00-1.308 5.135 9.687 9.687 0 01-1.764-.44l-.115-.04a.563.563 0 01-.373-.487l-.01-.121a3.75 3.75 0 013.57-4.047zM20.226 19.389a8.287 8.287 0 00-1.308-5.135 3.75 3.75 0 013.57 4.047l-.01.121a.563.563 0 01-.373.486l-.115.04c-.567.2-1.156.349-1.764.441z" />
        </svg>
    )
}

function HistoryIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M12 2.25c-5.385 0-9.75 4.365-9.75 9.75s4.365 9.75 9.75 9.75 9.75-4.365 9.75-9.75S17.385 2.25 12 2.25zM12.75 6a.75.75 0 00-1.5 0v6c0 .414.336.75.75.75h4.5a.75.75 0 000-1.5h-3.75V6z" clipRule="evenodd" />
        </svg>
    )
}

function BookmarksIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M6.32 2.577a49.255 49.255 0 0111.36 0c1.497.174 2.57 1.46 2.57 2.93V21a.75.75 0 01-1.085.67L12 18.089l-7.165 3.583A.75.75 0 013.75 21V5.507c0-1.47 1.073-2.756 2.57-2.93z" clipRule="evenodd" />
        </svg>
    )
}

function InboxIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M6.912 3a3 3 0 00-2.868 2.118l-2.411 7.838a3 3 0 00-.133.882V18a3 3 0 003 3h15a3 3 0 003-3v-4.162c0-.299-.045-.596-.133-.882l-2.412-7.838A3 3 0 0017.088 3H6.912zm13.823 9.75l-2.213-7.191A1.5 1.5 0 0017.088 4.5H6.912a1.5 1.5 0 00-1.434 1.059L3.265 12.75H6.11a3 3 0 012.684 1.658l.256.513a1.5 1.5 0 001.342.829h3.218a1.5 1.5 0 001.342-.83l.256-.512a3 3 0 012.684-1.658h2.844z" clipRule="evenodd" />
        </svg>
    )
}

function TemplatesIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path d="M11.644 1.59a.75.75 0 01.712 0l9.75 5.25a.75.75 0 010 1.32l-9.75 5.25a.75.75 0 01-.712 0l-9.75-5.25a.75.75 0 010-1.32l9.75-5.25z" />
            <path d="M3.265 10.602l7.668 4.129a2.25 2.25 0 002.134 0l7.668-4.13 1.37.739a.75.75 0 010 1.32l-9.75 5.25a.75.75 0 01-.712 0l-9.75-5.25a.75.75 0 010-1.32l1.37-.738z" />
            <path d="M10.933 19.231l-7.668-4.13-1.37.739a.75.75 0 000 1.32l9.75 5.25c.221.12.489.12.71 0l9.75-5.25a.75.75 0 000-1.32l-1.37-.738-7.668 4.13a2.25 2.25 0 01-2.134-.001z" />
        </svg>
    )
}

function SettingsIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M11.078 2.25c-.917 0-1.699.663-1.85 1.567L9.05 4.889c-.02.12-.115.26-.297.348a7.493 7.493 0 00-.986.57c-.166.115-.334.126-.45.083L6.3 5.508a1.875 1.875 0 00-2.282.819l-.922 1.597a1.875 1.875 0 00.432 2.385l.84.692c.095.078.17.229.154.43a7.598 7.598 0 000 1.139c.015.2-.059.352-.153.43l-.841.692a1.875 1.875 0 00-.432 2.385l.922 1.597a1.875 1.875 0 002.282.818l1.019-.382c.115-.043.283-.031.45.082.312.214.641.405.985.57.182.088.277.228.297.35l.178 1.071c.151.904.933 1.567 1.85 1.567h1.844c.916 0 1.699-.663 1.85-1.567l.178-1.072c.02-.12.114-.26.297-.349.344-.165.673-.356.985-.57.167-.114.335-.125.45-.082l1.02.382a1.875 1.875 0 002.28-.819l.923-1.597a1.875 1.875 0 00-.432-2.385l-.84-.692c-.095-.078-.17-.229-.154-.43a7.614 7.614 0 000-1.139c-.016-.2.059-.352.153-.43l.84-.692c.708-.582.891-1.59.433-2.385l-.922-1.597a1.875 1.875 0 00-2.282-.818l-1.02.382c-.114.043-.282.031-.449-.083a7.49 7.49 0 00-.985-.57c-.183-.087-.277-.227-.297-.348l-.179-1.072a1.875 1.875 0 00-1.85-1.567h-1.843zM12 15.75a3.75 3.75 0 100-7.5 3.75 3.75 0 000 7.5z" clipRule="evenodd" />
        </svg>
    )
}

function LogoutIcon() {
    return (
        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5">
            <path fillRule="evenodd" d="M7.5 3.75A1.5 1.5 0 006 5.25v13.5a1.5 1.5 0 001.5 1.5h6a1.5 1.5 0 001.5-1.5V15a.75.75 0 011.5 0v3.75a3 3 0 01-3 3h-6a3 3 0 01-3-3V5.25a3 3 0 013-3h6a3 3 0 013 3V9A.75.75 0 0115 9V5.25a1.5 1.5 0 00-1.5-1.5h-6zm10.72 4.72a.75.75 0 011.06 0l3 3a.75.75 0 010 1.06l-3 3a.75.75 0 11-1.06-1.06l1.72-1.72H9a.75.75 0 010-1.5h10.94l-1.72-1.72a.75.75 0 010-1.06z" clipRule="evenodd" />
        </svg>
    )
}
