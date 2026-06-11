'use client'

import { useEffect } from 'react'
import { useLanguage } from '@/contexts/LanguageContext'

/** Sets document.documentElement.lang when locale changes (for a11y and SEO). */
export default function SetHtmlLang() {
  const { locale } = useLanguage()
  useEffect(() => {
    document.documentElement.lang = locale
  }, [locale])
  return null
}
