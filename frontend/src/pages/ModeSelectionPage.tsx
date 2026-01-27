import { useNavigate } from 'react-router-dom'
import { motion } from 'framer-motion'
import TechSupportButton from '../components/TechSupportButton'


export default function ModeSelectionPage() {

    const navigate = useNavigate()



    return (
        <div className="min-h-screen p-4 sm:p-6 relative z-10">
            {/* Header */}
            <motion.header
                initial={{ opacity: 0, y: -20 }}
                animate={{ opacity: 1, y: 0 }}
                className="flex justify-between items-center mb-8 sm:mb-12"
            >
                <button onClick={() => navigate('/home')} className="btn-secondary">
                    <span>←</span>
                    <span className="hidden sm:inline">Back</span>
                </button>
                <div className="flex gap-3">
                    <TechSupportButton />
                </div>
            </motion.header>

            {/* Hero Section */}
            <motion.div
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: 0.1 }}
                className="max-w-3xl mx-auto text-center mb-12"
            >
                <motion.div
                    animate={{ scale: [1, 1.05, 1] }}
                    transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
                    className="mb-6 inline-block"
                >
                    <img
                        src="/logo.png"
                        alt="QuickFlow"
                        className="w-20 h-20 object-contain drop-shadow-[0_0_20px_rgba(59,130,246,0.5)]"
                    />
                </motion.div>
                <h1 className="text-3xl sm:text-4xl font-bold text-white mb-3">
                    Create Meeting Minutes
                </h1>
                <p className="text-slate-400 text-lg">
                    Generate professional meeting minutes (PV) with AI assistance
                </p>
            </motion.div>

            {/* Mode Selection Cards */}
            <div className="max-w-4xl mx-auto grid grid-cols-1 md:grid-cols-2 gap-6 mb-12">
                {/* Quick Mode Card */}
                <motion.div
                    initial={{ opacity: 0, x: -20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.2 }}
                    whileHover={{ scale: 1.02, y: -5 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => navigate('/minutes/quick')}
                    className="glass-card p-8 cursor-pointer group min-h-[320px] flex flex-col"
                >
                    <div className="flex items-start justify-between mb-4">
                        <motion.div
                            className="text-4xl text-yellow-400"
                            animate={{ rotate: [0, 10, -10, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut' }}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                                <path fillRule="evenodd" d="M14.615 1.595a.75.75 0 0 1 .359.852L12.982 9.75h7.268a.75.75 0 0 1 .548 1.262l-10.5 11.25a.75.75 0 0 1-1.272-.71l1.992-7.302H3.75a.75.75 0 0 1-.548-1.262l10.5-11.25a.75.75 0 0 1 .913-.143Z" clipRule="evenodd" />
                            </svg>
                        </motion.div>
                        <motion.div
                            className="text-2xl text-slate-500 group-hover:text-yellow-400 group-hover:translate-x-1 transition-all"
                        >
                            →
                        </motion.div>
                    </div>
                    <h3 className="text-xl font-bold text-white mb-3 group-hover:text-yellow-300 transition-colors">
                        Quick Mode
                    </h3>
                    <p className="text-slate-400 flex-grow">
                        Upload or paste your notes. AI extracts structure and generates
                        professional meeting minutes automatically.
                    </p>
                    <div className="mt-4 flex items-center gap-2 text-sm text-slate-500">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                            <path fillRule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm.75-13a.75.75 0 0 0-1.5 0v5c0 .414.336.75.75.75h4a.75.75 0 0 0 0-1.5h-3.25V5Z" clipRule="evenodd" />
                        </svg>
                        ~2-3 minutes
                    </div>
                    <motion.button
                        className="mt-4 btn-primary w-full"
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                    >
                        Start →
                    </motion.button>
                    <div className="mt-4 h-1 w-0 group-hover:w-full bg-gradient-to-r from-yellow-500 to-orange-500 rounded-full transition-all duration-300" />
                </motion.div>

                {/* Structured Mode Card */}
                <motion.div
                    initial={{ opacity: 0, x: 20 }}
                    animate={{ opacity: 1, x: 0 }}
                    transition={{ delay: 0.3 }}
                    whileHover={{ scale: 1.02, y: -5 }}
                    whileTap={{ scale: 0.98 }}
                    onClick={() => navigate('/minutes/structured')}
                    className="glass-card p-8 cursor-pointer group min-h-[320px] flex flex-col"
                >
                    <div className="flex items-start justify-between mb-4">
                        <motion.div
                            className="text-4xl text-blue-400"
                            animate={{ y: [0, -5, 0] }}
                            transition={{ duration: 2, repeat: Infinity, ease: 'easeInOut', delay: 0.5 }}
                        >
                            <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-12 h-12">
                                <path fillRule="evenodd" d="M7.502 6h7.128A3.375 3.375 0 0 1 18 9.375v9.375a3 3 0 0 0 3-3V6.108c0-1.505-1.125-2.811-2.664-2.94a48.972 48.972 0 0 0-.673-.05A3 3 0 0 0 15 1.5h-1.5a3 3 0 0 0-2.663 1.618c-.225.015-.45.032-.673.05C8.662 3.295 7.554 4.542 7.502 6ZM13.5 3A1.5 1.5 0 0 0 12 4.5h4.5A1.5 1.5 0 0 0 15 3h-1.5Z" clipRule="evenodd" />
                                <path fillRule="evenodd" d="M3 9.375C3 8.339 3.84 7.5 4.875 7.5h9.75c1.036 0 1.875.84 1.875 1.875v11.25c0 1.035-.84 1.875-1.875 1.875h-9.75A1.875 1.875 0 0 1 3 20.625V9.375ZM6 12a.75.75 0 0 1 .75-.75h.008a.75.75 0 0 1 .75.75v.008a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V12Zm2.25 0a.75.75 0 0 1 .75-.75h3.75a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75ZM6 15a.75.75 0 0 1 .75-.75h.008a.75.75 0 0 1 .75.75v.008a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V15Zm2.25 0a.75.75 0 0 1 .75-.75h3.75a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75ZM6 18a.75.75 0 0 1 .75-.75h.008a.75.75 0 0 1 .75.75v.008a.75.75 0 0 1-.75.75H6.75a.75.75 0 0 1-.75-.75V18Zm2.25 0a.75.75 0 0 1 .75-.75h3.75a.75.75 0 0 1 0 1.5H9a.75.75 0 0 1-.75-.75Z" clipRule="evenodd" />
                            </svg>
                        </motion.div>
                        <motion.div
                            className="text-2xl text-slate-500 group-hover:text-blue-400 group-hover:translate-x-1 transition-all"
                        >
                            →
                        </motion.div>
                    </div>
                    <h3 className="text-xl font-bold text-white mb-3 group-hover:text-blue-300 transition-colors">
                        Structured Mode
                    </h3>
                    <p className="text-slate-400 flex-grow">
                        Fill in a detailed form for formal meeting minutes with participants,
                        agenda, decisions, and action items.
                    </p>
                    <div className="mt-4 flex items-center gap-2 text-sm text-slate-500">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-4 h-4">
                            <path fillRule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16Zm.75-13a.75.75 0 0 0-1.5 0v5c0 .414.336.75.75.75h4a.75.75 0 0 0 0-1.5h-3.25V5Z" clipRule="evenodd" />
                        </svg>
                        ~8-12 minutes
                    </div>
                    <motion.button
                        className="mt-4 btn-primary w-full"
                        whileHover={{ scale: 1.02 }}
                        whileTap={{ scale: 0.98 }}
                    >
                        Start →
                    </motion.button>
                    <div className="mt-4 h-1 w-0 group-hover:w-full bg-gradient-to-r from-blue-500 to-purple-500 rounded-full transition-all duration-300" />
                </motion.div>
            </div>


        </div>
    )
}
