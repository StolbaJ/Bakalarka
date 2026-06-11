'use client'

import { Clock, CheckCircle, AlertCircle, User, Calendar, Eye, Edit, Wrench } from 'lucide-react'

export interface ServiceTask {
  id: string
  skiId: string
  customerName: string
  orderNumber: string
  tasks: string[]
  priority: 'Nízká' | 'Střední' | 'Vysoká' | 'Kritická'
  status: 'Čeká' | 'Probíhá' | 'Dokončeno' | 'Pozastaveno'
  estimatedTime: string
  actualTime?: string
  assignedTo?: string
  createdAt: string
  dueDate: string
  completedAt?: string
  notes?: string
}

interface ServiceTaskItemProps {
  task: ServiceTask
  onView?: (task: ServiceTask) => void
  onEdit?: (taskId: string) => void
  onDelete?: (taskId: string) => void
  variant?: 'table' | 'card'
}

const ServiceTaskItem: React.FC<ServiceTaskItemProps> = ({
  task,
  onView,
  onEdit,
  variant = 'table'
}) => {
  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'Nízká':
        return 'bg-green-100 text-green-800'
      case 'Střední':
        return 'bg-yellow-100 text-yellow-800'
      case 'Vysoká':
        return 'bg-orange-100 text-orange-800'
      case 'Kritická':
        return 'bg-red-100 text-red-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'Čeká':
        return 'bg-yellow-100 text-yellow-800'
      case 'Probíhá':
        return 'bg-blue-100 text-blue-800'
      case 'Dokončeno':
        return 'bg-green-100 text-green-800'
      case 'Pozastaveno':
        return 'bg-gray-100 text-gray-800'
      default:
        return 'bg-gray-100 text-gray-800'
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'Čeká':
        return <Clock className="w-4 h-4" />
      case 'Probíhá':
        return <Wrench className="w-4 h-4" />
      case 'Dokončeno':
        return <CheckCircle className="w-4 h-4" />
      case 'Pozastaveno':
        return <AlertCircle className="w-4 h-4" />
      default:
        return <Clock className="w-4 h-4" />
    }
  }

  if (variant === 'card') {
    return (
      <div className="bg-white rounded-lg shadow-md p-6 hover:shadow-lg transition-shadow">
        <div className="flex items-start justify-between mb-4">
          <div>
            <h3 className="text-lg font-semibold text-gray-900">{task.id}</h3>
            <p className="text-gray-600">{task.skiId}</p>
          </div>
          <div className="flex items-center space-x-2">
            <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getPriorityColor(task.priority)}`}>
              {task.priority}
            </span>
            <span className={`px-2 py-1 text-xs font-semibold rounded-full ${getStatusColor(task.status)}`}>
              {getStatusIcon(task.status)} {task.status}
            </span>
          </div>
        </div>

        <div className="space-y-3">
          <div className="flex items-center space-x-2">
            <User className="w-4 h-4 text-gray-400" />
            <span className="text-gray-900">{task.customerName}</span>
          </div>
          
          <div className="flex items-center space-x-2">
            <Calendar className="w-4 h-4 text-gray-400" />
            <span className="text-gray-900">Termín: {task.dueDate}</span>
          </div>

          {task.assignedTo && (
            <div>
              <span className="text-sm font-medium text-gray-700">Přiřazeno: </span>
              <span className="text-gray-900">{task.assignedTo}</span>
            </div>
          )}

          <div>
            <span className="text-sm font-medium text-gray-700">Čas: </span>
            <span className="text-gray-900">{task.estimatedTime}</span>
            {task.actualTime && (
              <span className="text-gray-600 ml-2">(skutečný: {task.actualTime})</span>
            )}
          </div>

          <div>
            <span className="text-sm font-medium text-gray-700">Úkoly: </span>
            <span className="text-gray-900">{task.tasks.join(', ')}</span>
          </div>
        </div>

        {task.notes && (
          <div className="mt-4 p-3 bg-gray-50 rounded-md">
            <p className="text-sm text-gray-700">{task.notes}</p>
          </div>
        )}

        <div className="mt-4 flex justify-end space-x-2">
          {onView && (
            <button
              onClick={() => onView(task)}
              className="p-2 text-blue-600 hover:text-blue-800"
              title="Zobrazit detail"
            >
              <Eye className="w-4 h-4" />
            </button>
          )}
          {onEdit && (
            <button
              onClick={() => onEdit(task.id)}
              className="p-2 text-yellow-600 hover:text-yellow-800"
              title="Upravit"
            >
              <Edit className="w-4 h-4" />
            </button>
          )}
        </div>
      </div>
    )
  }

  // Table variant (default)
  return (
    <tr className="hover:bg-gray-50">
      <td className="px-6 py-4 whitespace-nowrap text-sm font-medium text-gray-900">
        {task.id}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        <div className="flex items-center space-x-2">
          <User className="w-4 h-4 text-gray-400" />
          <span>{task.customerName}</span>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        {task.skiId}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getPriorityColor(task.priority)}`}>
          {task.priority}
        </span>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${getStatusColor(task.status)}`}>
          {getStatusIcon(task.status)} {task.status}
        </span>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        {task.assignedTo || 'Nepřiřazeno'}
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
        <div className="flex items-center space-x-1">
          <Calendar className="w-4 h-4 text-gray-400" />
          <span>{task.dueDate}</span>
        </div>
      </td>
      <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
        <div className="flex space-x-2">
          {onView && (
            <button
              onClick={() => onView(task)}
              className="text-blue-600 hover:text-blue-900"
              title="Zobrazit detail"
            >
              <Eye className="w-4 h-4" />
            </button>
          )}
          {onEdit && (
            <button
              onClick={() => onEdit(task.id)}
              className="text-yellow-600 hover:text-yellow-900"
              title="Upravit"
            >
              <Edit className="w-4 h-4" />
            </button>
          )}
        </div>
      </td>
    </tr>
  )
}

export default ServiceTaskItem
