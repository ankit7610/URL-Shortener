import { Link2, BarChart3, Shield, Zap } from 'lucide-react'
import Link from 'next/link'

export default function HomePage() {
    return (
        <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50 dark:from-gray-900 dark:via-gray-800 dark:to-gray-900">
            {/* Header */}
            <header className="border-b bg-white/50 dark:bg-gray-900/50 backdrop-blur-sm sticky top-0 z-50">
                <div className="container mx-auto px-4 py-4 flex justify-between items-center">
                    <div className="flex items-center space-x-2">
                        <Link2 className="h-8 w-8 text-blue-600" />
                        <span className="text-2xl font-bold bg-gradient-to-r from-blue-600 to-purple-600 bg-clip-text text-transparent">
                            LinkShort
                        </span>
                    </div>
                    <nav className="flex items-center space-x-6">
                        <Link href="/dashboard" className="text-gray-600 hover:text-gray-900 dark:text-gray-300 dark:hover:text-white transition">
                            Dashboard
                        </Link>
                        <Link href="/login" className="text-gray-600 hover:text-gray-900 dark:text-gray-300 dark:hover:text-white transition">
                            Login
                        </Link>
                        <Link href="/register" className="bg-blue-600 text-white px-4 py-2 rounded-lg hover:bg-blue-700 transition">
                            Sign Up
                        </Link>
                    </nav>
                </div>
            </header>

            {/* Hero Section */}
            <main className="container mx-auto px-4 py-16">
                <div className="text-center max-w-4xl mx-auto mb-16">
                    <h1 className="text-5xl md:text-6xl font-bold mb-6 bg-gradient-to-r from-blue-600 via-purple-600 to-pink-600 bg-clip-text text-transparent">
                        Shorten URLs with
                        <br />
                        Powerful Analytics
                    </h1>
                    <p className="text-xl text-gray-600 dark:text-gray-300 mb-8">
                        Create short, memorable links with advanced tracking, QR codes, and custom branding.
                        Built for professionals who need more than just a link shortener.
                    </p>
                </div>

                {/* URL Shortener Form */}
                <div className="max-w-3xl mx-auto mb-16">
                    <div className="bg-white dark:bg-gray-800 rounded-2xl shadow-2xl p-8">
                        <div className="flex flex-col md:flex-row gap-4">
                            <input
                                type="url"
                                placeholder="Enter your long URL here..."
                                className="flex-1 px-6 py-4 border border-gray-300 dark:border-gray-600 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:text-white text-lg"
                            />
                            <button className="bg-gradient-to-r from-blue-600 to-purple-600 text-white px-8 py-4 rounded-lg hover:from-blue-700 hover:to-purple-700 transition font-semibold text-lg shadow-lg">
                                Shorten URL
                            </button>
                        </div>
                        <div className="mt-4 flex items-center text-sm text-gray-500 dark:text-gray-400">
                            <Shield className="h-4 w-4 mr-2" />
                            No registration required • Free forever • HTTPS secure
                        </div>
                    </div>
                </div>

                {/* Features Grid */}
                <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-8 mb-16">
                    <FeatureCard
                        icon={<BarChart3 className="h-8 w-8 text-blue-600" />}
                        title="Advanced Analytics"
                        description="Track clicks, locations, devices, and referrers in real-time"
                    />
                    <FeatureCard
                        icon={<Link2 className="h-8 w-8 text-purple-600" />}
                        title="Custom Short Links"
                        description="Create branded short links with custom aliases"
                    />
                    <FeatureCard
                        icon={<Shield className="h-8 w-8 text-green-600" />}
                        title="Password Protection"
                        description="Secure your links with password protection"
                    />
                    <FeatureCard
                        icon={<Zap className="h-8 w-8 text-orange-600" />}
                        title="QR Codes"
                        description="Generate QR codes for every shortened URL"
                    />
                </div>

                {/* Stats Section */}
                <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-2xl p-12 text-white text-center">
                    <div className="grid md:grid-cols-3 gap-8">
                        <div>
                            <div className="text-4xl font-bold mb-2">10M+</div>
                            <div className="text-blue-100">Links Created</div>
                        </div>
                        <div>
                            <div className="text-4xl font-bold mb-2">100M+</div>
                            <div className="text-blue-100">Clicks Tracked</div>
                        </div>
                        <div>
                            <div className="text-4xl font-bold mb-2">50K+</div>
                            <div className="text-blue-100">Active Users</div>
                        </div>
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="border-t bg-white/50 dark:bg-gray-900/50 backdrop-blur-sm mt-16">
                <div className="container mx-auto px-4 py-8 text-center text-gray-600 dark:text-gray-400">
                    <p>© 2026 LinkShort. Built with ❤️ for FAANG interviews.</p>
                </div>
            </footer>
        </div>
    )
}

function FeatureCard({ icon, title, description }: { icon: React.ReactNode; title: string; description: string }) {
    return (
        <div className="bg-white dark:bg-gray-800 rounded-xl p-6 shadow-lg hover:shadow-xl transition">
            <div className="mb-4">{icon}</div>
            <h3 className="text-xl font-semibold mb-2 dark:text-white">{title}</h3>
            <p className="text-gray-600 dark:text-gray-300">{description}</p>
        </div>
    )
}
