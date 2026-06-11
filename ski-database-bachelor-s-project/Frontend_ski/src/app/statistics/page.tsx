'use client'

import { useState, useEffect } from 'react'
import { TrendingUp, Clock, CheckCircle, Wrench } from 'lucide-react'
import {
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts'
import ProtectedRoute from '@/components/ProtectedRoute'
import { useLanguage } from '@/contexts/LanguageContext'
import { apiClient, StatisticsResponse } from '@/lib/api'

const STATUS_KEYS = ['CEKA', 'PROBIHA', 'DOKONCENO'] as const
const STATUS_COLORS: Record<string, string> = { CEKA: '#f97316', PROBIHA: '#eab308', DOKONCENO: '#22c55e' }

function formatChartDate(iso: string, locale: string) {
  const d = new Date(iso)
  return d.toLocaleDateString(locale === 'cs' ? 'cs-CZ' : 'en-GB', { day: 'numeric', month: 'short' })
}

export default function StatisticsPage() {
  const { t, locale } = useLanguage()
  const [selectedPeriod, setSelectedPeriod] = useState('7d')
  const [stats, setStats] = useState<StatisticsResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    apiClient
      .getStatistics(selectedPeriod)
      .then((data) => {
        if (!cancelled) setStats(data)
      })
      .catch((err) => {
        if (!cancelled) setError(err instanceof Error ? err.message : t('statistics.loadError'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [selectedPeriod, t])

  const periodLabel =
    selectedPeriod === '7d'
      ? t('statistics.last7d')
      : selectedPeriod === '30d'
        ? t('statistics.last30d')
        : selectedPeriod === '90d'
          ? t('statistics.last90d')
          : t('statistics.lastYear')

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{t('statistics.title')}</h1>
          <p className="text-gray-600">{t('statistics.subtitle')}</p>
        </div>

        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex justify-between items-center">
            <h2 className="text-lg font-semibold text-gray-900">{t('statistics.period')}</h2>
            <select
              className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
              value={selectedPeriod}
              onChange={(e) => setSelectedPeriod(e.target.value)}
            >
              <option value="7d">{t('statistics.last7d')}</option>
              <option value="30d">{t('statistics.last30d')}</option>
              <option value="90d">{t('statistics.last90d')}</option>
              <option value="1y">{t('statistics.lastYear')}</option>
            </select>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 text-red-700 rounded-lg p-4">
            {error}
          </div>
        )}

        {loading && (
          <div className="text-center text-gray-500 py-8">{t('statistics.loading')}</div>
        )}

        {!loading && stats && (
          <>
            {/* Key Metrics */}
            <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
              <div className="bg-white rounded-lg shadow-md p-6">
                <div className="flex items-center">
                  <div className="p-3 rounded-lg bg-orange-500">
                    <Clock className="w-6 h-6 text-white" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">{t('status.CEKA')}</p>
                    <p className="text-2xl font-bold text-gray-900">{stats.waiting}</p>
                    <p className="text-xs text-orange-600">{periodLabel}</p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-md p-6">
                <div className="flex items-center">
                  <div className="p-3 rounded-lg bg-yellow-500">
                    <Wrench className="w-6 h-6 text-white" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">{t('status.PROBIHA')}</p>
                    <p className="text-2xl font-bold text-gray-900">{stats.inProgress}</p>
                    <p className="text-xs text-yellow-600">{periodLabel}</p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-md p-6">
                <div className="flex items-center">
                  <div className="p-3 rounded-lg bg-green-500">
                    <CheckCircle className="w-6 h-6 text-white" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">{t('status.DOKONCENO')}</p>
                    <p className="text-2xl font-bold text-gray-900">{stats.completed}</p>
                    <p className="text-xs text-green-600">{periodLabel}</p>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-lg shadow-md p-6">
                <div className="flex items-center">
                  <div className="p-3 rounded-lg bg-blue-500">
                    <TrendingUp className="w-6 h-6 text-white" />
                  </div>
                  <div className="ml-4">
                    <p className="text-sm font-medium text-gray-600">{t('statistics.avgTimePerSki')}</p>
                    <p className="text-2xl font-bold text-gray-900">
                      {stats.averageCompletionHours != null
                        ? `${stats.averageCompletionHours} h`
                        : '–'}
                    </p>
                    <p className="text-xs text-blue-600">
                      {stats.averageCompletionHours != null
                        ? t('statistics.fromAddToComplete')
                        : t('statistics.noSkisInPeriod')}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              <div className="bg-white rounded-lg shadow-md p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('statistics.distributionByStatus')}</h3>
                {(() => {
                  const pieData = STATUS_KEYS.map((k) => ({ key: k, name: t('status.' + k), value: k === 'CEKA' ? stats.waiting : k === 'PROBIHA' ? stats.inProgress : stats.completed })).filter((d) => d.value > 0)
                  return stats.waiting + stats.inProgress + stats.completed > 0 ? (
                    <ResponsiveContainer width="100%" height={280}>
                      <PieChart>
                        <Pie
                          data={pieData}
                          cx="50%"
                          cy="50%"
                          innerRadius={60}
                          outerRadius={100}
                          paddingAngle={2}
                          dataKey="value"
                          nameKey="name"
                          label={({ name, value }) => `${name}: ${value}`}
                        >
                          {pieData.map((entry) => (
                            <Cell key={entry.key} fill={STATUS_COLORS[entry.key]} />
                          ))}
                        </Pie>
                        <Tooltip formatter={(value: number) => [value, t('statistics.skisUnit')]} />
                      </PieChart>
                    </ResponsiveContainer>
                  ) : (
                    <p className="text-gray-500 py-8 text-center">{t('statistics.noSkisInOrders')}</p>
                  )
                })()}
              </div>

              <div className="bg-white rounded-lg shadow-md p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('statistics.activityInPeriod')}</h3>
                {stats.daily && stats.daily.length > 0 ? (
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart
                      data={stats.daily.map((d) => ({
                        ...d,
                        label: formatChartDate(d.date, locale),
                      }))}
                      margin={{ top: 8, right: 16, left: 0, bottom: 8 }}
                    >
                      <CartesianGrid strokeDasharray="3 3" className="stroke-gray-200" />
                      <XAxis dataKey="label" tick={{ fontSize: 12 }} />
                      <YAxis allowDecimals={false} tick={{ fontSize: 12 }} />
                      <Tooltip
                        labelFormatter={(_, payload) => payload?.[0]?.payload?.date && formatChartDate(payload[0].payload.date, locale)}
                        formatter={(value: number) => [value, '']}
                      />
                      <Legend />
                      <Bar dataKey="created" name={t('statistics.addedToOrder')} fill="#3b82f6" radius={[4, 4, 0, 0]} />
                      <Bar dataKey="completed" name={t('status.DOKONCENO')} fill="#22c55e" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <p className="text-gray-500 py-8 text-center">{t('statistics.noDailyData')}</p>
                )}
              </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
              <div className="bg-white rounded-lg shadow-md p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('statistics.topTaskTypes')}</h3>
                {(stats.topTaskTypes?.length ?? 0) > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">{t('statistics.taskType')}</th>
                          <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">{t('statistics.count')}</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {(stats.topTaskTypes ?? []).map((row) => (
                          <tr key={row.name} className="hover:bg-gray-50">
                            <td className="px-4 py-2 text-sm text-gray-900">{row.name}</td>
                            <td className="px-4 py-2 text-sm text-gray-600 text-right">{row.count}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-gray-500 py-6 text-center">{t('statistics.noTasksInPeriod')}</p>
                )}
              </div>
              <div className="bg-white rounded-lg shadow-md p-6">
                <h3 className="text-lg font-semibold text-gray-900 mb-4">{t('statistics.topStructures')}</h3>
                {(stats.topStructures?.length ?? 0) > 0 ? (
                  <div className="overflow-x-auto">
                    <table className="min-w-full divide-y divide-gray-200">
                      <thead className="bg-gray-50">
                        <tr>
                          <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">{t('statistics.structure')}</th>
                          <th className="px-4 py-2 text-right text-xs font-medium text-gray-500 uppercase">{t('statistics.count')}</th>
                        </tr>
                      </thead>
                      <tbody className="bg-white divide-y divide-gray-200">
                        {(stats.topStructures ?? []).map((row) => (
                          <tr key={row.name} className="hover:bg-gray-50">
                            <td className="px-4 py-2 text-sm text-gray-900">{row.name}</td>
                            <td className="px-4 py-2 text-sm text-gray-600 text-right">{row.count}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="text-gray-500 py-6 text-center">{t('statistics.noStructuresInPeriod')}</p>
                )}
              </div>
            </div>
          </>
        )}
      </div>
    </ProtectedRoute>
  )
}
