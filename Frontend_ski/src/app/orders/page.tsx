'use client'

import { useState, useEffect, useRef, useCallback, Suspense } from 'react'
import { useSearchParams } from 'next/navigation'
import {
  ChevronDown,
  ChevronRight,
  Search,
  Package,
  Layers,
  Wrench,
  User,
  Calendar,
  Loader2,
  Plus,
  Banknote,
  Pencil,
  Eye,
  QrCode,
  Mail,
  Phone,
} from 'lucide-react'
import ProtectedRoute from '@/components/ProtectedRoute'
import { useLanguage } from '@/contexts/LanguageContext'
import PaginationControls from '@/components/PaginationControls'
import QRScanner from '@/components/QRScanner'
import SkiDetail from '@/components/SkiDetail'
import { SkiData } from '@/components/SkiItem'
import SkiEditForm, { SkiFormData } from '@/components/SkiEditForm'
import { skiResponseToData } from '@/lib/skiUtils'
import apiClient, {
  OrderSummaryResponse,
  OrderDetailResponse,
  OrderTaskResponse,
  ServiceTaskItemResponse,
  CreateOrderRequest,
  CustomerSummaryResponse,
  CustomerDetailResponse,
  StrukturaOptionResponse,
  ModificationOptionResponse,
} from '@/lib/api'

function getOrderStatusColor(status: string) {
  switch (status) {
    case 'NOVE':
      return 'bg-sky-100 text-sky-800'
    case 'VE_ZPRACOVANI':
      return 'bg-blue-100 text-blue-800'
    case 'POZASTAVENA':
      return 'bg-amber-100 text-amber-800'
    case 'UKONCENA':
      return 'bg-green-100 text-green-800'
    case 'STORNOVANA':
      return 'bg-red-100 text-red-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
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

function getPriorityColor(priority: string) {
  switch (priority) {
    case 'NIZKA':
      return 'bg-green-100 text-green-800'
    case 'STREDNI':
      return 'bg-yellow-100 text-yellow-800'
    case 'VYSOKA':
      return 'bg-orange-100 text-orange-800'
    case 'KRITICKA':
      return 'bg-red-100 text-red-800'
    default:
      return 'bg-gray-100 text-gray-800'
  }
}

function EditCustomerModal({
  customerId,
  onClose,
  onSaved,
}: {
  customerId: number
  onClose: () => void
  onSaved: (list: CustomerSummaryResponse[]) => void
}) {
  const { t } = useLanguage()
  const [customer, setCustomer] = useState<CustomerDetailResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [address, setAddress] = useState('')

  useEffect(() => {
    let cancelled = false
    apiClient
      .getCustomer(customerId)
      .then((c) => {
        if (!cancelled) {
          setCustomer(c)
          setName(c.name ?? '')
          setEmail(c.email ?? '')
          setPhone(c.phone ?? '')
          setAddress(c.address ?? '')
        }
      })
      .catch(() => {
        if (!cancelled) setError(t('orders.loadCustomerError'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => { cancelled = true }
  }, [customerId, t])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    const nameTrim = name.trim()
    const emailTrim = email.trim()
    const phoneTrim = phone.trim()
    if (!nameTrim || !emailTrim || !phoneTrim) {
      setError(t('orders.fieldsRequired'))
      return
    }
    setSaving(true)
    try {
      await apiClient.updateCustomer(customerId, {
        name: nameTrim,
        email: emailTrim,
        phone: phoneTrim,
        address: address.trim() || null,
      })
      const res = await apiClient.getCustomers(0, 500)
      onSaved(res.content.slice().sort((a, b) => a.name.localeCompare(b.name)))
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : t('orders.saveError'))
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 cursor-pointer" onClick={onClose}>
      <div className="bg-white rounded-xl shadow-xl w-full max-w-md cursor-default" onClick={(e) => e.stopPropagation()}>
        <div className="p-6 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-900">{t('orders.editCustomerContacts')}</h2>
          <button type="button" onClick={onClose} className="text-gray-500 hover:text-gray-700 p-1 cursor-pointer">×</button>
        </div>
        <div className="p-6">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : customer ? (
            <form onSubmit={handleSubmit} className="space-y-4">
              {error && (
                <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.name')} *</label>
                <input type="text" value={name} onChange={(e) => setName(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.email')} *</label>
                <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.phone')} *</label>
                <input type="tel" value={phone} onChange={(e) => setPhone(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" required />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('orders.addressOptional')}</label>
                <input type="text" value={address} onChange={(e) => setAddress(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" placeholder={t('orders.addressPlaceholder')} />
              </div>
              <div className="flex gap-3 pt-2">
                <button type="submit" disabled={saving} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium cursor-pointer">
                  {saving ? t('common.saving') : t('common.save')}
                </button>
                <button type="button" onClick={onClose} disabled={saving} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                  {t('common.cancel')}
                </button>
              </div>
            </form>
          ) : (
            <p className="text-gray-500 py-4">{error ?? t('orders.customerNotFound')}</p>
          )}
        </div>
      </div>
    </div>
  )
}

const ORDERS_PAGE_SIZE_OPTIONS = [10, 20, 50, 100]

function OrdersPageContent() {
  const { t } = useLanguage()
  const searchParams = useSearchParams()
  const [orders, setOrders] = useState<OrderSummaryResponse[]>([])
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [showDone, setShowDone] = useState(true)
  const [loading, setLoading] = useState(true)
  const [expandedOrderId, setExpandedOrderId] = useState<number | null>(null)
  const expandFromUrl = useRef(false)
  const [expandedTaskId, setExpandedTaskId] = useState<number | null>(null)
  const [orderDetails, setOrderDetails] = useState<Record<number, OrderDetailResponse>>({})
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [selectedSkiForDetail, setSelectedSkiForDetail] = useState<SkiData | null>(null)
  const [skiDetailOrderContext, setSkiDetailOrderContext] = useState<{ order: OrderDetailResponse; skiNumber: string } | null>(null)
  const [loadingSkiDetail, setLoadingSkiDetail] = useState(false)
  const [strukturyOptions, setStrukturyOptions] = useState<StrukturaOptionResponse[]>([])
  const [modificationOptions, setModificationOptions] = useState<ModificationOptionResponse[]>([])

  const loadOrders = useCallback(async (pageNum: number, pageSize: number, searchTerm: string) => {
    setLoading(true)
    try {
      const res = await apiClient.getOrders(pageNum, pageSize, searchTerm || undefined)
      setOrders(res.content)
      setTotalElements(res.totalElements)
      setTotalPages(res.totalPages)
    } catch (e) {
      console.error('Failed to load orders:', e)
      setOrders([])
      setTotalElements(0)
      setTotalPages(0)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadOrders(page, size, search)
  }, [loadOrders, page, size, search])

  useEffect(() => {
    let cancelled = false
    Promise.all([apiClient.getStrukturyOptions(), apiClient.getModificationOptions()]).then(([s, u]) => {
      if (!cancelled) {
        setStrukturyOptions(s)
        setModificationOptions(u)
      }
    }).catch(() => {})
    return () => { cancelled = true }
  }, [])

  const expandId = searchParams.get('expand')
  useEffect(() => {
    if (expandId && !expandFromUrl.current) {
      const id = parseInt(expandId, 10)
      if (!isNaN(id)) {
        setExpandedOrderId(id)
        expandFromUrl.current = true
      }
    }
  }, [expandId])

  useEffect(() => {
    if (expandedOrderId && !orderDetails[expandedOrderId]) {
      apiClient.getOrder(expandedOrderId).then(d => setOrderDetails(prev => ({ ...prev, [expandedOrderId]: d }))).catch(console.error)
    }
  }, [expandedOrderId, orderDetails])

  const handleToggleOrder = async (orderId: number) => {
    if (expandedOrderId === orderId) {
      setExpandedOrderId(null)
      setExpandedTaskId(null)
      return
    }
    setExpandedOrderId(orderId)
    setExpandedTaskId(null)
    if (!orderDetails[orderId]) {
      try {
        const detail = await apiClient.getOrder(orderId)
        setOrderDetails(prev => ({ ...prev, [orderId]: detail }))
      } catch (e) {
        console.error('Failed to load order detail:', e)
      }
    }
  }

  const handleToggleTask = (taskId: number) => {
    setExpandedTaskId(prev => (prev === taskId ? null : taskId))
  }

  const handleOrderUpdated = (orderId: number, updatedDetail: OrderDetailResponse) => {
    setOrderDetails(prev => ({ ...prev, [orderId]: updatedDetail }))
    setOrders(prev => prev.map(o => 
      o.id === orderId 
        ? { 
            ...o, 
            orderDone: updatedDetail.orderDone, 
            taskCount: updatedDetail.tasks.length, 
            priority: updatedDetail.priority ?? o.priority, 
            status: updatedDetail.status ?? o.status,
            price: updatedDetail.price ?? o.price,
          } 
        : o
    ))
  }

  const handleOpenSkiDetail = async (skiId: number, orderDetail?: OrderDetailResponse, skiNumber?: string) => {
    setLoadingSkiDetail(true)
    setSkiDetailOrderContext(orderDetail && skiNumber ? { order: orderDetail, skiNumber } : null)
    try {
      const ski = await apiClient.getSki(skiId)
      setSelectedSkiForDetail(skiResponseToData(ski))
    } catch (e) {
      console.error('Failed to load ski detail:', e)
    } finally {
      setLoadingSkiDetail(false)
    }
  }

  const PRIORITY_ORDER = { KRITICKA: 0, VYSOKA: 1, STREDNI: 2, NIZKA: 3 } as Record<string, number>

  const filteredOrders = orders
    .filter(o => showDone || !o.orderDone)
    .sort((a, b) => {
      if (a.orderDone !== b.orderDone) return a.orderDone ? 1 : -1
      const pa = PRIORITY_ORDER[a.priority || 'STREDNI'] ?? 2
      const pb = PRIORITY_ORDER[b.priority || 'STREDNI'] ?? 2
      return pa - pb
    })

  const handlePageChange = useCallback((newPage: number) => {
    setPage(Math.max(0, Math.min(newPage, totalPages - 1)))
  }, [totalPages])

  const handleSizeChange = useCallback((newSize: number) => {
    setSize(newSize)
    setPage(0)
  }, [])

  return (
    <ProtectedRoute requiredRole="ADMIN_OR_TECHNICIAN">
      <div className="space-y-8">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-1">{t('orders.title')}</h1>
            <p className="text-gray-600">{t('orders.subtitle')}</p>
          </div>
          <button
            type="button"
            onClick={() => setShowCreateModal(true)}
            className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium cursor-pointer"
          >
            <Plus className="w-5 h-5" /> {t('orders.newOrder')}
          </button>
        </div>

        {showCreateModal && (
          <CreateOrderModal
            onClose={() => setShowCreateModal(false)}
            onCreated={async (created) => {
              setShowCreateModal(false)
              const res = await apiClient.getOrders(page, size, search || undefined)
              setOrders(res.content)
              setTotalElements(res.totalElements)
              setTotalPages(res.totalPages)
              setExpandedOrderId(created.id)
              setOrderDetails(prev => ({ ...prev, [created.id]: created }))
            }}
          />
        )}

        {/* Vyhledávání a filtry */}
        <div className="bg-white rounded-lg shadow-md p-4 space-y-4">
          <div className="relative">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
              <Search className="h-5 w-5 text-gray-400" />
            </div>
            <input
              type="text"
              placeholder={t('orders.searchPlaceholder')}
              value={search}
              onChange={e => { setSearch(e.target.value); setPage(0) }}
              className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500"
            />
          </div>
          <label className="flex items-center gap-2 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={showDone}
              onChange={e => setShowDone(e.target.checked)}
              className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer"
            />
            <span className="text-sm text-gray-700">{t('orders.showDone')}</span>
          </label>
        </div>

        {/* Seznam objednávek */}
        <div className="bg-white rounded-lg shadow-md overflow-hidden">
          {loading ? (
            <div className="flex items-center justify-center py-16">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : filteredOrders.length === 0 ? (
            <div className="text-center py-16 text-gray-500">
              <Package className="w-12 h-12 mx-auto mb-4 text-gray-400" />
              <p>{t('orders.noOrders')}</p>
            </div>
          ) : (
            <>
              <div className="divide-y divide-gray-200">
                {filteredOrders.map(order => (
                  <OrderRow
                    key={order.id}
                    order={order}
                    detail={orderDetails[order.id]}
                    isExpanded={expandedOrderId === order.id}
                    expandedTaskId={expandedTaskId}
                    onToggleOrder={() => handleToggleOrder(order.id)}
                    onToggleTask={handleToggleTask}
                    onOrderUpdated={handleOrderUpdated}
                    onOpenSkiDetail={handleOpenSkiDetail}
                    loadingSkiDetail={loadingSkiDetail}
                    strukturyOptions={strukturyOptions}
                    modificationOptions={modificationOptions}
                    t={t}
                  />
                ))}
              </div>
              {totalPages > 0 && (
                <PaginationControls
                  page={page}
                  size={size}
                  totalElements={totalElements}
                  totalPages={totalPages}
                  onPageChange={handlePageChange}
                  onSizeChange={handleSizeChange}
                  pageSizeOptions={ORDERS_PAGE_SIZE_OPTIONS}
                  t={t}
                />
              )}
            </>
          )}
        </div>
      </div>
      {selectedSkiForDetail && (
        <SkiDetail
          ski={selectedSkiForDetail}
          onClose={() => { setSelectedSkiForDetail(null); setSkiDetailOrderContext(null) }}
          orderFromScan={skiDetailOrderContext?.order ?? null}
          skiNumberForOrder={skiDetailOrderContext?.skiNumber ?? undefined}
          modificationOptions={modificationOptions}
          onOrderUpdated={skiDetailOrderContext ? (updated) => {
            handleOrderUpdated(updated.id, updated)
            setSkiDetailOrderContext(prev => prev && prev.order.id === updated.id ? { order: updated, skiNumber: prev.skiNumber } : prev)
          } : undefined}
        />
      )}
    </ProtectedRoute>
  )
}

export default function OrdersPage() {
  return (
    <Suspense fallback={
      <div className="flex items-center justify-center min-h-[200px]">
        <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
      </div>
    }>
      <OrdersPageContent />
    </Suspense>
  )
}

function OrderRow({
  order,
  detail,
  isExpanded,
  expandedTaskId,
  onToggleOrder,
  onToggleTask,
  onOrderUpdated,
  onOpenSkiDetail,
  loadingSkiDetail,
  strukturyOptions,
  modificationOptions,
  t,
}: {
  order: OrderSummaryResponse
  detail?: OrderDetailResponse | null
  isExpanded: boolean
  expandedTaskId: number | null
  onToggleOrder: () => void
  onToggleTask: (taskId: number) => void
  onOrderUpdated: (orderId: number, detail: OrderDetailResponse) => void
  onOpenSkiDetail: (skiId: number, orderDetail?: OrderDetailResponse, skiNumber?: string) => void
  loadingSkiDetail: boolean
  strukturyOptions: StrukturaOptionResponse[]
  modificationOptions: ModificationOptionResponse[]
  t: (key: string) => string
}) {
  const displayOrder = detail
    ? { ...order, orderDone: detail.orderDone, priority: detail.priority ?? order.priority, status: detail.status ?? order.status }
    : order
  return (
    <div>
      <button
        onClick={onToggleOrder}
        className="w-full flex items-center gap-3 px-6 py-4 text-left hover:bg-gray-50 transition-colors cursor-pointer"
        type="button"
      >
        {isExpanded ? (
          <ChevronDown className="w-5 h-5 text-gray-500 shrink-0" />
        ) : (
          <ChevronRight className="w-5 h-5 text-gray-500 shrink-0" />
        )}
        <Package className="w-5 h-5 text-blue-600 shrink-0" />
        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getPriorityColor(displayOrder.priority || 'STREDNI')}`}>
          {t('priority.' + (displayOrder.priority || 'STREDNI'))}
        </span>
        <span className={`px-2 py-0.5 text-xs font-medium rounded-full ${getOrderStatusColor(displayOrder.status || 'NOVE')}`}>
          {t('status.' + (displayOrder.status || 'NOVE'))}
        </span>
        <span className="font-semibold text-gray-900">{displayOrder.orderNumber}</span>
        <span className="text-gray-600">{displayOrder.customerName || t('common.notSet')}</span>
        <span className="text-gray-500 text-sm">{displayOrder.taskCount} {t('orders.skisCount')}</span>
        <span className="text-gray-600 text-sm flex items-center gap-1">
          <Banknote className="w-4 h-4" /> {displayOrder.price != null ? `${displayOrder.price} Kč` : '—'}
        </span>
        {order.dueDate && (
          <span className="text-gray-500 text-sm ml-auto flex items-center gap-1">
            <Calendar className="w-4 h-4" /> {order.dueDate}
          </span>
        )}
      </button>

      {isExpanded && detail && (
        <OrderDetailEdit
          detail={detail}
          onOrderUpdated={onOrderUpdated}
          expandedTaskId={expandedTaskId}
          onToggleTask={onToggleTask}
          onOpenSkiDetail={onOpenSkiDetail}
          loadingSkiDetail={loadingSkiDetail}
          strukturyOptions={strukturyOptions}
          modificationOptions={modificationOptions}
          t={t}
        />
      )}
    </div>
  )
}

const STATUS_OPTIONS = ['CEKA', 'PROBIHA', 'DOKONCENO', 'POZASTAVENO'] as const
const PRIORITY_OPTIONS = ['NIZKA', 'STREDNI', 'VYSOKA', 'KRITICKA'] as const
const ORDER_STATUS_OPTIONS = ['NOVE', 'VE_ZPRACOVANI', 'POZASTAVENA', 'UKONCENA', 'STORNOVANA'] as const
const ZMENA_STRUKTURY_NAZEV = 'Změna struktury' // API / backend expects this name

function OrderDetailEdit({
  detail,
  onOrderUpdated,
  expandedTaskId,
  onToggleTask,
  onOpenSkiDetail,
  loadingSkiDetail,
  strukturyOptions,
  modificationOptions,
  t,
}: {
  detail: OrderDetailResponse
  onOrderUpdated: (orderId: number, detail: OrderDetailResponse) => void
  expandedTaskId: number | null
  onToggleTask: (taskId: number) => void
  onOpenSkiDetail: (skiId: number, orderDetail?: OrderDetailResponse, skiNumber?: string) => void
  loadingSkiDetail: boolean
  strukturyOptions: StrukturaOptionResponse[]
  modificationOptions: ModificationOptionResponse[]
  t: (key: string) => string
}) {
  const [editing, setEditing] = useState(false)
  const [customers, setCustomers] = useState<CustomerSummaryResponse[]>([])
  const [skis, setSkis] = useState<{ id: number; skiNumber: string; brand: string; model: string; length: string }[]>([])
  const [customerId, setCustomerId] = useState<string>(detail.customerId != null ? String(detail.customerId) : '')
  const [dueDate, setDueDate] = useState(detail.dueDate || '')
  const [priority, setPriority] = useState(detail.priority || 'STREDNI')
  const [orderStatus, setOrderStatus] = useState(detail.status || 'NOVE')
  const [notes, setNotes] = useState(detail.notes || '')
  const [price, setPrice] = useState(detail.price != null ? String(detail.price) : '')
  const [skiIdFilter, setSkiIdFilter] = useState('')
  const [selectedSkiIdsToAdd, setSelectedSkiIdsToAdd] = useState<number[]>([])
  const [showQrAdd, setShowQrAdd] = useState(false)
  const [saving, setSaving] = useState(false)
  const [addingSki, setAddingSki] = useState(false)
  const [addSkiTargetStruktura, setAddSkiTargetStruktura] = useState('')
  const [editCustomerId, setEditCustomerId] = useState<number | null>(null)
  const [customerDetail, setCustomerDetail] = useState<CustomerDetailResponse | null>(null)
  const [customerDetailLoading, setCustomerDetailLoading] = useState(false)
  const customerDetailRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!editing) return
    let cancelled = false
    Promise.all([apiClient.getCustomers(0, 500), apiClient.getSkis(0, 500)]).then(([custRes, skiRes]) => {
      if (cancelled) return
      const custList = custRes.content
      const skiList = skiRes.content
      setCustomers(custList.slice().sort((a, b) => a.name.localeCompare(b.name)))
      setSkis(skiList.slice().sort((a, b) => a.id - b.id).map(s => ({ id: s.id, skiNumber: s.skiNumber, brand: s.brand, model: s.model, length: s.length })))
    })
    return () => { cancelled = true }
  }, [editing])

  useEffect(() => {
    if (!editing) return
    setCustomerId(detail.customerId != null ? String(detail.customerId) : '')
    setDueDate(detail.dueDate || '')
    setPriority(detail.priority || 'STREDNI')
    setOrderStatus(detail.status || 'NOVE')
    setNotes(detail.notes || '')
    setPrice(detail.price != null ? String(detail.price) : '')
  }, [editing, detail.id, detail.customerId, detail.dueDate, detail.priority, detail.status, detail.notes, detail.price])

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (customerDetailRef.current && !customerDetailRef.current.contains(e.target as Node)) {
        setCustomerDetail(null)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  const handleSave = async () => {
    setSaving(true)
    try {
      const updated = await apiClient.updateOrder(detail.id, {
        customerId: customerId === '' ? 0 : parseInt(customerId, 10),
        dueDate: dueDate || null,
        priority,
        status: orderStatus,
        notes: notes || undefined,
        price: price === '' ? null : parseFloat(price),
      })
      onOrderUpdated(detail.id, updated)
      setEditing(false)
    } catch (e) {
      console.error('Failed to update order:', e)
    } finally {
      setSaving(false)
    }
  }

  const currentSkiIds = new Set((detail.tasks || []).map(task => task.skiId).filter((id): id is number => id != null))
  const skisAvailable = skis.filter(s => !currentSkiIds.has(s.id))
  const skiFilteredForAdd = skiIdFilter.trim()
    ? skisAvailable.filter(s => String(s.id).includes(skiIdFilter.trim()))
    : skisAvailable
  const skiDisplayForAdd = skiFilteredForAdd.slice(0, 5)

  const toggleSkiToAdd = (id: number) => {
    setSelectedSkiIdsToAdd(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])
  }

  const targetStrukturaToSend = addSkiTargetStruktura.trim() || null

  const handleAddSelectedSkis = async () => {
    if (selectedSkiIdsToAdd.length === 0) return
    setAddingSki(true)
    try {
      let updated: OrderDetailResponse = detail
      for (const skiId of selectedSkiIdsToAdd) {
        updated = await apiClient.addOrderTask(detail.id, skiId, targetStrukturaToSend)
        onOrderUpdated(detail.id, updated)
      }
      setSelectedSkiIdsToAdd([])
    } catch (e) {
      console.error('Failed to add ski to order:', e)
    } finally {
      setAddingSki(false)
    }
  }

  const handleAddOneSki = async (skiId: number) => {
    setAddingSki(true)
    try {
      const updated = await apiClient.addOrderTask(detail.id, skiId, targetStrukturaToSend)
      onOrderUpdated(detail.id, updated)
      setSelectedSkiIdsToAdd(prev => prev.filter(id => id !== skiId))
    } catch (e) {
      console.error('Failed to add ski to order:', e)
    } finally {
      setAddingSki(false)
    }
  }

  const handleQrScanAdd = (result: string) => {
    const trimmed = result.trim()
    const byId = /^\d+$/.test(trimmed) ? skisAvailable.find(s => s.id === parseInt(trimmed, 10)) : null
    const byNumber = skisAvailable.find(s => s.skiNumber === trimmed)
    const ski = byId ?? byNumber
    if (ski) {
      apiClient.recordQrScan(trimmed).catch(() => {})
      handleAddOneSki(ski.id)
      setShowQrAdd(false)
    }
  }

  if (editing) {
    return (
      <div className="bg-gray-50 border-t border-gray-200 pl-14 pr-6 pb-4 space-y-4">
        <datalist id="upravy-add-item-list">
          <option value={ZMENA_STRUKTURY_NAZEV} />
          {modificationOptions.map(u => (
            <option key={u.id} value={u.name} />
          ))}
        </datalist>
        <datalist id="task-struktura-list">
          {strukturyOptions.map(s => (
            <option key={s.id} value={s.name} />
          ))}
        </datalist>
        <div className="flex items-center justify-between">
          <span className="text-sm font-medium text-gray-700">{t('orders.editOrder')}</span>
          <button type="button" onClick={() => setEditing(false)} className="px-3 py-1.5 text-sm font-medium text-red-700 bg-red-50 border border-red-200 rounded-md hover:bg-red-100 cursor-pointer">{t('common.cancel')}</button>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.customer')}</label>
          <div className="flex items-center gap-2 flex-wrap">
            <select value={customerId} onChange={e => setCustomerId(e.target.value)} className="block rounded-md border-gray-300 shadow-sm text-sm cursor-pointer">
              <option value="">{t('common.unassigned')}</option>
              {customers.map(c => (
                <option key={c.id} value={c.id}>{c.name}{c.phone ? ` (${c.phone})` : ''}</option>
              ))}
            </select>
            {customerId !== '' && (
              <button type="button" onClick={() => setEditCustomerId(parseInt(customerId, 10))} className="inline-flex items-center gap-1 px-2 py-1.5 text-sm font-medium text-blue-700 bg-blue-50 border border-blue-200 rounded-md hover:bg-blue-100 cursor-pointer">
                <Pencil className="w-4 h-4 shrink-0" /> {t('orders.editContacts')}
              </button>
            )}
          </div>
        </div>
        {editCustomerId != null && (
          <EditCustomerModal
            customerId={editCustomerId}
            onClose={() => setEditCustomerId(null)}
            onSaved={(list) => { setCustomers(list); setEditCustomerId(null) }}
          />
        )}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.dueDate')}</label>
          <input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} className="block w-full max-w-xs rounded-md border-gray-300 shadow-sm text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.priority')}</label>
          <select value={priority} onChange={e => setPriority(e.target.value)} className="block w-full max-w-xs rounded-md border-gray-300 shadow-sm text-sm cursor-pointer">
            {PRIORITY_OPTIONS.map(p => (
              <option key={p} value={p}>{t('priority.' + p)}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.orderStatus')}</label>
          <select value={orderStatus} onChange={e => setOrderStatus(e.target.value)} className="block w-full max-w-xs rounded-md border-gray-300 shadow-sm text-sm cursor-pointer">
            {ORDER_STATUS_OPTIONS.map(s => (
              <option key={s} value={s}>{t('status.' + s)}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.priceCzk')}</label>
          <input type="number" step="0.01" min="0" value={price} onChange={e => setPrice(e.target.value)} placeholder={t('common.optional')} className="block w-full max-w-xs rounded-md border-gray-300 shadow-sm text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.notes')}</label>
          <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={2} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">{t('orders.addSki')}</label>
          <div className="mb-2">
            <label className="block text-xs font-medium text-gray-600 mb-1">{t('orders.targetStructure')}</label>
            <input
              type="text"
              list="add-ski-struktury-list"
              value={addSkiTargetStruktura}
              onChange={e => setAddSkiTargetStruktura(e.target.value)}
              placeholder={t('orders.targetStructurePlaceholder')}
              className="block w-full max-w-xs rounded-md border-gray-300 shadow-sm text-sm py-1.5 px-2"
            />
            <datalist id="add-ski-struktury-list">
              {strukturyOptions.map(s => (
                <option key={s.id} value={s.name} />
              ))}
            </datalist>
          </div>
          <div className="flex flex-wrap items-center gap-2 mb-2">
            <input type="text" value={skiIdFilter} onChange={e => setSkiIdFilter(e.target.value)} placeholder={t('orders.filterById')} className="flex-1 min-w-[180px] max-w-xs rounded-md border border-gray-300 shadow-sm text-sm py-2 px-3" />
            <button type="button" onClick={() => setShowQrAdd(true)} className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-700 cursor-pointer shrink-0">
              <QrCode className="w-4 h-4 shrink-0" /> {t('orders.scanQr')}
            </button>
          </div>
          <div className="border border-gray-200 rounded-md max-h-48 overflow-y-auto divide-y divide-gray-100 mb-2">
            {skiDisplayForAdd.length === 0 ? (
              <p className="text-gray-500 text-sm px-3 py-4">{skisAvailable.length === 0 ? t('orders.allSkisInOrder') : t('orders.noSkisMatchFilter')}</p>
            ) : (
              skiDisplayForAdd.map(ski => (
                <div key={ski.id} className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={selectedSkiIdsToAdd.includes(ski.id)}
                    onChange={() => toggleSkiToAdd(ski.id)}
                    className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer"
                  />
                  <span className="font-medium text-gray-900 w-10">ID {ski.id}</span>
                  <span className="text-gray-600 text-sm flex-1">{ski.skiNumber} — {ski.brand} {ski.model} {ski.length}</span>
                  <button type="button" onClick={() => handleAddOneSki(ski.id)} disabled={addingSki} className="px-2 py-1 bg-green-600 text-white text-xs rounded hover:bg-green-700 disabled:opacity-50 shrink-0 cursor-pointer">
                    {t('common.add')}
                  </button>
                </div>
              ))
            )}
          </div>
          {skiFilteredForAdd.length > 5 && <p className="text-gray-500 text-xs mb-2">{t('orders.showingFirst5')}</p>}
          <button type="button" onClick={handleAddSelectedSkis} disabled={addingSki || selectedSkiIdsToAdd.length === 0} className="px-3 py-1.5 bg-green-600 text-white text-sm rounded-md hover:bg-green-700 disabled:opacity-50 cursor-pointer">
            {addingSki ? t('orders.adding') : `${t('orders.addSelected')}${selectedSkiIdsToAdd.length > 0 ? ` (${selectedSkiIdsToAdd.length})` : ''}`}
          </button>
        </div>
        {showQrAdd && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 p-4" onClick={() => setShowQrAdd(false)}>
            <div className="bg-white rounded-xl shadow-xl p-4 max-w-lg w-full" onClick={e => e.stopPropagation()}>
              <h3 className="text-lg font-semibold text-gray-900 mb-2">{t('orders.scanQrTitle')}</h3>
              <p className="text-sm text-gray-600 mb-3">{t('orders.scanQrDesc')}</p>
              <QRScanner onScan={handleQrScanAdd} onClose={() => setShowQrAdd(false)} />
            </div>
          </div>
        )}
        <p className="text-sm font-semibold text-gray-700">{t('orders.skisInOrder')}</p>
        <div className="space-y-2">
          {detail.tasks && detail.tasks.length > 0 ? (
            detail.tasks.map(task => (
              <TaskRow
                key={task.id}
                orderId={detail.id}
                detail={detail}
                task={task}
                isExpanded={expandedTaskId === task.id}
                onToggle={() => onToggleTask(task.id)}
                onOrderUpdated={onOrderUpdated}
                onOpenSkiDetail={onOpenSkiDetail}
                loadingSkiDetail={loadingSkiDetail}
                strukturyOptions={strukturyOptions}
                modificationOptions={modificationOptions}
                t={t}
              />
            ))
          ) : (
            <p className="text-gray-500 text-sm py-2">{t('orders.noSkisInOrder')}</p>
          )}
        </div>
        <div className="pt-2">
          <button type="button" onClick={handleSave} disabled={saving} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium cursor-pointer">
            {saving ? t('common.saving') : t('common.save')}
          </button>
        </div>
      </div>
    )
  }

  const handleCustomerNameClick = async (e: React.MouseEvent) => {
    e.stopPropagation()
    if (detail.customerId == null) return
    if (customerDetail?.id === detail.customerId) {
      setCustomerDetail(null)
      return
    }
    setCustomerDetailLoading(true)
    try {
      const c = await apiClient.getCustomer(detail.customerId)
      setCustomerDetail(c)
    } catch {
      setCustomerDetail(null)
    } finally {
      setCustomerDetailLoading(false)
    }
  }

  return (
    <div className="bg-gray-50 border-t border-gray-200 pl-14 pr-6 pb-4">
      <datalist id="upravy-add-item-list">
        <option value={ZMENA_STRUKTURY_NAZEV} />
        {modificationOptions.map(u => (
          <option key={u.id} value={u.name} />
        ))}
      </datalist>
      <datalist id="task-struktura-list">
        {strukturyOptions.map(s => (
          <option key={s.id} value={s.name} />
        ))}
      </datalist>
      <div className="pt-5 grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4 text-sm" ref={customerDetailRef}>
        <div className="relative">
          <span className="font-medium text-gray-600">{t('common.customer')}:</span>{' '}
          {detail.customerId != null && detail.customerName ? (
            <button
              type="button"
              onClick={handleCustomerNameClick}
              disabled={customerDetailLoading}
              className="text-blue-600 hover:text-blue-800 hover:underline underline-offset-2 cursor-pointer font-medium inline-flex items-center gap-1"
            >
              {detail.customerName}
              <ChevronDown className={`w-4 h-4 transition-transform ${customerDetail ? 'rotate-180' : ''}`} />
            </button>
          ) : (
            <span className="text-gray-700">{detail.customerName || t('common.notSet')}</span>
          )}
          {customerDetailLoading && (
            <span className="ml-1 inline-block">
              <Loader2 className="w-4 h-4 animate-spin text-blue-600" />
            </span>
          )}
          {customerDetail && customerDetail.id === detail.customerId && (
            <div className="absolute left-0 top-full mt-1 z-10 min-w-[220px] rounded-lg border border-gray-200 bg-white py-3 px-3 shadow-lg">
              {customerDetail.email && (
                <div className="flex items-center gap-2 text-sm text-gray-700 mb-1">
                  <Mail className="w-4 h-4 text-gray-500 shrink-0" />
                  <a href={`mailto:${customerDetail.email}`} className="text-blue-600 hover:underline break-all">
                    {customerDetail.email}
                  </a>
                </div>
              )}
              {customerDetail.phone && (
                <div className="flex items-center gap-2 text-sm text-gray-700">
                  <Phone className="w-4 h-4 text-gray-500 shrink-0" />
                  <a href={`tel:${customerDetail.phone}`} className="text-blue-600 hover:underline">
                    {customerDetail.phone}
                  </a>
                </div>
              )}
              {!customerDetail.email && !customerDetail.phone && (
                <p className="text-sm text-gray-500">{t('common.notSet')}</p>
              )}
            </div>
          )}
        </div>
        <div><span className="font-medium text-gray-600">{t('common.dueDate')}:</span> {detail.dueDate || t('common.notSet')}</div>
      </div>
      {detail.notes && <p className="text-sm text-gray-700 mb-4"><span className="font-medium text-gray-600">{t('common.notes')}:</span> {detail.notes}</p>}
      <div className="flex items-center gap-3 mb-3">
        <button type="button" onClick={() => setEditing(true)} className="inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium cursor-pointer">
          <Pencil className="w-4 h-4" /> {t('orders.editOrder')}
        </button>
      </div>
      <p className="text-sm font-semibold text-gray-700 mb-2">{t('orders.skisInOrder')}</p>
      <div className="space-y-2">
        {detail.tasks && detail.tasks.length > 0 ? (
          detail.tasks.map(task => (
            <TaskRow
              key={task.id}
              orderId={detail.id}
              detail={detail}
              task={task}
              isExpanded={expandedTaskId === task.id}
              onToggle={() => onToggleTask(task.id)}
              onOrderUpdated={onOrderUpdated}
              onOpenSkiDetail={onOpenSkiDetail}
              loadingSkiDetail={loadingSkiDetail}
              strukturyOptions={strukturyOptions}
              modificationOptions={modificationOptions}
              t={t}
            />
          ))
        ) : (
          <p className="text-gray-500 text-sm py-2">{t('orders.noSkisInOrder')}</p>
        )}
      </div>
    </div>
  )
}

function TaskRow({
  orderId,
  detail,
  task,
  isExpanded,
  onToggle,
  onOrderUpdated,
  onOpenSkiDetail,
  loadingSkiDetail,
  strukturyOptions,
  modificationOptions,
  t,
}: {
  orderId: number
  detail: OrderDetailResponse
  task: OrderTaskResponse
  isExpanded: boolean
  onToggle: () => void
  onOrderUpdated: (orderId: number, detail: OrderDetailResponse) => void
  onOpenSkiDetail: (skiId: number, orderDetail?: OrderDetailResponse, skiNumber?: string) => void
  loadingSkiDetail: boolean
  strukturyOptions: StrukturaOptionResponse[]
  modificationOptions: ModificationOptionResponse[]
  t: (key: string) => string
}) {
  const [editing, setEditing] = useState(false)
  const [status, setStatus] = useState(task.status)
  const [saving, setSaving] = useState(false)
  const [addingItem, setAddingItem] = useState(false)
  const [newItemName, setNewItemName] = useState('')
  const [newItemInstruction, setNewItemInstruction] = useState('')
  const [newItemDesc, setNewItemDesc] = useState('')
  const [addingSaving, setAddingSaving] = useState(false)

  const isZmenaStruktury = newItemName.trim() === ZMENA_STRUKTURY_NAZEV

  useEffect(() => {
    const opt = modificationOptions.find(o => o.name === newItemName.trim())
    if (opt?.description && addingItem) setNewItemInstruction(prev => prev || opt.description || '')
  }, [newItemName, modificationOptions, addingItem])

  const handleSaveTask = async () => {
    setSaving(true)
    try {
      const updated = await apiClient.updateTask(orderId, task.id, { status })
      onOrderUpdated(orderId, updated)
      setEditing(false)
    } catch (e) {
      console.error('Failed to update task:', e)
    } finally {
      setSaving(false)
    }
  }

  const syncOrderStatusFromTaskItems = async (order: OrderDetailResponse) => {
    const allCompleted = order.tasks.every(t => t.taskItems.length === 0 || t.taskItems.every(i => i.completed))
    if (allCompleted && order.status !== 'UKONCENA') {
      try {
        const withStatus = await apiClient.updateOrder(orderId, { status: 'UKONCENA' })
        onOrderUpdated(orderId, withStatus)
      } catch (e) {
        console.error('Failed to set order status', e)
      }
    } else if (!allCompleted && order.status === 'UKONCENA') {
      try {
        const withStatus = await apiClient.updateOrder(orderId, { status: 'NOVE' })
        onOrderUpdated(orderId, withStatus)
      } catch (e) {
        console.error('Failed to set order status', e)
      }
    }
  }

  const handleItemCompleted = async (itemId: number, completed: boolean, currentTaskDescription?: string | null) => {
    try {
      const payload: { completed: boolean; taskDescription?: string | null } = { completed }
      if (currentTaskDescription !== undefined) payload.taskDescription = currentTaskDescription
      const updated = await apiClient.updateTaskItem(orderId, task.id, itemId, payload)
      onOrderUpdated(orderId, updated)
      await syncOrderStatusFromTaskItems(updated)
    } catch (e) {
      console.error('Failed to update item:', e)
    }
  }

  const handleAddItem = async () => {
    if (!newItemName.trim()) return
    if (isZmenaStruktury && !newItemDesc.trim()) return
    setAddingSaving(true)
    try {
      const desc = newItemDesc.trim() || undefined
      const instruction = newItemInstruction.trim() || undefined
      const selectedOption = modificationOptions.find(o => o.name === newItemName.trim())
      const modificationOptionId = selectedOption?.id
      let updated = await apiClient.addTaskItem(orderId, task.id, newItemName.trim(), desc, instruction, modificationOptionId)
      if (isZmenaStruktury && desc) {
        updated = await apiClient.updateTask(orderId, task.id, { targetStruktura: desc })
        onOrderUpdated(orderId, updated)
      } else {
        onOrderUpdated(orderId, updated)
      }
      await syncOrderStatusFromTaskItems(updated)
      setNewItemName('')
      setNewItemInstruction('')
      setNewItemDesc('')
      setAddingItem(false)
    } catch (e) {
      console.error('Failed to add item:', e)
    } finally {
      setAddingSaving(false)
    }
  }

  const handleDeleteItem = async (itemId: number) => {
    try {
      const updated = await apiClient.deleteTaskItem(orderId, task.id, itemId)
      onOrderUpdated(orderId, updated)
      await syncOrderStatusFromTaskItems(updated)
    } catch (e) {
      console.error('Failed to delete item:', e)
    }
  }

  const handleSaveNote = async (itemId: number, taskDescription: string | null) => {
    const item = task.taskItems.find(i => i.id === itemId)
    try {
      const updated = await apiClient.updateTaskItem(orderId, task.id, itemId, {
        taskDescription,
        completed: item?.completed,
      })
      onOrderUpdated(orderId, updated)
    } catch (e) {
      console.error('Failed to update note:', e)
    }
  }

  const handleSaveInstruction = async (itemId: number, taskInstruction: string | null) => {
    const item = task.taskItems.find(i => i.id === itemId)
    try {
      const updated = await apiClient.updateTaskItem(orderId, task.id, itemId, {
        taskInstruction,
        completed: item?.completed,
      })
      onOrderUpdated(orderId, updated)
    } catch (e) {
      console.error('Failed to update instruction:', e)
    }
  }

  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
      <div className="w-full flex items-center gap-3 px-4 py-3 hover:bg-gray-50">
        <button
          onClick={onToggle}
          className="flex items-center gap-3 text-left flex-1 min-w-0 cursor-pointer"
          type="button"
        >
          {isExpanded ? (
            <ChevronDown className="w-4 h-4 text-gray-500 shrink-0" />
          ) : (
            <ChevronRight className="w-4 h-4 text-gray-500 shrink-0" />
          )}
          <Layers className="w-4 h-4 text-green-600 shrink-0" />
          <span className="font-medium text-gray-900 truncate">{task.skiNumber || task.skiInfo || `Task #${task.id}`}</span>
          <span className="text-gray-600 text-sm truncate hidden sm:inline">{task.skiInfo}</span>
          <span className={`px-2 py-0.5 text-xs font-medium rounded-full shrink-0 ${getStatusColor(task.status)}`}>
            {t('status.' + task.status) || task.status}
          </span>
          {task.assignedTo && (
            <span className="text-gray-500 text-sm flex items-center gap-1 shrink-0">
              <User className="w-4 h-4" /> {task.assignedTo}
            </span>
          )}
        </button>
        {task.skiId != null && (
          <button
            type="button"
            onClick={(e) => { e.stopPropagation(); onOpenSkiDetail(task.skiId!, detail, task.skiNumber ?? undefined) }}
            disabled={loadingSkiDetail}
            className="shrink-0 p-2 text-blue-600 hover:bg-blue-50 rounded-md transition-colors disabled:opacity-50"
            title={t('common.showSkiDetail')}
          >
            <Eye className="w-4 h-4" />
          </button>
        )}
      </div>

      {isExpanded && (
        <div className="border-t border-gray-200 bg-gray-50 px-4 py-3">
          {editing ? (
            <div className="space-y-3 mb-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.status')}</label>
                <select
                  value={status}
                  onChange={e => setStatus(e.target.value)}
                  className="block w-full rounded-md border-gray-300 shadow-sm text-sm cursor-pointer"
                >
                  {STATUS_OPTIONS.map(s => (
                    <option key={s} value={s}>{t('status.' + s)}</option>
                  ))}
                </select>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={handleSaveTask}
                  disabled={saving}
                  className="px-3 py-1.5 bg-blue-600 text-white text-sm rounded-md hover:bg-blue-700 disabled:opacity-50 cursor-pointer"
                >
                  {saving ? t('common.saving') : t('common.save')}
                </button>
                <button
                  onClick={() => { setEditing(false); setStatus(task.status) }}
                  className="px-3 py-1.5 bg-gray-200 text-gray-800 text-sm rounded-md hover:bg-gray-300 cursor-pointer"
                >
                  {t('common.cancel')}
                </button>
              </div>
            </div>
          ) : (
            <>
              <div className="mb-3 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
                {(task.skiStruktura != null && task.skiStruktura !== '') && (
                  <span className="text-gray-600">{t('orders.skiStructure')}: <strong>{task.skiStruktura}</strong></span>
                )}
                {(() => {
                  const targetStrukturaValue = task.targetStruktura ?? task.taskItems?.find(i => i.taskName === ZMENA_STRUKTURY_NAZEV)?.taskDescription ?? null
                  if (targetStrukturaValue) {
                    return <span className="text-gray-600">{t('orders.targetStructureLabel')}: <strong>{targetStrukturaValue}</strong></span>
                  }
                  return (!task.skiStruktura || task.skiStruktura === '') && <span className="text-gray-500">{t('orders.targetStructureNotSet')}</span>
                })()}
              </div>
              <button
                onClick={() => { setStatus(task.status); setEditing(true) }}
                className="text-sm text-blue-600 hover:text-blue-800 mb-3 cursor-pointer"
              >
                {t('orders.editStatus')}
              </button>
            </>
          )}
          <p className="text-sm font-medium text-gray-700 mb-2">{t('orders.serviceTasks')}</p>
          <ul className="space-y-2">
            {task.taskItems.map(item => (
              <TaskItemRow
                key={item.id}
                item={item}
                orderId={orderId}
                taskId={task.id}
                modificationOptions={modificationOptions}
                onToggleCompleted={(completed, currentTaskDescription) => handleItemCompleted(item.id, completed, currentTaskDescription)}
                onDelete={() => handleDeleteItem(item.id)}
                onSaveNote={handleSaveNote}
                onSaveInstruction={handleSaveInstruction}
                t={t}
              />
            ))}
          </ul>
          {addingItem ? (
            <div className="mt-2 space-y-2 p-2 bg-white rounded border border-gray-200">
              <input
                type="text"
                list="upravy-add-item-list"
                placeholder={t('orders.taskPlaceholder')}
                value={newItemName}
                onChange={e => setNewItemName(e.target.value)}
                className="block w-full rounded-md border-gray-300 text-sm py-1.5 px-2"
              />
              {isZmenaStruktury ? (
                <>
                  <label className="block text-xs font-medium text-gray-600">{t('orders.structureLabel')}</label>
                  <input
                    type="text"
                    list="task-struktura-list"
                    placeholder={task.skiStruktura ?? t('orders.targetStructurePlaceholder')}
                    value={newItemDesc}
                    onChange={e => setNewItemDesc(e.target.value)}
                    className="block w-full rounded-md border-gray-300 text-sm py-1.5 px-2"
                  />
                  {task.skiStruktura != null && task.skiStruktura !== '' && (
                    <p className="text-xs text-gray-500">{t('orders.currentOnSki')} {task.skiStruktura}</p>
                  )}
                </>
              ) : (
                <>
                  <label className="block text-xs font-medium text-gray-600">{t('orders.howToProcess')}</label>
                  <input
                    type="text"
                    placeholder={t('orders.howToProcessPlaceholder')}
                    value={newItemInstruction}
                    onChange={e => setNewItemInstruction(e.target.value)}
                    className="block w-full rounded-md border-gray-300 text-sm py-1.5 px-2"
                  />
                  <label className="block text-xs font-medium text-gray-600">{t('orders.resultOptional')}</label>
                  <input
                    type="text"
                    placeholder={t('orders.resultPlaceholder')}
                    value={newItemDesc}
                    onChange={e => setNewItemDesc(e.target.value)}
                    className="block w-full rounded-md border-gray-300 text-sm py-1.5 px-2"
                  />
                </>
              )}
              <div className="flex gap-2">
                <button
                  onClick={handleAddItem}
                  disabled={addingSaving || !newItemName.trim() || (isZmenaStruktury && !newItemDesc.trim())}
                  className="px-2 py-1 bg-blue-600 text-white text-sm rounded cursor-pointer hover:bg-blue-700 disabled:opacity-50"
                >
                  {t('common.add')}
                </button>
                <button onClick={() => { setAddingItem(false); setNewItemName(''); setNewItemInstruction(''); setNewItemDesc('') }} className="px-2 py-1 bg-gray-200 text-sm rounded cursor-pointer hover:bg-gray-300">{t('common.cancel')}</button>
              </div>
            </div>
          ) : (
            <button onClick={() => setAddingItem(true)} className="mt-2 text-sm text-blue-600 hover:text-blue-800 cursor-pointer">{t('orders.addTask')}</button>
          )}
          {task.taskItems.length === 0 && !addingItem && (
            <p className="text-gray-500 text-sm mt-1">{t('orders.noTasks')}</p>
          )}
        </div>
      )}
    </div>
  )
}

const NOTE_SAVE_DELAY_MS = 600

function TaskItemRow({
  item,
  modificationOptions,
  onToggleCompleted,
  onDelete,
  onSaveNote,
  onSaveInstruction,
  t,
}: {
  item: ServiceTaskItemResponse
  orderId: number
  taskId: number
  modificationOptions: ModificationOptionResponse[]
  onToggleCompleted: (completed: boolean, currentTaskDescription?: string | null) => void
  onDelete: () => void
  onSaveNote: (itemId: number, taskDescription: string | null) => Promise<void>
  onSaveInstruction: (itemId: number, taskInstruction: string | null) => Promise<void>
  t: (key: string) => string
}) {
  const matchingOption = modificationOptions.find(o => o.name === item.taskName)
  const requiresDescription = matchingOption !== undefined ? matchingOption.requiresWorkDescription : item.requiresWorkDescription
  const [note, setNote] = useState(item.taskDescription ?? '')
  const [instruction, setInstruction] = useState(item.taskInstruction ?? '')
  const [savingNote, setSavingNote] = useState(false)
  const [savingInstruction, setSavingInstruction] = useState(false)
  const [showDescriptionError, setShowDescriptionError] = useState(false)
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const lastSavedRef = useRef<string>(item.taskDescription ?? '')
  const lastSavedInstructionRef = useRef<string>(item.taskInstruction ?? '')

  useEffect(() => {
    setNote(item.taskDescription ?? '')
    lastSavedRef.current = item.taskDescription ?? ''
  }, [item.taskDescription])

  useEffect(() => {
    setInstruction(item.taskInstruction ?? '')
    lastSavedInstructionRef.current = item.taskInstruction ?? ''
  }, [item.taskInstruction])

  const flushSave = useCallback(async () => {
    if (debounceRef.current != null) {
      clearTimeout(debounceRef.current)
      debounceRef.current = null
    }
    const value = note.trim() || ''
    const toSave = value || null
    if (toSave === (lastSavedRef.current || '')) return
    setSavingNote(true)
    try {
      await onSaveNote(item.id, toSave)
    } finally {
      setSavingNote(false)
    }
  }, [note, item.id, onSaveNote])

  useEffect(() => {
    const value = note.trim() || ''
    if (value === (lastSavedRef.current || '')) return
    if (debounceRef.current != null) clearTimeout(debounceRef.current)
    debounceRef.current = setTimeout(() => {
      debounceRef.current = null
      setSavingNote(true)
      onSaveNote(item.id, value || null).finally(() => setSavingNote(false))
    }, NOTE_SAVE_DELAY_MS)
    return () => {
      if (debounceRef.current != null) clearTimeout(debounceRef.current)
    }
  }, [note, item.id, onSaveNote])

  const handleNoteBlur = () => {
    flushSave()
  }

  const handleInstructionBlur = async () => {
    const value = instruction.trim() || ''
    const toSave = value || null
    if (toSave === (lastSavedInstructionRef.current || '')) return
    setSavingInstruction(true)
    try {
      await onSaveInstruction(item.id, toSave)
      lastSavedInstructionRef.current = value || ''
    } finally {
      setSavingInstruction(false)
    }
  }

  const handleCompletedChange = (completed: boolean) => {
    if (completed && requiresDescription && !note.trim()) {
      setShowDescriptionError(true)
      return
    }
    setShowDescriptionError(false)
    onToggleCompleted(completed, completed ? (note.trim() || null) : undefined)
  }

  return (
    <li className="flex flex-wrap items-start gap-2 text-sm group py-1">
      <input
        type="checkbox"
        checked={item.completed}
        onChange={e => handleCompletedChange(e.target.checked)}
        className="mt-2 rounded border-gray-300 text-green-600 focus:ring-green-500 cursor-pointer shrink-0"
      />
      <Wrench className="w-4 h-4 text-gray-400 shrink-0 mt-2" />
      <div className="flex-1 min-w-0 space-y-1">
        <div className="flex items-center gap-2 flex-wrap">
          <span className={`font-medium ${item.completed ? 'text-gray-600 line-through' : 'text-gray-900'}`}>
            {item.taskName}
          </span>
        </div>
        <input
          type="text"
          value={instruction}
          onChange={e => setInstruction(e.target.value)}
          onBlur={handleInstructionBlur}
          placeholder={t('orders.howToProcessShort')}
          className="w-full rounded border border-gray-300 py-1 px-2 text-sm text-gray-700 placeholder:text-gray-400 focus:outline-none focus:ring-1 focus:ring-blue-500"
          disabled={savingInstruction}
        />
        <div>
          <input
            type="text"
            value={note}
            onChange={e => {
              setNote(e.target.value)
              if (e.target.value.trim()) setShowDescriptionError(false)
            }}
            onBlur={handleNoteBlur}
            placeholder={requiresDescription ? t('orders.resultRequiredShort') : t('orders.resultOptionalShort')}
            className={`w-full rounded border py-1 px-2 text-sm text-gray-700 placeholder:text-gray-400 focus:outline-none focus:ring-1 min-w-0 ${
              showDescriptionError
                ? 'border-red-500 bg-red-50 focus:ring-red-500 focus:border-red-500'
                : 'border-gray-300 focus:ring-blue-500 focus:border-blue-500'
            }`}
            disabled={savingNote}
          />
          {showDescriptionError && (
            <p className="text-xs text-red-600 mt-0.5">{t('orders.resultRequired')}</p>
          )}
        </div>
      </div>
      <button onClick={onDelete} className="self-center ml-auto text-red-600 hover:text-red-800 text-xs opacity-0 group-hover:opacity-100 cursor-pointer" title={t('common.delete')}>{t('common.delete')}</button>
    </li>
  )
}

type CreateOrderStep = 1 | 2 | 3 | 4 | 5

interface TaskItemDraft {
  taskName: string
  taskInstruction: string
  taskDescription: string
}

function CreateOrderModal({
  onClose,
  onCreated,
}: {
  onClose: () => void
  onCreated: (detail: OrderDetailResponse) => void
}) {
  const { t } = useLanguage()
  const [step, setStep] = useState<CreateOrderStep>(1)
  const [customers, setCustomers] = useState<CustomerSummaryResponse[]>([])
  const [skis, setSkis] = useState<{ id: number; skiNumber: string; brand: string; model: string; length: string; struktura: string | null }[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [customerSearch, setCustomerSearch] = useState('')
  const [customerId, setCustomerId] = useState<string>('')
  const [priority, setPriority] = useState('STREDNI')
  const [skiIdFilter, setSkiIdFilter] = useState('')
  const [selectedSkiIds, setSelectedSkiIds] = useState<number[]>([])
  const [showQrCreate, setShowQrCreate] = useState(false)
  const [dueDate, setDueDate] = useState('')
  const [price, setPrice] = useState('')
  const [itemsPerSki, setItemsPerSki] = useState<Record<number, TaskItemDraft[]>>({})
  const [targetStrukturaPerSki, setTargetStrukturaPerSki] = useState<Record<number, string>>({})
  const [notes, setNotes] = useState('')
  const [strukturyOptions, setStrukturyOptions] = useState<StrukturaOptionResponse[]>([])
  const [modificationOptions, setModificationOptions] = useState<ModificationOptionResponse[]>([])
  const [createdCustomerInThisFlow, setCreatedCustomerInThisFlow] = useState(false)
  const [newCustomerName, setNewCustomerName] = useState('')
  const [newCustomerEmail, setNewCustomerEmail] = useState('')
  const [newCustomerPhone, setNewCustomerPhone] = useState('')
  const [newCustomerAddress, setNewCustomerAddress] = useState('')
  const [creatingCustomer, setCreatingCustomer] = useState(false)
  const [editCustomerId, setEditCustomerId] = useState<number | null>(null)
  const [needAddSkisStep, setNeedAddSkisStep] = useState(false)
  const [addSkiFormKey, setAddSkiFormKey] = useState(0)
  const [creatingSki, setCreatingSki] = useState(false)

  useEffect(() => {
    let cancelled = false
    async function load() {
      setLoading(true)
      setError(null)
      try {
        const [custRes, skiRes, struktury, upravy] = await Promise.all([
          apiClient.getCustomers(0, 500),
          apiClient.getSkis(0, 500),
          apiClient.getStrukturyOptions(),
          apiClient.getModificationOptions(),
        ])
        if (!cancelled) {
          const custList = custRes.content
          const skiList = skiRes.content
          setCustomers(custList.slice().sort((a, b) => a.name.localeCompare(b.name)))
          setSkis(skiList.slice().sort((a, b) => a.id - b.id).map(s => ({ id: s.id, skiNumber: s.skiNumber, brand: s.brand, model: s.model, length: s.length, struktura: s.struktura ?? null })))
          setStrukturyOptions(struktury)
          setModificationOptions(upravy)
        }
      } catch {
        if (!cancelled) setError(t('orders.loadError'))
      } finally {
        if (!cancelled) setLoading(false)
      }
    }
    load()
    return () => { cancelled = true }
  }, [t])

  const customerFiltered = customerSearch.trim()
    ? customers.filter(c => {
        const q = customerSearch.trim().toLowerCase()
        const matchName = c.name.toLowerCase().includes(q)
        const matchPhone = c.phone != null && c.phone.includes(q)
        return matchName || matchPhone
      })
    : customers
  const skiFiltered = skiIdFilter.trim()
    ? skis.filter(s => String(s.id).includes(skiIdFilter.trim()))
    : skis

  const toggleSki = (id: number) => {
    setSelectedSkiIds(prev => prev.includes(id) ? prev.filter(x => x !== id) : [...prev, id])
  }

  const handleQrScanCreate = (result: string) => {
    const trimmed = result.trim()
    const byId = /^\d+$/.test(trimmed) ? skis.find(s => s.id === parseInt(trimmed, 10)) : null
    const byNumber = skis.find(s => s.skiNumber === trimmed)
    const ski = byId ?? byNumber
    if (ski && !selectedSkiIds.includes(ski.id)) {
      apiClient.recordQrScan(trimmed).catch(() => {})
      setSelectedSkiIds(prev => [...prev, ski.id])
      setShowQrCreate(false)
    }
  }

  const goToStep2 = () => {
    setError(null)
    const noSkis = selectedSkiIds.length === 0
    setNeedAddSkisStep(noSkis)
    if (!noSkis) {
      setItemsPerSki(prev => {
        const next = { ...prev }
        selectedSkiIds.forEach(sid => {
          if (!(sid in next)) next[sid] = []
        })
        return next
      })
      setTargetStrukturaPerSki(prev => {
        const next = { ...prev }
        selectedSkiIds.forEach(sid => {
          if (!(sid in next)) {
            const s = skis.find(x => x.id === sid)
            next[sid] = s?.struktura ?? ''
          }
        })
        return next
      })
    }
    if (customerId === '') {
      setStep(2)
    } else if (noSkis) {
      setStep(3)
    } else {
      setStep(3)
    }
  }

  const handleCreateCustomerAndNext = async () => {
    const name = newCustomerName.trim()
    const email = newCustomerEmail.trim()
    const phone = newCustomerPhone.trim()
    if (!name) {
      setError(t('orders.enterCustomerName'))
      return
    }
    if (!email) {
      setError(t('orders.enterCustomerEmail'))
      return
    }
    const emailRegex = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/
    if (!emailRegex.test(email)) {
      setError(t('orders.invalidEmail'))
      return
    }
    if (!phone) {
      setError(t('orders.enterPhone'))
      return
    }
    if (!/^\+?[\d\s\-()]+$/.test(phone)) {
      setError(t('orders.invalidPhone'))
      return
    }
    const phoneDigitsOnly = phone.replace(/\D/g, '')
    if (phoneDigitsOnly.length < 9 || phoneDigitsOnly.length > 15) {
      setError(t('orders.phoneLength'))
      return
    }
    setError(null)
    setCreatingCustomer(true)
    try {
      const created = await apiClient.createCustomer({
        name,
        email,
        phone,
        address: newCustomerAddress.trim() || null,
      })
      setCustomerId(String(created.id))
      setCreatedCustomerInThisFlow(true)
      setCustomers(prev => [...prev, created].sort((a, b) => a.name.localeCompare(b.name)))
      setStep(3)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('orders.createCustomerError'))
    } finally {
      setCreatingCustomer(false)
    }
  }

  const addItemToSki = (skiId: number) => {
    setItemsPerSki(prev => ({
      ...prev,
      [skiId]: [...(prev[skiId] || []), { taskName: '', taskInstruction: '', taskDescription: '' }],
    }))
  }

  const updateItemAtSki = (skiId: number, index: number, field: 'taskName' | 'taskInstruction' | 'taskDescription', value: string) => {
    setItemsPerSki(prev => {
      const list = [...(prev[skiId] || [])]
      if (list[index]) list[index] = { ...list[index], [field]: value }
      return { ...prev, [skiId]: list }
    })
  }

  const removeItemFromSki = (skiId: number, index: number) => {
    setItemsPerSki(prev => {
      const list = (prev[skiId] || []).filter((_, i) => i !== index)
      return { ...prev, [skiId]: list }
    })
  }

  const fillItemsAndTargetForSelectedSkis = () => {
    setItemsPerSki(prev => {
      const next = { ...prev }
      selectedSkiIds.forEach(sid => {
        if (!(sid in next)) next[sid] = []
      })
      return next
    })
    setTargetStrukturaPerSki(prev => {
      const next = { ...prev }
      selectedSkiIds.forEach(sid => {
        if (!(sid in next)) {
          const s = skis.find(x => x.id === sid)
          next[sid] = s?.struktura ?? ''
        }
      })
      return next
    })
  }

  const handleAddSkiInOrder = async (data: SkiFormData) => {
    setError(null)
    setCreatingSki(true)
    try {
      const created = await apiClient.createSki({
        brand: data.brand,
        model: data.model,
        length: data.length,
        year: data.year,
        condition: data.condition,
        status: data.status,
        location: data.location,
        notes: data.notes,
        nextServiceDate: data.nextServiceDate,
      })
      const newSki = { id: created.id, skiNumber: created.skiNumber, brand: created.brand, model: created.model, length: created.length, struktura: created.struktura ?? null }
      setSkis(prev => [...prev, newSki].sort((a, b) => a.id - b.id))
      setSelectedSkiIds(prev => [...prev, created.id])
      setItemsPerSki(prev => ({ ...prev, [created.id]: [] }))
      setTargetStrukturaPerSki(prev => ({ ...prev, [created.id]: created.struktura ?? '' }))
      setAddSkiFormKey(k => k + 1)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('orders.createSkiError'))
    } finally {
      setCreatingSki(false)
    }
  }

  const goFromAddSkisToTasks = () => {
    if (selectedSkiIds.length === 0) {
      setError(t('orders.selectAtLeastOneSki'))
      return
    }
    setError(null)
    fillItemsAndTargetForSelectedSkis()
    setStep(4)
  }

  const handleSubmit = async () => {
    if (selectedSkiIds.length === 0) {
      setError(t('orders.selectAtLeastOneSki'))
      return
    }
    setSaving(true)
    setError(null)
    try {
      const payload: CreateOrderRequest = {
        customerId: customerId === '' ? null : parseInt(customerId, 10),
        dueDate: dueDate || null,
        priority,
        notes: notes || null,
        price: price === '' ? null : parseFloat(price),
        skiIds: selectedSkiIds,
        targetStruktura: selectedSkiIds.map(sid => targetStrukturaPerSki[sid] ?? null),
      }
      const created = await apiClient.createOrder(payload)
      for (const task of created.tasks) {
        const skiId = task.skiId
        if (skiId == null) continue
        const items = itemsPerSki[skiId] || []
        for (const it of items) {
          if (!it.taskName.trim()) continue
          const modificationOptionId = modificationOptions.find(o => o.name === it.taskName.trim())?.id
          await apiClient.addTaskItem(created.id, task.id, it.taskName.trim(), it.taskDescription.trim() || undefined, it.taskInstruction.trim() || undefined, modificationOptionId)
        }
      }
      const refreshed = await apiClient.getOrder(created.id)
      onCreated(refreshed)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('orders.createOrderError'))
    } finally {
      setSaving(false)
    }
  }

  const selectedSkis = selectedSkiIds.map(id => skis.find(s => s.id === id)).filter(Boolean) as { id: number; skiNumber: string; brand: string; model: string; length: string; struktura: string | null }[]

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 cursor-pointer" onClick={onClose}>
      <div className={`bg-white rounded-xl shadow-xl w-full max-h-[90vh] overflow-y-auto cursor-default ${step === 3 || step === 4 || step === 5 ? 'max-w-4xl' : 'max-w-lg'}`} onClick={e => e.stopPropagation()}>
        <div className="p-6 border-b border-gray-200 flex items-center justify-between">
          <h2 className="text-xl font-semibold text-gray-900">{t('orders.createOrderTitle')} {step}/{needAddSkisStep ? 5 : 4}</h2>
          <button type="button" onClick={onClose} className="text-gray-500 hover:text-gray-700 p-1 cursor-pointer">×</button>
        </div>
        <div className="p-6 space-y-4">
          {loading ? (
            <div className="flex items-center justify-center py-8">
              <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
            </div>
          ) : (
            <>
              {error && (
                <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
              )}

              {step === 1 && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.stepCustomer')}</p>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">{t('orders.customerSearch')}</label>
                    <input type="text" value={customerSearch} onChange={e => setCustomerSearch(e.target.value)} placeholder={t('orders.searchPlaceholderShort')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm mb-1" />
                    <div className="border border-gray-200 rounded-md max-h-36 overflow-y-auto divide-y divide-gray-100">
                      <label className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 cursor-pointer">
                        <input type="radio" name="customer" checked={customerId === ''} onChange={() => setCustomerId('')} className="rounded-full border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer" />
                        <span className="text-gray-500 text-sm">{t('common.unassigned')}</span>
                      </label>
                      {customerFiltered.slice(0, 5).map(c => (
                        <label key={c.id} className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 cursor-pointer">
                          <input type="radio" name="customer" checked={customerId === String(c.id)} onChange={() => setCustomerId(String(c.id))} className="rounded-full border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer" />
                          <span className="font-medium text-gray-900 text-sm">{c.name}</span>
                          {c.phone && <span className="text-gray-500 text-sm">{c.phone}</span>}
                        </label>
                      ))}
                    </div>
                    {customerFiltered.length > 5 && <p className="text-gray-500 text-xs mt-1">{t('orders.showingFirst5')}</p>}
                    {customerId !== '' && (
                      <button type="button" onClick={() => setEditCustomerId(parseInt(customerId, 10))} className="mt-2 inline-flex items-center gap-1 px-2 py-1.5 text-sm font-medium text-blue-700 bg-blue-50 border border-blue-200 rounded-md hover:bg-blue-100 cursor-pointer">
                        <Pencil className="w-4 h-4 shrink-0" /> {t('orders.editCustomerContactsBtn')}
                      </button>
                    )}
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.priority')}</label>
                    <select value={priority} onChange={e => setPriority(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm cursor-pointer">
                      {PRIORITY_OPTIONS.map(p => (
                        <option key={p} value={p}>{t('priority.' + p)}</option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">{t('orders.skisInOrderOptional')}</label>
                    <div className="flex flex-wrap items-center gap-2 mb-2">
                      <input type="text" value={skiIdFilter} onChange={e => setSkiIdFilter(e.target.value)} placeholder={t('orders.filterById')} className="flex-1 min-w-[180px] rounded-md border border-gray-300 shadow-sm text-sm py-2 px-3" />
                      <button type="button" onClick={() => setShowQrCreate(true)} className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium bg-blue-600 text-white rounded-lg hover:bg-blue-700 cursor-pointer shrink-0">
                        <QrCode className="w-4 h-4 shrink-0" /> {t('orders.scanQr')}
                      </button>
                    </div>
                    <div className="border border-gray-200 rounded-md max-h-40 overflow-y-auto divide-y divide-gray-100">
                      {skiFiltered.map(ski => (
                        <label key={ski.id} className="flex items-center gap-3 px-3 py-2 hover:bg-gray-50 cursor-pointer">
                          <input type="checkbox" checked={selectedSkiIds.includes(ski.id)} onChange={() => toggleSki(ski.id)} className="rounded border-gray-300 text-blue-600 focus:ring-blue-500 cursor-pointer" />
                          <span className="font-medium text-gray-900 w-10">ID {ski.id}</span>
                          <span className="text-gray-600 text-sm">{ski.skiNumber} — {ski.brand} {ski.model} {ski.length}</span>
                        </label>
                      ))}
                    </div>
                    {skis.length === 0 && <p className="text-gray-500 text-sm py-2">{t('orders.noSkisInSystem')}</p>}
                  </div>
                  {showQrCreate && (
                    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/60 p-4" onClick={() => setShowQrCreate(false)}>
                      <div className="bg-white rounded-xl shadow-xl p-4 max-w-lg w-full" onClick={e => e.stopPropagation()}>
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">{t('orders.scanQrTitle')}</h3>
                        <p className="text-sm text-gray-600 mb-3">{t('orders.skisAddedToSelection')}</p>
                        <QRScanner onScan={handleQrScanCreate} onClose={() => setShowQrCreate(false)} />
                      </div>
                    </div>
                  )}
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={goToStep2} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium cursor-pointer">
                      {t('common.next')}
                    </button>
                    <button type="button" onClick={onClose} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.cancel')}
                    </button>
                  </div>
                </>
              )}

              {step === 2 && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.newCustomerStep')}</p>
                  <div className="space-y-3">
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.name')} *</label>
                      <input type="text" value={newCustomerName} onChange={e => setNewCustomerName(e.target.value)} placeholder={t('orders.namePlaceholder')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.email')} *</label>
                      <input type="email" value={newCustomerEmail} onChange={e => setNewCustomerEmail(e.target.value)} placeholder={t('orders.emailPlaceholder')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.phone')} *</label>
                      <input type="tel" value={newCustomerPhone} onChange={e => setNewCustomerPhone(e.target.value)} placeholder={t('orders.phonePlaceholder')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-1">{t('orders.addressOptional')}</label>
                      <input type="text" value={newCustomerAddress} onChange={e => setNewCustomerAddress(e.target.value)} placeholder={t('orders.addressPlaceholderShort')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                    </div>
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={handleCreateCustomerAndNext} disabled={creatingCustomer} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium cursor-pointer">
                      {creatingCustomer ? t('orders.creating') : t('common.next')}
                    </button>
                    <button type="button" onClick={() => { setError(null); setStep(1); }} disabled={creatingCustomer} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.back')}
                    </button>
                  </div>
                </>
              )}

              {step === 3 && needAddSkisStep && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.addSkiStepIntro')}</p>
                  {selectedSkis.length > 0 && (
                    <div className="mb-4 p-3 bg-gray-50 rounded-lg border border-gray-200">
                      <p className="text-sm font-medium text-gray-700 mb-2">{t('orders.addedSkisToOrder')}</p>
                      <ul className="text-sm text-gray-600 space-y-1">
                        {selectedSkis.map(s => (
                          <li key={s.id}>ID {s.id} — {s.skiNumber} {s.brand} {s.model} {s.length}</li>
                        ))}
                      </ul>
                    </div>
                  )}
                  <div className="border border-gray-200 rounded-lg p-4 bg-white mb-4">
                    <h3 className="text-sm font-medium text-gray-900 mb-3">{t('orders.createSkiInOrder')}</h3>
                    <SkiEditForm
                      key={addSkiFormKey}
                      onSubmit={handleAddSkiInOrder}
                      onCancel={() => {}}
                    />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={goFromAddSkisToTasks} disabled={selectedSkiIds.length === 0 || creatingSki} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium cursor-pointer">
                      {t('orders.continueToTasks')}
                    </button>
                    <button type="button" onClick={() => setStep(createdCustomerInThisFlow ? 2 : 1)} disabled={creatingSki} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.back')}
                    </button>
                  </div>
                </>
              )}

              {(step === 3 && !needAddSkisStep) && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.step3Intro')}</p>
                  <div className="space-y-2 mb-4">
                    <label className="block text-sm font-medium text-gray-700">{t('orders.estimatedDue')}</label>
                    <input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                  </div>
                  <div className="space-y-4">
                    {selectedSkis.map(ski => (
                      <div key={ski.id} className="border border-gray-200 rounded-lg p-3 bg-gray-50">
                        <p className="font-medium text-gray-900 text-sm mb-2">ID {ski.id} — {ski.skiNumber} {ski.brand} {ski.model} {ski.length}</p>
                        <div className="mb-3">
                          <label className="block text-xs font-medium text-gray-700 mb-1">{t('orders.targetStructureSki')}</label>
                          <input
                            type="text"
                            list={`struktury-create-${ski.id}`}
                            value={targetStrukturaPerSki[ski.id] ?? ''}
                            onChange={e => setTargetStrukturaPerSki(prev => ({ ...prev, [ski.id]: e.target.value }))}
                            placeholder={ski.struktura ?? t('orders.targetStructurePlaceholder')}
                            className="block w-full rounded-md border-gray-300 shadow-sm text-sm py-1.5 px-2"
                          />
                          <datalist id={`struktury-create-${ski.id}`}>
                            {strukturyOptions.map(s => (
                              <option key={s.id} value={s.name} />
                            ))}
                          </datalist>
                          {ski.struktura != null && ski.struktura !== '' && (
                            <p className="text-xs text-gray-500 mt-0.5">{t('orders.currentOnSki')} {ski.struktura}</p>
                          )}
                        </div>
                        <p className="text-xs text-gray-600 mb-2">{t('orders.tasksOnSki')}</p>
                        {(itemsPerSki[ski.id] || []).map((item, idx) => (
                          <div key={idx} className="space-y-1 mb-3 p-2 bg-white rounded border border-gray-200">
                            <div className="flex gap-2 items-center">
                              <input
                                type="text"
                                list="upravy-create-list"
                                value={item.taskName}
                                onChange={e => updateItemAtSki(ski.id, idx, 'taskName', e.target.value)}
                                placeholder={t('orders.taskNamePlaceholder')}
                                className="min-w-0 flex-1 rounded-md border-gray-300 shadow-sm text-sm py-1.5 px-2"
                              />
                              <button type="button" onClick={() => removeItemFromSki(ski.id, idx)} className="shrink-0 px-2 py-1.5 text-red-600 hover:text-red-800 hover:bg-red-50 text-sm rounded cursor-pointer">{t('common.delete')}</button>
                            </div>
                            <input type="text" value={item.taskInstruction} onChange={e => updateItemAtSki(ski.id, idx, 'taskInstruction', e.target.value)} placeholder={t('orders.howToProcessShort')} className="w-full rounded-md border-gray-300 shadow-sm text-sm py-1 px-2" />
                            <input type="text" value={item.taskDescription} onChange={e => updateItemAtSki(ski.id, idx, 'taskDescription', e.target.value)} placeholder={t('orders.resultOptionalShort')} className="w-full rounded-md border-gray-300 shadow-sm text-sm py-1 px-2" />
                          </div>
                        ))}
                        <button type="button" onClick={() => addItemToSki(ski.id)} className="text-sm text-blue-600 hover:text-blue-800 cursor-pointer">
                          {t('orders.addTask')}
                        </button>
                      </div>
                    ))}
                  </div>
                  <datalist id="upravy-create-list">
                    {modificationOptions.map(u => (
                      <option key={u.id} value={u.name} />
                    ))}
                  </datalist>
                  <div className="mt-4 space-y-2">
                    <label className="block text-sm font-medium text-gray-700">{t('orders.orderPrice')}</label>
                    <input type="number" step="0.01" min="0" value={price} onChange={e => setPrice(e.target.value)} placeholder={t('common.optional')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={() => setStep(4)} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium cursor-pointer">
                      {t('common.next')}
                    </button>
                    <button type="button" onClick={() => setStep(createdCustomerInThisFlow ? 2 : 1)} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.back')}
                    </button>
                  </div>
                </>
              )}

              {step === 4 && needAddSkisStep && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.step3Intro')}</p>
                  <div className="space-y-2 mb-4">
                    <label className="block text-sm font-medium text-gray-700">{t('orders.estimatedDue')}</label>
                    <input type="date" value={dueDate} onChange={e => setDueDate(e.target.value)} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                  </div>
                  <div className="space-y-4">
                    {selectedSkis.map(ski => (
                      <div key={ski.id} className="border border-gray-200 rounded-lg p-3 bg-gray-50">
                        <p className="font-medium text-gray-900 text-sm mb-2">ID {ski.id} — {ski.skiNumber} {ski.brand} {ski.model} {ski.length}</p>
                        <div className="mb-3">
                          <label className="block text-xs font-medium text-gray-700 mb-1">{t('orders.targetStructureSki')}</label>
                          <input
                            type="text"
                            list={`struktury-create-4-${ski.id}`}
                            value={targetStrukturaPerSki[ski.id] ?? ''}
                            onChange={e => setTargetStrukturaPerSki(prev => ({ ...prev, [ski.id]: e.target.value }))}
                            placeholder={ski.struktura ?? t('orders.targetStructurePlaceholder')}
                            className="block w-full rounded-md border-gray-300 shadow-sm text-sm py-1.5 px-2"
                          />
                          <datalist id={`struktury-create-4-${ski.id}`}>
                            {strukturyOptions.map(s => (
                              <option key={s.id} value={s.name} />
                            ))}
                          </datalist>
                          {ski.struktura != null && ski.struktura !== '' && (
                            <p className="text-xs text-gray-500 mt-0.5">{t('orders.currentOnSki')} {ski.struktura}</p>
                          )}
                        </div>
                        <p className="text-xs text-gray-600 mb-2">{t('orders.tasksOnSki')}</p>
                        {(itemsPerSki[ski.id] || []).map((item, idx) => (
                          <div key={idx} className="space-y-1 mb-3 p-2 bg-white rounded border border-gray-200">
                            <div className="flex gap-2 items-center">
                              <input
                                type="text"
                                list="upravy-create-list-4"
                                value={item.taskName}
                                onChange={e => updateItemAtSki(ski.id, idx, 'taskName', e.target.value)}
                                placeholder={t('orders.taskNamePlaceholder')}
                                className="min-w-0 flex-1 rounded-md border-gray-300 shadow-sm text-sm py-1.5 px-2"
                              />
                              <button type="button" onClick={() => removeItemFromSki(ski.id, idx)} className="shrink-0 px-2 py-1.5 text-red-600 hover:text-red-800 hover:bg-red-50 text-sm rounded cursor-pointer">{t('common.delete')}</button>
                            </div>
                            <input type="text" value={item.taskInstruction} onChange={e => updateItemAtSki(ski.id, idx, 'taskInstruction', e.target.value)} placeholder={t('orders.howToProcessShort')} className="w-full rounded-md border-gray-300 shadow-sm text-sm py-1 px-2" />
                            <input type="text" value={item.taskDescription} onChange={e => updateItemAtSki(ski.id, idx, 'taskDescription', e.target.value)} placeholder={t('orders.resultOptionalShort')} className="w-full rounded-md border-gray-300 shadow-sm text-sm py-1 px-2" />
                          </div>
                        ))}
                        <button type="button" onClick={() => addItemToSki(ski.id)} className="text-sm text-blue-600 hover:text-blue-800 cursor-pointer">
                          {t('orders.addTask')}
                        </button>
                      </div>
                    ))}
                  </div>
                  <datalist id="upravy-create-list-4">
                    {modificationOptions.map(u => (
                      <option key={u.id} value={u.name} />
                    ))}
                  </datalist>
                  <div className="mt-4 space-y-2">
                    <label className="block text-sm font-medium text-gray-700">{t('orders.orderPrice')}</label>
                    <input type="number" step="0.01" min="0" value={price} onChange={e => setPrice(e.target.value)} placeholder={t('common.optional')} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={() => setStep(5)} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 font-medium cursor-pointer">
                      {t('common.next')}
                    </button>
                    <button type="button" onClick={() => setStep(3)} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.back')}
                    </button>
                  </div>
                </>
              )}

              {(step === 4 && !needAddSkisStep) && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.step4Intro')}</p>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.notes')}</label>
                    <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={4} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" placeholder={t('common.optional')} />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={handleSubmit} disabled={saving} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium cursor-pointer">
                      {saving ? t('orders.creating') : t('orders.createOrder')}
                    </button>
                    <button type="button" onClick={() => setStep(3)} disabled={saving} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.back')}
                    </button>
                  </div>
                </>
              )}

              {step === 5 && (
                <>
                  <p className="text-sm text-gray-600">{t('orders.step4Intro')}</p>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">{t('common.notes')}</label>
                    <textarea value={notes} onChange={e => setNotes(e.target.value)} rows={4} className="block w-full rounded-md border-gray-300 shadow-sm text-sm" placeholder={t('common.optional')} />
                  </div>
                  <div className="flex gap-3 pt-2">
                    <button type="button" onClick={handleSubmit} disabled={saving} className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 font-medium cursor-pointer">
                      {saving ? t('orders.creating') : t('orders.createOrder')}
                    </button>
                    <button type="button" onClick={() => setStep(4)} disabled={saving} className="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300 font-medium cursor-pointer">
                      {t('common.back')}
                    </button>
                  </div>
                </>
              )}
            </>
          )}
        </div>
        {editCustomerId != null && (
          <EditCustomerModal
            customerId={editCustomerId}
            onClose={() => setEditCustomerId(null)}
            onSaved={(list) => { setCustomers(list); setEditCustomerId(null) }}
          />
        )}
      </div>
    </div>
  )
}
