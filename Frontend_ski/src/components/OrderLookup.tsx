'use client'

import { useState } from 'react'
import { User, Search, Phone, Loader2 } from 'lucide-react'
import { useLanguage } from '@/contexts/LanguageContext'

interface OrderLookupProps {
  onLookup: (orderNumber: string, phone: string) => Promise<boolean>
  onLogout?: () => void
  isCustomer?: boolean
  customerName?: string
}

export default function OrderLookup({
  onLookup,
  onLogout,
  isCustomer,
  customerName,
}: OrderLookupProps) {
  const { t } = useLanguage()
  const [orderNumber, setOrderNumber] = useState('')
  const [phone, setPhone] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [error, setError] = useState('')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    const trimmedOrder = orderNumber.trim()
    const trimmedPhone = phone.trim()
    if (!trimmedOrder || !trimmedPhone) return
    setError('')
    setIsLoading(true)
    try {
      const success = await onLookup(trimmedOrder, trimmedPhone)
      if (success) {
        setOrderNumber('')
        setPhone('')
        setError('')
      } else {
        setError(t('orderLookup.notFound'))
      }
    } catch (err) {
      const msg = err instanceof Error ? err.message : t('orderLookup.errorLookup')
      setError(msg)
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <div className="max-w-md mx-auto">
      <div className="text-center mb-6">
        <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
          <User className="w-8 h-8 text-white" />
        </div>
        <h2 className="text-2xl font-bold text-gray-900 mb-2">{t('orderLookup.title')}</h2>
        <p className="text-gray-600">
          {t('orderLookup.desc')}
        </p>
      </div>

      {isCustomer && customerName && (
        <p className="text-center text-sm text-gray-600 mb-4">
          {t('orderLookup.loggedInAs')} <strong>{customerName}</strong>.
          {onLogout && (
            <button type="button" onClick={onLogout} className="ml-2 text-blue-600 hover:underline">
              {t('orderLookup.logoutLink')}
            </button>
          )}
        </p>
      )}

      <form onSubmit={handleSubmit} className="bg-white p-8 rounded-lg shadow-md space-y-6">
        <div>
          <label htmlFor="orderNumber" className="block text-sm font-medium text-gray-700 mb-2">
            {t('orderLookup.orderNumber')}
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-gray-400" aria-hidden="true" />
            </div>
            <input
              type="text"
              id="orderNumber"
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder={t('orderLookup.orderNumberPlaceholder')}
              value={orderNumber}
              onChange={(e) => setOrderNumber(e.target.value)}
              required
              disabled={isLoading}
            />
          </div>
        </div>
        <div>
          <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-2">
            {t('orderLookup.phone')}
          </label>
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Phone className="h-5 w-5 text-gray-400" aria-hidden="true" />
            </div>
            <input
              type="tel"
              id="phone"
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder={t('orderLookup.phonePlaceholder')}
              value={phone}
              onChange={(e) => setPhone(e.target.value)}
              required
              disabled={isLoading}
            />
          </div>
        </div>
        {error && <p className="text-red-600 text-sm text-center">{error}</p>}
        <button
          type="submit"
          className="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50 disabled:cursor-not-allowed"
          disabled={isLoading}
        >
          {isLoading ? (
            <span className="flex items-center">
              <Loader2 className="animate-spin h-5 w-5 mr-3" />
              {t('common.loading')}
            </span>
          ) : (
            t('orderLookup.submit')
          )}
        </button>
      </form>
    </div>
  )
}
