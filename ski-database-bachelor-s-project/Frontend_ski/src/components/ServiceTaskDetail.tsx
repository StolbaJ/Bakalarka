'use client'

import { useState } from 'react'
import { X, Edit, Clock, CheckCircle, AlertCircle, User, Calendar, Wrench, Save, Plus, Trash2 } from 'lucide-react'
import { ServiceTask } from './ServiceTaskItem'

const STATUS_OPTIONS: ServiceTask['status'][] = ['Čeká', 'Probíhá', 'Dokončeno', 'Pozastaveno']
const PRIORITY_OPTIONS: ServiceTask['priority'][] = ['Nízká', 'Střední', 'Vysoká', 'Kritická']

interface ServiceTaskDetailProps {
  task: ServiceTask
  onClose: () => void
  onEdit?: (taskId: string) => void
  onSave?: (task: ServiceTask) => void
  onDelete?: (taskId: string) => void
}

const ServiceTaskDetail: React.FC<ServiceTaskDetailProps> = ({
  task,
  onClose,
  onEdit,
  onSave,
  onDelete
}) => {
  const [isEditing, setIsEditing] = useState(false)
  const [editTask, setEditTask] = useState<ServiceTask>({ ...task })
  const [newTaskItem, setNewTaskItem] = useState('')
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
        return <Clock className="w-5 h-5" />
      case 'Probíhá':
        return <Wrench className="w-5 h-5" />
      case 'Dokončeno':
        return <CheckCircle className="w-5 h-5" />
      case 'Pozastaveno':
        return <AlertCircle className="w-5 h-5" />
      default:
        return <Clock className="w-5 h-5" />
    }
  }

  const handleSave = () => {
    onSave?.(editTask)
    setIsEditing(false)
    onClose()
  }

  const handleCancelEdit = () => {
    setEditTask({ ...task })
    setNewTaskItem('')
    setIsEditing(false)
  }

  const addTaskItem = () => {
    const t = newTaskItem.trim()
    if (t && !editTask.tasks.includes(t)) {
      setEditTask(prev => ({ ...prev, tasks: [...prev.tasks, t] }))
      setNewTaskItem('')
    }
  }

  const removeTaskItem = (index: number) => {
    setEditTask(prev => ({ ...prev, tasks: prev.tasks.filter((_, i) => i !== index) }))
  }

  const displayTask = isEditing ? editTask : task

  return (
    <div className="fixed inset-0 backdrop-blur-sm flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl p-8 w-full max-w-4xl max-h-[90vh] overflow-y-auto relative">
        {/* Close button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600"
        >
          <X className="w-6 h-6" />
        </button>
        
        {/* Header */}
        <div className="mb-6">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Detail úkolu: {displayTask.id}</h2>
          <div className="flex items-center space-x-4">
            <span className={`px-3 py-1 text-sm font-semibold rounded-full ${getPriorityColor(displayTask.priority)}`}>
              {displayTask.priority} priorita
            </span>
            <span className={`px-3 py-1 text-sm font-semibold rounded-full ${getStatusColor(displayTask.status)} flex items-center space-x-1`}>
              {getStatusIcon(displayTask.status)}
              <span>{displayTask.status}</span>
            </span>
          </div>
        </div>
        
        {/* Content */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Základní informace</h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700">Zákazník</label>
                  <div className="flex items-center space-x-2">
                    <User className="w-4 h-4 text-gray-400" />
                    <p className="text-gray-900 text-lg">{displayTask.customerName}</p>
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Číslo objednávky</label>
                  <p className="text-gray-900 text-lg">{displayTask.orderNumber}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">ID Lyže</label>
                  <p className="text-gray-900 text-lg">{displayTask.skiId}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700">Vytvořeno</label>
                  <p className="text-gray-900 text-lg">{displayTask.createdAt}</p>
                </div>
              </div>
            </div>
          </div>
          
          <div className="space-y-6">
            <div>
              <h3 className="text-lg font-semibold text-gray-900 mb-4">Servisní informace</h3>
              <div className="space-y-4">
                {isEditing ? (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Stav</label>
                      <select
                        value={editTask.status}
                        onChange={e => setEditTask(prev => ({ ...prev, status: e.target.value as ServiceTask['status'] }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                      >
                        {STATUS_OPTIONS.map(s => <option key={s} value={s}>{s}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Priorita</label>
                      <select
                        value={editTask.priority}
                        onChange={e => setEditTask(prev => ({ ...prev, priority: e.target.value as ServiceTask['priority'] }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                      >
                        {PRIORITY_OPTIONS.map(p => <option key={p} value={p}>{p}</option>)}
                      </select>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Přiřazeno</label>
                      <input
                        type="text"
                        value={editTask.assignedTo ?? ''}
                        onChange={e => setEditTask(prev => ({ ...prev, assignedTo: e.target.value || undefined }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                        placeholder="Jméno technika"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Odhadovaný čas</label>
                      <input
                        type="text"
                        value={editTask.estimatedTime}
                        onChange={e => setEditTask(prev => ({ ...prev, estimatedTime: e.target.value }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Skutečný čas</label>
                      <input
                        type="text"
                        value={editTask.actualTime ?? ''}
                        onChange={e => setEditTask(prev => ({ ...prev, actualTime: e.target.value || undefined }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                        placeholder="např. 1.5 hodiny"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Termín</label>
                      <input
                        type="text"
                        value={editTask.dueDate}
                        onChange={e => setEditTask(prev => ({ ...prev, dueDate: e.target.value }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                      />
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Dokončeno (datum)</label>
                      <input
                        type="text"
                        value={editTask.completedAt ?? ''}
                        onChange={e => setEditTask(prev => ({ ...prev, completedAt: e.target.value || undefined }))}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                        placeholder="např. 2024-01-20"
                      />
                    </div>
                  </>
                ) : (
                  <>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Přiřazeno</label>
                      <p className="text-gray-900 text-lg">{displayTask.assignedTo || 'Nepřiřazeno'}</p>
                    </div>
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Odhadovaný čas</label>
                      <p className="text-gray-900 text-lg">{displayTask.estimatedTime}</p>
                    </div>
                    {displayTask.actualTime && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700">Skutečný čas</label>
                        <p className="text-gray-900 text-lg">{displayTask.actualTime}</p>
                      </div>
                    )}
                    <div>
                      <label className="block text-sm font-medium text-gray-700">Termín</label>
                      <div className="flex items-center space-x-2">
                        <Calendar className="w-4 h-4 text-gray-400" />
                        <p className="text-gray-900 text-lg">{displayTask.dueDate}</p>
                      </div>
                    </div>
                    {displayTask.completedAt && (
                      <div>
                        <label className="block text-sm font-medium text-gray-700">Dokončeno</label>
                        <p className="text-gray-900 text-lg">{displayTask.completedAt}</p>
                      </div>
                    )}
                  </>
                )}
              </div>
            </div>
          </div>
        </div>
        
        {/* Tasks */}
        <div className="mt-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Servisní úkony</h3>
          {isEditing ? (
            <div className="space-y-2">
              {editTask.tasks.map((taskItem, index) => (
                <div key={index} className="flex items-center gap-2">
                  <span className="flex-1 text-gray-900">{taskItem}</span>
                  <button type="button" onClick={() => removeTaskItem(index)} className="text-red-600 hover:text-red-800 p-1">
                    <Trash2 className="w-4 h-4" />
                  </button>
                </div>
              ))}
              <div className="flex gap-2 mt-2">
                <input
                  type="text"
                  value={newTaskItem}
                  onChange={e => setNewTaskItem(e.target.value)}
                  onKeyDown={e => e.key === 'Enter' && (e.preventDefault(), addTaskItem())}
                  placeholder="Nový úkon"
                  className="flex-1 rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                />
                <button type="button" onClick={addTaskItem} className="flex items-center gap-1 px-3 py-1.5 bg-gray-200 rounded-md hover:bg-gray-300 text-sm">
                  <Plus className="w-4 h-4" /> Přidat
                </button>
              </div>
            </div>
          ) : (
            <ul className="list-disc list-inside space-y-2">
              {displayTask.tasks.map((taskItem, index) => (
                <li key={index} className="text-gray-900 text-lg">{taskItem}</li>
              ))}
            </ul>
          )}
        </div>
        
        {/* Notes */}
        <div className="mt-8">
          <h3 className="text-lg font-semibold text-gray-900 mb-4">Poznámky</h3>
          {isEditing ? (
            <textarea
              value={editTask.notes ?? ''}
              onChange={e => setEditTask(prev => ({ ...prev, notes: e.target.value || undefined }))}
              rows={3}
              className="block w-full rounded-md border-gray-300 shadow-sm focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
              placeholder="Poznámky k úkolu"
            />
          ) : (
            <div className="bg-gray-50 rounded-lg p-4">
              <p className="text-gray-900">{displayTask.notes || '—'}</p>
            </div>
          )}
        </div>
        
        {/* Actions */}
        <div className="mt-8 flex justify-end space-x-3">
          {isEditing ? (
            <>
              <button onClick={handleCancelEdit} className="px-6 py-2 bg-gray-500 text-white rounded-md hover:bg-gray-600 transition-colors">
                Zrušit
              </button>
              <button onClick={handleSave} className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors flex items-center space-x-2">
                <Save className="w-4 h-4" />
                <span>Uložit</span>
              </button>
            </>
          ) : (
            <>
              <button onClick={onClose} className="px-6 py-2 bg-gray-600 text-white rounded-md hover:bg-gray-700 transition-colors">
                Zavřít
              </button>
              {onSave && (
                <button
                  onClick={() => setIsEditing(true)}
                  className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors flex items-center space-x-2"
                >
                  <Edit className="w-4 h-4" />
                  <span>Upravit</span>
                </button>
              )}
              {onEdit && !onSave && (
                <button
                  onClick={() => onEdit(task.id)}
                  className="px-6 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 transition-colors flex items-center space-x-2"
                >
                  <Edit className="w-4 h-4" />
                  <span>Upravit</span>
                </button>
              )}
              {onDelete && (
                <button onClick={() => onDelete(task.id)} className="px-6 py-2 bg-red-600 text-white rounded-md hover:bg-red-700 transition-colors">
                  Smazat
                </button>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default ServiceTaskDetail
