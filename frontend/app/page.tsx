'use client'

import { useState } from 'react'
import { Link2, BarChart3, Shield, Zap, Sparkles, Copy, Check, ArrowRight, Github, Twitter, Linkedin, MousePointerClick, LinkIcon, Share2 } from 'lucide-react'
import Link from 'next/link'
import { ThemeToggle } from '@/components/theme-toggle'
import { AnimatedButton } from '@/components/animated-button'
import { GlassCard } from '@/components/glass-card'

export default function HomePage() {
    const [url, setUrl] = useState('')
    const [shortenedUrl, setShortenedUrl] = useState('')
    const [isLoading, setIsLoading] = useState(false)
    const [copied, setCopied] = useState(false)
    const [error, setError] = useState('')

    const handleShorten = async () => {
        if (!url.trim()) {
            setError('Please enter a URL')
            return
        }

        setError('')
        setIsLoading(true)

        // Simulate URL shortening (demo mode)
        await new Promise(resolve => setTimeout(resolve, 1200))

        const shortCode = Math.random().toString(36).substring(2, 9)
        setShortenedUrl(`https://lnk.sh/${shortCode}`)
        setIsLoading(false)
    }

    const handleCopy = async () => {
        await navigator.clipboard.writeText(shortenedUrl)
        setCopied(true)
        setTimeout(() => setCopied(false), 2000)
    }

    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 via-indigo-50 to-purple-50 dark:from-gray-950 dark:via-blue-950 dark:to-purple-950 animate-gradient">
            {/* Header */}
            <header className="border-b border-white/20 dark:border-gray-800/50 bg-white/30 dark:bg-gray-900/30 backdrop-blur-xl sticky top-0 z-50 shadow-lg">
                <div className="container mx-auto px-4 py-4 flex justify-between items-center">
                    <div className="flex items-center space-x-3 group">
                        <div className="relative">
                            <Link2 className="h-8 w-8 text-blue-600 dark:text-blue-400 group-hover:rotate-12 transition-transform duration-300" />
                            <Sparkles className="h-4 w-4 text-purple-500 absolute -top-1 -right-1 animate-pulse" />
                        </div>
                        <span className="text-2xl font-bold bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 dark:from-blue-400 dark:via-purple-400 dark:to-pink-400 bg-clip-text text-transparent">
                            LinkShort
                        </span>
                    </div>
                    <nav className="flex items-center space-x-6">
                        <Link href="/dashboard" className="text-gray-700 hover:text-gray-900 dark:text-gray-300 dark:hover:text-white transition font-medium">
                            Dashboard
                        </Link>
                        <Link href="/login" className="text-gray-700 hover:text-gray-900 dark:text-gray-300 dark:hover:text-white transition font-medium">
                            Login
                        </Link>
                        <AnimatedButton variant="primary" size="sm" className="hidden md:inline-flex">
                            Sign Up Free
                        </AnimatedButton>
                        <ThemeToggle />
                    </nav>
                </div>
            </header>

            {/* Hero Section */}
            <main className="container mx-auto px-4 py-16 animate-fade-in">
                <div className="text-center max-w-4xl mx-auto mb-16">
                    <div className="inline-block mb-4 px-4 py-2 bg-blue-100 dark:bg-blue-900/30 rounded-full text-blue-700 dark:text-blue-300 text-sm font-semibold">
                        ✨ Production-Ready URL Shortener
                    </div>
                    <h1 className="text-5xl md:text-7xl font-extrabold mb-6 bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 dark:from-blue-400 dark:via-purple-400 dark:to-pink-400 bg-clip-text text-transparent leading-tight">
                        Shorten URLs with
                        <br />
                        <span className="relative">
                            Powerful Analytics
                            <svg className="absolute -bottom-2 left-0 w-full" height="12" viewBox="0 0 200 12" fill="none">
                                <path d="M2 10C50 2 150 2 198 10" stroke="url(#gradient)" strokeWidth="3" strokeLinecap="round" />
                                <defs>
                                    <linearGradient id="gradient" x1="0%" y1="0%" x2="100%" y2="0%">
                                        <stop offset="0%" stopColor="#3B82F6" />
                                        <stop offset="50%" stopColor="#A855F7" />
                                        <stop offset="100%" stopColor="#EC4899" />
                                    </linearGradient>
                                </defs>
                            </svg>
                        </span>
                    </h1>
                    <p className="text-xl text-gray-600 dark:text-gray-300 mb-8 leading-relaxed">
                        Create short, memorable links with advanced tracking, QR codes, and custom branding.
                        <br className="hidden md:block" />
                        Built for professionals who need more than just a link shortener.
                    </p>
                </div>

                {/* URL Shortener Form */}
                <div className="max-w-3xl mx-auto mb-16">
                    <GlassCard className="p-8">
                        <div className="flex flex-col md:flex-row gap-4">
                            <input
                                type="url"
                                value={url}
                                onChange={(e) => { setUrl(e.target.value); setError(''); setShortenedUrl('') }}
                                placeholder="✨ Paste your long URL here..."
                                className="flex-1 px-6 py-4 border-2 border-gray-200 dark:border-gray-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-purple-500 focus:border-transparent dark:bg-gray-800/50 dark:text-white text-lg placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all"
                                onKeyDown={(e) => e.key === 'Enter' && handleShorten()}
                            />
                            <AnimatedButton
                                variant="primary"
                                size="lg"
                                onClick={handleShorten}
                                disabled={isLoading}
                            >
                                {isLoading ? (
                                    <span className="flex items-center gap-2">
                                        <span className="animate-spin h-5 w-5 border-2 border-white/30 border-t-white rounded-full" />
                                        Shortening...
                                    </span>
                                ) : (
                                    <>
                                        <Zap className="h-5 w-5" />
                                        Shorten URL
                                    </>
                                )}
                            </AnimatedButton>
                        </div>

                        {/* Error message */}
                        {error && (
                            <div className="mt-3 text-red-500 dark:text-red-400 text-sm font-medium animate-fade-in">
                                {error}
                            </div>
                        )}

                        {/* Result */}
                        {shortenedUrl && (
                            <div className="mt-6 p-4 bg-green-50 dark:bg-green-900/20 border border-green-200 dark:border-green-800 rounded-xl animate-fade-in">
                                <p className="text-sm text-green-600 dark:text-green-400 font-medium mb-2">
                                    ✅ Your shortened URL is ready!
                                </p>
                                <div className="flex items-center gap-3">
                                    <code className="flex-1 px-4 py-2 bg-white dark:bg-gray-800 rounded-lg text-blue-600 dark:text-blue-400 font-mono text-lg truncate">
                                        {shortenedUrl}
                                    </code>
                                    <button
                                        onClick={handleCopy}
                                        className="flex items-center gap-2 px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white rounded-lg transition-colors font-medium"
                                    >
                                        {copied ? <Check className="h-4 w-4" /> : <Copy className="h-4 w-4" />}
                                        {copied ? 'Copied!' : 'Copy'}
                                    </button>
                                </div>
                            </div>
                        )}

                        <div className="mt-4 flex items-center justify-center text-sm text-gray-500 dark:text-gray-400">
                            <Shield className="h-4 w-4 mr-2 text-green-500" />
                            No registration required • Free forever • HTTPS secure
                        </div>
                    </GlassCard>
                </div>

                {/* Features Grid */}
                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-6 mb-16">
                    <FeatureCard
                        icon={<BarChart3 className="h-10 w-10 text-blue-600 dark:text-blue-400" />}
                        title="Advanced Analytics"
                        description="Track clicks, locations, devices, and referrers in real-time with beautiful charts"
                        gradient="from-blue-500 to-cyan-500"
                    />
                    <FeatureCard
                        icon={<Link2 className="h-10 w-10 text-purple-600 dark:text-purple-400" />}
                        title="Custom Short Links"
                        description="Create branded short links with custom aliases that match your brand"
                        gradient="from-purple-500 to-pink-500"
                    />
                    <FeatureCard
                        icon={<Shield className="h-10 w-10 text-green-600 dark:text-green-400" />}
                        title="Password Protection"
                        description="Secure your links with password protection and expiration dates"
                        gradient="from-green-500 to-emerald-500"
                    />
                    <FeatureCard
                        icon={<Zap className="h-10 w-10 text-orange-600 dark:text-orange-400" />}
                        title="QR Codes"
                        description="Generate beautiful QR codes for every shortened URL automatically"
                        gradient="from-orange-500 to-red-500"
                    />
                </div>

                {/* How it Works Section */}
                <div className="mb-16">
                    <h2 className="text-3xl md:text-4xl font-bold text-center mb-12 bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
                        How It Works
                    </h2>
                    <div className="grid md:grid-cols-3 gap-8 max-w-4xl mx-auto">
                        <StepCard
                            step={1}
                            icon={<LinkIcon className="h-6 w-6" />}
                            title="Paste Your URL"
                            description="Enter any long URL you want to shorten — no signup needed"
                        />
                        <StepCard
                            step={2}
                            icon={<MousePointerClick className="h-6 w-6" />}
                            title="Click Shorten"
                            description="We generate a short, memorable link powered by base62 encoding"
                        />
                        <StepCard
                            step={3}
                            icon={<Share2 className="h-6 w-6" />}
                            title="Share & Track"
                            description="Share your link anywhere and track every click with our analytics"
                        />
                    </div>
                </div>

                {/* Stats Section */}
                <GlassCard className="bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 dark:from-blue-500 dark:via-purple-500 dark:to-pink-500 text-white border-none p-12">
                    <div className="grid md:grid-cols-3 gap-8 text-center">
                        <div className="group cursor-default">
                            <div className="text-5xl font-bold mb-2 group-hover:scale-110 transition-transform">10M+</div>
                            <div className="text-blue-100">Links Created</div>
                        </div>
                        <div className="group cursor-default">
                            <div className="text-5xl font-bold mb-2 group-hover:scale-110 transition-transform">100M+</div>
                            <div className="text-purple-100">Clicks Tracked</div>
                        </div>
                        <div className="group cursor-default">
                            <div className="text-5xl font-bold mb-2 group-hover:scale-110 transition-transform">50K+</div>
                            <div className="text-pink-100">Active Users</div>
                        </div>
                    </div>
                </GlassCard>
            </main>

            {/* Footer */}
            <footer className="border-t border-white/20 dark:border-gray-800/50 bg-white/30 dark:bg-gray-900/30 backdrop-blur-xl mt-16">
                <div className="container mx-auto px-4 py-12">
                    <div className="grid md:grid-cols-4 gap-8 mb-8">
                        {/* Brand */}
                        <div className="md:col-span-1">
                            <div className="flex items-center space-x-2 mb-4">
                                <Link2 className="h-6 w-6 text-blue-600 dark:text-blue-400" />
                                <span className="text-xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 dark:from-blue-400 dark:to-purple-400 bg-clip-text text-transparent">
                                    LinkShort
                                </span>
                            </div>
                            <p className="text-gray-500 dark:text-gray-400 text-sm leading-relaxed">
                                Production-grade URL shortener with analytics, QR codes, and enterprise security.
                            </p>
                        </div>

                        {/* Product */}
                        <div>
                            <h4 className="font-semibold text-gray-900 dark:text-white mb-4">Product</h4>
                            <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
                                <li><Link href="/dashboard" className="hover:text-blue-600 dark:hover:text-blue-400 transition">Dashboard</Link></li>
                                <li><Link href="/pricing" className="hover:text-blue-600 dark:hover:text-blue-400 transition">Pricing</Link></li>
                                <li><a href="/health" className="hover:text-blue-600 dark:hover:text-blue-400 transition">API Status</a></li>
                            </ul>
                        </div>

                        {/* Resources */}
                        <div>
                            <h4 className="font-semibold text-gray-900 dark:text-white mb-4">Resources</h4>
                            <ul className="space-y-2 text-sm text-gray-600 dark:text-gray-400">
                                <li><a href="/docs" className="hover:text-blue-600 dark:hover:text-blue-400 transition">API Docs</a></li>
                                <li><Link href="/privacy" className="hover:text-blue-600 dark:hover:text-blue-400 transition">Privacy Policy</Link></li>
                                <li><Link href="/terms" className="hover:text-blue-600 dark:hover:text-blue-400 transition">Terms of Service</Link></li>
                            </ul>
                        </div>

                        {/* Connect */}
                        <div>
                            <h4 className="font-semibold text-gray-900 dark:text-white mb-4">Connect</h4>
                            <div className="flex space-x-3">
                                <a href="https://github.com/ankit7610/URL-Shortener" target="_blank" rel="noopener noreferrer"
                                    className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-colors group">
                                    <Github className="h-5 w-5 text-gray-600 dark:text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400" />
                                </a>
                                <a href="#" className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-colors group">
                                    <Twitter className="h-5 w-5 text-gray-600 dark:text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400" />
                                </a>
                                <a href="#" className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-blue-100 dark:hover:bg-blue-900/30 transition-colors group">
                                    <Linkedin className="h-5 w-5 text-gray-600 dark:text-gray-400 group-hover:text-blue-600 dark:group-hover:text-blue-400" />
                                </a>
                            </div>
                        </div>
                    </div>

                    <div className="border-t border-gray-200 dark:border-gray-800 pt-6 text-center text-gray-500 dark:text-gray-400 text-sm">
                        <p>© 2026 LinkShort. All rights reserved. Built with ❤️ using Scala, ZIO & Next.js</p>
                    </div>
                </div>
            </footer>
        </div>
    )
}

function FeatureCard({ icon, title, description, gradient }: {
    icon: React.ReactNode
    title: string
    description: string
    gradient: string
}) {
    return (
        <GlassCard className="group cursor-default">
            <div className={`mb-4 w-16 h-16 rounded-2xl bg-gradient-to-br ${gradient} p-3 group-hover:scale-110 group-hover:rotate-3 transition-all duration-300 shadow-lg`}>
                <div className="w-full h-full flex items-center justify-center text-white">
                    {icon}
                </div>
            </div>
            <h3 className="text-xl font-bold mb-2 dark:text-white group-hover:text-blue-600 dark:group-hover:text-blue-400 transition-colors">
                {title}
            </h3>
            <p className="text-gray-600 dark:text-gray-300 leading-relaxed">
                {description}
            </p>
        </GlassCard>
    )
}

function StepCard({ step, icon, title, description }: {
    step: number
    icon: React.ReactNode
    title: string
    description: string
}) {
    return (
        <div className="text-center group">
            <div className="relative inline-flex items-center justify-center w-16 h-16 mb-4 rounded-full bg-gradient-to-br from-blue-600 to-purple-600 text-white shadow-lg group-hover:scale-110 transition-transform duration-300">
                {icon}
                <span className="absolute -top-2 -right-2 w-7 h-7 bg-pink-500 text-white text-xs font-bold rounded-full flex items-center justify-center shadow-md">
                    {step}
                </span>
            </div>
            {step < 3 && (
                <ArrowRight className="hidden md:block absolute right-0 top-1/2 -translate-y-1/2 h-6 w-6 text-gray-300 dark:text-gray-600" />
            )}
            <h3 className="text-lg font-bold mb-2 dark:text-white">
                {title}
            </h3>
            <p className="text-gray-600 dark:text-gray-300 text-sm leading-relaxed">
                {description}
            </p>
        </div>
    )
}
