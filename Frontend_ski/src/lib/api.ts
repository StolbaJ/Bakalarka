const getApiBaseUrl = (): string => {
  const envUrl = process.env.NEXT_PUBLIC_API_URL
  // Explicitně nastavená URL (např. http://localhost:8080 nebo EC2)
  if (envUrl) return envUrl
  // Prázdné nebo nenastaveno = same origin (Next.js rewrites / proxy nebo nginx proxy /api na backend)
  return ''
}

const API_BASE_URL = getApiBaseUrl()

if (typeof window !== 'undefined') {
  console.log('API_BASE_URL resolved to:', API_BASE_URL)
}

export interface LoginRequest {
  username: string
  password: string
}

export interface CustomerLoginRequest {
  orderNumber: string
  phone: string
}

export interface AuthResponse {
  token: string
  userId: number
  username: string
  role: string
  fullName: string | null
  email: string | null
}

export interface CredentialsHintLogin {
  label: string
  username: string
  password: string
}

export type CredentialsHintResponse =
  | { showHint: false }
  | { showHint: true; logins: CredentialsHintLogin[] }
  | { showHint: true; username: string; password: string }

class ApiClient {
  private baseUrl: string
  /** Volá se při 401 – umožňuje AuthContext odhlásit uživatele bez častého dotazování serveru */
  private onUnauthorized: (() => void) | null = null

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || API_BASE_URL
    console.log('ApiClient initialized with baseUrl:', this.baseUrl)
  }

  setOnUnauthorized(callback: (() => void) | null): void {
    this.onUnauthorized = callback
  }

  /** Zpětná kompatibilita: starý BE vrací pole, nový PageResponse. Vždy vrátíme PageResponse. */
  private normalizePageResponse<T>(data: PageResponse<T> | T[]): PageResponse<T> {
    if (Array.isArray(data)) {
      return {
        content: data,
        totalElements: data.length,
        totalPages: data.length === 0 ? 0 : 1,
        size: data.length,
        number: 0,
        first: true,
        last: true,
      }
    }
    return data
  }

  private async request<T>(
    endpoint: string,
    options: RequestInit = {}
  ): Promise<T> {
    const url = `${this.baseUrl}${endpoint}`
    console.log('API Request:', url, options.method || 'GET')
    
    const config: RequestInit = {
      headers: {
        'Content-Type': 'application/json',
        ...options.headers,
      },
      ...options,
    }

    // Přidat token, pokud existuje
    const token = this.getToken()
    if (token) {
      config.headers = {
        ...config.headers,
        Authorization: `Bearer ${token}`,
      }
    }

    try {
      const response = await fetch(url, config)
      console.log('API Response status:', response.status, response.statusText)
      
      if (!response.ok) {
        if (response.status === 401) {
          this.onUnauthorized?.()
        }
        let errorMessage = `HTTP error! status: ${response.status}`
        let retryAfter: number | undefined
        try {
          const text = await response.text()
          if (text) {
            try {
              const error = JSON.parse(text)
              errorMessage = error.message || errorMessage
              if (response.status === 429 && typeof error.retryAfter === 'number') {
                retryAfter = error.retryAfter
              }
            } catch {
              errorMessage = text || errorMessage
            }
          }
          if (response.status === 429 && retryAfter === undefined) {
            const header = response.headers.get('Retry-After')
            if (header) {
              const parsed = parseInt(header, 10)
              if (!Number.isNaN(parsed)) retryAfter = parsed
            }
          }
          console.error('API Error:', errorMessage)
        } catch {
          // ignore
        }
        const err = new Error(errorMessage) as Error & { status?: number; retryAfter?: number }
        err.status = response.status
        if (retryAfter !== undefined) err.retryAfter = retryAfter
        throw err
      }

      // 204 No Content nebo prázdné body
      const contentType = response.headers.get('content-type')
      const text = await response.text()
      if (response.status === 204 || !text) {
        return {} as T
      }
      if (contentType && contentType.includes('application/json')) {
        const data = JSON.parse(text)
        console.log('API Response data:', data)
        return data
      }
      return {} as T
    } catch (error) {
      console.error('API request failed:', error)
      if (error instanceof Error && error.message) {
        console.error('Error message:', error.message)
      }
      throw error
    }
  }

  private getToken(): string | null {
    if (typeof window === 'undefined') return null
    const user = localStorage.getItem('user')
    if (user) {
      try {
        const parsed = JSON.parse(user)
        return parsed.token || null
      } catch {
        return null
      }
    }
    return null
  }

  /**
   * URL pro vstup do Swagger UI (pouze pro admina).
   * Otevřít v novém okně; backend ověří token a přesměruje na Swagger.
   */
  getSwaggerEntryUrl(token: string): string {
    const base = this.baseUrl || (typeof window !== 'undefined' ? window.location.origin : '')
    return `${base}/api/swagger-entry?token=${encodeURIComponent(token)}`
  }

  // Auth endpoints
  async login(username: string, password: string): Promise<AuthResponse> {
    console.log('ApiClient.login called with username:', username)
    const endpoint = '/api/auth/login'
    const body = JSON.stringify({ username, password })
    console.log('Calling login endpoint:', endpoint, 'with body:', body)
    return this.request<AuthResponse>(endpoint, {
      method: 'POST',
      body: body,
    })
  }

  async loginCustomer(orderNumber: string, phone: string): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/login/customer', {
      method: 'POST',
      body: JSON.stringify({ orderNumber, phone }),
    })
  }

  /**
   * Ověří platnost aktuálního tokenu u serveru (volá POST /api/auth/refresh).
   * Při reloadu stačí jedno volání; při 401 token už není platný (expirovaný, změna hesla, deaktivace).
   */
  async validateSession(): Promise<AuthResponse> {
    return this.request<AuthResponse>('/api/auth/refresh', { method: 'POST' })
  }

  async changePassword(currentPassword: string, newPassword: string): Promise<void> {
    return this.request<void>('/api/auth/change-password', {
      method: 'PATCH',
      body: JSON.stringify({ currentPassword, newPassword }),
    })
  }

  async updateProfile(data: { fullName?: string | null; email?: string | null }): Promise<void> {
    return this.request<void>('/api/auth/profile', {
      method: 'PATCH',
      body: JSON.stringify({
        fullName: data.fullName ?? null,
        email: data.email ?? null,
      }),
    })
  }

  // Health check
  async healthCheck(): Promise<{ status: string }> {
    return this.request<{ status: string }>('/actuator/health')
  }

  /** Nápověda k výchozím přihlašovacím údajům – jen v dev (na prod showHint: false) */
  async getCredentialsHint(): Promise<CredentialsHintResponse> {
    return this.request<CredentialsHintResponse>('/api/public/credentials-hint')
  }

  // User management endpoints
  async getUsers(): Promise<UserResponse[]> {
    return this.request<UserResponse[]>('/api/admin/users')
  }

  async createUser(user: CreateUserRequest): Promise<UserResponse> {
    return this.request<UserResponse>('/api/admin/users', {
      method: 'POST',
      body: JSON.stringify(user),
    })
  }

  async updateUserRole(userId: number, role: string): Promise<UserResponse> {
    return this.request<UserResponse>(`/api/admin/users/${userId}/role`, {
      method: 'PATCH',
      body: JSON.stringify({ role }),
    })
  }

  async resetUserPassword(userId: number): Promise<{ newPassword: string | null }> {
    return this.request<{ newPassword: string | null }>(`/api/admin/users/${userId}/reset-password`, {
      method: 'POST',
    })
  }

  async deactivateUser(userId: number): Promise<UserResponse> {
    return this.request<UserResponse>(`/api/admin/users/${userId}/deactivate`, {
      method: 'PATCH',
    })
  }

  async reactivateUser(userId: number): Promise<UserResponse> {
    return this.request<UserResponse>(`/api/admin/users/${userId}/reactivate`, {
      method: 'PATCH',
    })
  }

  async getAuditLog(page = 0, size = 50, userId?: number): Promise<AuditLogPage> {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (userId != null) params.set('userId', String(userId))
    return this.request<AuditLogPage>(`/api/admin/users/audit-log?${params}`)
  }

  async getStatistics(period?: string): Promise<StatisticsResponse> {
    const params = period ? `?period=${encodeURIComponent(period)}` : ''
    return this.request<StatisticsResponse>(`/api/admin/statistics${params}`)
  }

  async getMonitoringOverview(): Promise<MonitoringOverviewResponse> {
    return this.request<MonitoringOverviewResponse>('/api/admin/monitoring/overview')
  }

  // Ski endpoints
  async getSkis(page = 0, size = 20): Promise<PageResponse<SkiResponse>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    const data = await this.request<PageResponse<SkiResponse> | SkiResponse[]>(`/api/technician/skis?${params}`)
    return this.normalizePageResponse(data)
  }

  async getSki(id: number): Promise<SkiResponse> {
    return this.request<SkiResponse>(`/api/technician/skis/${id}`)
  }

  async getSkiByNumber(skiNumber: string): Promise<SkiResponse> {
    return this.request<SkiResponse>(`/api/technician/skis/by-number/${encodeURIComponent(skiNumber)}`)
  }

  async getSkiServiceHistory(skiId: number): Promise<SkiServiceHistoryEntry[]> {
    return this.request<SkiServiceHistoryEntry[]>(`/api/technician/skis/${skiId}/service-history`)
  }

  async createSki(ski: CreateSkiRequest): Promise<SkiResponse> {
    return this.request<SkiResponse>('/api/technician/skis', {
      method: 'POST',
      body: JSON.stringify(ski),
    })
  }

  async updateSki(id: number, ski: UpdateSkiRequest): Promise<SkiResponse> {
    return this.request<SkiResponse>(`/api/technician/skis/${id}`, {
      method: 'PUT',
      body: JSON.stringify(ski),
    })
  }

  async deleteSki(id: number): Promise<void> {
    return this.request<void>(`/api/technician/skis/${id}`, {
      method: 'DELETE',
    })
  }

  // Orders endpoints (Admin, Technician)
  async getOrders(page = 0, size = 20, search?: string): Promise<PageResponse<OrderSummaryResponse>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    if (search != null && search.trim() !== '') params.set('search', search.trim())
    const data = await this.request<PageResponse<OrderSummaryResponse> | OrderSummaryResponse[]>(`/api/technician/orders?${params}`)
    return this.normalizePageResponse(data)
  }

  async getOrder(id: number): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>(`/api/technician/orders/${id}`)
  }

  async getOrderByNumber(orderNumber: string): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>(`/api/technician/orders/by-number/${encodeURIComponent(orderNumber)}`)
  }

  async getOrderBySkiNumber(skiNumber: string): Promise<OrderDetailResponse | null> {
    try {
      return await this.request<OrderDetailResponse>(`/api/technician/orders/by-ski/${encodeURIComponent(skiNumber)}`)
    } catch {
      return null
    }
  }

  async createOrder(data: CreateOrderRequest): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>('/api/technician/orders', {
      method: 'POST',
      body: JSON.stringify(data),
    })
  }

  async updateOrder(orderId: number, data: { notes?: string; priority?: string; status?: string; price?: number | null; discount?: number | null; customerId?: number | null; dueDate?: string | null }): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>(`/api/technician/orders/${orderId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  }

  async addOrderTask(orderId: number, skiId: number, targetStruktura?: string | null): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>(`/api/technician/orders/${orderId}/tasks`, {
      method: 'POST',
      body: JSON.stringify({ skiId, targetStruktura: targetStruktura ?? null }),
    })
  }

  async getCustomers(page = 0, size = 20): Promise<PageResponse<CustomerSummaryResponse>> {
    const params = new URLSearchParams({ page: String(page), size: String(size) })
    const data = await this.request<PageResponse<CustomerSummaryResponse> | CustomerSummaryResponse[]>(`/api/technician/customers?${params}`)
    return this.normalizePageResponse(data)
  }

  async getCustomer(id: number): Promise<CustomerDetailResponse> {
    return this.request<CustomerDetailResponse>(`/api/technician/customers/${id}`)
  }

  async createCustomer(data: { name: string; email: string; phone: string; address?: string | null }): Promise<CustomerSummaryResponse> {
    return this.request<CustomerSummaryResponse>('/api/technician/customers', {
      method: 'POST',
      body: JSON.stringify({
        name: data.name.trim(),
        email: data.email.trim(),
        phone: data.phone.trim(),
        address: data.address != null && data.address.trim() !== '' ? data.address.trim() : null,
      }),
    })
  }

  async updateCustomer(
    id: number,
    data: { name: string; email: string; phone: string; address?: string | null }
  ): Promise<CustomerSummaryResponse> {
    return this.request<CustomerSummaryResponse>(`/api/technician/customers/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: data.name.trim(),
        email: data.email.trim(),
        phone: data.phone.trim(),
        address: data.address != null && data.address.trim() !== '' ? data.address.trim() : null,
      }),
    })
  }

  async updateTask(orderId: number, taskId: number, data: { status?: string; targetStruktura?: string }): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>(`/api/technician/orders/${orderId}/tasks/${taskId}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  }

  async addTaskItem(orderId: number, taskId: number, taskName: string, taskDescription?: string, taskInstruction?: string, modificationOptionId?: number, price?: number | null): Promise<OrderDetailResponse> {
    const body: { taskName: string; taskDescription: string | null; taskInstruction: string | null; modificationOptionId?: number; price?: number | null } = {
      taskName,
      taskDescription: taskDescription || null,
      taskInstruction: taskInstruction || null,
    }
    if (modificationOptionId != null) body.modificationOptionId = modificationOptionId
    if (price != null) body.price = price
    return this.request<OrderDetailResponse>(`/api/technician/orders/${orderId}/tasks/${taskId}/items`, {
      method: 'POST',
      body: JSON.stringify(body),
    })
  }

  async deleteTaskItem(orderId: number, taskId: number, itemId: number): Promise<OrderDetailResponse> {
    return this.request<OrderDetailResponse>(`/api/technician/orders/${orderId}/tasks/${taskId}/items/${itemId}`, {
      method: 'DELETE',
    })
  }

  async updateTaskItem(
    orderId: number,
    taskId: number,
    itemId: number,
    data: { completed?: boolean; taskDescription?: string | null; taskInstruction?: string | null; price?: number | null }
  ): Promise<OrderDetailResponse> {
    const body: { completed?: boolean; taskDescription?: string | null; taskInstruction?: string | null; price?: number | null } = {}
    if (data.completed !== undefined) body.completed = data.completed
    if (data.taskDescription !== undefined) body.taskDescription = data.taskDescription
    if (data.taskInstruction !== undefined) body.taskInstruction = data.taskInstruction
    if (data.price !== undefined) body.price = data.price
    return this.request<OrderDetailResponse>(`/api/technician/orders/${orderId}/tasks/${taskId}/items/${itemId}`, {
      method: 'PATCH',
      body: JSON.stringify(body),
    })
  }

  // Customer orders (všechny objednávky přihlášeného zákazníka)
  async getMyOrders(): Promise<OrderDetailResponse[]> {
    return this.request<OrderDetailResponse[]>('/api/customer/orders')
  }

  /** Veřejné zobrazení objednávky podle tokenu z e-mailu (bez přihlášení). Vrátí objednávku a přihlašovací údaje. */
  async viewOrderByToken(token: string): Promise<ViewOrderByTokenResponse> {
    const url = `${this.baseUrl}/api/public/orders/view?token=${encodeURIComponent(token)}`
    const res = await fetch(url, { method: 'GET', headers: { 'Content-Type': 'application/json' } })
    if (!res.ok) {
      const text = await res.text()
      let msg = `HTTP ${res.status}`
      try {
        const j = JSON.parse(text)
        if (j.message) msg = j.message
      } catch {
        if (text) msg = text
      }
      const err = new Error(msg) as Error & { status?: number }
      err.status = res.status
      throw err
    }
    return res.json()
  }

  // QR sken – ukládání a načtení posledních 10 (sync mobil ↔ počítač)
  async recordQrScan(scannedValue: string): Promise<QrScanEntryResponse> {
    return this.request<QrScanEntryResponse>('/api/technician/qr-scans', {
      method: 'POST',
      body: JSON.stringify({ scannedValue }),
    })
  }

  async getRecentQrScans(): Promise<QrScanEntryResponse[]> {
    return this.request<QrScanEntryResponse[]>('/api/technician/qr-scans')
  }

  // Možnosti struktur a úprav (čtení pro všechny, zápis jen ADMIN)
  async getStrukturyOptions(): Promise<StrukturaOptionResponse[]> {
    return this.request<StrukturaOptionResponse[]>('/api/technician/options/struktury')
  }

  async getModificationOptions(): Promise<ModificationOptionResponse[]> {
    return this.request<ModificationOptionResponse[]>('/api/technician/options/upravy')
  }

  async addStrukturaOption(name: string, sortOrder?: number, price?: number | null): Promise<StrukturaOptionResponse> {
    return this.request<StrukturaOptionResponse>('/api/technician/options/struktury', {
      method: 'POST',
      body: JSON.stringify({ name, sortOrder: sortOrder ?? 0, price: price ?? null }),
    })
  }

  async updateStrukturaOption(id: number, data: { price?: number | null; clearPrice?: boolean }): Promise<StrukturaOptionResponse> {
    return this.request<StrukturaOptionResponse>(`/api/technician/options/struktury/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  }

  async deleteStrukturaOption(id: number): Promise<void> {
    return this.request<void>(`/api/technician/options/struktury/${id}`, { method: 'DELETE' })
  }

  async addModificationOption(name: string, description?: string | null, sortOrder?: number, requiresWorkDescription?: boolean, price?: number | null): Promise<ModificationOptionResponse> {
    return this.request<ModificationOptionResponse>('/api/technician/options/upravy', {
      method: 'POST',
      body: JSON.stringify({
        name,
        description: description ?? null,
        sortOrder: sortOrder ?? 0,
        requiresWorkDescription: requiresWorkDescription ?? false,
        price: price ?? null,
      }),
    })
  }

  async updateModificationOption(id: number, data: { requiresWorkDescription?: boolean; price?: number | null; clearPrice?: boolean }): Promise<ModificationOptionResponse> {
    return this.request<ModificationOptionResponse>(`/api/technician/options/upravy/${id}`, {
      method: 'PATCH',
      body: JSON.stringify(data),
    })
  }

  async deleteModificationOption(id: number): Promise<void> {
    return this.request<void>(`/api/technician/options/upravy/${id}`, { method: 'DELETE' })
  }
}

export interface QrScanEntryResponse {
  id: number
  skiId: number | null
  skiNumber: string | null
  skiInfo: string | null
  scannedAt: string | null
}

export interface OrderSummaryResponse {
  id: number
  orderNumber: string
  customerName: string | null
  createdAt: string | null
  dueDate: string | null
  taskCount: number
  orderDone: boolean
  priority: string | null
  status: string | null
  price: number | null
  discount: number | null
  pohodaId: number | null
}

export interface ViewOrderByTokenResponse {
  order: OrderDetailResponse
  authToken: string
  authResponse: AuthResponse
}

export interface OrderDetailResponse {
  id: number
  orderNumber: string
  customerId: number | null
  customerName: string | null
  createdAt: string | null
  dueDate: string | null
  notes: string | null
  orderDone: boolean
  priority: string | null
  status: string | null
  price: number | null
  discount: number | null
  pohodaId: number | null
  tasks: OrderTaskResponse[]
}

export interface CreateOrderRequest {
  customerId?: number | null
  dueDate?: string | null
  priority?: string
  status?: string
  notes?: string | null
  price?: number | null
  discount?: number | null
  pohodaId?: number | null
  skiIds: number[]
  /** Cílová struktura pro každou lyži (stejné pořadí jako skiIds) */
  targetStruktura?: (string | null)[]
}

export interface CustomerSummaryResponse {
  id: number
  customerNumber: string
  name: string
  phone: string | null
  email: string | null
}

export interface CustomerDetailResponse {
  id: number
  customerNumber: string
  name: string
  phone: string | null
  email: string | null
  address: string | null
}

export interface OrderTaskResponse {
  id: number
  skiId: number | null
  skiNumber: string | null
  skiInfo: string | null
  status: string
  priority: string
  assignedTo: string | null
  skiStruktura: string | null
  targetStruktura: string | null
  taskItems: ServiceTaskItemResponse[]
}

export interface ServiceTaskItemResponse {
  id: number
  taskName: string
  taskInstruction: string | null
  taskDescription: string | null
  completed: boolean
  completedAt: string | null
  requiresWorkDescription: boolean
  price: number | null
}

export interface SkiServiceHistoryEntry {
  taskItemId: number
  orderNumber: string
  taskName: string
  taskInstruction: string | null
  taskDescription: string | null
  completed: boolean
  orderDate: string | null
  completedDate: string | null
  completedAt: string | null
}

export interface SkiResponse {
  id: number
  skiNumber: string
  brand: string
  model: string
  length: string
  year: number | null
  skiType: string | null
  weightKg: number | null
  condition: string
  status: string
  location: string | null
  notes: string | null
  lastServiceDate: string | null
  nextServiceDate: string | null
  struktura: string | null
  strukturaRecordedAt: string | null
  ean: string | null
  partNo: string | null
  serialNo: string | null
  skiUsage: string | null
}

export interface CreateSkiRequest {
  brand: string
  model: string
  length: string
  year?: number
  skiType?: string
  weightKg?: number
  condition?: string
  status?: string
  location?: string
  notes?: string
  lastServiceDate?: string
  nextServiceDate?: string
  struktura?: string | null
  ean?: string | null
  partNo?: string | null
  serialNo?: string | null
  skiUsage?: string | null
}

export interface UpdateSkiRequest {
  brand: string
  model: string
  length: string
  year?: number
  skiType?: string
  weightKg?: number
  condition?: string
  status?: string
  location?: string
  notes?: string
  lastServiceDate?: string
  nextServiceDate?: string
  struktura?: string | null
  ean?: string | null
  partNo?: string | null
  serialNo?: string | null
  skiUsage?: string | null
}

export interface UserResponse {
  id: number
  username: string
  role: string
  fullName: string | null
  email: string | null
  phone: string | null
  active: boolean
  createdAt: string
}

export interface CreateUserRequest {
  username: string
  password?: string
  role: 'ADMIN' | 'TECHNICIAN'
  fullName?: string
  email?: string
  generatePassword?: boolean
}

export interface AuditLogEntry {
  id: number
  action: string
  targetUserId: number
  targetUsername: string
  performedBy: string
  details: string | null
  createdAt: string
}

export interface AuditLogPage {
  content: AuditLogEntry[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

/** Stránkovaná odpověď (lyže, objednávky, zákazníci). */
export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface DailyStats {
  date: string
  created: number
  completed: number
}

export interface NameCount {
  name: string
  count: number
}

export interface RecordedErrorResponse {
  timestamp: number
  method: string
  path: string
  status: number
}

export interface MonitoringOverviewResponse {
  status: string
  uptimeMs: number
  uptimeFormatted: string
  requestRatePerSecond: number
  responseTimeAvgMs: number
  responseTimeMaxMs: number
  errorRatePercent: number
  database: Record<string, unknown>
  disk: Record<string, unknown>
  memory: Record<string, unknown>
  recentErrors: RecordedErrorResponse[]
  timestamp: number
}

export interface StatisticsResponse {
  waiting: number
  inProgress: number
  completed: number
  averageCompletionHours: number | null
  period: string
  daily: DailyStats[]
  topTaskTypes: NameCount[]
  topStructures: NameCount[]
}

export interface StrukturaOptionResponse {
  id: number
  name: string
  sortOrder: number
  price: number | null
}

export interface ModificationOptionResponse {
  id: number
  name: string
  description: string | null
  sortOrder: number
  requiresWorkDescription: boolean
  price: number | null
}

export const apiClient = new ApiClient()
export default apiClient
