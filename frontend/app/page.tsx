import { Link2, BarChart3, Shield, Zap, Sparkles } from 'lucide-react'
import Link from 'next/link'
import { ThemeToggle } from '@/components/theme-toggle'
import { AnimatedButton } from '@/components/animated-button'
import { GlassCard } from '@/components/glass-card'

export default function HomePage() {
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
                                placeholder="✨ Paste your long URL here..."
                                className="flex-1 px-6 py-4 border-2 border-gray-200 dark:border-gray-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-blue-500 dark:focus:ring-purple-500 focus:border-transparent dark:bg-gray-800/50 dark:text-white text-lg placeholder:text-gray-400 dark:placeholder:text-gray-500 transition-all"
                            />
                            <AnimatedButton variant="primary" size="lg">
                                <Zap className="h-5 w-5" />
                                Shorten URL
                            </AnimatedButton>
                        </div>
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
                <div className="container mx-auto px-4 py-8 text-center text-gray-600 dark:text-gray-400">
                    <p>© 2026 LinkShort. All rights reserved.</p>
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

