import { motion } from 'framer-motion'

export default function NebulaBackground() {
    return (
        <div className="nebula-bg">
            <motion.div
                className="nebula-glow nebula-glow-1"
                animate={{
                    scale: [1, 1.1, 1],
                    opacity: [0.4, 0.7, 0.4],
                }}
                transition={{
                    duration: 8,
                    repeat: Infinity,
                    ease: 'easeInOut',
                }}
            />
            <motion.div
                className="nebula-glow nebula-glow-2"
                animate={{
                    scale: [1, 1.15, 1],
                    opacity: [0.3, 0.6, 0.3],
                }}
                transition={{
                    duration: 10,
                    repeat: Infinity,
                    ease: 'easeInOut',
                    delay: 2,
                }}
            />
            <motion.div
                className="nebula-glow nebula-glow-3"
                animate={{
                    scale: [1, 1.08, 1],
                    opacity: [0.5, 0.8, 0.5],
                    x: [0, 30, -20, 0],
                    y: [0, -20, 30, 0],
                }}
                transition={{
                    duration: 12,
                    repeat: Infinity,
                    ease: 'easeInOut',
                    delay: 4,
                }}
            />
            <motion.div
                className="nebula-glow nebula-glow-4"
                animate={{
                    scale: [1, 1.12, 1],
                    opacity: [0.25, 0.5, 0.25],
                }}
                transition={{
                    duration: 9,
                    repeat: Infinity,
                    ease: 'easeInOut',
                    delay: 6,
                }}
            />
        </div>
    )
}
