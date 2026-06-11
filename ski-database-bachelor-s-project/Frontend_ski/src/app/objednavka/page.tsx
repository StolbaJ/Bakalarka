'use client'

import { useState, useEffect, Suspense } from 'react'
import { useSearchParams } from 'next/navigation'
import Link from 'next/link'
import {
  Layers,
  Wrench,
  CheckCircle,
  Clock,
  Calendar,
  ChevronDown,
  ChevronRight,
  Loader2,
  AlertCircle,
  ExternalLink,
} from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'
import apiClient, {
  OrderDetailResponse,
  OrderTaskResponse,
} from '@/lib/api'

const STATUS_LABELS: Record<string, string> = {
  CEKA: 'Čeká',
  PROBIHA: 'Probíhá',
  DOKONCENO: 'Dokončeno',
  POZASTAVENO: 'Pozastaveno',
}

function getStatusColor(status: string) {
  switch (status) {
    case 'CEKA':
      return 'bg-yellow-100 text-yellow-800'
    case 'PROBIHA':
      return 'bg-blue-100 text-blue-800'
    case 'DOKONCENO':
      return 'bg-green-100 text-green-800'
    case 'POZASTAVENO':
      return 'bg-gray-100 text-gray-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

function OrderViewContent() {
  const searchParams = useSearchParams()
  const token = searchParams.get('token')
  const { setUserFromAuthResponse } = useAuth()
  const [order, setOrder] = useState<OrderDetailResponse | null>(null)
  const [loading, setLoading] = useState(!!token)
  const [error, setError] = useState<string | null>(null)
  const [expandedTaskId, setExpandedTaskId] = useState<number | null>(null)

  useEffect(() => {
    if (!token) {
      setLoading(false)
      setError('V adrese chybí odkaz na objednávku.')
      return
    }
    let cancelled = false
    apiClient
      .viewOrderByToken(token)
      .then((data) => {
        if (cancelled) return
        setOrder(data.order)
        setUserFromAuthResponse(data.authResponse)
        setError(null)
      })
      .catch((err) => {
        if (cancelled) return
        setError(err instanceof Error ? err.message : 'Nepodařilo se načíst objednávku.')
        setOrder(null)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [token, setUserFromAuthResponse])

  if (loading) {
    return (
      <div className="min-h-[40vh] flex flex-col items-center justify-center">
        <Loader2 className="w-12 h-12 animate-spin text-blue-600 mb-4" />
        <p className="text-gray-600">Načítám objednávku...</p>
      </div>
    )
  }

  if (error || !order) {
    return (
      <div className="max-w-md mx-auto mt-12 text-center">
        <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
        <h1 className="text-xl font-semibold text-gray-900 mb-2">Objednávku nelze zobrazit</h1>
        <p className="text-gray-600 mb-6">{error}</p>
        <Link
          href="/"
          className="inline-flex items-center gap-2 text-blue-600 hover:underline"
        >
          Zpět na úvodní stránku
        </Link>
      </div>
    )
  }

  return (
    <div className="max-w-2xl mx-auto py-8 px-4">
      <div className="mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <h1 className="text-2xl font-bold text-gray-900">Přehled objednávky</h1>
        <Link
          href="/customer"
          className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          <ExternalLink className="w-4 h-4" />
          Zobrazit všechny moje objednávky
        </Link>
      </div>

      <div className="bg-white rounded-lg shadow-md overflow-hidden">
        <div className="p-6 border-b border-gray-200">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="flex items-center gap-3">
              <h2 className="text-xl font-semibold text-gray-900">Objednávka #{order.orderNumber}</h2>
            </div>
            <div className="flex items-center gap-4 text-sm text-gray-600">
              {order.createdAt && (
                <span className="flex items-center gap-1">
                  <Calendar className="w-4 h-4" />
                  {order.createdAt.split('T')[0]}
                </span>
              )}
              {order.dueDate && (
                <span className="flex items-center gap-1">
                  <Calendar className="w-4 h-4" />
                  Termín: {order.dueDate}
                </span>
              )}
            </div>
          </div>
          {order.notes && (
            <p className="mt-2 text-gray-600">{order.notes}</p>
          )}
        </div>

        <div className="divide-y divide-gray-200">
          {order.tasks.map((task) => (
            <TaskRow
              key={task.id}
              task={task}
              isExpanded={expandedTaskId === task.id}
              onToggle={() => setExpandedTaskId((prev) => (prev === task.id ? null : task.id))}
            />
          ))}
        </div>
      </div>

      <p className="mt-6 text-center text-sm text-gray-500">
        Stav můžete kdykoli zkontrolovat na{' '}
        <Link href="/" className="text-blue-600 hover:underline">bezkyservis.xyz</Link> pomocí čísla objednávky a telefonu.
      </p>
    </div>
  )
}

function TaskRow({
  task,
  isExpanded,
  onToggle,
}: {
  task: OrderTaskResponse
  isExpanded: boolean
  onToggle: () => void
}) {
  return (
    <div>
      <button
        type="button"
        onClick={onToggle}
        className="w-full flex items-center gap-3 px-6 py-4 text-left hover:bg-gray-50 transition-colors"
      >
        {isExpanded ? (
          <ChevronDown className="w-5 h-5 text-gray-500 shrink-0" />
        ) : (
          <ChevronRight className="w-5 h-5 text-gray-500 shrink-0" />
        )}
        <Layers className="w-5 h-5 text-green-600 shrink-0" />
        <span className="font-medium text-gray-900">{task.skiInfo || `Lyže #${task.skiNumber}`}</span>
        <span className="text-gray-500 text-sm">{task.skiNumber}</span>
        <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(task.status)} ml-auto`}>
          {STATUS_LABELS[task.status] || task.status}
        </span>
      </button>
      {isExpanded && (
        <div className="bg-gray-50 border-t border-gray-200 px-6 pb-4 pt-2">
          <p className="text-sm font-medium text-gray-700 mb-2">Servisní úkony</p>
          <ul className="space-y-2">
            {task.taskItems.map((item) => (
              <li key={item.id} className="flex items-center gap-2 text-sm">
                {item.completed ? (
                  <CheckCircle className="w-4 h-4 text-green-600 shrink-0" />
                ) : (
                  <Clock className="w-4 h-4 text-gray-400 shrink-0" />
                )}
                <Wrench className="w-4 h-4 text-gray-400 shrink-0" />
                <span className={item.completed ? 'text-gray-600 line-through' : 'text-gray-900'}>
                  {item.taskName}
                </span>
                {item.taskInstruction && (
                  <span className="text-gray-600">— Návod: {item.taskInstruction}</span>
                )}
                {item.taskDescription && (
                  <span className="text-gray-500">— Výsledek: {item.taskDescription}</span>
                )}
              </li>
            ))}
          </ul>
          {task.taskItems.length === 0 && (
            <p className="text-gray-500 text-sm">Žádné úkony</p>
          )}
        </div>
      )}
    </div>
  )
}

export default function ObjednavkaPage() {
  return (
    <Suspense
      fallback={
        <div className="min-h-[40vh] flex flex-col items-center justify-center">
          <Loader2 className="w-12 h-12 animate-spin text-blue-600 mb-4" />
          <p className="text-gray-600">Načítám...</p>
        </div>
      }
    >
      <OrderViewContent />
    </Suspense>
  )
}
