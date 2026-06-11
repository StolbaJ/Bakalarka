'use client'

import { useState, useEffect, useCallback } from 'react'
import Link from 'next/link'
import {
  Activity,
  Server,
  Database,
  HardDrive,
  Cpu,
  Clock,
  AlertCircle,
  CheckCircle2,
  RefreshCw,
  ArrowLeft,
  AlertTriangle,
} from 'lucide-react'
import ProtectedRoute from '@/components/ProtectedRoute'
import apiClient, {
  MonitoringOverviewResponse,
  RecordedErrorResponse,
} from '@/lib/api'

const POLL_INTERVAL_MS = 15_000

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`
}

function formatTime(ms: number): string {
  const d = new Date(ms)
  return d.toLocaleTimeString('cs-CZ', {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

function formatErrorTime(ts: number): string {
  const d = new Date(ts)
  return d.toLocaleString('cs-CZ', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

export default function SettingsMonitoringPage() {
  const [overview, setOverview] = useState<MonitoringOverviewResponse | null>(null)
  const [lastResponseTimeMs, setLastResponseTimeMs] = useState<number | null>(null)
  const [lastCheckAt, setLastCheckAt] = useState<number | null>(null)
  const [offlineSince, setOfflineSince] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const fetchOverview = useCallback(async () => {
    const start = performance.now()
    try {
      const data = await apiClient.getMonitoringOverview()
      const elapsed = Math.round(performance.now() - start)
      setOverview(data)
      setLastResponseTimeMs(elapsed)
      setLastCheckAt(Date.now())
      setOfflineSince(null)
      setError(null)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Backend nedostupný')
      if (offlineSince === null) setOfflineSince(Date.now())
    } finally {
      setLoading(false)
    }
  }, [offlineSince])

  useEffect(() => {
    fetchOverview()
    const id = setInterval(fetchOverview, POLL_INTERVAL_MS)
    return () => clearInterval(id)
  }, [fetchOverview])

  const isUp = overview?.status === 'UP'
  const offlineDuration =
    offlineSince != null ? Math.round((Date.now() - offlineSince) / 1000) : null

  const recentErrors: RecordedErrorResponse[] = overview?.recentErrors ?? []

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <Link
              href="/settings"
              className="inline-flex items-center gap-1 text-sm text-gray-600 hover:text-gray-900 mb-2"
            >
              <ArrowLeft className="w-4 h-4" />
              Struktury a úpravy
            </Link>
            <h1 className="text-3xl font-bold text-gray-900 mb-1 flex items-center gap-2">
              <Activity className="w-8 h-8 text-blue-600" />
              Monitoring backendu
            </h1>
            <p className="text-gray-600">
              Zdraví služby, metriky, disk, JVM a chyby 5xx. Obnovuje se každých{' '}
              {POLL_INTERVAL_MS / 1000} s.
            </p>
          </div>
          <button
            type="button"
            onClick={() => {
              setLoading(true)
              fetchOverview()
            }}
            className="flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
          >
            <RefreshCw className="w-4 h-4" />
            Obnovit
          </button>
        </div>

        {!loading && error && (
          <div className="bg-red-50 border border-red-200 rounded-xl p-4 flex items-center gap-3">
            <AlertCircle className="w-8 h-8 text-red-600 shrink-0" />
            <div>
              <p className="font-semibold text-red-800">Backend nedostupný</p>
              <p className="text-red-700 text-sm">{error}</p>
              {offlineDuration != null && (
                <p className="text-red-600 text-sm mt-1">
                  Výpadek trvá cca {offlineDuration} s.
                </p>
              )}
            </div>
          </div>
        )}

        {loading && !overview && (
          <div className="text-center py-12 text-gray-500">Načítám monitoring…</div>
        )}

        {!loading && overview && (
          <>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
              <div
                className={`rounded-xl shadow-md p-6 flex items-center gap-4 ${
                  isUp ? 'bg-emerald-50 border border-emerald-200' : 'bg-amber-50 border border-amber-200'
                }`}
              >
                {isUp ? (
                  <CheckCircle2 className="w-12 h-12 text-emerald-600 shrink-0" />
                ) : (
                  <AlertCircle className="w-12 h-12 text-amber-600 shrink-0" />
                )}
                <div>
                  <p className="text-sm font-medium text-gray-600">Stav backendu</p>
                  <p className={`text-2xl font-bold ${isUp ? 'text-emerald-700' : 'text-amber-700'}`}>
                    {overview.status === 'UP' ? 'Běží' : overview.status}
                  </p>
                </div>
              </div>
              <div className="bg-white rounded-xl shadow-md p-6 flex items-center gap-4">
                <Clock className="w-12 h-12 text-blue-600 shrink-0" />
                <div>
                  <p className="text-sm font-medium text-gray-600">Uptime</p>
                  <p className="text-xl font-bold text-gray-900">{overview.uptimeFormatted}</p>
                </div>
              </div>
              <div className="bg-white rounded-xl shadow-md p-6 flex items-center gap-4">
                <Server className="w-12 h-12 text-gray-600 shrink-0" />
                <div>
                  <p className="text-sm font-medium text-gray-600">Poslední kontrola</p>
                  <p className="text-xl font-bold text-gray-900">
                    {lastCheckAt != null ? formatTime(lastCheckAt) : '–'}
                  </p>
                  {lastResponseTimeMs != null && (
                    <p className="text-xs text-gray-500">odezva {lastResponseTimeMs} ms</p>
                  )}
                </div>
              </div>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
              <div className="bg-white rounded-xl shadow-md p-6">
                <p className="text-sm font-medium text-gray-600 mb-1">Požadavky za sekundu</p>
                <p className="text-2xl font-bold text-gray-900">
                  {overview.requestRatePerSecond.toFixed(2)}
                </p>
              </div>
              <div className="bg-white rounded-xl shadow-md p-6">
                <p className="text-sm font-medium text-gray-600 mb-1">Průměrná doba odezvy</p>
                <p className="text-2xl font-bold text-gray-900">
                  {overview.responseTimeAvgMs.toFixed(0)} ms
                </p>
              </div>
              <div className="bg-white rounded-xl shadow-md p-6">
                <p className="text-sm font-medium text-gray-600 mb-1">Max. doba odezvy</p>
                <p className="text-2xl font-bold text-gray-900">
                  {overview.responseTimeMaxMs.toFixed(0)} ms
                </p>
              </div>
              <div className="bg-white rounded-xl shadow-md p-6">
                <p className="text-sm font-medium text-gray-600 mb-1">Podíl chyb (5xx)</p>
                <p
                  className={`text-2xl font-bold ${
                    overview.errorRatePercent > 1 ? 'text-amber-600' : 'text-gray-900'
                  }`}
                >
                  {overview.errorRatePercent.toFixed(2)} %
                </p>
              </div>
            </div>

            {/* Disk – jen grafika */}
            <div className="bg-white rounded-xl shadow-md p-6">
              <div className="flex items-center gap-2 mb-3">
                <HardDrive className="w-5 h-5 text-teal-600" />
                <h3 className="font-semibold text-gray-900">Disk</h3>
              </div>
              <DiskBar disk={overview.disk} />
            </div>

            {/* Databáze – status + validace (čitelně) */}
            <div className="bg-white rounded-xl shadow-md p-6">
              <div className="flex items-center gap-2 mb-3">
                <Database className="w-5 h-5 text-indigo-600" />
                <h3 className="font-semibold text-gray-900">Databáze</h3>
              </div>
              <DatabaseStatus database={overview.database} />
            </div>

            {/* JVM paměť – grafika + vysvětlení */}
            <div className="bg-white rounded-xl shadow-md p-6">
              <div className="flex items-center gap-2 mb-3">
                <Cpu className="w-5 h-5 text-violet-600" />
                <h3 className="font-semibold text-gray-900">Paměť (JVM)</h3>
              </div>
              <JvmMemory memory={overview.memory} />
            </div>

            {/* Poslední chyby 5xx */}
            <div className="bg-white rounded-xl shadow-md p-6">
              <div className="flex items-center gap-2 mb-3">
                <AlertTriangle className="w-5 h-5 text-red-600" />
                <h3 className="font-semibold text-gray-900">Poslední chyby 5xx</h3>
              </div>
              <RecentErrorsList errors={recentErrors} />
            </div>
          </>
        )}
      </div>
    </ProtectedRoute>
  )
}

function DiskBar({ disk }: { disk: Record<string, unknown> }) {
  const details = disk.details as Record<string, unknown> | undefined
  const total = typeof details?.total === 'number' ? details.total : 0
  const free = typeof details?.free === 'number' ? details.free : 0
  const status = disk.status as string | undefined
  const used = total > 0 ? total - free : 0
  const percent = total > 0 ? Math.round((used / total) * 100) : 0

  if (!total && !status) {
    return <p className="text-gray-500 text-sm">Údaje nejsou k dispozici</p>
  }

  return (
    <div className="space-y-2">
      <div className="flex justify-between text-sm text-gray-600">
        <span>Využito {total > 0 ? formatBytes(used) : '–'} z {total > 0 ? formatBytes(total) : '–'}</span>
        {status && (
          <span className={status === 'UP' ? 'text-emerald-600' : 'text-amber-600'}>{status}</span>
        )}
      </div>
      <div className="h-4 bg-gray-200 rounded-full overflow-hidden">
        <div
          className={`h-full rounded-full transition-all ${
            percent > 90 ? 'bg-red-500' : percent > 70 ? 'bg-amber-500' : 'bg-teal-500'
          }`}
          style={{ width: `${Math.min(100, percent)}%` }}
        />
      </div>
    </div>
  )
}

function DatabaseStatus({ database }: { database: Record<string, unknown> }) {
  const status = database.status as string | undefined
  const details = database.details as Record<string, unknown> | undefined
  const dbName = details?.database as string | undefined
  const validationQuery = details?.validationQuery as string | undefined

  if (!status && !dbName) {
    return <p className="text-gray-500 text-sm">Údaje nejsou k dispozici</p>
  }

  return (
    <div className="space-y-1 text-sm">
      <p>
        Stav: <strong className={status === 'UP' ? 'text-emerald-600' : 'text-amber-600'}>{status ?? 'N/A'}</strong>
      </p>
      {dbName != null && <p>Databáze: {String(dbName)}</p>}
      {validationQuery != null && <p className="text-gray-600">Validace: {String(validationQuery)}</p>}
    </div>
  )
}

function JvmMemory({ memory }: { memory: Record<string, unknown> }) {
  const usedBytes = memory.usedBytes as number | undefined
  const maxBytes = memory.maxBytes as number | undefined
  const usedPercent = memory.usedPercent as number | undefined

  const hasData = typeof usedBytes === 'number'

  return (
    <div className="space-y-3">
      {hasData && (
        <>
          <div className="flex justify-between text-sm text-gray-600">
            <span>
              {formatBytes(usedBytes)} / {maxBytes != null ? formatBytes(maxBytes) : '?'}
            </span>
            {typeof usedPercent === 'number' && (
              <span className="font-medium">{usedPercent} %</span>
            )}
          </div>
          <div className="h-4 bg-gray-200 rounded-full overflow-hidden">
            <div
              className="h-full bg-violet-500 rounded-full transition-all"
              style={{
                width: `${
                  typeof usedPercent === 'number' ? Math.min(100, usedPercent) : 0
                }%`,
              }}
            />
          </div>
        </>
      )}

    </div>
  )
}

function RecentErrorsList({ errors }: { errors: RecordedErrorResponse[] }) {
  if (errors.length === 0) {
    return (
      <p className="text-gray-500 text-sm">Žádné chyby 5xx v záznamu. Ukládáme posledních 20 výskytů.</p>
    )
  }

  return (
    <ul className="space-y-2 max-h-48 overflow-auto">
      {errors.map((e, i) => (
        <li
          key={`${e.timestamp}-${e.path}-${i}`}
          className="flex items-center gap-2 text-sm py-2 px-3 bg-red-50 rounded-lg border border-red-100"
        >
          <span className="font-mono font-semibold text-red-700 w-10">{e.status}</span>
          <span className="text-gray-600 shrink-0">{e.method}</span>
          <span className="text-gray-800 truncate">{e.path}</span>
          <span className="text-gray-500 text-xs shrink-0">{formatErrorTime(e.timestamp)}</span>
        </li>
      ))}
    </ul>
  )
}
