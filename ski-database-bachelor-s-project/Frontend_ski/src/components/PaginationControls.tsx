'use client'

import { ChevronLeft, ChevronRight } from 'lucide-react'

const DEFAULT_PAGE_SIZES = [10, 20, 50, 100]

export interface PaginationControlsProps {
  /** Aktuální stránka (0-based) */
  page: number
  /** Počet položek na stránce */
  size: number
  /** Celkový počet položek */
  totalElements: number
  /** Celkový počet stránek */
  totalPages: number
  /** Volba velikosti stránky */
  onSizeChange: (size: number) => void
  /** Přechod na stránku */
  onPageChange: (page: number) => void
  /** Možné velikosti stránky */
  pageSizeOptions?: number[]
  /** Překladový klíč předpony (např. "database" pro t("database.pageInfo")) */
  labelPrefix?: string
  /** Funkce překladu t(key) */
  t?: (key: string) => string
}

export default function PaginationControls({
  page,
  size,
  totalElements,
  totalPages,
  onSizeChange,
  onPageChange,
  pageSizeOptions = DEFAULT_PAGE_SIZES,
  t = (k: string) => k,
}: PaginationControlsProps) {
  const from = totalElements === 0 ? 0 : page * size + 1
  const to = Math.min((page + 1) * size, totalElements)

  return (
    <div className="flex flex-wrap items-center justify-between gap-4 py-3 px-4 bg-gray-50 border-t border-gray-200 rounded-b-lg">
      <div className="flex items-center gap-4 flex-wrap">
        <span className="text-sm text-gray-700">
          {t('pagination.pageInfo')
            ? t('pagination.pageInfo').replace('{from}', String(from)).replace('{to}', String(to)).replace('{total}', String(totalElements))
            : `Zobrazeno ${from}–${to} z ${totalElements}`}
        </span>
        <label className="flex items-center gap-2 text-sm text-gray-700">
          {t('pagination.pageSize') || 'Na stránku:'}
          <select
            value={size}
            onChange={(e) => onSizeChange(Number(e.target.value))}
            className="rounded-md border-gray-300 text-sm py-1.5 pl-2 pr-8 focus:ring-blue-500 focus:border-blue-500"
          >
            {pageSizeOptions.map((s) => (
              <option key={s} value={s}>{s}</option>
            ))}
          </select>
        </label>
      </div>
      <div className="flex items-center gap-1">
        <button
          type="button"
          onClick={() => onPageChange(page - 1)}
          disabled={page <= 0}
          className="p-2 rounded-md border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          aria-label={t('pagination.prev') || 'Předchozí'}
        >
          <ChevronLeft className="w-5 h-5" />
        </button>
        <span className="px-3 py-1.5 text-sm text-gray-700">
          {t('pagination.pageOf')
            ? t('pagination.pageOf').replace('{current}', String(page + 1)).replace('{total}', String(totalPages || 1))
            : `Stránka ${page + 1} / ${totalPages || 1}`}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page >= totalPages - 1 || totalPages === 0}
          className="p-2 rounded-md border border-gray-300 bg-white text-gray-700 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
          aria-label={t('pagination.next') || 'Další'}
        >
          <ChevronRight className="w-5 h-5" />
        </button>
      </div>
    </div>
  )
}
