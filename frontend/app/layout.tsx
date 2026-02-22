import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import { ThemeProvider } from '@/components/theme-provider'
import { Toaster } from 'sonner'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
    title: 'LinkShort — Fast, Secure URL Shortener with Analytics',
    description: 'Production-grade URL shortener with advanced analytics, QR codes, custom short links, and password protection. Built with Scala, ZIO & Next.js.',
    keywords: ['url shortener', 'link shortener', 'analytics', 'qr code', 'custom links', 'link management'],
    authors: [{ name: 'LinkShort' }],
    openGraph: {
        title: 'LinkShort — Shorten URLs with Powerful Analytics',
        description: 'Create short, memorable links with advanced tracking, QR codes, and custom branding. Free and production-ready.',
        type: 'website',
        locale: 'en_US',
        siteName: 'LinkShort',
    },
    twitter: {
        card: 'summary_large_image',
        title: 'LinkShort — Fast, Secure URL Shortener',
        description: 'Shorten URLs, generate QR codes, and track clicks with real-time analytics.',
    },
    robots: {
        index: true,
        follow: true,
    },
}

export default function RootLayout({
    children,
}: {
    children: React.ReactNode
}) {
    return (
        <html lang="en" suppressHydrationWarning>
            <body className={inter.className}>
                <ThemeProvider
                    attribute="class"
                    defaultTheme="system"
                    enableSystem
                    disableTransitionOnChange
                >
                    {children}
                    <Toaster position="top-right" richColors />
                </ThemeProvider>
            </body>
        </html>
    )
}
