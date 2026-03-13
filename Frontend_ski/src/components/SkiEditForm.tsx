'use client'

import { useState, useEffect } from 'react'
import { SkiData } from './SkiItem'

const STATUS_OPTIONS = [
  { value: 'DOSTUPNY', label: 'Dostupný' },
  { value: 'V_SERVISU', label: 'V servisu' },
  { value: 'REZERVOVANO', label: 'Rezervováno' },
  { value: 'NEDOSTUPNY', label: 'Nedostupný' },
]

const CONDITION_OPTIONS = [
  { value: 'VYORNY', label: 'Výborný' },
  { value: 'DOBRY', label: 'Dobrý' },
  { value: 'STREDNI', label: 'Střední' },
  { value: 'SPATNY', label: 'Špatný' },
]

const SKI_USAGE_OPTIONS = [
  { value: '', label: '— Nevybráno' },
  { value: 'BEZECKE_KLASIKA', label: 'Běžecké lyže – klasika' },
  { value: 'BEZECKE_SKATE', label: 'Běžecké lyže – skate' },
  { value: 'BEZECKE_KLASIKA_SKIN', label: 'Běžecké lyže – klasika se skinem' },
  { value: 'SJEZDOVE', label: 'Sjezdové lyže' },
  { value: 'SKI_ALP', label: 'Ski alp' },
]

const REVERSE_STATUS: Record<string, string> = {
  'Dostupný': 'DOSTUPNY',
  'V servisu': 'V_SERVISU',
  'Rezervováno': 'REZERVOVANO',
  'Nedostupný': 'NEDOSTUPNY',
}

const REVERSE_CONDITION: Record<string, string> = {
  'Výborný': 'VYORNY',
  'Dobrý': 'DOBRY',
  'Střední': 'STREDNI',
  'Špatný': 'SPATNY',
}

interface SkiEditFormProps {
  ski?: SkiData | null
  onSubmit: (data: SkiFormData) => Promise<void>
  onCancel: () => void
}

export interface SkiFormData {
  brand: string
  model: string
  length: string
  year?: number
  skiType?: string
  weightKg?: number
  condition: string
  status: string
  location?: string
  notes?: string
  nextServiceDate?: string
  ean?: string
  partNo?: string
  serialNo?: string
  skiUsage?: string
}

const SkiEditForm: React.FC<SkiEditFormProps> = ({ ski, onSubmit, onCancel }) => {
  const [brand, setBrand] = useState('')
  const [model, setModel] = useState('')
  const [length, setLength] = useState('')
  const [year, setYear] = useState<number | ''>('')
  const [condition, setCondition] = useState('DOBRY')
  const [status, setStatus] = useState('DOSTUPNY')
  const [location, setLocation] = useState('')
  const [notes, setNotes] = useState('')
  const [nextServiceDate, setNextServiceDate] = useState('')
  const [ean, setEan] = useState('')
  const [partNo, setPartNo] = useState('')
  const [serialNo, setSerialNo] = useState('')
  const [skiUsage, setSkiUsage] = useState('')
  const [error, setError] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  useEffect(() => {
    if (ski) {
      setBrand(ski.brand)
      setModel(ski.model)
      setLength(ski.length)
      setYear(ski.year ? Number(ski.year) : '')
      setCondition(REVERSE_CONDITION[ski.condition] ?? 'DOBRY')
      setStatus(REVERSE_STATUS[ski.status] ?? 'DOSTUPNY')
      setLocation(ski.location ?? '')
      setNotes(ski.notes ?? '')
      setNextServiceDate(ski.nextService ?? '')
      setEan(ski.ean ?? '')
      setPartNo(ski.partNo ?? '')
      setSerialNo(ski.serialNo ?? '')
      setSkiUsage(ski.skiUsage ?? '')
    }
  }, [ski])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!brand.trim() || !model.trim() || !length.trim()) {
      setError('Značka, model a délka jsou povinné.')
      return
    }
    setIsSubmitting(true)
    try {
      await onSubmit({
        brand: brand.trim(),
        model: model.trim(),
        length: length.trim(),
        year: year !== '' ? Number(year) : undefined,
        condition,
        status,
        location: location.trim() || undefined,
        notes: notes.trim() || undefined,
        nextServiceDate: nextServiceDate || undefined,
        ean: ean.trim() || undefined,
        partNo: partNo.trim() || undefined,
        serialNo: serialNo.trim() || undefined,
        skiUsage: skiUsage || undefined,
      })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Chyba při ukládání')
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      {error && (
        <div className="p-3 bg-red-50 text-red-700 rounded-md text-sm">{error}</div>
      )}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Značka *</label>
          <input
            type="text"
            value={brand}
            onChange={(e) => setBrand(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Model *</label>
          <input
            type="text"
            value={model}
            onChange={(e) => setModel(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Délka (cm) *</label>
          <input
            type="text"
            value={length}
            onChange={(e) => setLength(e.target.value)}
            placeholder="např. 175"
            className="w-full border border-gray-300 rounded-md px-3 py-2"
            required
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Rok</label>
          <input
            type="number"
            value={year}
            onChange={(e) => setYear(e.target.value === '' ? '' : parseInt(e.target.value, 10))}
            min={1900}
            max={2100}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Použití</label>
          <select
            value={skiUsage}
            onChange={(e) => setSkiUsage(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          >
            {SKI_USAGE_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">EAN</label>
          <input
            type="text"
            value={ean}
            onChange={(e) => setEan(e.target.value)}
            placeholder="Čárový kód EAN"
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Part No</label>
          <input
            type="text"
            value={partNo}
            onChange={(e) => setPartNo(e.target.value)}
            placeholder="Číslo dílu"
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Serial No</label>
          <input
            type="text"
            value={serialNo}
            onChange={(e) => setSerialNo(e.target.value)}
            placeholder="Sériové číslo"
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Stav</label>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Kondice</label>
          <select
            value={condition}
            onChange={(e) => setCondition(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          >
            {CONDITION_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>{opt.label}</option>
            ))}
          </select>
        </div>
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Umístění</label>
          <input
            type="text"
            value={location}
            onChange={(e) => setLocation(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">Další servis</label>
          <input
            type="date"
            value={nextServiceDate}
            onChange={(e) => setNextServiceDate(e.target.value)}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
        <div className="md:col-span-2">
          <label className="block text-sm font-medium text-gray-700 mb-1">Poznámky</label>
          <textarea
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={3}
            className="w-full border border-gray-300 rounded-md px-3 py-2"
          />
        </div>
      </div>
      <div className="flex justify-end space-x-3 pt-4">
        <button
          type="button"
          onClick={onCancel}
          className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50"
        >
          Zrušit
        </button>
        <button
          type="submit"
          disabled={isSubmitting}
          className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 disabled:opacity-50"
        >
          {isSubmitting ? 'Ukládám...' : (ski ? 'Uložit' : 'Přidat')}
        </button>
      </div>
    </form>
  )
}

export default SkiEditForm
