import { ReactNode } from 'react'
import { motion, AnimatePresence } from 'framer-motion'

interface CollapsibleSectionProps {
    title: string
    icon?: ReactNode
    count?: number
    isOpen: boolean
    onToggle: () => void
    children: ReactNode
    defaultContent?: ReactNode
}

export default function CollapsibleSection({
    title,
    icon,
    count,
    isOpen,
    onToggle,
    children,
    defaultContent
}: CollapsibleSectionProps) {
    return (
        <div className="border border-slate-700/50 rounded-xl overflow-hidden bg-slate-900/30">
            <button
                onClick={onToggle}
                className="w-full flex items-center justify-between p-4 hover:bg-slate-800/30 transition-colors"
            >
                <div className="flex items-center gap-3">
                    {icon && <span className="text-blue-400">{icon}</span>}
                    <span className="font-medium text-white">{title}</span>
                    {count !== undefined && (
                        <span className="px-2 py-0.5 text-xs bg-slate-700 rounded-full text-slate-300">
                            {count}
                        </span>
                    )}
                </div>
                <motion.span
                    animate={{ rotate: isOpen ? 180 : 0 }}
                    transition={{ duration: 0.2 }}
                    className="text-slate-400"
                >
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor" className="w-5 h-5">
                        <path fillRule="evenodd" d="M5.23 7.21a.75.75 0 0 1 1.06.02L10 11.168l3.71-3.938a.75.75 0 1 1 1.08 1.04l-4.25 4.5a.75.75 0 0 1-1.08 0l-4.25-4.5a.75.75 0 0 1 .02-1.06Z" clipRule="evenodd" />
                    </svg>
                </motion.span>
            </button>
            <AnimatePresence>
                {isOpen && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2 }}
                        className="overflow-hidden"
                    >
                        <div className="p-4 pt-0 border-t border-slate-700/50">
                            {children}
                        </div>
                    </motion.div>
                )}
                {!isOpen && defaultContent && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        className="px-4 pb-4 text-sm text-slate-500"
                    >
                        {defaultContent}
                    </motion.div>
                )}
            </AnimatePresence>
        </div>
    )
}
