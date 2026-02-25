'use client'

import { useState, useEffect } from 'react'
import {
  User,
  Mail,
  Package,
  Layers,
  Wrench,
  CheckCircle,
  Clock,
  Calendar,
  ChevronDown,
  ChevronRight,
  Loader2,
} from 'lucide-react'
import { useAuth } from '@/contexts/AuthContext'
import { useLanguage } from '@/contexts/LanguageContext'
import ProtectedRoute from '@/components/ProtectedRoute'
import apiClient, {
  OrderDetailResponse,
  OrderTaskResponse,
} from '@/lib/api'

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

export default function CustomerPage() {
  const { t } = useLanguage()
  const [orders, setOrders] = useState<OrderDetailResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [expandedTaskId, setExpandedTaskId] = useState<number | null>(null)
  const { user } = useAuth()

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      try {
        const data = await apiClient.getMyOrders()
        if (!cancelled) setOrders(data ?? [])
      } catch (e) {
        console.error('Failed to load orders:', e)
        if (!cancelled) setOrders([])
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    if (user?.role === 'CUSTOMER') {
      load()
    }
    return () => { cancelled = true }
  }, [user?.role])

  if (loading) {
    return (
      <ProtectedRoute requiredRole="CUSTOMER">
        <div className="text-center py-12">
          <Loader2 className="w-12 h-12 animate-spin text-blue-600 mx-auto mb-4" />
          <p className="text-gray-600">{t('customer.loadingOrders')}</p>
        </div>
      </ProtectedRoute>
    )
  }

  return (
    <ProtectedRoute requiredRole="CUSTOMER">
      <div className="space-y-8">
        {/* Zákazník */}
        <div className="bg-white rounded-lg shadow-md p-6">
          <div className="flex items-center space-x-4">
            <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center">
              <User className="w-8 h-8 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">{user?.fullName || orders[0]?.customerName || t('customer.customer')}</h1>
              {user?.email && (
                <div className="flex items-center space-x-1 text-gray-600 mt-1">
                  <Mail className="w-4 h-4" />
                  <span>{user.email}</span>
                </div>
              )}
            </div>
          </div>
        </div>

        {/* Seznam objednávek */}
        {orders.length === 0 ? (
          <div className="text-center py-12 bg-white rounded-lg shadow-md">
            <Package className="w-12 h-12 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-500 text-lg">{t('customer.noOrdersShort')}</p>
          </div>
        ) : (
          <div className="space-y-6">
            <h2 className="text-lg font-semibold text-gray-900">{t('customer.myOrders')} ({orders.length})</h2>
            {orders.map(order => (
              <div key={order.id} className="bg-white rounded-lg shadow-md overflow-hidden">
                <div className="p-6 border-b border-gray-200">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <h3 className="text-xl font-semibold text-gray-900">{t('customer.order')} #{order.orderNumber}</h3>
                    </div>
                    <div className="flex items-center gap-4 text-sm text-gray-600">
                      {order.createdAt && (
                        <span className="flex items-center gap-1">
                          <Calendar className="w-4 h-4" />
                          {t('customer.created')}: {order.createdAt.split('T')[0]}
                        </span>
                      )}
                      {order.dueDate && (
                        <span className="flex items-center gap-1">
                          <Calendar className="w-4 h-4" />
                          {t('customer.due')}: {order.dueDate}
                        </span>
                      )}
                    </div>
                  </div>
                  {order.notes && (
                    <p className="mt-2 text-gray-600">{order.notes}</p>
                  )}
                </div>

                <div className="divide-y divide-gray-200">
                  {order.tasks.map(task => (
                    <CustomerTaskRow
                      key={task.id}
                      task={task}
                      isExpanded={expandedTaskId === task.id}
                      onToggle={() => setExpandedTaskId(prev => (prev === task.id ? null : task.id))}
                      t={t}
                    />
                  ))}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </ProtectedRoute>
  )
}

function CustomerTaskRow({
  task,
  isExpanded,
  onToggle,
  t,
}: {
  task: OrderTaskResponse
  isExpanded: boolean
  onToggle: () => void
  t: (key: string) => string
}) {
  return (
    <div>
      <button
        onClick={onToggle}
        className="w-full flex items-center gap-3 px-6 py-4 text-left hover:bg-gray-50 transition-colors"
      >
        {isExpanded ? (
          <ChevronDown className="w-5 h-5 text-gray-500 shrink-0" />
        ) : (
          <ChevronRight className="w-5 h-5 text-gray-500 shrink-0" />
        )}
        <Layers className="w-5 h-5 text-green-600 shrink-0" />
        <span className="font-medium text-gray-900">{task.skiInfo || `${t('customer.skiLabel')} #${task.skiNumber}`}</span>
        <span className="text-gray-500 text-sm">{task.skiNumber}</span>
        <span className={`px-2 py-1 text-xs font-medium rounded-full ${getStatusColor(task.status)} ml-auto`}>
          {t('status.' + task.status) || task.status}
        </span>
      </button>

      {isExpanded && (
        <div className="bg-gray-50 border-t border-gray-200 px-6 pb-4 pt-2">
          <p className="text-sm font-medium text-gray-700 mb-2">{t('customer.serviceTasks')}</p>
          <ul className="space-y-2">
            {task.taskItems.map(item => (
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
                  <span className="text-gray-600">— {t('customer.instruction')}: {item.taskInstruction}</span>
                )}
                {item.taskDescription && (
                  <span className="text-gray-500">— {t('customer.result')}: {item.taskDescription}</span>
                )}
              </li>
            ))}
          </ul>
          {task.taskItems.length === 0 && (
            <p className="text-gray-500 text-sm">{t('customer.noTasks')}</p>
          )}
        </div>
      )}
    </div>
  )
}
