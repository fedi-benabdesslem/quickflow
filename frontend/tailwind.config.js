/** @type {import('tailwindcss').Config} */
export default {
    content: [
        "./index.html",
        "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
        extend: {
            colors: {
                nebula: {
                    dark: '#0a0a0f',
                    base: '#12121a',
                    blue: '#1e40af',
                    'blue-light': '#3b82f6',
                    pink: '#FF1493',
                    'pink-light': '#FF9ECA',
                },
            },
            fontFamily: {
                sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
            },
            backdropBlur: {
                xs: '2px',
            },
            animation: {
                'nebula-breathe': 'nebulaBreathe 8s ease-in-out infinite',
                'glow-pulse': 'glowPulse 4s ease-in-out infinite',
                'float': 'float 20s ease-in-out infinite',
                'shimmer': 'shimmer 3s ease-in-out infinite',
            },
            keyframes: {
                nebulaBreathe: {
                    '0%, 100%': { opacity: '0.6', transform: 'scale(1)' },
                    '50%': { opacity: '0.9', transform: 'scale(1.05)' },
                },
                glowPulse: {
                    '0%, 100%': { boxShadow: '0 0 20px rgba(59, 130, 246, 0.3)' },
                    '50%': { boxShadow: '0 0 40px rgba(59, 130, 246, 0.6)' },
                },
                float: {
                    '0%, 100%': { transform: 'translate(0, 0) scale(1)' },
                    '33%': { transform: 'translate(50px, -50px) scale(1.1)' },
                    '66%': { transform: 'translate(-30px, 30px) scale(0.9)' },
                },
                shimmer: {
                    '0%, 100%': { transform: 'translateX(-100%)' },
                    '50%': { transform: 'translateX(100%)' },
                },
            },
        },
    },
    plugins: [],
}
