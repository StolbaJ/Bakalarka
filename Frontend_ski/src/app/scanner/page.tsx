'use client'

import { useState, useRef, useEffect, useCallback } from 'react'
import { QrCode, CheckCircle, AlertCircle, Keyboard, Plus, Eye, RefreshCw } from 'lucide-react'
import QRScanner from '@/components/QRScanner'
import ProtectedRoute from '@/components/ProtectedRoute'
import SkiDetail from '@/components/SkiDetail'
import { SkiData } from '@/components/SkiItem'
import apiClient, { OrderDetailResponse, QrScanEntryResponse } from '@/lib/api'
import { skiResponseToData } from '@/lib/skiUtils'
import { useLanguage } from '@/contexts/LanguageContext'

const POLL_INTERVAL_MS = 8000

export default function ScannerPage() {
  const { t } = useLanguage()
  const [showScanner, setShowScanner] = useState(false)
  const [recentScans, setRecentScans] = useState<QrScanEntryResponse[]>([])
  const [loadingScans, setLoadingScans] = useState(true)
  const [lastScanned, setLastScanned] = useState<string>('')
  const [manualCode, setManualCode] = useState('')
  const [showManualInput, setShowManualInput] = useState(false)
  const [selectedSki, setSelectedSki] = useState<SkiData | null>(null)
  const [orderForSki, setOrderForSki] = useState<OrderDetailResponse | null>(null)
  const scannedSetRef = useRef<Set<string>>(new Set())

  const fetchRecentScans = useCallback(async () => {
    try {
      const list = await apiClient.getRecentQrScans()
      setRecentScans(list)
    } catch {
      setRecentScans([])
    } finally {
      setLoadingScans(false)
    }
  }, [])

  useEffect(() => {
    fetchRecentScans()
  }, [fetchRecentScans])

  useEffect(() => {
    const t = setInterval(fetchRecentScans, POLL_INTERVAL_MS)
    return () => clearInterval(t)
  }, [fetchRecentScans])

  const handleScan = async (result: string) => {
    if (!scannedSetRef.current.has(result)) {
      scannedSetRef.current.add(result)
      setLastScanned(result)
      try {
        await apiClient.recordQrScan(result)
        await fetchRecentScans()
      } catch {
        // lyže nemusí být v DB, přesto zobrazíme detail pokud jde načíst
      }
      await handleShowSkiDetails(result)
    }
  }

  const handleManualAdd = async () => {
    const code = manualCode.trim()
    if (!code) return
    if (!scannedSetRef.current.has(code)) {
      scannedSetRef.current.add(code)
      setLastScanned(code)
      setManualCode('')
      setShowManualInput(false)
      try {
        await apiClient.recordQrScan(code)
        await fetchRecentScans()
      } catch {
        // ignore
      }
      await handleShowSkiDetails(code)
    }
  }

  const handleShowSkiDetails = async (value: string) => {
    setOrderForSki(null)
    try {
      const ski = /^\d+$/.test(value)
        ? await apiClient.getSki(parseInt(value, 10))
        : await apiClient.getSkiByNumber(value)
      setSelectedSki(skiResponseToData(ski))
      const order = await apiClient.getOrderBySkiNumber(ski.skiNumber)
      if (order) setOrderForSki(order)
    } catch {
      setSelectedSki({
        id: value,
        brand: t('scanner.unknown'),
        model: t('scanner.unknownModel'),
        length: 'N/A',
        year: 'N/A',
        condition: t('scanner.unknownModel'),
        status: t('scanner.unknownModel'),
        lastService: 'N/A',
        nextService: 'N/A',
        location: 'N/A',
        notes: t('scanner.notInDb')
      })
    }
  }

  return (
    <ProtectedRoute requiredRole="ADMIN_OR_TECHNICIAN">
      <div className="space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{t('scanner.title')}</h1>
          <p className="text-gray-600">{t('scanner.subtitle')}</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Scanner Section */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="text-center mb-6">
              <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <QrCode className="w-8 h-8 text-white" />
              </div>
              <h2 className="text-xl font-semibold text-gray-900 mb-2">{t('scanner.scanning')}</h2>
              <p className="text-gray-600">{t('scanner.scanOrManual')}</p>
            </div>

            <div className="space-y-3">
            <button
              onClick={() => {
                setShowScanner(true)
                setLastScanned('')
                scannedSetRef.current.clear()
              }}
              className="w-full bg-blue-600 text-white py-3 px-4 rounded-md hover:bg-blue-700 transition-colors font-medium"
            >
              {t('scanner.startScanner')}
            </button>
              
              <button
                onClick={() => setShowManualInput(!showManualInput)}
                className="w-full bg-gray-600 text-white py-3 px-4 rounded-md hover:bg-gray-700 transition-colors font-medium flex items-center justify-center space-x-2"
              >
                <Keyboard className="w-4 h-4" />
                <span>{t('scanner.enterManually')}</span>
              </button>
            </div>

            {showManualInput && (
              <div className="mt-4 p-4 bg-gray-50 rounded-lg">
                <label htmlFor="manualCode" className="block text-sm font-medium text-gray-700 mb-2">
                  {t('scanner.skiCode')}
                </label>
                <div className="flex space-x-2">
                  <input
                    type="text"
                    id="manualCode"
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
                    placeholder={t('scanner.codePlaceholder')}
                    value={manualCode}
                    onChange={(e) => setManualCode(e.target.value)}
                    onKeyPress={(e) => e.key === 'Enter' && handleManualAdd()}
                  />
                  <button
                    onClick={handleManualAdd}
                    className="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 transition-colors flex items-center space-x-1"
                    disabled={!manualCode.trim()}
                  >
                    <Plus className="w-4 h-4" />
                    <span>{t('common.add')}</span>
                  </button>
                </div>
              </div>
            )}

            {lastScanned && (
              <div className="mt-6 p-4 bg-green-50 border border-green-200 rounded-lg">
                <div className="flex items-center space-x-2">
                  <CheckCircle className="w-5 h-5 text-green-500" />
                  <p className="text-green-700 font-medium">{t('scanner.lastScanned')}</p>
                </div>
                <p className="text-green-800 mt-1 font-mono text-sm">{lastScanned}</p>
              </div>
            )}
          </div>

          {/* Results Section */}
          <div className="bg-white rounded-lg shadow-md p-6">
            <div className="flex justify-between items-center mb-2">
              <h2 className="text-xl font-semibold text-gray-900">{t('scanner.loadedRecent')}</h2>
              <button
                onClick={() => { setLoadingScans(true); fetchRecentScans() }}
                className="text-gray-600 hover:text-gray-900 p-1 cursor-pointer"
                title={t('scanner.refresh')}
              >
                <RefreshCw className="w-4 h-4" />
              </button>
            </div>
            <p className="text-xs text-gray-500 mb-4">{t('scanner.scansHint')}</p>

            {loadingScans ? (
              <div className="text-center py-8 text-gray-500">{t('scanner.loading')}</div>
            ) : recentScans.length > 0 ? (
              <div className="space-y-3">
                {recentScans.map((entry, index) => (
                  <div key={entry.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                    <div className="flex items-center space-x-3 min-w-0">
                      <div className="w-8 h-8 bg-green-100 rounded-full flex items-center justify-center shrink-0">
                        <CheckCircle className="w-4 h-4 text-green-600" />
                      </div>
                      <div className="min-w-0">
                        <span className="font-mono text-sm text-gray-800 block truncate">{entry.skiNumber ?? '—'}</span>
                        {entry.skiInfo && <span className="text-xs text-gray-500 block truncate">{entry.skiInfo}</span>}
                        {entry.scannedAt && <span className="text-xs text-gray-400 block">{new Date(entry.scannedAt).toLocaleString('cs-CZ')}</span>}
                      </div>
                    </div>
                    <div className="flex items-center space-x-2 shrink-0">
                      <button
                        onClick={() => {
                          const v = entry.skiNumber ?? (entry.skiId != null ? String(entry.skiId) : '')
                          if (v) handleShowSkiDetails(v)
                        }}
                        className="text-blue-600 hover:text-blue-900 p-1 cursor-pointer"
                        title={t('scanner.showSkiDetails')}
                      >
                        <Eye className="w-4 h-4" />
                      </button>
                      <span className="text-xs text-gray-500">#{index + 1}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-8">
                <AlertCircle className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <p className="text-gray-500">{t('scanner.noScansYet')}</p>
              </div>
            )}
          </div>
        </div>

        {/* Instructions */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-lg font-semibold text-blue-900 mb-3">{t('scanner.instructions')}</h3>
          <ul className="space-y-2 text-blue-800">
              <li className="flex items-start space-x-2">
                <span className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></span>
                <span>{t('scanner.instr1')}</span>
              </li>
            <li className="flex items-start space-x-2">
              <span className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></span>
              <span>{t('scanner.instr2')}</span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></span>
              <span>{t('scanner.instr3')}</span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></span>
              <span>{t('scanner.instr4')}</span>
            </li>
            <li className="flex items-start space-x-2">
              <span className="w-2 h-2 bg-blue-500 rounded-full mt-2 flex-shrink-0"></span>
              <span>{t('scanner.instr5')}</span>
            </li>
          </ul>
        </div>
      </div>

      {showScanner && (
        <QRScanner
          onScan={handleScan}
          onClose={() => setShowScanner(false)}
        />
      )}

      {/* Ski Details Modal */}
      {selectedSki && (
        <SkiDetail
          ski={selectedSki}
          onClose={() => { setSelectedSki(null); setOrderForSki(null) }}
          onEdit={(skiId) => console.log('Edit ski:', skiId)}
          onDelete={(skiId) => console.log('Delete ski:', skiId)}
          orderFromScan={orderForSki}
          skiNumberForOrder={selectedSki.id}
          onOrderUpdated={(o) => setOrderForSki(o)}
        />
      )}
    </ProtectedRoute>
  )
}
