'use client'

import { useState, useEffect, useCallback } from 'react'
import { Plus, Key, Shield, UserX, UserCheck, History } from 'lucide-react'
import ProtectedRoute from '@/components/ProtectedRoute'
import ConfirmModal from '@/components/ConfirmModal'
import { apiClient, UserResponse, CreateUserRequest, AuditLogPage } from '@/lib/api'

type ActionModal =
  | { type: 'role'; user: UserResponse }
  | { type: 'reset'; user: UserResponse }
  | { type: 'deactivate'; user: UserResponse }
  | { type: 'reactivate'; user: UserResponse }
  | null

export default function UsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [showForm, setShowForm] = useState(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [generatePassword, setGeneratePassword] = useState(false)
  const [formData, setFormData] = useState<CreateUserRequest>({
    username: '',
    password: '',
    role: 'TECHNICIAN',
    fullName: '',
    email: '',
    generatePassword: false,
  })
  const [formError, setFormError] = useState('')
  const [actionModal, setActionModal] = useState<ActionModal>(null)
  const [actionLoading, setActionLoading] = useState(false)
  const [showAuditLog, setShowAuditLog] = useState(false)
  const [auditLog, setAuditLog] = useState<AuditLogPage | null>(null)
  const [auditLoading, setAuditLoading] = useState(false)
  const [resetPasswordResult, setResetPasswordResult] = useState<{ password: string | null } | null>(null)

  const loadUsers = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const data = await apiClient.getUsers()
      setUsers(data)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chyba při načítání uživatelů')
      setUsers([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  const loadAuditLog = useCallback(async (userId?: number) => {
    setAuditLoading(true)
    try {
      const data = await apiClient.getAuditLog(0, 100, userId)
      setAuditLog(data)
    } catch {
      setAuditLog(null)
    } finally {
      setAuditLoading(false)
    }
  }, [])

  useEffect(() => {
    loadUsers()
  }, [loadUsers])

  useEffect(() => {
    if (showAuditLog) loadAuditLog()
  }, [showAuditLog, loadAuditLog])

  const handleCreateUser = async (e: React.FormEvent) => {
    e.preventDefault()
    setFormError('')
    if (!formData.username.trim()) {
      setFormError('Uživatelské jméno je povinné.')
      return
    }
    if (!generatePassword) {
      if (!formData.password?.trim()) {
        setFormError('Heslo je povinné.')
        return
      }
      if ((formData.password?.length ?? 0) < 6) {
        setFormError('Heslo musí mít alespoň 6 znaků.')
        return
      }
    } else {
      if (!formData.email?.trim()) {
        setFormError('E-mail je povinný pro odeslání vygenerovaného hesla.')
        return
      }
    }
    setIsSubmitting(true)
    try {
      await apiClient.createUser({
        username: formData.username.trim(),
        password: generatePassword ? undefined : formData.password,
        role: formData.role,
        fullName: formData.fullName?.trim() || undefined,
        email: formData.email?.trim() || undefined,
        generatePassword: generatePassword || undefined,
      })
      setFormData({ username: '', password: '', role: 'TECHNICIAN', fullName: '', email: '', generatePassword: false })
      setGeneratePassword(false)
      setShowForm(false)
      await loadUsers()
    } catch (err) {
      setFormError(err instanceof Error ? err.message : 'Chyba při vytváření uživatele')
    } finally {
      setIsSubmitting(false)
    }
  }

  const handleUpdateRole = async () => {
    if (!actionModal || actionModal.type !== 'role') return
    const newRole = (document.getElementById('role-select') as HTMLSelectElement)?.value as 'ADMIN' | 'TECHNICIAN'
    setActionLoading(true)
    try {
      await apiClient.updateUserRole(actionModal.user.id, newRole)
      setActionModal(null)
      await loadUsers()
    } catch {
      // error handled by api
    } finally {
      setActionLoading(false)
    }
  }

  const handleResetPassword = async () => {
    if (!actionModal || actionModal.type !== 'reset') return
    setActionLoading(true)
    setResetPasswordResult(null)
    try {
      const res = await apiClient.resetUserPassword(actionModal.user.id)
      setResetPasswordResult({ password: res.newPassword ?? null })
      await loadUsers()
      // Keep modal open to show result, user will close manually
    } catch {
      setActionModal(null)
    } finally {
      setActionLoading(false)
    }
  }

  const handleDeactivate = async () => {
    if (!actionModal || actionModal.type !== 'deactivate') return
    setActionLoading(true)
    try {
      await apiClient.deactivateUser(actionModal.user.id)
      setActionModal(null)
      await loadUsers()
    } catch {
      // error handled by api
    } finally {
      setActionLoading(false)
    }
  }

  const handleReactivate = async () => {
    if (!actionModal || actionModal.type !== 'reactivate') return
    setActionLoading(true)
    try {
      await apiClient.reactivateUser(actionModal.user.id)
      setActionModal(null)
      await loadUsers()
    } catch {
      // error handled by api
    } finally {
      setActionLoading(false)
    }
  }

  const getRoleLabel = (role: string) => {
    switch (role) {
      case 'ADMIN':
        return 'Administrátor'
      case 'TECHNICIAN':
        return 'Technik'
      default:
        return role
    }
  }

  const getActionLabel = (action: string) => {
    switch (action) {
      case 'CREATE':
        return 'Vytvoření účtu'
      case 'UPDATE_ROLE':
        return 'Změna role'
      case 'RESET_PASSWORD':
        return 'Reset hesla'
      case 'DEACTIVATE':
        return 'Deaktivace'
      case 'REACTIVATE':
        return 'Reaktivace'
      default:
        return action
    }
  }

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-8">
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="text-center sm:text-left">
            <h1 className="text-3xl font-bold text-gray-900 mb-1">Správa uživatelů</h1>
            <p className="text-gray-600">Vytváření a správa účtů pro administrátory a techniky</p>
          </div>
          <div className="flex flex-wrap gap-2 justify-center sm:justify-end">
            <button
              onClick={() => {
                setShowAuditLog(!showAuditLog)
                if (!showAuditLog) loadAuditLog()
              }}
              className="flex items-center space-x-2 bg-gray-100 text-gray-700 px-4 py-2 rounded-md hover:bg-gray-200 transition-colors"
            >
              <History className="w-4 h-4" />
              <span>Audit log</span>
            </button>
            <button
              onClick={() => {
                setShowForm(!showForm)
                setFormError('')
                setFormData({ username: '', password: '', role: 'TECHNICIAN', fullName: '', email: '', generatePassword: false })
                setGeneratePassword(false)
              }}
              className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>Přidat uživatele</span>
            </button>
          </div>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">{error}</div>
        )}

        {showForm && (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">Nový uživatel</h2>
            <form onSubmit={handleCreateUser} className="space-y-4 max-w-md">
              {formError && (
                <div className="p-3 bg-red-50 text-red-700 rounded-md text-sm">{formError}</div>
              )}
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="generatePassword"
                  checked={generatePassword}
                  onChange={(e) => setGeneratePassword(e.target.checked)}
                  className="rounded border-gray-300"
                />
                <label htmlFor="generatePassword" className="text-sm font-medium text-gray-700">
                  Vygenerovat heslo a poslat emailem
                </label>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Uživatelské jméno *</label>
                <input
                  type="text"
                  value={formData.username}
                  onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                  className="w-full border border-gray-300 rounded-md px-3 py-2"
                  required
                />
              </div>
              {!generatePassword && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">Heslo *</label>
                  <input
                    type="password"
                    value={formData.password ?? ''}
                    onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                    className="w-full border border-gray-300 rounded-md px-3 py-2"
                    required={!generatePassword}
                    minLength={6}
                  />
                </div>
              )}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Role</label>
                <select
                  value={formData.role}
                  onChange={(e) => setFormData({ ...formData, role: e.target.value as 'ADMIN' | 'TECHNICIAN' })}
                  className="w-full border border-gray-300 rounded-md px-3 py-2"
                >
                  <option value="ADMIN">Administrátor</option>
                  <option value="TECHNICIAN">Technik</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">Jméno</label>
                <input
                  type="text"
                  value={formData.fullName ?? ''}
                  onChange={(e) => setFormData({ ...formData, fullName: e.target.value })}
                  className="w-full border border-gray-300 rounded-md px-3 py-2"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  E-mail {generatePassword && '*'}
                </label>
                <input
                  type="email"
                  value={formData.email ?? ''}
                  onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                  className="w-full border border-gray-300 rounded-md px-3 py-2"
                  required={generatePassword}
                />
                {generatePassword && (
                  <p className="text-xs text-gray-500 mt-1">
                    Heslo bude vygenerováno a odesláno na tento e-mail.
                  </p>
                )}
              </div>
              <div className="flex space-x-3">
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                >
                  Zrušit
                </button>
                <button
                  type="submit"
                  disabled={isSubmitting}
                  className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
                >
                  {isSubmitting ? 'Vytvářím...' : 'Vytvořit'}
                </button>
              </div>
            </form>
          </div>
        )}

        {showAuditLog && (
          <div className="bg-white rounded-lg shadow-md overflow-hidden">
            <h2 className="text-xl font-semibold text-gray-900 p-4 border-b flex items-center gap-2">
              <History className="w-5 h-5" />
              Audit log změn účtů
            </h2>
            {auditLoading ? (
              <div className="p-8 text-center text-gray-500">Načítám...</div>
            ) : auditLog && auditLog.content.length > 0 ? (
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Datum</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Akce</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Uživatel</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Provedl</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Detaily</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {auditLog.content.map((entry) => (
                      <tr key={entry.id}>
                        <td className="px-4 py-3 text-sm text-gray-600 whitespace-nowrap">
                          {new Date(entry.createdAt).toLocaleString('cs-CZ')}
                        </td>
                        <td className="px-4 py-3 text-sm font-medium text-gray-900">
                          {getActionLabel(entry.action)}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600">{entry.targetUsername}</td>
                        <td className="px-4 py-3 text-sm text-gray-600">{entry.performedBy}</td>
                        <td className="px-4 py-3 text-sm text-gray-500 max-w-xs truncate">
                          {entry.details || '-'}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <div className="p-8 text-center text-gray-500">Žádné záznamy</div>
            )}
          </div>
        )}

        <div className="bg-white rounded-lg shadow-md overflow-hidden">
          <h2 className="text-xl font-semibold text-gray-900 p-4 border-b">Seznam uživatelů</h2>
          {isLoading ? (
            <div className="p-8 text-center text-gray-500">Načítám uživatele...</div>
          ) : users.length === 0 ? (
            <div className="p-8 text-center text-gray-500">Žádní uživatelé</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-gray-200">
                <thead className="bg-gray-50">
                  <tr>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Uživatel</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Jméno</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">E-mail</th>
                    <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Aktivní</th>
                    <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 uppercase">Akce</th>
                  </tr>
                </thead>
                <tbody className="bg-white divide-y divide-gray-200">
                  {users.map((user) => (
                    <tr key={user.id} className={!user.active ? 'bg-gray-50 opacity-75' : ''}>
                      <td className="px-4 py-3 text-sm font-medium text-gray-900">{user.username}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{getRoleLabel(user.role)}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{user.fullName || '-'}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{user.email || '-'}</td>
                      <td className="px-4 py-3 text-sm text-gray-600">{user.active ? 'Ano' : 'Ne'}</td>
                      <td className="px-4 py-3 text-right">
                        <div className="flex justify-end gap-1">
                          {user.active ? (
                            <>
                              <button
                                onClick={() => setActionModal({ type: 'role', user })}
                                className="p-2 text-blue-600 hover:bg-blue-50 rounded-md"
                                title="Změnit roli"
                              >
                                <Shield className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => {
                                  setActionModal({ type: 'reset', user })
                                  setResetPasswordResult(null)
                                }}
                                className="p-2 text-amber-600 hover:bg-amber-50 rounded-md"
                                title="Resetovat heslo"
                              >
                                <Key className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => setActionModal({ type: 'deactivate', user })}
                                className="p-2 text-red-600 hover:bg-red-50 rounded-md"
                                title="Deaktivovat"
                              >
                                <UserX className="w-4 h-4" />
                              </button>
                            </>
                          ) : (
                            <button
                              onClick={() => setActionModal({ type: 'reactivate', user })}
                              className="p-2 text-green-600 hover:bg-green-50 rounded-md"
                              title="Reaktivovat"
                            >
                              <UserCheck className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {actionModal?.type === 'role' && (
        <div className="fixed inset-0 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Změnit roli: {actionModal.user.username}</h3>
            <div className="mb-6">
              <label className="block text-sm font-medium text-gray-700 mb-2">Nová role</label>
              <select
                id="role-select"
                defaultValue={actionModal.user.role}
                className="w-full border border-gray-300 rounded-md px-3 py-2"
              >
                <option value="ADMIN">Administrátor</option>
                <option value="TECHNICIAN">Technik</option>
              </select>
            </div>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setActionModal(null)}
                disabled={actionLoading}
                className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
              >
                Zrušit
              </button>
              <button
                onClick={handleUpdateRole}
                disabled={actionLoading}
                className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
              >
                {actionLoading ? 'Ukládám...' : 'Uložit'}
              </button>
            </div>
          </div>
        </div>
      )}

      {actionModal?.type === 'reset' && (
        <div className="fixed inset-0 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-white rounded-lg shadow-xl p-6 w-full max-w-md">
            <h3 className="text-lg font-semibold text-gray-900 mb-4">Reset hesla: {actionModal.user.username}</h3>
            {resetPasswordResult ? (
              <>
                <p className="text-gray-600 mb-4">
                  Nové heslo bylo vygenerováno a odesláno na e-mail uživatele.
                </p>
                {resetPasswordResult.password && (
                  <>
                    <div className="bg-gray-100 p-4 rounded-md mb-4 font-mono text-sm break-all">
                      {resetPasswordResult.password}
                    </div>
                    <p className="text-sm text-gray-500 mb-4">
                      Uložte si heslo pro případ, že e-mail nedorazí. Po zavření ho již neuvidíte.
                    </p>
                  </>
                )}
                <button
                  onClick={() => {
                    setActionModal(null)
                    setResetPasswordResult(null)
                  }}
                  className="w-full px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
                >
                  Zavřít
                </button>
              </>
            ) : (
              <>
                <p className="text-gray-600 mb-6">
                  Vygeneruje nové heslo a pošle ho na e-mail {actionModal.user.email || 'uživatele'}.
                  Uživatel musí mít nastavený e-mail.
                </p>
                <div className="flex justify-end gap-2">
                  <button
                    onClick={() => {
                      setActionModal(null)
                      setResetPasswordResult(null)
                    }}
                    disabled={actionLoading}
                    className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
                  >
                    Zrušit
                  </button>
                  <button
                    onClick={handleResetPassword}
                    disabled={actionLoading || !actionModal.user.email}
                    className="px-4 py-2 bg-amber-600 text-white rounded-md hover:bg-amber-700 disabled:opacity-50"
                  >
                    {actionLoading ? 'Resetuji...' : 'Resetovat heslo'}
                  </button>
                </div>
                {!actionModal.user.email && (
                  <p className="text-sm text-red-600 mt-2">Uživatel nemá nastavený e-mail.</p>
                )}
              </>
            )}
          </div>
        </div>
      )}

      {actionModal?.type === 'deactivate' && (
        <ConfirmModal
          title="Deaktivovat uživatele"
          message={`Opravdu chcete deaktivovat uživatele ${actionModal.user.username}? Nebude se moci přihlásit.`}
          confirmText="Deaktivovat"
          confirmVariant="danger"
          onConfirm={handleDeactivate}
          onCancel={() => setActionModal(null)}
          isLoading={actionLoading}
        />
      )}

      {actionModal?.type === 'reactivate' && (
        <ConfirmModal
          title="Reaktivovat uživatele"
          message={`Opravdu chcete reaktivovat uživatele ${actionModal.user.username}?`}
          confirmText="Reaktivovat"
          onConfirm={handleReactivate}
          onCancel={() => setActionModal(null)}
          isLoading={actionLoading}
        />
      )}
    </ProtectedRoute>
  )
}
