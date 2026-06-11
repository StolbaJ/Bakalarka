'use client'

import React, { createContext, useContext, useState, useEffect, useCallback, ReactNode } from 'react'

export type Locale = 'cs' | 'en'

const STORAGE_KEY = 'ski-locale'

const locales: Locale[] = ['cs', 'en']

function loadLocale(): Locale {
  if (typeof window === 'undefined') return 'cs'
  const stored = localStorage.getItem(STORAGE_KEY)
  if (stored === 'cs' || stored === 'en') return stored
  return 'cs'
}

type Translations = Record<string, unknown>

async function loadTranslations(locale: Locale): Promise<Translations> {
  if (locale === 'cs') {
    const mod = await import('@/locales/cs.json')
    return mod.default as Translations
  }
  const mod = await import('@/locales/en.json')
  return mod.default as Translations
}

function getNested(obj: Record<string, unknown>, path: string): string | undefined {
  const parts = path.split('.')
  let current: unknown = obj
  for (const part of parts) {
    if (current == null || typeof current !== 'object') return undefined
    current = (current as Record<string, unknown>)[part]
  }
  return typeof current === 'string' ? current : undefined
}

interface LanguageContextType {
  locale: Locale
  setLocale: (locale: Locale) => void
  t: (key: string) => string
  locales: Locale[]
  isReady: boolean
}

const LanguageContext = createContext<LanguageContextType | undefined>(undefined)

export function useLanguage() {
  const ctx = useContext(LanguageContext)
  if (ctx === undefined) {
    throw new Error('useLanguage must be used within a LanguageProvider')
  }
  return ctx
}

interface LanguageProviderProps {
  children: ReactNode
}

export function LanguageProvider({ children }: LanguageProviderProps) {
  const [locale, setLocaleState] = useState<Locale>('cs')
  const [translations, setTranslations] = useState<Translations>({})
  const [isReady, setIsReady] = useState(false)

  useEffect(() => {
    setLocaleState(loadLocale())
  }, [])

  useEffect(() => {
    if (!locale) return
    let cancelled = false
    loadTranslations(locale).then((t) => {
      if (!cancelled) {
        setTranslations(t)
        setIsReady(true)
      }
    })
    return () => { cancelled = true }
  }, [locale])

  const setLocale = useCallback((newLocale: Locale) => {
    setLocaleState(newLocale)
    if (typeof window !== 'undefined') {
      localStorage.setItem(STORAGE_KEY, newLocale)
    }
  }, [])

  const t = useCallback(
    (key: string): string => {
      const value = getNested(translations as Record<string, unknown>, key)
      return value ?? key
    },
    [translations]
  )

  const value: LanguageContextType = {
    locale,
    setLocale,
    t,
    locales,
    isReady,
  }

  return (
    <LanguageContext.Provider value={value}>
      {children}
    </LanguageContext.Provider>
  )
}
