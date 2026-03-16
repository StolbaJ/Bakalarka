'use client'

import { useState } from 'react'
import { Search, Grid, List, MoreHorizontal } from 'lucide-react'
import SkiItem, { SkiData } from './SkiItem'

interface SkiListProps {
  skis: SkiData[]
  onEdit?: (skiId: string, numericId?: number) => void
  onDelete?: (skiId: string, numericId?: number) => void
  onView?: (skiId: string) => void
  title?: string
  emptyMessage?: string
  showSearch?: boolean
  showFilters?: boolean
  defaultView?: 'grid' | 'list'
  className?: string
}

const SkiList: React.FC<SkiListProps> = ({
  skis,
  onEdit,
  onDelete,
  onView,
  title = 'Lyže',
  emptyMessage = 'Žádné lyže nenalezeny',
  showSearch = true,
  showFilters = true,
  defaultView = 'grid',
  className = ''
}) => {
  const [searchTerm, setSearchTerm] = useState('')
  const [statusFilter, setStatusFilter] = useState('all')
  const [conditionFilter, setConditionFilter] = useState('all')
  const [viewMode, setViewMode] = useState<'grid' | 'list'>(defaultView)

  // Filtrování lyží
  const filteredSkis = skis.filter(ski => {
    const matchesSearch = searchTerm === '' || 
      ski.id.toLowerCase().includes(searchTerm.toLowerCase()) ||
      ski.brand.toLowerCase().includes(searchTerm.toLowerCase()) ||
      ski.model.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (ski.customer && ski.customer.toLowerCase().includes(searchTerm.toLowerCase()))

    const matchesStatus = statusFilter === 'all' || ski.status.toLowerCase() === statusFilter.toLowerCase()
    const matchesCondition = conditionFilter === 'all' || ski.condition.toLowerCase() === conditionFilter.toLowerCase()

    return matchesSearch && matchesStatus && matchesCondition
  })

  // Získání unikátních hodnot pro filtry
  const statuses = [...new Set(skis.map(ski => ski.status))]
  const conditions = [...new Set(skis.map(ski => ski.condition))]

  return (
    <div className={`space-y-4 ${className}`}>
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-3">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">{title}</h2>
          <p className="text-gray-600">
            {filteredSkis.length} z {skis.length} lyží
          </p>
        </div>

        {/* View mode toggle */}
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setViewMode('grid')}
            className={`p-2 rounded-md ${viewMode === 'grid' ? 'bg-blue-100 text-blue-600' : 'text-gray-400 hover:text-gray-600'}`}
            title="Mřížka"
          >
            <Grid className="w-5 h-5" />
          </button>
          <button
            onClick={() => setViewMode('list')}
            className={`p-2 rounded-md ${viewMode === 'list' ? 'bg-blue-100 text-blue-600' : 'text-gray-400 hover:text-gray-600'}`}
            title="Seznam"
          >
            <List className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Search and filters - stejná výška řádku */}
      {(showSearch || showFilters) && (
        <div className="bg-white p-4 rounded-lg shadow-sm border">
          <div className="flex flex-col lg:flex-row lg:items-center gap-3">
            {/* Search */}
            {showSearch && (
              <div className="flex-1 min-w-0">
                <div className="relative h-10">
                  <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4 pointer-events-none" />
                  <input
                    type="text"
                    placeholder="Hledat lyže..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full h-full pl-10 pr-4 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              </div>
            )}

            {/* Filters - stejná výška jako vyhledávání */}
            {showFilters && (
              <div className="flex flex-wrap items-center gap-3">
                <select
                  value={statusFilter}
                  onChange={(e) => setStatusFilter(e.target.value)}
                  className="h-10 px-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 min-w-[140px]"
                >
                  <option value="all">Všechny stavy</option>
                  {statuses.map(status => (
                    <option key={status} value={status.toLowerCase()}>{status}</option>
                  ))}
                </select>

                <select
                  value={conditionFilter}
                  onChange={(e) => setConditionFilter(e.target.value)}
                  className="h-10 px-3 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500 focus:border-blue-500 min-w-[140px]"
                >
                  <option value="all">Všechny kondice</option>
                  {conditions.map(condition => (
                    <option key={condition} value={condition.toLowerCase()}>{condition}</option>
                  ))}
                </select>

                <button
                  onClick={() => {
                    setSearchTerm('')
                    setStatusFilter('all')
                    setConditionFilter('all')
                  }}
                  className="h-10 px-3 text-gray-600 hover:text-gray-800 text-sm font-medium rounded-md border border-gray-300 hover:bg-gray-50"
                >
                  Vymazat filtry
                </button>
              </div>
            )}
          </div>
        </div>
      )}

      {/* Results */}
      {filteredSkis.length === 0 ? (
        <div className="text-center py-12">
          <MoreHorizontal className="w-12 h-12 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-500 text-lg">{emptyMessage}</p>
          {searchTerm || statusFilter !== 'all' || conditionFilter !== 'all' ? (
            <p className="text-gray-400 text-sm mt-2">
              Zkuste změnit filtry nebo vyhledávací termín
            </p>
          ) : null}
        </div>
      ) : (
        <div className={
          viewMode === 'grid' 
            ? 'grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 xl:grid-cols-5 gap-4'
            : 'space-y-4'
        }>
          {filteredSkis.map(ski => (
            <SkiItem
              key={ski.id}
              ski={ski}
              onEdit={onEdit}
              onDelete={onDelete}
              onView={onView}
              variant={viewMode === 'grid' ? 'card' : 'list'}
            />
          ))}
        </div>
      )}
    </div>
  )
}

export default SkiList
