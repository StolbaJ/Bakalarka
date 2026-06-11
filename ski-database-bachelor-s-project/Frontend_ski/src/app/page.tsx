'use client'

import { 
  QrCode, 
  Database, 
  Wrench, 
  BarChart3, 
  Clock,
  CheckCircle,
  ArrowRight
} from 'lucide-react'
import Link from 'next/link'
import { useAuth } from '@/contexts/AuthContext'
import { useLanguage } from '@/contexts/LanguageContext'
import OrderLookup from '@/components/OrderLookup'

export default function Home() {
  const { user, loginCustomer, logout } = useAuth()
  const { t } = useLanguage()

  const handleOrderLookup = async (orderNumberInput: string, phoneInput: string): Promise<boolean> => {
    return loginCustomer(orderNumberInput, phoneInput)
  }

  const stats = [
    { label: t('home.statsTotalSkis'), value: '1,247', icon: Database, color: 'bg-blue-500' },
    { label: t('home.statsInService'), value: '23', icon: Wrench, color: 'bg-yellow-500' },
    { label: t('home.statsDoneToday'), value: '15', icon: CheckCircle, color: 'bg-green-500' },
    { label: t('home.statsAvgTime'), value: '2.5h', icon: Clock, color: 'bg-purple-500' },
  ]

  const quickActions = [
    {
      title: t('home.scanQr'),
      description: t('home.scanQrDesc'),
      href: '/scanner',
      icon: QrCode,
      color: 'bg-blue-600 hover:bg-blue-700'
    },
    {
      title: t('home.database'),
      description: t('home.databaseDesc'),
      href: '/database',
      icon: Database,
      color: 'bg-green-600 hover:bg-green-700'
    },
    {
      title: t('home.serviceTasks'),
      description: t('home.serviceTasksDesc'),
      href: '/service',
      icon: Wrench,
      color: 'bg-yellow-600 hover:bg-yellow-700'
    },
    {
      title: t('nav.statistics'),
      description: t('home.statisticsDesc'),
      href: '/statistics',
      icon: BarChart3,
      color: 'bg-purple-600 hover:bg-purple-700'
    },
  ]

  // Nepřihlášený nebo zákazník: vítací stránka + vyhledání objednávky
  if (!user || user.role === 'CUSTOMER') {
    return (
      <div className="space-y-8">
        <div className="text-center">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">{t('home.welcome')}</h1>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto">
            {t('home.welcomeDesc')}
          </p>
        </div>
        <OrderLookup
          onLookup={handleOrderLookup}
          onLogout={user?.role === 'CUSTOMER' ? logout : undefined}
          isCustomer={user?.role === 'CUSTOMER'}
          customerName={user?.fullName ?? undefined}
        />
        {user?.role === 'CUSTOMER' && (
          <div className="text-center">
            <Link
              href="/customer"
              className="inline-flex items-center space-x-2 text-blue-600 hover:text-blue-800 font-medium"
            >
              <span>{t('home.goToMyOrders')}</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
          </div>
        )}
      </div>
    )
  }

  // If user is logged in as admin or technician, show dashboard
  if (user.role === 'ADMIN' || user.role === 'TECHNICIAN') {
    return (
      <div className="space-y-8">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-4xl font-bold text-gray-900 mb-4">
            {t('home.title')}
          </h1>
          <p className="text-xl text-gray-600 max-w-3xl mx-auto">
            {t('home.subtitle')}
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
          {stats.map((stat) => {
            const Icon = stat.icon
            return (
              <div key={stat.label} className="bg-white rounded-lg shadow-md p-6">
                <div className="flex items-center">
                  <div className={`p-3 rounded-lg ${stat.color}`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">{stat.label}</p>
                    <p className="text-2xl font-bold text-gray-900">{stat.value}</p>
                  </div>
                </div>
              </div>
            )
          })}
        </div>

        {/* Admin Quick Actions */}
        <div>
          <h2 className="text-2xl font-bold text-gray-900 mb-6">{t('home.quickActions')}</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
            {quickActions.map((action) => {
              const Icon = action.icon
              return (
                <Link
                  key={action.title}
                  href={action.href}
                  className="group bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow"
                >
                  <div className={`w-12 h-12 ${action.color} rounded-lg flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                    <Icon className="w-6 h-6 text-white" />
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">
                    {action.title}
                  </h3>
                  <p className="text-gray-600 text-sm">
                    {action.description}
                  </p>
                </Link>
              )
            })}
          </div>
        </div>
      </div>
    )
  }

  // Admin / technik: dashboard s rychlými akcemi
  return null
}