import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { motion, AnimatePresence } from 'framer-motion'

export default function TechSupportButton() {
    const navigate = useNavigate()
    const [showTooltip, setShowTooltip] = useState(false)

    return (
        <div
            className="relative flex items-center z-50"
            onMouseEnter={() => setShowTooltip(true)}
            onMouseLeave={() => setShowTooltip(false)}
        >
            <img
                src="/tech-support-logo.png"
                alt="Tech Support"
                className="h-8 w-auto object-contain opacity-70 cursor-pointer hover:opacity-100 transition-opacity"
                onClick={() => navigate('/tech-support')}
            />

            <AnimatePresence>
                {showTooltip && (
                    <motion.div
                        initial={{ opacity: 0, scale: 0.95 }}
                        animate={{ opacity: 1, scale: 1 }}
                        exit={{ opacity: 0, scale: 0.95 }}
                        transition={{ duration: 0.1 }}
                        className="absolute top-full right-0 mt-1 px-3 py-1.5 bg-black/60 backdrop-blur-sm text-white text-xs rounded-lg whitespace-nowrap border border-white/10 shadow-lg"
                        style={{ pointerEvents: 'none' }}
                    >
                        Tech Support
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    )
}
