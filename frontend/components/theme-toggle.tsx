'use client'

import { Moon, Sun } from 'lucide-react'
import { useTheme } from 'next-themes'
import { useEffect, useState } from 'react'

export function ThemeToggle() {
    const [mounted, setMounted] = useState(false)
    const { theme, setTheme } = useTheme()

    // Avoid hydration mismatch
    useEffect(() => {
        setMounted(true)
    }, [])

    if (!mounted) {
        return (
            <div className="w-10 h-10 rounded-full bg-gray-200 dark:bg-gray-700 animate-pulse" />
        )
    }

    return (
        <button
            onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}
            className="relative w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 dark:from-purple-600 dark:to-pink-600 p-[2px] hover:scale-110 transition-all duration-300 group shadow-lg hover:shadow-xl"
            aria-label="Toggle theme"
        >
            <div className="w-full h-full rounded-full bg-white dark:bg-gray-900 flex items-center justify-center">
                <div className="relative w-5 h-5">
                    <Sun
                        className={`absolute inset-0 h-5 w-5 text-yellow-500 transition-all duration-500 ${theme === 'dark'
                                ? 'rotate-90 scale-0 opacity-0'
                                : 'rotate-0 scale-100 opacity-100'
                            }`}
                    />
                    <Moon
                        className={`absolute inset-0 h-5 w-5 text-blue-600 transition-all duration-500 ${theme === 'dark'
                                ? 'rotate-0 scale-100 opacity-100'
                                : '-rotate-90 scale-0 opacity-0'
                            }`}
                    />
                </div>
            </div>

            {/* Tooltip */}
            <span className="absolute -bottom-10 left-1/2 -translate-x-1/2 px-2 py-1 bg-gray-900 dark:bg-gray-100 text-white dark:text-gray-900 text-xs rounded opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap pointer-events-none">
                {theme === 'dark' ? 'Light mode' : 'Dark mode'}
            </span>
        </button>
    )
}
