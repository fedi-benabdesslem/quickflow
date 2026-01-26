import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'
import { useSidebar } from '../contexts/SidebarContext'
import UserAvatar from '../components/UserAvatar'
import UserProfileSidebar from '../components/UserProfileSidebar'

export default function HomePage() {
    const { user } = useAuth()
    const { openSidebar } = useSidebar()
    const navigate = useNavigate()

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Profile Sidebar */}
            <UserProfileSidebar />

            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8 sm:mb-12"
            >
                <div className="flex items-center gap-3">
                    <UserAvatar
                        fullName={user?.username || user?.email}
                        size="md"
                        showOnlineIndicator
                        onClick={openSidebar}
                    />
                </div>
                <div className="flex items-center gap-3">
                    <img src="/tech-support-logo.png" alt="QuickFlow" className="h-8 w-auto object-contain opacity-70" />
                </div>
            </motion.header>

            {/* Welcome Card */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="max-w-2xl mx-auto text-center mb-12"
            >
                <motion.div
                    animate={{ scale: [1, 1.1, 1] }}
                    transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                    className="mb-6 inline-block"
                >
                    <img
                        src="/logo.png"
                        alt="Welcome"
                        className="w-24 h-24 object-contain drop-shadow-[0_0_20px_rgba(168,85,247,0.5)]"
                    />
                </motion.div>
                <h1 className="text-3xl sm:text-4xl font-bold text-white mb-3">
                    Welcome back,{' '}
                    <span className="bg-gradient-to-r from-blue-400 via-purple-400 to-pink-400 bg-clip-text text-transparent">
                        {user?.username}
                    </span>
                    !
                </h1>
                <p className="text-slate-400 text-lg">
                    Choose an action to get started with AI assistance
                </p>
            </motion.div>

            {/* Action Cards */}
            <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-6">
                {/* Meeting Card */}
                <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2 }}
                    whileHover={{ scale: 1.02, y: -5 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => navigate('/minutes/new')}
                    className="glass-card p-8 cursor-pointer group"
                >
                    <div className="flex items-start justify-between mb-4">
                        <motion.div
                            className="text-4xl text-blue-400"
                            animate={{ rotate: [0, 5, -5, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                                <path fillRule="evenodd" d="M5.625 1.5c-1.036 0-1.875.84-1.875 1.875v17.25c0 1.035.84 1.875 1.875 1.875h12.75c1.035 0 1.875-.84 1.875-1.875V12.75A3.75 3.75 0 0016.5 9h-1.875a1.875 1.875 0 01-1.875-1.875V5.25A3.75 3.75 0 009 1.5H5.625zM7.5 15a.75.75 0 01.75-.75h7.5a.75.75 0 010 1.5h-7.5A.75.75 0 017.5 15zm.75 2.25a.75.75 0 000 1.5H12a.75.75 0 000-1.5H8.25z" clipRule="evenodd" />
                                <path d="M12.971 1.816A5.23 5.23 0 0114.25 5.25v1.875c0 .207.168.375.375.375H16.5a5.23 5.23 0 013.434 1.279 9.768 9.768 0 00-6.963-6.963z" />
                            </svg>
                        </motion.div>
                        <motion.div
                            className="text-2xl text-slate-500 group-hover:text-blue-400 group-hover:translate-x-1 transition-all"
                        >
                            →
                        </motion.div>
                    </div>
                    <h3 className="text-xl font-bold text-white mb-2 group-hover:text-blue-300 transition-colors">
                        Create Meeting Minutes
                    </h3>
                    <p className="text-slate-400">
                        Generate professional meeting minutes (PV) with AI
                    </p>
                    <div className="mt-4 h-1 w-0 group-hover:w-full bg-gradient-to-r from-blue-500 to-purple-500 rounded-full transition-all duration-300" />
                </motion.div>

                {/* Email Card */}
                <motion.div
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.3 }}
                    whileHover={{ scale: 1.02, y: -5 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => navigate('/email')}
                    className="glass-card p-8 cursor-pointer group"
                >
                    <div className="flex items-start justify-between mb-4">
                        <motion.div
                            className="text-4xl text-pink-400"
                            animate={{ y: [0, -5, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut', delay: 0.5 }}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                                <path d="M1.5 8.67v8.58a3 3 0 003 3h15a3 3 0 003-3V8.67l-8.928 5.493a3 3 0 01-3.144 0L1.5 8.67z" />
                                <path d="M22.5 6.908V6.75a3 3 0 00-3-3h-15a3 3 0 00-3 3v.158l9.714 5.978a1.5 1.5 0 001.572 0L22.5 6.908z" />
                            </svg>
                        </motion.div>
                        <motion.div
                            className="text-2xl text-slate-500 group-hover:text-pink-400 group-hover:translate-x-1 transition-all"
                        >
                            →
                        </motion.div>
                    </div>
                    <h3 className="text-xl font-bold text-white mb-2 group-hover:text-pink-300 transition-colors">
                        Draft Email
                    </h3>
                    <p className="text-slate-400">
                        Create polished emails with AI-powered assistance
                    </p>
                    <div className="mt-4 h-1 w-0 group-hover:w-full bg-gradient-to-r from-pink-500 to-purple-500 rounded-full transition-all duration-300" />
                </motion.div>

                {/* Templates Card */}
                <motion.div
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    transition={{ delay: 0.4 }}
                    whileHover={{ scale: 1.02, y: -5 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => navigate('/templates')}
                    className="glass-card p-8 cursor-pointer group md:col-span-2 sm:col-span-1"
                >
                    <div className="flex items-start justify-between mb-4">
                        <motion.div
                            className="text-4xl text-purple-400"
                            animate={{ rotate: [0, -5, 5, 0] }}
                            transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                                <path d="M11.644 1.59a.75.75 0 0 1 .712 0l9.75 5.25a.75.75 0 0 1 0 1.32l-9.75 5.25a.75.75 0 0 1-.712 0l-9.75-5.25a.75.75 0 0 1 0-1.32l9.75-5.25Z" />
                                <path d="M3.265 10.602l7.668 4.129a2.25 2.25 0 0 0 2.134 0l7.668-4.13 1.37.739a.75.75 0 0 1 0 1.32l-9.75 5.25a.75.75 0 0 1-.712 0l-9.75-5.25a.75.75 0 0 1 0-1.32l1.37-.738Z" />
                                <path d="M10.933 19.231l-7.668-4.13-1.37.739a.75.75 0 0 0 0 1.32l9.75 5.25c.221.12.489.12.71 0l9.75-5.25a.75.75 0 0 0 0-1.32l-1.37-.738-7.668 4.13a2.25 2.25 0 0 1-2.134 0Z" />
                            </svg>
                        </motion.div>
                        <motion.div
                            className="text-2xl text-slate-500 group-hover:text-purple-400 group-hover:translate-x-1 transition-all"
                        >
                            →
                        </motion.div>
                    </div>
                    <h3 className="text-xl font-bold text-white mb-2 group-hover:text-purple-300 transition-colors">
                        Manage Templates
                    </h3>
                    <p className="text-slate-400">
                        Create and manage reusable meeting structures
                    </p>
                    <div className="mt-4 h-1 w-0 group-hover:w-full bg-gradient-to-r from-purple-500 to-blue-500 rounded-full transition-all duration-300" />
                </motion.div>
            </div>
        </div>
    )
}
