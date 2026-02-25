'use client'

import { useState, useEffect, useCallback } from 'react'
import { Plus } from 'lucide-react'
import ProtectedRoute from '@/components/ProtectedRoute'
import { useLanguage } from '@/contexts/LanguageContext'
import SkiList from '@/components/SkiList'
import SkiDetail from '@/components/SkiDetail'
import SkiEditForm, { SkiFormData } from '@/components/SkiEditForm'
import DeleteConfirmModal from '@/components/DeleteConfirmModal'
import { SkiData } from '@/components/SkiItem'
import { apiClient } from '@/lib/api'
import { skiResponseToData } from '@/lib/skiUtils'

export default function DatabasePage() {
  const { t } = useLanguage()
  const [skis, setSkis] = useState<SkiData[]>([])
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState('')
  const [selectedSki, setSelectedSki] = useState<SkiData | null>(null)
  const [editingSki, setEditingSki] = useState<SkiData | null>(null)
  const [showAddForm, setShowAddForm] = useState(false)
  const [deletingSki, setDeletingSki] = useState<SkiData | null>(null)
  const [isDeleting, setIsDeleting] = useState(false)

  const loadSkis = useCallback(async () => {
    setIsLoading(true)
    setError('')
    try {
      const data = await apiClient.getSkis()
      setSkis(data.map(skiResponseToData))
    } catch (err) {
      setError(err instanceof Error ? err.message : t('database.loadError'))
      setSkis([])
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    loadSkis()
  }, [loadSkis])

  const handleViewSki = async (skiId: string) => {
    const fromList = skis.find((s) => s.id === skiId)
    const numericId = fromList?.numericId
    try {
      const res = numericId != null
        ? await apiClient.getSki(numericId)
        : await apiClient.getSkiByNumber(skiId)
      setSelectedSki(skiResponseToData(res))
    } catch {
      if (fromList) setSelectedSki(fromList)
    }
  }

  const handleEditSki = (skiId: string, numericId?: number) => {
    const ski = skis.find((s) => s.id === skiId || s.numericId === numericId)
    if (ski) {
      setEditingSki(ski)
      setShowAddForm(false)
      setSelectedSki(null)
    }
  }

  const handleDeleteSki = (skiId: string, numericId?: number) => {
    const ski = skis.find((s) => s.id === skiId || s.numericId === numericId)
    if (ski) setDeletingSki(ski)
  }

  const confirmDelete = async () => {
    if (!deletingSki?.numericId) return
    setIsDeleting(true)
    try {
      await apiClient.deleteSki(deletingSki.numericId)
      setDeletingSki(null)
      await loadSkis()
      setSelectedSki(null)
      setEditingSki(null)
    } catch (err) {
      setError(err instanceof Error ? err.message : t('database.deleteError'))
    } finally {
      setIsDeleting(false)
    }
  }

  const handleAddSki = () => {
    setEditingSki(null)
    setShowAddForm(true)
    setSelectedSki(null)
  }

  const handleSaveSki = async (data: SkiFormData) => {
    try {
      if (editingSki?.numericId) {
        await apiClient.updateSki(editingSki.numericId, {
          brand: data.brand,
          model: data.model,
          length: data.length,
          year: data.year,
          condition: data.condition,
          status: data.status,
          location: data.location,
          notes: data.notes,
          lastServiceDate: editingSki.lastService ?? undefined,
          nextServiceDate: data.nextServiceDate,
          struktura: editingSki.struktura ?? null,
        })
        setEditingSki(null)
      } else {
        await apiClient.createSki({
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
        setShowAddForm(false)
      }
      await loadSkis()
    } catch (err) {
      throw err
    }
  }

  return (
    <ProtectedRoute requiredRole="ADMIN_OR_TECHNICIAN">
      <div className="space-y-8">
        <div className="text-center">
          <h1 className="text-3xl font-bold text-gray-900 mb-4">{t('database.title')}</h1>
          <p className="text-gray-600">{t('database.subtitle')}</p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-md">
            <p className="font-medium">{t('common.error')}: {error}</p>
            <p className="text-sm mt-1">{t('database.errorBackend')}</p>
          </div>
        )}

        <div className="flex justify-end">
          <button
            onClick={handleAddSki}
            className="flex items-center space-x-2 bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 transition-colors"
          >
            <Plus className="w-4 h-4" />
            <span>{t('database.addSki')}</span>
          </button>
        </div>

        {isLoading ? (
          <div className="text-center py-12 bg-white rounded-lg shadow-md p-8">
            <p className="text-gray-600 text-lg mb-2">{t('database.loading')}</p>
            <p className="text-gray-400 text-sm">{t('database.backendCheck')} {typeof window !== 'undefined' ? (process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080') : 'http://localhost:8080'}</p>
          </div>
        ) : showAddForm ? (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">{t('database.addSkiTitle')}</h2>
            <SkiEditForm
              onSubmit={handleSaveSki}
              onCancel={() => setShowAddForm(false)}
            />
          </div>
        ) : editingSki ? (
          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold text-gray-900 mb-4">
              {t('database.editSkiTitle')}: {editingSki.id}
            </h2>
            <SkiEditForm
              ski={editingSki}
              onSubmit={handleSaveSki}
              onCancel={() => setEditingSki(null)}
            />
          </div>
        ) : (
          <SkiList
            skis={skis}
            onView={handleViewSki}
            onEdit={handleEditSki}
            onDelete={handleDeleteSki}
            title={t('database.allSkis')}
            emptyMessage={t('database.emptyMessage')}
            showSearch={true}
            showFilters={true}
            defaultView="grid"
          />
        )}

        {selectedSki && (
          <SkiDetail
            ski={selectedSki}
            onClose={() => setSelectedSki(null)}
            onEdit={handleEditSki}
            onDelete={handleDeleteSki}
          />
        )}

        {deletingSki && (
          <DeleteConfirmModal
            title={t('database.deleteTitle')}
            message={t('database.deleteConfirm')}
            onConfirm={confirmDelete}
            onCancel={() => setDeletingSki(null)}
            isLoading={isDeleting}
          />
        )}
      </div>
    </ProtectedRoute>
  )
}
