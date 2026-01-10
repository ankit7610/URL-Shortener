'use client'

import { ButtonHTMLAttributes, ReactNode } from 'react'

interface AnimatedButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
    children: ReactNode
    variant?: 'primary' | 'secondary' | 'outline'
    size?: 'sm' | 'md' | 'lg'
}

export function AnimatedButton({
    children,
    variant = 'primary',
    size = 'md',
    className = '',
    ...props
}: AnimatedButtonProps) {
    const baseStyles = 'relative overflow-hidden font-semibold rounded-lg transition-all duration-300 active:scale-95 disabled:opacity-50 disabled:cursor-not-allowed'

    const variants = {
        primary: 'bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white shadow-lg hover:shadow-xl',
        secondary: 'bg-gray-200 dark:bg-gray-700 hover:bg-gray-300 dark:hover:bg-gray-600 text-gray-900 dark:text-white',
        outline: 'border-2 border-blue-600 dark:border-purple-600 text-blue-600 dark:text-purple-600 hover:bg-blue-50 dark:hover:bg-purple-900/20'
    }

    const sizes = {
        sm: 'px-4 py-2 text-sm',
        md: 'px-6 py-3 text-base',
        lg: 'px-8 py-4 text-lg'
    }

    return (
        <button
            className={`${baseStyles} ${variants[variant]} ${sizes[size]} ${className} group`}
            {...props}
        >
            {/* Ripple effect */}
            <span className="absolute inset-0 overflow-hidden rounded-lg">
                <span className="absolute inset-0 bg-white/20 translate-y-full group-hover:translate-y-0 transition-transform duration-500" />
            </span>

            {/* Content */}
            <span className="relative z-10 flex items-center justify-center gap-2">
                {children}
            </span>
        </button>
    )
}
