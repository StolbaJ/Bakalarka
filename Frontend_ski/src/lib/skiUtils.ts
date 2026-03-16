import { SkiResponse } from './api'
import { SkiData } from '@/components/SkiItem'

const STATUS_MAP: Record<string, string> = {
  DOSTUPNY: 'Dostupný',
  V_SERVISU: 'V servisu',
  REZERVOVANO: 'Rezervováno',
  NEDOSTUPNY: 'Nedostupný',
}

const CONDITION_MAP: Record<string, string> = {
  VYORNY: 'Výborný',
  DOBRY: 'Dobrý',
  STREDNI: 'Střední',
  SPATNY: 'Špatný',
}

export function skiResponseToData(res: SkiResponse): SkiData {
  return {
    id: res.skiNumber,
    numericId: res.id,
    brand: res.brand,
    model: res.model,
    length: res.length,
    year: res.year ?? undefined,
    condition: CONDITION_MAP[res.condition] ?? res.condition,
    status: STATUS_MAP[res.status] ?? res.status,
    lastService: res.lastServiceDate ?? undefined,
    nextService: res.nextServiceDate ?? undefined,
    location: res.location ?? undefined,
    notes: res.notes ?? undefined,
    struktura: res.struktura ?? undefined,
    strukturaRecordedAt: res.strukturaRecordedAt ?? undefined,
    structureChangeCount: res.structureChangeCount ?? 0,
    ean: res.ean ?? undefined,
    partNo: res.partNo ?? undefined,
    serialNo: res.serialNo ?? undefined,
    skiUsage: res.skiUsage ?? undefined,
  }
}

export function dataToCreateSkiRequest(data: Partial<SkiData>) {
  return {
    brand: data.brand ?? '',
    model: data.model ?? '',
    length: data.length ?? '',
    year: data.year ? Number(data.year) : undefined,
    skiType: undefined,
    weightKg: undefined,
    condition: reverseCondition(data.condition),
    status: reverseStatus(data.status),
    location: data.location ?? undefined,
    notes: data.notes ?? undefined,
    lastServiceDate: data.lastService ?? undefined,
    nextServiceDate: data.nextService ?? undefined,
    struktura: data.struktura ?? null,
    structureChangeCount: data.structureChangeCount,
  }
}

export function dataToUpdateSkiRequest(data: Partial<SkiData>) {
  return dataToCreateSkiRequest(data)
}

function reverseCondition(cs?: string): string | undefined {
  const map: Record<string, string> = {
    'Výborný': 'VYORNY',
    'Dobrý': 'DOBRY',
    'Střední': 'STREDNI',
    'Špatný': 'SPATNY',
  }
  return cs ? (map[cs] ?? cs) : undefined
}

function reverseStatus(cs?: string): string | undefined {
  const map: Record<string, string> = {
    'Dostupný': 'DOSTUPNY',
    'V servisu': 'V_SERVISU',
    'Rezervováno': 'REZERVOVANO',
    'Nedostupný': 'NEDOSTUPNY',
  }
  return cs ? (map[cs] ?? cs) : undefined
}
