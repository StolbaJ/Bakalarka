'use client'

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import apiClient, { AuthResponse } from '@/lib/api'

interface User {
  username: string
  role: 'ADMIN' | 'TECHNICIAN' | 'CUSTOMER'
  token: string
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
  /** Přihlásí zákazníka podle odpovědi z odkazů v e-mailu (token z objednávky). */
  setUserFromAuthResponse: (response: AuthResponse) => void
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

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  const logout = () => {
    setUser(null)
    localStorage.removeItem('user')
  }

  const refreshUser = async () => {
    const stored = localStorage.getItem('user')
    if (!stored) return
    try {
      const parsed = JSON.parse(stored) as User
      const response = await apiClient.validateSession()
      const updated: User = {
        ...parsed,
        username: response.username,
        token: response.token,
        fullName: response.fullName ?? undefined,
        email: response.email ?? undefined,
        userId: response.userId,
      }
      setUser(updated)
      localStorage.setItem('user', JSON.stringify(updated))
    } catch {
      // session invalid
    }
  }

  useEffect(() => {
    let cancelled = false
    const storedUser = localStorage.getItem('user')
    if (!storedUser) {
      setIsLoading(false)
      return
    }
    let parsed: User
    try {
      parsed = JSON.parse(storedUser) as User
    } catch (error) {
      console.error('Failed to parse stored user:', error)
      localStorage.removeItem('user')
      setIsLoading(false)
      return
    }
    setUser(parsed)
    apiClient.validateSession()
      .then((response) => {
        if (cancelled) return
        const updated: User = {
          username: response.username,
          role: parsed.role,
          token: response.token,
          fullName: response.fullName ?? undefined,
          email: response.email ?? undefined,
          userId: response.userId,
          orderNumber: parsed.orderNumber,
        }
        setUser(updated)
        localStorage.setItem('user', JSON.stringify(updated))
      })
      .catch((err: Error & { status?: number }) => {
        if (cancelled) return
        if (err?.status === 401) {
          setUser(null)
          localStorage.removeItem('user')
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })
    return () => { cancelled = true }
  }, [])

  useEffect(() => {
    apiClient.setOnUnauthorized(logout)
    return () => apiClient.setOnUnauthorized(null)
  }, [])

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
        token: response.token,
        fullName: response.fullName ?? undefined,
        email: response.email ?? undefined,
        userId: response.userId,
      }
      setUser(newUser)
      localStorage.setItem('user', JSON.stringify(newUser))
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
        token: response.token,
        fullName: response.fullName || undefined,
        email: response.email || undefined,
        userId: response.userId,
      }
      
      setUser(newUser)
      localStorage.setItem('user', JSON.stringify(newUser))
      return true
    } catch (error) {
      const e = error as Error & { status?: number; retryAfter?: number }
      if (e.status === 429) throw error
      console.error('Customer login failed:', error)
      return false
    }
  }

  const setUserFromAuthResponse = (response: AuthResponse) => {
    if (response.role !== 'CUSTOMER') return
    const newUser: User = {
      username: response.username,
      role: 'CUSTOMER',
      token: response.token,
      fullName: response.fullName ?? undefined,
      email: response.email ?? undefined,
      userId: response.userId,
    }
    setUser(newUser)
    localStorage.setItem('user', JSON.stringify(newUser))
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
