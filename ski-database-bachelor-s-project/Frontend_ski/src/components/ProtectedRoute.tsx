'use client'

import { ReactNode } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { useRouter } from 'next/navigation'
import { useEffect } from 'react'

interface ProtectedRouteProps {
  children: ReactNode
  requiredRole?: 'ADMIN' | 'CUSTOMER' | 'ADMIN_OR_TECHNICIAN'
  fallback?: ReactNode
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ 
  children, 
  requiredRole, 
  fallback 
}) => {
  const { user, isLoading } = useAuth()
  const router = useRouter()

  const hasAccess = !user ? false
    : !requiredRole ? true
    : requiredRole === 'ADMIN_OR_TECHNICIAN'
      ? (user.role === 'ADMIN' || user.role === 'TECHNICIAN')
      : user.role === requiredRole

  useEffect(() => {
    if (isLoading) return
    if (!user) {
      router.push('/')
      return
    }

    if (requiredRole && !hasAccess) {
      router.push('/')
      return
    }
  }, [user, requiredRole, hasAccess, isLoading, router])

  if (isLoading) {
    return fallback ?? (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <div className="inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-blue-600 border-r-transparent mb-4" />
          <p className="text-gray-600">Načítání…</p>
        </div>
      </div>
    )
  }

  if (!user) {
    return fallback || (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Přístup odepřen</h2>
          <p className="text-gray-600">Pro přístup k této stránce se musíte přihlásit.</p>
        </div>
      </div>
    )
  }

  if (requiredRole && !hasAccess) {
    return fallback || (
      <div className="min-h-screen flex items-center justify-center">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-900 mb-4">Nedostatečná oprávnění</h2>
          <p className="text-gray-600">Nemáte oprávnění pro přístup k této stránce.</p>
        </div>
      </div>
    )
  }

  return <>{children}</>
}

export default ProtectedRoute
