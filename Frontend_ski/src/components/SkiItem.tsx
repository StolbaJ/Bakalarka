'use client'

import { Edit, Trash2, Eye, Calendar, MapPin } from 'lucide-react'

export interface SkiData {
  id: string
  numericId?: number
  brand: string
  model: string
  length: string
  year?: number | string
  condition: string
  status: string
  lastService?: string
  nextService?: string
  location?: string
  notes?: string
  customer?: string
  priority?: 'low' | 'medium' | 'high'
  struktura?: string
  strukturaRecordedAt?: string | null
  structureChangeCount?: number
  ean?: string | null
  partNo?: string | null
  serialNo?: string | null
  skiUsage?: string | null
}

interface SkiItemProps {
  ski: SkiData
  onEdit?: (skiId: string, numericId?: number) => void
  onDelete?: (skiId: string, numericId?: number) => void
  onView?: (skiId: string) => void
  showActions?: boolean
  variant?: 'card' | 'list' | 'compact'
  className?: string
}

const SkiItem: React.FC<SkiItemProps> = ({
  ski,
  onEdit,
  onDelete,
  onView,
  showActions = true,
  variant = 'card',
  className = ''
}) => {
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

  const displayName = `${ski.brand} ${ski.model}${ski.length ? ` ${ski.length}` : ''}`.trim()

  if (variant === 'compact') {
    return (
      <div className={`flex items-center justify-between p-3 bg-white rounded-lg shadow-sm border ${className}`}>
        <div className="flex items-center space-x-3">
          <div className="flex-1">
            <h4 className="text-lg font-semibold text-gray-900 uppercase tracking-tight">{displayName}</h4>
            <p className="text-xs text-gray-500 mt-0.5">{ski.id}</p>
          </div>
          <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(ski.status)}`}>
            {ski.status}
          </span>
        </div>
        {showActions && (
          <div className="flex items-center space-x-2">
            {onView && (
              <button
                onClick={() => onView(ski.id)}
                className="p-1 text-gray-400 hover:text-blue-600"
                title="Zobrazit detaily"
              >
                <Eye className="w-4 h-4" />
              </button>
            )}
            {onEdit && (
              <button
                onClick={() => onEdit(ski.id, ski.numericId)}
                className="p-1 text-gray-400 hover:text-blue-600"
                title="Upravit"
              >
                <Edit className="w-4 h-4" />
              </button>
            )}
            {onDelete && (
              <button
                onClick={() => onDelete(ski.id, ski.numericId)}
                className="p-1 text-gray-400 hover:text-red-600"
                title="Smazat"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}
          </div>
        )}
      </div>
    )
  }

  if (variant === 'list') {
    return (
      <div className={`p-4 bg-white rounded-lg shadow-sm border ${className}`}>
        <div className="flex items-center justify-between">
          <div className="flex-1">
            <div className="flex items-center space-x-4">
              <div>
                <h4 className="text-lg font-semibold text-gray-900 uppercase tracking-tight">{displayName}</h4>
                <p className="text-xs text-gray-500 mt-0.5">{ski.id}</p>
              </div>
              <div className="flex items-center space-x-2">
                <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(ski.status)}`}>
                  {ski.status}
                </span>
                <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getConditionColor(ski.condition)}`}>
                  {ski.condition}
                </span>
                {ski.priority && (
                  <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getPriorityColor(ski.priority)}`}>
                    {ski.priority === 'high' ? 'Vysoká' : ski.priority === 'medium' ? 'Střední' : 'Nízká'} priorita
                  </span>
                )}
              </div>
            </div>
            {ski.customer && (
              <p className="text-sm text-gray-500 mt-1">Zákazník: {ski.customer}</p>
            )}
            {ski.location && (
              <div className="flex items-center space-x-1 text-sm text-gray-500 mt-1">
                <MapPin className="w-3 h-3" />
                <span>{ski.location}</span>
              </div>
            )}
            {ski.nextService && (
              <div className="flex items-center space-x-1 text-sm text-gray-500 mt-1">
                <Calendar className="w-3 h-3" />
                <span>Další servis: {ski.nextService}</span>
              </div>
            )}
          </div>
          {showActions && (
            <div className="flex items-center space-x-2">
              {onView && (
                <button
                  onClick={() => onView(ski.id)}
                  className="p-2 text-gray-400 hover:text-blue-600"
                  title="Zobrazit detaily"
                >
                  <Eye className="w-4 h-4" />
                </button>
              )}
              {onEdit && (
                <button
                  onClick={() => onEdit(ski.id, ski.numericId)}
                  className="p-2 text-gray-400 hover:text-blue-600"
                  title="Upravit"
                >
                  <Edit className="w-4 h-4" />
                </button>
              )}
              {onDelete && (
                <button
                  onClick={() => onDelete(ski.id, ski.numericId)}
                  className="p-2 text-gray-400 hover:text-red-600"
                  title="Smazat"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              )}
            </div>
          )}
        </div>
      </div>
    )
  }

  // Default card variant - kompaktnější
  return (
    <div className={`bg-white rounded-lg shadow-md p-4 ${className}`}>
      <div className="flex items-center justify-between mb-3">
        <div className="min-w-0">
          <h3 className="text-base font-bold text-gray-900 uppercase tracking-tight truncate">{displayName}</h3>
          <p className="text-xs text-gray-500 mt-0.5">{ski.id}</p>
        </div>
        <div className="flex items-center space-x-1.5 shrink-0 ml-2">
          <span className={`px-1.5 py-0.5 text-xs font-semibold rounded-full ${getStatusColor(ski.status)}`}>
            {ski.status}
          </span>
          <span className={`px-1.5 py-0.5 text-xs font-semibold rounded-full ${getConditionColor(ski.condition)}`}>
            {ski.condition}
          </span>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 mb-3 text-sm">
        <div>
          <p className="text-xs font-medium text-gray-600">Délka</p>
          <p className="text-gray-900">{ski.length}</p>
        </div>
        <div>
          <p className="text-xs font-medium text-gray-600">Rok</p>
          <p className="text-gray-900">{ski.year || 'N/A'}</p>
        </div>
        <div>
          <p className="text-xs font-medium text-gray-600">Změny struktury</p>
          <p className="text-gray-900">{ski.structureChangeCount ?? 0}</p>
        </div>
        {ski.location && (
          <div className="col-span-2">
            <p className="text-xs font-medium text-gray-600">Umístění</p>
            <p className="text-gray-900 truncate">{ski.location}</p>
          </div>
        )}
      </div>

      {ski.customer && (
        <div className="mb-2">
          <p className="text-xs font-medium text-gray-600">Zákazník</p>
          <p className="text-gray-900 text-sm">{ski.customer}</p>
        </div>
      )}

      {ski.priority && (
        <div className="mb-2">
          <span className={`px-1.5 py-0.5 text-xs font-semibold rounded-full ${getPriorityColor(ski.priority)}`}>
            Priorita: {ski.priority === 'high' ? 'Vysoká' : ski.priority === 'medium' ? 'Střední' : 'Nízká'}
          </span>
        </div>
      )}

      {ski.nextService && (
        <div className="mb-2">
          <p className="text-xs font-medium text-gray-600">Další servis</p>
          <p className="text-gray-900 text-sm">{ski.nextService}</p>
        </div>
      )}

      {ski.notes && (
        <div className="mb-3">
          <p className="text-xs font-medium text-gray-600">Poznámky</p>
          <p className="text-gray-900 text-xs line-clamp-2">{ski.notes}</p>
        </div>
      )}

      {showActions && (
        <div className="flex justify-end space-x-1 pt-1">
          {onView && (
            <button
              onClick={() => onView(ski.id)}
              className="px-2 py-1 text-blue-600 hover:text-blue-800 text-xs font-medium"
            >
              Zobrazit
            </button>
          )}
          {onEdit && (
            <button
              onClick={() => onEdit(ski.id, ski.numericId)}
              className="px-2 py-1 text-blue-600 hover:text-blue-800 text-xs font-medium"
            >
              Upravit
            </button>
          )}
          {onDelete && (
            <button
              onClick={() => onDelete(ski.id, ski.numericId)}
              className="px-2 py-1 text-red-600 hover:text-red-800 text-xs font-medium"
            >
              Smazat
            </button>
          )}
        </div>
      )}
    </div>
  )
}

export default SkiItem
