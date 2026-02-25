'use client'

import { useState, useEffect, useCallback } from 'react'
import { X, Edit, Trash2, Calendar, MapPin, User, AlertTriangle, Wrench, CheckCircle, ChevronLeft, ChevronRight, Package, QrCode } from 'lucide-react'
import { SkiData } from './SkiItem'
import { apiClient, SkiServiceHistoryEntry, OrderDetailResponse, ModificationOptionResponse } from '@/lib/api'

const PRIORITY_LABELS: Record<string, string> = { NIZKA: 'Nízká', STREDNI: 'Střední', VYSOKA: 'Vysoká', KRITICKA: 'Kritická' }
const ORDER_STATUS_LABELS: Record<string, string> = { NOVE: 'Nové', VE_ZPRACOVANI: 'Ve zpracování', POZASTAVENA: 'Pozastavená', UKONCENA: 'Dokončená', STORNOVANA: 'Stornovaná' }
function getOrderPriorityColor(p: string) {
  switch (p) { case 'NIZKA': return 'bg-green-100 text-green-800'; case 'STREDNI': return 'bg-yellow-100 text-yellow-800'; case 'VYSOKA': return 'bg-orange-100 text-orange-800'; case 'KRITICKA': return 'bg-red-100 text-red-800'; default: return 'bg-gray-100 text-gray-800' }
}
function getOrderStatusColor(s: string) {
  switch (s) { case 'NOVE': return 'bg-sky-100 text-sky-800'; case 'VE_ZPRACOVANI': return 'bg-blue-100 text-blue-800'; case 'POZASTAVENA': return 'bg-amber-100 text-amber-800'; case 'UKONCENA': return 'bg-green-100 text-green-800'; case 'STORNOVANA': return 'bg-red-100 text-red-800'; default: return 'bg-gray-100 text-gray-800' }
}

function escapeHtml(text: string): string {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}

export interface SkiDetailProps {
  ski: SkiData
  onClose: () => void
  onEdit?: (skiId: string, numericId?: number) => void
  onDelete?: (skiId: string, numericId?: number) => void
  orderFromScan?: OrderDetailResponse | null
  skiNumberForOrder?: string
  modificationOptions?: ModificationOptionResponse[]
  onOrderUpdated?: (order: OrderDetailResponse) => void
}

const SkiDetail: React.FC<SkiDetailProps> = ({
  ski,
  onClose,
  onEdit,
  onDelete,
  orderFromScan,
  skiNumberForOrder,
  modificationOptions = [],
  onOrderUpdated,
}) => {
  const [serviceHistory, setServiceHistory] = useState<SkiServiceHistoryEntry[]>([])
  const [historyLoading, setHistoryLoading] = useState(false)
  const [historyPage, setHistoryPage] = useState(1)
  const [taskItemDescEdits, setTaskItemDescEdits] = useState<Record<number, string>>({})
  const [taskItemInstructionEdits, setTaskItemInstructionEdits] = useState<Record<number, string>>({})
  const [taskItemDescriptionErrorId, setTaskItemDescriptionErrorId] = useState<number | null>(null)
  const HISTORY_PAGE_SIZE = 5
  const totalHistoryPages = Math.max(1, Math.ceil(serviceHistory.length / HISTORY_PAGE_SIZE))
  const paginatedHistory = serviceHistory.slice(
    (historyPage - 1) * HISTORY_PAGE_SIZE,
    historyPage * HISTORY_PAGE_SIZE
  )

  useEffect(() => {
    if (ski.numericId) {
      setHistoryLoading(true)
      apiClient.getSkiServiceHistory(ski.numericId)
        .then((entries) => {
          const sorted = [...entries].sort((a, b) => {
            const dateA = a.completedDate || a.orderDate || ''
            const dateB = b.completedDate || b.orderDate || ''
            return dateB.localeCompare(dateA)
          })
          setServiceHistory(sorted)
        })
        .catch(() => setServiceHistory([]))
        .finally(() => setHistoryLoading(false))
    } else {
      setServiceHistory([])
    }
    setHistoryPage(1)
  }, [ski.numericId])

  const getStatusColor = (status: string) => {
    switch (status.toLowerCase()) {
      case 'dostupný':
        return 'bg-green-100 text-green-800'
      case 'v servisu':
        return 'bg-yellow-100 text-yellow-800'
      case 'rezervováno':
        return 'bg-blue-100 text-blue-800'
      case 'nedostupný':
        return 'bg-red-100 text-red-800'
      case 'potřebuje servis':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getConditionColor = (condition: string) => {
    switch (condition.toLowerCase()) {
      case 'výborný':
        return 'bg-green-100 text-green-800'
      case 'dobrý':
        return 'bg-blue-100 text-blue-800'
      case 'střední':
        return 'bg-yellow-100 text-yellow-800'
      case 'špatný':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getPriorityColor = (priority?: string) => {
    switch (priority) {
      case 'high':
        return 'bg-red-100 text-red-800'
      case 'medium':
        return 'bg-yellow-100 text-yellow-800'
      case 'low':
        return 'bg-green-100 text-green-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const handlePrintQr = useCallback(async () => {
    const payload = ski.id
    try {
      const QRCode = (await import('qrcode')).default
      const dataUrl = await QRCode.toDataURL(payload, { width: 256, margin: 2 })
      const printWindow = window.open('', '_blank')
      if (!printWindow) {
        alert('Povolte vyskakovací okna pro tisk QR kódu.')
        return
      }
      printWindow.document.write(`
        <!DOCTYPE html>
        <html>
          <head>
            <title>QR kód lyže ${escapeHtml(payload)}</title>
            <style>
              body { font-family: system-ui, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; margin: 0; }
              .label { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
              img { display: block; }
            </style>
          </head>
          <body>
            <p class="label">Lyže: ${escapeHtml(payload)}</p>
            <img src="${dataUrl}" alt="QR kód" width="256" height="256" />
          </body>
        </html>
      `)
      printWindow.document.close()
      printWindow.focus()
      setTimeout(() => {
        printWindow.print()
        printWindow.close()
      }, 250)
    } catch (e) {
      console.error('QR generation failed:', e)
      alert('Nepodařilo se vygenerovat QR kód.')
    }
  }, [ski.id])

  return (
    <div className="fixed inset-0 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-5 w-full max-w-4xl max-h-[90vh] overflow-y-auto relative">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-3 right-3 text-gray-400 hover:text-gray-600"
        >
          <X className="w-5 h-5" />
        </button>

        {/* Header: Detail lyže + Umístění vlevo (ne u křížku) */}
        <div className="mb-4">
          <h2 className="text-xl font-bold text-gray-900 mb-1.5">Detail lyže: {ski.id}</h2>
          <div className="flex items-center flex-wrap gap-2">
            <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${getStatusColor(ski.status)}`}>
              {ski.status}
            </span>
            <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${getConditionColor(ski.condition)}`}>
              {ski.condition}
            </span>
            {ski.priority && (
              <span className={`px-2 py-0.5 text-xs font-semibold rounded-full ${getPriorityColor(ski.priority)}`}>
                {ski.priority === 'high' ? 'Vysoká priorita' : ski.priority === 'medium' ? 'Střední priorita' : 'Nízká priorita'}
              </span>
            )}
            {ski.location && (
              <span className="flex items-center gap-1.5 text-sm text-gray-600 ml-1">
                <MapPin className="w-3.5 h-3.5 text-gray-500 shrink-0" />
                {ski.location}
              </span>
            )}
          </div>
        </div>

        {/* Content */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {/* Basic info */}
          <div className="space-y-3">
            <div>
              <h3 className="text-base font-semibold text-gray-900 mb-2">Informace o lyži</h3>
              <div className="space-y-1.5">
                <div className="flex gap-2">
                  <span className="text-xs font-medium text-gray-500 w-20 shrink-0">Značka</span>
                  <span className="text-sm text-gray-900">{ski.brand}</span>
                </div>
                <div className="flex gap-2">
                  <span className="text-xs font-medium text-gray-500 w-20 shrink-0">Model</span>
                  <span className="text-sm text-gray-900">{ski.model}</span>
                </div>
                <div className="flex gap-2">
                  <span className="text-xs font-medium text-gray-500 w-20 shrink-0">Délka</span>
                  <span className="text-sm text-gray-900">{ski.length}</span>
                </div>
                <div className="flex gap-2">
                  <span className="text-xs font-medium text-gray-500 w-20 shrink-0">Rok</span>
                  <span className="text-sm text-gray-900">{ski.year || 'N/A'}</span>
                </div>
                {ski.customer && (
                  <div className="flex gap-2 items-center">
                    <User className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                    <span className="text-xs font-medium text-gray-500 w-20 shrink-0">Zákazník</span>
                    <span className="text-sm text-gray-900">{ski.customer}</span>
                  </div>
                )}
              </div>
            </div>
          </div>

          {/* Servisní údaje: struktura + úkony z historie s datem zapsání */}
          <div className="space-y-3">
            <div>
              <h3 className="text-base font-semibold text-gray-900 mb-2">Servisní údaje</h3>
              <div className="space-y-2">
                {/* Struktura + kdy zapsáno */}
                <div>
                  <div className="text-xs font-medium text-gray-500 mb-0.5">Struktura</div>
                  <p className="text-sm text-gray-900">{ski.struktura && ski.struktura.trim() !== '' ? ski.struktura : '—'}</p>
                  {ski.strukturaRecordedAt && (
                    <p className="text-xs text-gray-500 mt-0.5">Zapsáno: {new Date(ski.strukturaRecordedAt).toLocaleDateString('cs-CZ')}</p>
                  )}
                </div>
                {/* Broušení hran – poslední z historie */}
                {(() => {
                  const last = serviceHistory
                    .filter(e => e.completed && (e.taskName?.toLowerCase().includes('broušení') || e.taskName?.toLowerCase().includes('hran')))
                    .sort((a, b) => (b.completedDate || b.orderDate || '').localeCompare(a.completedDate || a.orderDate || ''))[0]
                  return last ? (
                    <div>
                      <div className="text-xs font-medium text-gray-500 mb-0.5">Broušení hran</div>
                      {last.taskDescription && <p className="text-sm text-gray-900">{last.taskDescription}</p>}
                      <p className="text-xs text-gray-500 mt-0.5">Zapsáno: {last.completedDate ? new Date(last.completedDate).toLocaleDateString('cs-CZ') : (last.orderDate ? new Date(last.orderDate).toLocaleDateString('cs-CZ') : '—')}</p>
                    </div>
                  ) : (
                    <div>
                      <div className="text-xs font-medium text-gray-500 mb-0.5">Broušení hran</div>
                      <p className="text-sm text-gray-500">—</p>
                    </div>
                  )
                })()}
                {/* Měření lyží – poslední z historie */}
                {(() => {
                  const last = serviceHistory
                    .filter(e => e.completed && (e.taskName?.toLowerCase().includes('měření') || e.taskName?.toLowerCase().includes('mereni') || e.taskName?.toLowerCase().includes('měřen')))
                    .sort((a, b) => (b.completedDate || b.orderDate || '').localeCompare(a.completedDate || a.orderDate || ''))[0]
                  return last ? (
                    <div>
                      <div className="text-xs font-medium text-gray-500 mb-0.5">Měření lyží</div>
                      {last.taskDescription && <p className="text-sm text-gray-900">{last.taskDescription}</p>}
                      <p className="text-xs text-gray-500 mt-0.5">Zapsáno: {last.completedDate ? new Date(last.completedDate).toLocaleDateString('cs-CZ') : (last.orderDate ? new Date(last.orderDate).toLocaleDateString('cs-CZ') : '—')}</p>
                    </div>
                  ) : (
                    <div>
                      <div className="text-xs font-medium text-gray-500 mb-0.5">Měření lyží</div>
                      <p className="text-sm text-gray-500">—</p>
                    </div>
                  )
                })()}
                {ski.lastService && (
                  <div className="flex gap-2 items-center pt-1 border-t border-gray-100">
                    <Calendar className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                    <span className="text-xs font-medium text-gray-500 shrink-0">Poslední servis</span>
                    <span className="text-sm text-gray-900">{ski.lastService}</span>
                  </div>
                )}
                {ski.nextService && (
                  <div className="flex gap-2 items-center">
                    <Calendar className="w-3.5 h-3.5 text-gray-400 shrink-0" />
                    <span className="text-xs font-medium text-gray-500 shrink-0">Další servis</span>
                    <span className="text-sm text-gray-900">{ski.nextService}</span>
                  </div>
                )}
              </div>
            </div>

            {/* Priority warning */}
            {ski.priority === 'high' && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-2.5">
                <div className="flex items-center space-x-2">
                  <AlertTriangle className="w-4 h-4 text-red-600 shrink-0" />
                  <h4 className="text-red-800 font-medium text-sm">Vysoká priorita</h4>
                </div>
                <p className="text-red-700 text-xs mt-0.5">
                  Tato lyže vyžaduje okamžitou pozornost
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Service history – poslední úkony, po 5 s listováním */}
        <div className="mt-4">
          <h3 className="text-base font-semibold text-gray-900 mb-2 flex items-center gap-1.5">
            <Wrench className="w-4 h-4" />
            Historie servisních úkonů
            {serviceHistory.length > 0 && (
              <span className="text-xs font-normal text-gray-500">
                (celkem {serviceHistory.length})
              </span>
            )}
          </h3>
          {historyLoading ? (
            <div className="text-gray-500 py-2 text-sm">Načítám...</div>
          ) : serviceHistory.length > 0 ? (
            <>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 text-sm">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-2 py-1.5 text-left text-xs font-medium text-gray-500 uppercase">Datum</th>
                      <th className="px-2 py-1.5 text-left text-xs font-medium text-gray-500 uppercase">Úkon</th>
                      <th className="px-2 py-1.5 text-left text-xs font-medium text-gray-500 uppercase">Objednávka</th>
                      <th className="px-2 py-1.5 text-left text-xs font-medium text-gray-500 uppercase">Stav</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {paginatedHistory.map((entry) => (
                      <tr key={entry.taskItemId}>
                        <td className="px-2 py-1.5 text-gray-600 whitespace-nowrap">
                          {entry.completedDate || entry.orderDate
                            ? new Date(entry.completedDate || entry.orderDate!).toLocaleDateString('cs-CZ')
                            : '-'}
                        </td>
                        <td className="px-2 py-1.5">
                          <div>
                            <p className="font-medium text-gray-900">{entry.taskName}</p>
                            {entry.taskInstruction && (
                              <p className="text-xs text-gray-600">Návod: {entry.taskInstruction}</p>
                            )}
                            {entry.taskDescription && (
                              <p className="text-xs text-gray-500">Výsledek: {entry.taskDescription}</p>
                            )}
                          </div>
                        </td>
                        <td className="px-2 py-1.5 text-gray-600">{entry.orderNumber}</td>
                        <td className="px-2 py-1.5">
                          {entry.completed ? (
                            <span className="inline-flex items-center gap-0.5 px-1.5 py-0.5 text-xs font-medium rounded-full bg-green-100 text-green-800">
                              <CheckCircle className="w-3 h-3" />
                              Dokončeno
                            </span>
                          ) : (
                            <span className="px-1.5 py-0.5 text-xs font-medium rounded-full bg-yellow-100 text-yellow-800">
                              Probíhá
                            </span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              {totalHistoryPages > 1 && (
                <div className="flex items-center justify-between mt-2 px-1">
                  <button
                    type="button"
                    onClick={() => setHistoryPage(p => Math.max(1, p - 1))}
                    disabled={historyPage <= 1}
                    className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    <ChevronLeft className="w-3.5 h-3.5" />
                    Předchozí
                  </button>
                  <span className="text-xs text-gray-600">
                    Stránka {historyPage} z {totalHistoryPages}
                  </span>
                  <button
                    type="button"
                    onClick={() => setHistoryPage(p => Math.min(totalHistoryPages, p + 1))}
                    disabled={historyPage >= totalHistoryPages}
                    className="flex items-center gap-1 px-2 py-1 rounded text-xs font-medium text-gray-700 bg-white border border-gray-300 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    Další
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              )}
            </>
          ) : (
            <p className="text-gray-500 py-2 text-sm">Žádná historie servisních úkonů</p>
          )}
        </div>

        {/* Objednávka z QR skenu */}
        {orderFromScan && skiNumberForOrder && onOrderUpdated && (
          <div className="mt-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
            <div className="flex items-center justify-between mb-2">
              <h3 className="text-base font-semibold text-blue-900 flex items-center gap-1.5">
                <Package className="w-4 h-4" />
                Objednávka {orderFromScan.orderNumber}
              </h3>
              <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getOrderPriorityColor(orderFromScan.priority || 'STREDNI')}`}>
                {PRIORITY_LABELS[orderFromScan.priority || 'STREDNI']}
              </span>
              <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getOrderStatusColor(orderFromScan.status || 'NOVE')}`}>
                {ORDER_STATUS_LABELS[orderFromScan.status || 'NOVE']}
              </span>
            </div>
            <p className="text-xs text-blue-800 mb-2">{orderFromScan.customerName || '—'}</p>
            {(() => {
              const task = orderFromScan.tasks?.find(t => t.skiNumber === skiNumberForOrder)
              if (!task) return null
              return (
                <>
                  <p className="text-xs font-medium text-gray-700 mb-1.5">Úkony na této lyži (popis lze upravit):</p>
                  <ul className="space-y-2">
                    {task.taskItems.map(item => {
                      const matchingOption = modificationOptions.find(o => o.name === item.taskName)
                      const requiresDescription = matchingOption !== undefined ? matchingOption.requiresWorkDescription : item.requiresWorkDescription
                      const currentDesc = (taskItemDescEdits[item.id] ?? item.taskDescription ?? '').trim()
                      const hasDescError = taskItemDescriptionErrorId === item.id
                      return (
                        <li key={item.id} className="border-b border-blue-100 pb-2 last:border-0 last:pb-0">
                          <div className="flex items-start gap-2">
                            <input
                              type="checkbox"
                              checked={item.completed}
                              onChange={e => {
                                const completed = e.target.checked
                                if (completed && requiresDescription && !currentDesc) {
                                  setTaskItemDescriptionErrorId(item.id)
                                  return
                                }
                                setTaskItemDescriptionErrorId(prev => prev === item.id ? null : prev)
                                apiClient.updateTaskItem(orderFromScan.id, task.id, item.id, { completed })
                                  .then(updated => {
                                    onOrderUpdated(updated)
                                    const allCompleted = updated.tasks.every(t => t.taskItems.length === 0 || t.taskItems.every(i => i.completed))
                                    if (allCompleted && updated.status !== 'UKONCENA') {
                                      return apiClient.updateOrder(orderFromScan.id, { status: 'UKONCENA' }).then(onOrderUpdated)
                                    }
                                    if (!allCompleted && updated.status === 'UKONCENA') {
                                      return apiClient.updateOrder(orderFromScan.id, { status: 'NOVE' }).then(onOrderUpdated)
                                    }
                                  })
                                  .catch(console.error)
                              }}
                              className="mt-1 rounded border-gray-300 text-green-600 focus:ring-green-500 shrink-0"
                            />
                            <div className="min-w-0 flex-1 space-y-1">
                              <span className={`text-xs font-medium block ${item.completed ? 'text-gray-600 line-through' : 'text-gray-900'}`}>{item.taskName}</span>
                              <textarea
                                className="mt-0.5 w-full text-xs rounded px-2 py-1 border border-gray-300 focus:ring-1 focus:ring-blue-500 min-w-0"
                                rows={1}
                                placeholder="Jak zpracovat (návod)"
                                value={taskItemInstructionEdits[item.id] ?? item.taskInstruction ?? ''}
                                onChange={e => setTaskItemInstructionEdits(prev => ({ ...prev, [item.id]: e.target.value }))}
                                onBlur={e => {
                                  const v = e.target.value.trim()
                                  const current = item.taskInstruction ?? ''
                                  if (v === current) {
                                    setTaskItemInstructionEdits(prev => { const next = { ...prev }; delete next[item.id]; return next })
                                    return
                                  }
                                  apiClient.updateTaskItem(orderFromScan.id, task.id, item.id, { taskInstruction: v || null })
                                    .then(updated => {
                                      setTaskItemInstructionEdits(prev => { const next = { ...prev }; delete next[item.id]; return next })
                                      onOrderUpdated(updated)
                                    })
                                    .catch(console.error)
                                }}
                              />
                              <textarea
                                className={`w-full text-xs rounded px-2 py-1 focus:ring-1 min-w-0 ${
                                  hasDescError ? 'border-red-500 bg-red-50 focus:ring-red-500 focus:border-red-500' : 'border border-gray-300 focus:ring-blue-500 focus:border-blue-500'
                                }`}
                                rows={2}
                                placeholder={requiresDescription ? 'Výsledek (povinné) – doplňte po měření' : 'Výsledek (volitelné)'}
                                value={taskItemDescEdits[item.id] ?? item.taskDescription ?? ''}
                                onChange={e => {
                                  setTaskItemDescEdits(prev => ({ ...prev, [item.id]: e.target.value }))
                                  if (e.target.value.trim()) setTaskItemDescriptionErrorId(prev => prev === item.id ? null : prev)
                                }}
                                onBlur={e => {
                                  const v = e.target.value.trim()
                                  const current = item.taskDescription ?? ''
                                  if (v === current) {
                                    setTaskItemDescEdits(prev => { const next = { ...prev }; delete next[item.id]; return next })
                                    return
                                  }
                                  apiClient.updateTaskItem(orderFromScan.id, task.id, item.id, { taskDescription: v || null })
                                    .then(updated => {
                                      setTaskItemDescEdits(prev => { const next = { ...prev }; delete next[item.id]; return next })
                                      onOrderUpdated(updated)
                                    })
                                    .catch(console.error)
                                }}
                              />
                              {hasDescError && (
                                <p className="text-xs text-red-600 mt-0.5">Doplňte výsledek (zakončovací popis) před označením úkonu jako dokončený.</p>
                              )}
                            </div>
                          </div>
                        </li>
                      )
                    })}
                  </ul>
                </>
              )
            })()}
          </div>
        )}

        {/* Notes */}
        {ski.notes && (
          <div className="mt-4">
            <h3 className="text-base font-semibold text-gray-900 mb-1.5">Poznámky</h3>
            <div className="bg-gray-50 rounded-lg p-2.5">
              <p className="text-sm text-gray-900">{ski.notes}</p>
            </div>
          </div>
        )}

        {/* Actions */}
        <div className="mt-4 flex justify-end flex-wrap gap-2">
          <button
            type="button"
            onClick={handlePrintQr}
            className="px-4 py-1.5 text-sm bg-emerald-600 text-white rounded-md hover:bg-emerald-700 transition-colors flex items-center space-x-1.5"
          >
            <QrCode className="w-3.5 h-3.5" />
            <span>Vytisknout QR kód</span>
          </button>
          {orderFromScan && onOrderUpdated && (
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors"
            >
              Uložit
            </button>
          )}
          <button
            onClick={onClose}
            className="px-4 py-1.5 text-sm bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors"
          >
            Zavřít
          </button>
          {onEdit && (
            <button
              onClick={() => onEdit(ski.id, ski.numericId)}
              className="px-4 py-1.5 text-sm bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors flex items-center space-x-1.5"
            >
              <Edit className="w-3.5 h-3.5" />
              <span>Upravit</span>
            </button>
          )}
          {onDelete && (
            <button
              onClick={() => onDelete(ski.id, ski.numericId)}
              className="px-4 py-1.5 text-sm bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors flex items-center space-x-1.5"
            >
              <Trash2 className="w-3.5 h-3.5" />
              <span>Smazat</span>
            </button>
          )}
        </div>
      </div>
    </div>
  )
}

export default SkiDetail
