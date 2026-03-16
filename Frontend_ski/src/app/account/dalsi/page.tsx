'use client'

import { SlidersHorizontal } from 'lucide-react'
import { useLanguage } from '@/contexts/LanguageContext'

export default function DalsiNastaveniPage() {
  const { t } = useLanguage()
  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200 bg-white">
        <h1 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
          <SlidersHorizontal className="w-5 h-5 text-gray-600" />
          {t('account.otherSettings')}
        </h1>
        <p className="text-sm text-gray-500 mt-1">{t('account.otherSettingsSubtitle')}</p>
      </div>
      <div className="p-6 text-gray-500 text-sm">
        {t('account.otherSettingsDesc')}
      </div>
    </div>
  )
}
