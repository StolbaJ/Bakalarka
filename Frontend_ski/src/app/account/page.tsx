'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'
import { useLanguage } from '@/contexts/LanguageContext'

export default function AccountPage() {
  const router = useRouter()
  const { t } = useLanguage()

  useEffect(() => {
    router.replace('/account/profile')
  }, [router])

  return (
    <div className="flex items-center justify-center py-12">
      <p className="text-gray-500">{t('account.redirecting')}</p>
    </div>
  )
}
