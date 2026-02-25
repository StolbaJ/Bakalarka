'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { useAuth } from '@/contexts/AuthContext'
import ProtectedRoute from '@/components/ProtectedRoute'
import { User, Key, SlidersHorizontal, Activity } from 'lucide-react'

const navItems: { href: string; label: string; icon: typeof User; roles?: ('ADMIN' | 'TECHNICIAN' | 'CUSTOMER')[] }[] = [
  { href: '/account/profile', label: 'Profil', icon: User },
  { href: '/account/password', label: 'Změna hesla', icon: Key, roles: ['ADMIN', 'TECHNICIAN'] },
  { href: '/account/dalsi', label: 'Další nastavení', icon: SlidersHorizontal },
  { href: '/account/monitoring', label: 'Monitoring', icon: Activity, roles: ['ADMIN'] },
]

function AccountLayoutContent({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const { user } = useAuth()

  const visibleItems = navItems.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role))
  )

  return (
    <div className="flex gap-0 -mx-4 md:-mx-6 lg:mx-0 lg:gap-8 w-full max-w-6xl">
      {/* Levé menu – pevné, bez scrollu */}
      <aside className="w-56 shrink-0 flex flex-col border-r border-gray-200 bg-gray-50/80 rounded-l-lg overflow-hidden">
        <div className="p-4 border-b border-gray-200">
          <h2 className="font-semibold text-gray-900">Nastavení účtu</h2>
          <p className="text-xs text-gray-500 mt-0.5">{user?.fullName || user?.username}</p>
        </div>
        <nav className="flex flex-col p-2">
          {visibleItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-md text-sm font-medium transition-colors ${
                  isActive
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-700 hover:bg-gray-200'
                }`}
              >
                <Icon className="w-5 h-5 shrink-0" />
                <span>{item.label}</span>
              </Link>
            )
          })}
        </nav>
      </aside>

      {/* Obsah vpravo – většina obrazovky */}
      <div className="flex-1 min-w-0 py-2">
        {children}
      </div>
    </div>
  )
}

export default function AccountLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <AccountLayoutContent>{children}</AccountLayoutContent>
    </ProtectedRoute>
  )
}
