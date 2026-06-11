'use client'

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react'
import apiClient, { AuthSessionInfo } from '@/lib/api'

const SESSION_USER_KEY = 'ski_session_user'

interface User {
  username: string
  role: 'ADMIN' | 'TECHNICIAN' | 'CUSTOMER'
  fullName?: string
  orderNumber?: string
  userId?: number
  email?: string
}

/** Role, které se přihlašují přes /api/auth/login (jedno volání, role z odpovědi). */
const STAFF_ROLES: User['role'][] = ['ADMIN', 'TECHNICIAN']

interface AuthContextType {
  user: User | null
  /** Jedno volání API – přihlásí jako ADMIN nebo TECHNICIAN (případně další role v STAFF_ROLES). */
  loginStaff: (username: string, password: string) => Promise<boolean>
  loginCustomer: (orderNumber: string, phone: string) => Promise<boolean>
  /** Přihlásí zákazníka podle odpovědi z odkazů v e-mailu (session cookie nastaví server). */
  setUserFromAuthResponse: (response: AuthSessionInfo) => void
  logout: () => void
  refreshUser: () => Promise<void>
  isLoading: boolean
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const useAuth = () => {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}

interface AuthProviderProps {
  children: ReactNode
}

function sessionToUser(parsed: Record<string, unknown>): User | null {
  if (typeof parsed.username !== 'string' || typeof parsed.role !== 'string') return null
  const role = parsed.role as User['role']
  return {
    username: parsed.username as string,
    role,
    fullName: typeof parsed.fullName === 'string' ? parsed.fullName : undefined,
    orderNumber: typeof parsed.orderNumber === 'string' ? parsed.orderNumber : undefined,
    userId: typeof parsed.userId === 'number' ? parsed.userId : undefined,
    email: typeof parsed.email === 'string' ? parsed.email : undefined,
  }
}

function persistUser(u: User) {
  sessionStorage.setItem(SESSION_USER_KEY, JSON.stringify(u))
}

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const logout = useCallback(() => {
    setUser(null)
    sessionStorage.removeItem(SESSION_USER_KEY)
    try {
      localStorage.removeItem('user')
    } catch {
      /* ignore */
    }
    void apiClient.logout().catch(() => {})
  }, [])

  const refreshUser = async () => {
    const stored = sessionStorage.getItem(SESSION_USER_KEY)
    if (!stored) return
    try {
      const parsed = JSON.parse(stored) as Record<string, unknown>
      const prev = sessionToUser(parsed)
      if (!prev) return
      const response = await apiClient.validateSession()
      const updated: User = {
        ...prev,
        username: response.username,
        fullName: response.fullName ?? undefined,
        email: response.email ?? undefined,
        userId: response.userId,
      }
      setUser(updated)
      persistUser(updated)
    } catch {
      // session invalid
    }
  }

  useEffect(() => {
    let active = true
    try {
      localStorage.removeItem('user')
    } catch {
      /* ignore */
    }

    const run = async () => {
      try {
        await apiClient.ensureCsrfCookie()
        const storedUser = sessionStorage.getItem(SESSION_USER_KEY)
        if (!storedUser) return

        let parsed: Record<string, unknown>
        try {
          parsed = JSON.parse(storedUser) as Record<string, unknown>
        } catch {
          sessionStorage.removeItem(SESSION_USER_KEY)
          return
        }
        const initial = sessionToUser(parsed)
        if (!initial) {
          sessionStorage.removeItem(SESSION_USER_KEY)
          return
        }
        if (active) setUser(initial)

        try {
          const response = await apiClient.validateSession()
          if (!active) return
          const updated: User = {
            username: response.username,
            role: initial.role,
            fullName: response.fullName ?? undefined,
            email: response.email ?? undefined,
            userId: response.userId,
            orderNumber: initial.orderNumber,
          }
          setUser(updated)
          persistUser(updated)
        } catch (err: unknown) {
          if (!active) return
          const status = (err as Error & { status?: number })?.status
          if (status === 401) {
            setUser(null)
            sessionStorage.removeItem(SESSION_USER_KEY)
          }
        }
      } catch {
        if (!active) return
        setUser(null)
        sessionStorage.removeItem(SESSION_USER_KEY)
      } finally {
        if (active) setIsLoading(false)
      }
    }

    void run()
    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    apiClient.setOnUnauthorized(logout)
    return () => apiClient.setOnUnauthorized(null)
  }, [logout])

  const loginStaff = async (username: string, password: string): Promise<boolean> => {
    try {
      const response = await apiClient.login(username, password)
      const role = response.role as User['role']
      if (!STAFF_ROLES.includes(role)) {
        return false
      }
      const newUser: User = {
        username: response.username,
        role,
        fullName: response.fullName ?? undefined,
        email: response.email ?? undefined,
        userId: response.userId,
      }
      setUser(newUser)
      persistUser(newUser)
      await apiClient.ensureCsrfCookie()
      return true
    } catch (error) {
      const e = error as Error & { status?: number; retryAfter?: number }
      if (e.status === 429) throw error
      console.error('Staff login failed:', error)
      return false
    }
  }

  const loginCustomer = async (orderNumber: string, phone: string): Promise<boolean> => {
    try {
      const response = await apiClient.loginCustomer(orderNumber, phone)

      if (response.role !== 'CUSTOMER') {
        throw new Error('User does not have CUSTOMER role')
      }

      const newUser: User = {
        username: response.username,
        role: 'CUSTOMER',
        fullName: response.fullName || undefined,
        email: response.email || undefined,
        userId: response.userId,
      }

      setUser(newUser)
      persistUser(newUser)
      await apiClient.ensureCsrfCookie()
      return true
    } catch (error) {
      const e = error as Error & { status?: number; retryAfter?: number }
      if (e.status === 429) throw error
      console.error('Customer login failed:', error)
      return false
    }
  }

  const setUserFromAuthResponse = (response: AuthSessionInfo) => {
    if (response.role !== 'CUSTOMER') return
    const newUser: User = {
      username: response.username,
      role: 'CUSTOMER',
      fullName: response.fullName ?? undefined,
      email: response.email ?? undefined,
      userId: response.userId,
    }
    setUser(newUser)
    persistUser(newUser)
  }

  const value: AuthContextType = {
    user,
    loginStaff,
    loginCustomer,
    setUserFromAuthResponse,
    logout,
    refreshUser,
    isLoading,
  }

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
