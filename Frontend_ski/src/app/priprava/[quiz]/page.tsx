'use client'

import { useParams, useRouter } from 'next/navigation'
import { ArrowLeft, ExternalLink } from 'lucide-react'

export default function QuizPage() {
  const params = useParams()
  const router = useRouter()
  const quiz = params.quiz as string

  const quizUrl = `/priprava/${quiz}.html`

  return (
    <div className="flex flex-col" style={{ height: 'calc(100vh - 128px)' }}>
      <div className="flex items-center justify-between mb-3 flex-shrink-0">
        <button
          onClick={() => router.push('/priprava')}
          className="flex items-center space-x-1.5 text-sm text-gray-500 hover:text-gray-900 transition-colors"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Zpět na výběr</span>
        </button>
        <a
          href={quizUrl}
          target="_blank"
          rel="noopener noreferrer"
          className="flex items-center space-x-1.5 text-sm text-blue-600 hover:text-blue-800 transition-colors"
        >
          <ExternalLink className="w-4 h-4" />
          <span>Otevřít v novém okně</span>
        </a>
      </div>
      <iframe
        src={quizUrl}
        className="flex-1 w-full rounded-xl border border-gray-200 bg-white"
        title={`Kvíz – ${quiz}`}
      />
    </div>
  )
}
