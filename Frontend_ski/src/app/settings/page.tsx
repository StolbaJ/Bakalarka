'use client'

import { useState, useEffect, useCallback } from 'react'
import { Settings, Trash2, Loader2, Layers, Wrench } from 'lucide-react'
import ProtectedRoute from '@/components/ProtectedRoute'
import apiClient, {
  StrukturaOptionResponse,
  ModificationOptionResponse,
} from '@/lib/api'

export default function SettingsPage() {
  const [struktury, setStruktury] = useState<StrukturaOptionResponse[]>([])
  const [upravy, setUpravy] = useState<ModificationOptionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const [newStruktura, setNewStruktura] = useState('')
  const [newUpravaName, setNewUpravaName] = useState('')
  const [newUpravaDesc, setNewUpravaDesc] = useState('')
  const [newUpravaRequiresDesc, setNewUpravaRequiresDesc] = useState(false)
  const [savingStruktura, setSavingStruktura] = useState(false)
  const [savingUprava, setSavingUprava] = useState(false)
  const [togglingRequiresId, setTogglingRequiresId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const [s, u] = await Promise.all([
        apiClient.getStrukturyOptions(),
        apiClient.getModificationOptions(),
      ])
      setStruktury(s)
      setUpravy(u)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chyba při načítání')
      setStruktury([])
      setUpravy([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
  }, [load])

  const handleAddStruktura = async (e: React.FormEvent) => {
    e.preventDefault()
    const name = newStruktura.trim()
    if (!name) return
    setSavingStruktura(true)
    try {
      await apiClient.addStrukturaOption(name)
      setNewStruktura('')
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chyba při přidání struktury')
    } finally {
      setSavingStruktura(false)
    }
  }

  const handleDeleteStruktura = async (id: number) => {
    setDeletingId(`s-${id}`)
    try {
      await apiClient.deleteStrukturaOption(id)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chyba při mazání')
    } finally {
      setDeletingId(null)
    }
  }

  const handleAddUprava = async (e: React.FormEvent) => {
    e.preventDefault()
    const name = newUpravaName.trim()
    if (!name) return
    setSavingUprava(true)
    try {
      await apiClient.addModificationOption(name, newUpravaDesc.trim() || null, undefined, newUpravaRequiresDesc)
      setNewUpravaName('')
      setNewUpravaDesc('')
      setNewUpravaRequiresDesc(false)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chyba při přidání úpravy')
    } finally {
      setSavingUprava(false)
    }
  }

  const handleToggleRequiresWorkDescription = async (id: number, current: boolean) => {
    setTogglingRequiresId(id)
    try {
      await apiClient.updateModificationOption(id, { requiresWorkDescription: !current })
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chyba při úpravě')
    } finally {
      setTogglingRequiresId(null)
    }
  }

  const handleDeleteUprava = async (id: number) => {
    setDeletingId(`u-${id}`)
    try {
      await apiClient.deleteModificationOption(id)
      await load()
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Chyba při mazání')
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <ProtectedRoute requiredRole="ADMIN">
      <div className="space-y-8">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 mb-1 flex items-center gap-2">
            <Settings className="w-8 h-8" />
            Struktury a úpravy
          </h1>
          <p className="text-gray-600">
            Nabídky struktur lyží a běžných servisních úprav, které se zobrazují při vytváření objednávek a přidávání úkonů. Lze stále zadat i vlastní text.
          </p>
        </div>

        {error && (
          <div className="p-3 rounded-lg bg-red-50 text-red-700 text-sm">{error}</div>
        )}

        {loading ? (
          <div className="flex items-center justify-center py-12">
            <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          </div>
        ) : (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
            {/* Struktury */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
                <Layers className="w-5 h-5 text-blue-600" />
                Struktury lyží
              </h2>
              <p className="text-sm text-gray-500 mb-4">
                Např. woodcore, sandwich, cap – zobrazí se při výběru cílové struktury u objednávky.
              </p>
              <form onSubmit={handleAddStruktura} className="flex gap-2 mb-4">
                <input
                  type="text"
                  value={newStruktura}
                  onChange={e => setNewStruktura(e.target.value)}
                  placeholder="Název struktury"
                  className="flex-1 rounded-md border-gray-300 shadow-sm text-sm py-2 px-3"
                />
                <button
                  type="submit"
                  disabled={savingStruktura || !newStruktura.trim()}
                  className="px-4 py-2 bg-blue-600 text-white text-sm font-medium rounded-md hover:bg-blue-700 disabled:opacity-50 cursor-pointer"
                >
                  {savingStruktura ? 'Přidávám...' : 'Přidat'}
                </button>
              </form>
              <ul className="space-y-2">
                {struktury.length === 0 ? (
                  <li className="text-gray-500 text-sm py-2">Žádné struktury. Přidejte první.</li>
                ) : (
                  struktury.map(s => (
                    <li
                      key={s.id}
                      className="flex items-center justify-between py-2 px-3 bg-gray-50 rounded-md"
                    >
                      <span className="font-medium text-gray-900">{s.name}</span>
                      <button
                        type="button"
                        onClick={() => handleDeleteStruktura(s.id)}
                        disabled={deletingId === `s-${s.id}`}
                        className="p-1.5 text-red-600 hover:bg-red-50 rounded disabled:opacity-50 cursor-pointer"
                        title="Odebrat"
                      >
                        {deletingId === `s-${s.id}` ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Trash2 className="w-4 h-4" />
                        )}
                      </button>
                    </li>
                  ))
                )}
              </ul>
            </div>

            {/* Úpravy */}
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-lg font-semibold text-gray-900 mb-3 flex items-center gap-2">
                <Wrench className="w-5 h-5 text-amber-600" />
                Běžné servisní úpravy
              </h2>
              <p className="text-sm text-gray-500 mb-4">
                Nejčastější úkony (voskování, broušení…) – zobrazí se při přidávání úkonu k lyži v objednávce.
              </p>
              <p className="text-xs text-gray-500 mb-2">
                <strong>Návod</strong> (jak to zpracovat) se zkopíruje k úkonu při vytvoření. <strong>Vyžadovat výsledek</strong> znamená, že před dokončením úkonu musí být vyplněn zakončovací popis (např. výsledky měření).
              </p>
              <form onSubmit={handleAddUprava} className="space-y-2 mb-4">
                <input
                  type="text"
                  value={newUpravaName}
                  onChange={e => setNewUpravaName(e.target.value)}
                  placeholder="Název úpravy"
                  className="block w-full rounded-md border-gray-300 shadow-sm text-sm py-2 px-3"
                />
                <input
                  type="text"
                  value={newUpravaDesc}
                  onChange={e => setNewUpravaDesc(e.target.value)}
                  placeholder="Návod / jak to zpracovat (zobrazí se u úkonu)"
                  className="block w-full rounded-md border-gray-300 shadow-sm text-sm py-2 px-3"
                />
                <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
                  <input
                    type="checkbox"
                    checked={newUpravaRequiresDesc}
                    onChange={e => setNewUpravaRequiresDesc(e.target.checked)}
                    className="rounded border-gray-300 text-amber-600 focus:ring-amber-500"
                  />
                  Vyžadovat výsledek (zakončovací popis) před dokončením úkonu
                </label>
                <button
                  type="submit"
                  disabled={savingUprava || !newUpravaName.trim()}
                  className="px-4 py-2 bg-amber-600 text-white text-sm font-medium rounded-md hover:bg-amber-700 disabled:opacity-50 cursor-pointer"
                >
                  {savingUprava ? 'Přidávám...' : 'Přidat'}
                </button>
              </form>
              <ul className="space-y-2">
                {upravy.length === 0 ? (
                  <li className="text-gray-500 text-sm py-2">Žádné úpravy. Přidejte první.</li>
                ) : (
                  upravy.map(u => (
                    <li
                      key={u.id}
                      className="flex items-center justify-between gap-2 py-2 px-3 bg-gray-50 rounded-md"
                    >
                      <div className="min-w-0 flex-1">
                        <span className="font-medium text-gray-900">{u.name}</span>
                        {u.description && (
                          <span className="text-gray-500 text-sm ml-2">— Návod: {u.description}</span>
                        )}
                      </div>
                      <label className="flex items-center gap-1.5 shrink-0 text-xs text-gray-600 cursor-pointer" title="Vyžadovat popis práce před dokončením">
                        {togglingRequiresId === u.id ? (
                          <Loader2 className="w-3.5 h-3.5 animate-spin text-gray-400" />
                        ) : (
                          <input
                            type="checkbox"
                            checked={u.requiresWorkDescription}
                            onChange={() => handleToggleRequiresWorkDescription(u.id, u.requiresWorkDescription)}
                            className="rounded border-gray-300 text-amber-600 focus:ring-amber-500"
                          />
                        )}
                        <span className="hidden sm:inline">Popis povinný</span>
                      </label>
                      <button
                        type="button"
                        onClick={() => handleDeleteUprava(u.id)}
                        disabled={deletingId === `u-${u.id}`}
                        className="p-1.5 text-red-600 hover:bg-red-50 rounded disabled:opacity-50 cursor-pointer"
                        title="Odebrat"
                      >
                        {deletingId === `u-${u.id}` ? (
                          <Loader2 className="w-4 h-4 animate-spin" />
                        ) : (
                          <Trash2 className="w-4 h-4" />
                        )}
                      </button>
                    </li>
                  ))
                )}
              </ul>
            </div>
          </div>
        )}
      </div>
    </ProtectedRoute>
  )
}
