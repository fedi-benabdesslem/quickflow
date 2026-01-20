import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import { useAuth } from '../contexts/AuthContext'

export default function HomePage() {
    const { user, signOut } = useAuth()
    const navigate = useNavigate()

    const handleLogout = async () => {
        await signOut()
        navigate('/auth')
    }

    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8 sm:mb-12"
            >
                <div className="flex items-center gap-3">
                    <span className="text-2xl">⚡</span>
                    <span className="text-xl font-bold bg-gradient-to-r from-blue-400 to-purple-400 bg-clip-text text-transparent">
                        QuickFlow
                    </span>
                </div>
                <div className="flex gap-3">
                    <button onClick={handleLogout} className="btn-logout">
                        <span>🚪</span>
                        <span className="hidden sm:inline">Logout</span>
                    </button>
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
                    className="text-6xl mb-6 inline-block"
                >
                    👋
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
                    onClick={() => navigate('/meeting')}
                    className="glass-card p-8 cursor-pointer group"
                >
                    <div className="flex items-start justify-between mb-4">
                        <motion.div
                            className="text-4xl"
                            animate={{ rotate: [0, 5, -5, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        >
                            📝
                        </motion.div>
                        <motion.div
                            className="text-2xl text-slate-500 group-hover:text-blue-400 group-hover:translate-x-1 transition-all"
                        >
                            →
                        </motion.div>
                    </div>
                    <h3 className="text-xl font-bold text-white mb-2 group-hover:text-blue-300 transition-colors">
                        Create Meeting Summary
                    </h3>
                    <p className="text-slate-400">
                        Generate professional meeting summaries (PV) with AI
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
                            className="text-4xl"
                            animate={{ y: [0, -5, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut', delay: 0.5 }}
                        >
                            ✉️
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
            </div>
        </div>
    )
}
