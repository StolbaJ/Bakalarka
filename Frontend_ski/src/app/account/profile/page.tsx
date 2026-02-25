'use client'

import { useState, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { useLanguage } from '@/contexts/LanguageContext'
import { apiClient } from '@/lib/api'
import { User, Loader2 } from 'lucide-react'

export default function ProfilePage() {
  const { t } = useLanguage()
  const { user, refreshUser } = useAuth()
  const canEditProfile = user?.role === 'ADMIN' || user?.role === 'TECHNICIAN'

  const [fullName, setFullName] = useState(user?.fullName ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [profileSaving, setProfileSaving] = useState(false)
  const [profileError, setProfileError] = useState('')
  const [profileSuccess, setProfileSuccess] = useState(false)

  useEffect(() => {
    if (user) {
      setFullName(user.fullName ?? '')
      setEmail(user.email ?? '')
    }
  }, [user])

  const handleProfileSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setProfileError('')
    setProfileSuccess(false)
    setProfileSaving(true)
    try {
      await apiClient.updateProfile({ fullName: fullName.trim() || null, email: email.trim() || null })
      await refreshUser()
      setProfileSuccess(true)
    } catch (err) {
      const msg = err instanceof Error ? err.message : ''
      setProfileError(msg || t('account.saveError'))
    } finally {
      setProfileSaving(false)
    }
  }

  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200 bg-white">
        <h1 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
          <User className="w-5 h-5 text-gray-600" />
          {t('account.profile')}
        </h1>
        <p className="text-sm text-gray-500 mt-1">{t('account.profileDesc')}</p>
      </div>
      <div className="p-6">
        {canEditProfile ? (
          <form onSubmit={handleProfileSubmit} className="space-y-4 max-w-md">
            <div>
              <label htmlFor="fullName" className="block text-sm font-medium text-gray-700 mb-1">
                {t('common.name')}
              </label>
              <input
                id="fullName"
                type="text"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={fullName}
                onChange={(e) => setFullName(e.target.value)}
                disabled={profileSaving}
              />
            </div>
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-1">
                {t('common.email')}
              </label>
              <input
                id="email"
                type="email"
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                disabled={profileSaving}
              />
            </div>
            {profileError && <p className="text-sm text-red-600">{profileError}</p>}
            {profileSuccess && <p className="text-sm text-green-600">{t('account.saved')}</p>}
            <button
              type="submit"
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
              disabled={profileSaving}
            >
              {profileSaving ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" />
                  {t('common.saving')}
                </>
              ) : (
                t('common.save')
              )}
            </button>
          </form>
        ) : (
          <div className="space-y-2 text-gray-600 max-w-md">
            <p><span className="font-medium text-gray-700">{t('common.name')}:</span> {user?.fullName || t('common.notSet')}</p>
            <p><span className="font-medium text-gray-700">{t('common.email')}:</span> {user?.email || t('common.notSet')}</p>
            <p className="text-sm mt-2">{t('account.customerNoEdit')}</p>
          </div>
        )}
      </div>
    </div>
  )
}
