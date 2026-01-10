import { ReactNode } from 'react'

interface GlassCardProps {
    children: ReactNode
    className?: string
    hover?: boolean
}

export function GlassCard({ children, className = '', hover = true }: GlassCardProps) {
    return (
        <div
            className={`
                relative rounded-2xl p-6
                bg-white/70 dark:bg-gray-800/70
                backdrop-blur-xl
                border border-white/20 dark:border-gray-700/50
                shadow-xl
                ${hover ? 'hover:scale-[1.02] hover:shadow-2xl transition-all duration-300' : ''}
                ${className}
            `}
        >
            {/* Gradient border effect */}
            <div className="absolute inset-0 rounded-2xl bg-gradient-to-br from-blue-500/10 via-purple-500/10 to-pink-500/10 dark:from-blue-400/20 dark:via-purple-400/20 dark:to-pink-400/20 -z-10" />

            {children}
        </div>
    )
}
