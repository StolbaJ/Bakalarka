import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'
import Navigation from '@/components/Navigation'
import { AuthProvider } from '@/contexts/AuthContext'
import { LanguageProvider } from '@/contexts/LanguageContext'
import SetHtmlLang from '@/components/SetHtmlLang'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'Ski Inventory Management',
  description: 'Aplikace pro inventuru lyží s napojením zařízení',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="cs" suppressHydrationWarning>
      <body className={inter.className}>
        <LanguageProvider>
          <SetHtmlLang />
          <AuthProvider>
            <div className="min-h-screen bg-gray-50">
              <Navigation />
              <main className="container mx-auto px-4 py-8">
                {children}
              </main>
            </div>
          </AuthProvider>
        </LanguageProvider>
      </body>
    </html>
  )
}
