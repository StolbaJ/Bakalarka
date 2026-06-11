'use client'

import Link from 'next/link'
import Image from 'next/image'
import { usePathname } from 'next/navigation'
import { 
  Home, 
  QrCode, 
  Database, 
  Package,
  BarChart3, 
  User,
  Shield,
  LogOut,
  Users,
  Settings,
  ChevronDown,
  BookOpen,
  Globe
} from 'lucide-react'
import { useState, useRef, useEffect } from 'react'
import { useAuth } from '@/contexts/AuthContext'
import { useLanguage } from '@/contexts/LanguageContext'
import { apiClient } from '@/lib/api'
import AdminLogin from './AdminLogin'
import type { Locale } from '@/contexts/LanguageContext'

const FLAGS: Record<Locale, string> = { cs: '🇨🇿', en: '🇬🇧' }

const Navigation = () => {
  const pathname = usePathname()
  const [showAdminLogin, setShowAdminLogin] = useState(false)
  const [showUserMenu, setShowUserMenu] = useState(false)
  const [showLangMenu, setShowLangMenu] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const langMenuRef = useRef<HTMLDivElement>(null)
  const mobileMenuRef = useRef<HTMLDivElement>(null)
  const [showMobileMenu, setShowMobileMenu] = useState(false)
  const { user, logout } = useAuth()
  const { locale, setLocale, t, locales, isReady } = useLanguage()

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      const target = e.target as Node
      const clickedOutsideUserMenu =
        menuRef.current && !menuRef.current.contains(target)
      const clickedOutsideLangMenu =
        langMenuRef.current && !langMenuRef.current.contains(target)
      const clickedOutsideMobileMenu =
        mobileMenuRef.current && !mobileMenuRef.current.contains(target)

      if (clickedOutsideUserMenu && clickedOutsideLangMenu && clickedOutsideMobileMenu) {
        setShowUserMenu(false)
        setShowLangMenu(false)
        setShowMobileMenu(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const getNavigation = () => {
    if (user?.role === 'ADMIN') {
      return [
        { name: t('nav.home'), href: '/', icon: Home },
        { name: t('nav.qrScanner'), href: '/scanner', icon: QrCode },
        { name: t('nav.skiDatabase'), href: '/database', icon: Database },
        { name: t('nav.orders'), href: '/orders', icon: Package },
        { name: t('nav.statistics'), href: '/statistics', icon: BarChart3 },
        { name: t('nav.settings'), href: '/settings', icon: Settings },
        { name: t('nav.userManagement'), href: '/users', icon: Users },
      ]
    } else if (user?.role === 'TECHNICIAN') {
      return [
        { name: t('nav.home'), href: '/', icon: Home },
        { name: t('nav.qrScanner'), href: '/scanner', icon: QrCode },
        { name: t('nav.skiDatabase'), href: '/database', icon: Database },
        { name: t('nav.orders'), href: '/orders', icon: Package },
      ]
    } else if (user?.role === 'CUSTOMER') {
      return [
        { name: t('nav.home'), href: '/', icon: Home },
        { name: t('nav.myOrders'), href: '/customer', icon: User },
      ]
    } else {
      return [
        { name: t('nav.home'), href: '/', icon: Home },
      ]
    }
  }

  const navigation = getNavigation()

  return (
    <nav className="bg-white shadow-lg border-b">
      <div ref={mobileMenuRef} className="md:contents">
        <div className="container mx-auto px-4">
          <div className="flex justify-between items-center h-16">
            <div className="flex items-center space-x-8">
            {/* Logo + název jako mobilní menu trigger */}
            <button
              type="button"
              onClick={() => setShowMobileMenu((prev) => !prev)}
              className="flex items-center space-x-2 md:hidden focus:outline-none"
            >
              <Image src="/logo.svg" alt="Logo" width={32} height={32} className="rounded-lg" />
              <span className="font-bold text-xl text-gray-800">
                {isReady ? t('nav.appName') : 'Ski Inventory'}
              </span>
              <ChevronDown
                className={`w-4 h-4 text-gray-500 transition-transform ${
                  showMobileMenu ? 'rotate-180' : ''
                }`}
              />
            </button>

            {/* Původní odkaz na domovskou stránku pro větší obrazovky */}
            <Link
              href="/"
              className="hidden md:flex items-center space-x-2"
            >
              <Image src="/logo.svg" alt="Logo" width={32} height={32} className="rounded-lg" />
              <span className="font-bold text-xl text-gray-800">
                {isReady ? t('nav.appName') : 'Ski Inventory'}
              </span>
            </Link>
            
            <div className="hidden md:flex space-x-1">
              {navigation.map((item) => {
                const Icon = item.icon
                const isActive = pathname === item.href
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-blue-100 text-blue-700'
                        : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span>{item.name}</span>
                  </Link>
                )
              })}
            </div>
          </div>

          <div className="flex items-center space-x-2 sm:space-x-4 relative">
            {/* Language selector (flag) */}
            {isReady && (
              <div className="relative" ref={langMenuRef}>
                <button
                  type="button"
                  onClick={() => setShowLangMenu((v) => !v)}
                  className="flex items-center space-x-1.5 px-2 py-1.5 rounded-md hover:bg-gray-100 transition-colors"
                  title={t('language.' + locale)}
                  aria-label={t('language.' + locale)}
                >
                  <span className="text-xl leading-none" aria-hidden>{FLAGS[locale]}</span>
                  <Globe className="w-4 h-4 text-gray-500" />
                  <ChevronDown className={`w-4 h-4 text-gray-500 transition-transform ${showLangMenu ? 'rotate-180' : ''}`} />
                </button>
                {showLangMenu && (
                  <div className="absolute right-0 top-full mt-1 w-40 bg-white rounded-md shadow-lg border border-gray-200 py-1 z-40">
                    {locales.map((loc) => (
                      <button
                        key={loc}
                        type="button"
                        onClick={() => {
                          setLocale(loc)
                          setShowLangMenu(false)
                        }}
                        className={`flex items-center space-x-2 w-full px-4 py-2 text-sm text-left hover:bg-gray-100 ${locale === loc ? 'bg-blue-50 text-blue-700' : 'text-gray-700'}`}
                      >
                        <span className="text-lg">{FLAGS[loc]}</span>
                        <span>{t('language.' + loc)}</span>
                      </button>
                    ))}
                  </div>
                )}
              </div>
            )}
            {user ? (
              <div className="flex items-center space-x-3" ref={menuRef}>
                <button
                  onClick={() => setShowUserMenu(!showUserMenu)}
                  className="flex items-center space-x-2 px-2 py-1.5 rounded-md hover:bg-gray-100 transition-colors"
                >
                  {user.role === 'ADMIN' ? (
                    <Shield className="w-4 h-4 text-blue-500" />
                  ) : (
                    <User className="w-4 h-4 text-green-500" />
                  )}
                  <span className="text-sm font-medium text-gray-700">
                    {user.role === 'ADMIN' ? t('users.roleAdmin') : user.role === 'TECHNICIAN' ? t('users.roleTechnician') : (user.fullName || user.username)}
                  </span>
                  <ChevronDown className={`w-4 h-4 text-gray-500 transition-transform ${showUserMenu ? 'rotate-180' : ''}`} />
                </button>
                {showUserMenu && (
                  <div className="absolute right-0 top-full mt-1 w-48 bg-white rounded-md shadow-lg border border-gray-200 py-1 z-40">
                    <Link
                      href="/account"
                      onClick={() => setShowUserMenu(false)}
                      className="flex items-center space-x-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      <Settings className="w-4 h-4" />
                      <span>{t('nav.accountSettings')}</span>
                    </Link>
                    {user?.role === 'ADMIN' && (
                      <a
                        href={apiClient.getSwaggerEntryUrl()}
                        target="_blank"
                        rel="noopener noreferrer"
                        onClick={() => setShowUserMenu(false)}
                        className="flex items-center space-x-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                      >
                        <BookOpen className="w-4 h-4" />
                        <span>{t('nav.apiDocs')}</span>
                      </a>
                    )}
                    <button
                      onClick={() => {
                        setShowUserMenu(false)
                        logout()
                      }}
                      className="flex items-center space-x-2 w-full px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                    >
                      <LogOut className="w-4 h-4" />
                      <span>{t('nav.logout')}</span>
                    </button>
                  </div>
                )}
              </div>
            ) : (
              <button
                onClick={() => setShowAdminLogin(true)}
                className="flex items-center space-x-2 px-3 py-1 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
              >
                <Shield className="w-4 h-4" />
                <span>{t('nav.login')}</span>
              </button>
            )}
          </div>
        </div>
      </div>

        {/* Mobilní menu s položkami navigace – uvnitř ref, aby klik na odkaz nezpůsobil „click outside“ */}
        {showMobileMenu && (
          <div className="md:hidden border-t bg-white">
            <div className="container mx-auto px-4 py-2 space-y-1">
              {navigation.map((item) => {
                const Icon = item.icon
                const isActive = pathname === item.href
                return (
                  <Link
                    key={item.name}
                    href={item.href}
                    onClick={() => setShowMobileMenu(false)}
                    className={`flex items-center space-x-2 px-3 py-2 rounded-md text-sm font-medium transition-colors ${
                      isActive
                        ? 'bg-blue-100 text-blue-700'
                        : 'text-gray-600 hover:text-gray-900 hover:bg-gray-100'
                    }`}
                  >
                    <Icon className="w-4 h-4" />
                    <span>{item.name}</span>
                  </Link>
                )
              })}
            </div>
          </div>
        )}
      </div>

      {showAdminLogin && (
        <AdminLogin onClose={() => setShowAdminLogin(false)} />
      )}
    </nav>
  )
}

export default Navigation
