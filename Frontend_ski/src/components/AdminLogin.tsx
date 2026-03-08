'use client'

import { useState, useEffect, useRef } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { useLanguage } from '@/contexts/LanguageContext'
import { apiClient, CredentialsHintResponse } from '@/lib/api'
import { X, Shield, Loader2 } from 'lucide-react'

interface AdminLoginProps {
  onClose: () => void
}

const AdminLogin: React.FC<AdminLoginProps> = ({ onClose }) => {
  const { t } = useLanguage()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')
  const [rateLimitSeconds, setRateLimitSeconds] = useState<number | null>(null)
  const [credentialsHint, setCredentialsHint] = useState<CredentialsHintResponse | null>(null)
  const { loginStaff } = useAuth()

  useEffect(() => {
    if (rateLimitSeconds === null || rateLimitSeconds <= 0) return
    const id = setInterval(() => {
      setRateLimitSeconds((s) => (s === null || s <= 1 ? null : s - 1))
    }, 1000)
    return () => clearInterval(id)
  }, [rateLimitSeconds])
  const prevRateLimitRef = useRef<number | null>(null)
  useEffect(() => {
    if (prevRateLimitRef.current !== null && rateLimitSeconds === null) setError('')
    prevRateLimitRef.current = rateLimitSeconds
  }, [rateLimitSeconds])

  useEffect(() => {
    apiClient.getCredentialsHint().then(setCredentialsHint).catch(() => setCredentialsHint({ showHint: false }))
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)
    setError('')

    try {
      const success = await loginStaff(username, password)
      if (success) {
        onClose()
        return
      }
      setError(t('adminLogin.invalidCredentials'))
    } catch (err) {
      const e = err as Error & { status?: number; retryAfter?: number }
      if (e.status === 429 && typeof e.retryAfter === 'number') {
        setRateLimitSeconds(e.retryAfter)
        setError(t('rateLimit.wait').replace('{seconds}', String(e.retryAfter)))
      } else {
        setError(err instanceof Error ? err.message : t('adminLogin.errorLogin'))
      }
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="fixed inset-0 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-md relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
        >
          <X className="w-5 h-5" />
        </button>
        
        <div className="text-center mb-6">
          <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <Shield className="w-8 h-8 text-white" />
          </div>
          <h2 className="text-2xl font-bold text-gray-900">{t('adminLogin.title')}</h2>
          <p className="text-gray-600 mt-2">{t('adminLogin.subtitle')}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="username" className="block text-sm font-medium text-gray-700 mb-2">
              {t('adminLogin.username')}
            </label>
            <input
              type="text"
              id="username"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              disabled={isLoading}
            />
          </div>
          
          <div>
            <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
              {t('adminLogin.password')}
            </label>
            <input
              type="password"
              id="password"
              className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              disabled={isLoading}
            />
          </div>
          
          {error && (
            <p className={`text-sm text-center ${rateLimitSeconds !== null ? 'text-amber-700' : 'text-red-600'}`}>
              {rateLimitSeconds !== null
                ? t('rateLimit.wait').replace('{seconds}', String(rateLimitSeconds))
                : error}
            </p>
          )}
          <button
            type="submit"
            className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
            disabled={isLoading || rateLimitSeconds !== null}
          >
            {isLoading ? (
              <span className="flex items-center">
                <Loader2 className="animate-spin h-5 w-5 mr-3" />
                {t('adminLogin.submitting')}
              </span>
            ) : (
              t('adminLogin.submit')
            )}
          </button>
        </form>
        
        {credentialsHint?.showHint && (
          <div className="mt-6 text-center text-sm text-gray-500 space-y-2">
            <p>{t('adminLogin.defaultCredentials')}</p>
            <p className="text-left">{t('adminLogin.adminCreds')} <strong>admin</strong>, {t('adminLogin.password').toLowerCase()} <strong>admin123</strong></p>
            <p className="text-left">{t('adminLogin.technicianCreds')} <strong>technician</strong>, {t('adminLogin.password').toLowerCase()} <strong>tech123</strong></p>
          </div>
        )}
      </div>
    </div>
  )
}

export default AdminLogin
