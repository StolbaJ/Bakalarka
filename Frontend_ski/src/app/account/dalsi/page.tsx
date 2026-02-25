'use client'

import { SlidersHorizontal } from 'lucide-react'

export default function DalsiNastaveniPage() {
  return (
    <div className="bg-white rounded-lg border border-gray-200 overflow-hidden">
      <div className="px-6 py-4 border-b border-gray-200 bg-white">
        <h1 className="text-xl font-semibold text-gray-900 flex items-center gap-2">
          <SlidersHorizontal className="w-5 h-5 text-gray-600" />
          Další nastavení
        </h1>
        <p className="text-sm text-gray-500 mt-1">Rozšířené volby účtu</p>
      </div>
      <div className="p-6 text-gray-500 text-sm">
        Zde v budoucnu přibudou další volby (např. notifikace, vzhled, jazyk).
      </div>
    </div>
  )
}
