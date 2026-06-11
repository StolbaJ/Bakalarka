import fs from 'fs'
import path from 'path'
import Link from 'next/link'
import { GraduationCap, BookOpen, ArrowRight } from 'lucide-react'

function extractTitle(html: string): string {
  const match = html.match(/<title[^>]*>([^<]+)<\/title>/)
  return match ? match[1].trim() : 'Kvíz'
}

function getQuizzes() {
  const dir = path.join(process.cwd(), 'public', 'priprava')
  try {
    const files = fs.readdirSync(dir)
    return files
      .filter((f) => f.endsWith('.html'))
      .map((file) => {
        const html = fs.readFileSync(path.join(dir, file), 'utf-8')
        const title = extractTitle(html)
        const slug = file.replace('.html', '')
        return { slug, title }
      })
      .sort((a, b) => a.slug.localeCompare(b.slug))
  } catch {
    return []
  }
}

export default function PripravaPage() {
  const quizzes = getQuizzes()

  return (
    <div className="max-w-5xl mx-auto">
      <div className="mb-8">
        <div className="flex items-center space-x-3 mb-2">
          <GraduationCap className="w-8 h-8 text-blue-600" />
          <h1 className="text-3xl font-bold text-gray-900">Cvičení na státnice</h1>
        </div>
        <p className="text-gray-500">
          Vyberte předmět a procvičte si znalosti formou interaktivního kvízu.
          {quizzes.length > 0 && (
            <span className="ml-2 inline-block px-2 py-0.5 bg-gray-100 text-gray-600 text-xs rounded-full font-medium">
              {quizzes.length} kvízů
            </span>
          )}
        </p>
      </div>

      {quizzes.length === 0 ? (
        <div className="text-center py-16 text-gray-400">
          <GraduationCap className="w-12 h-12 mx-auto mb-3 opacity-40" />
          <p>Žádné kvízy nebyly nalezeny.</p>
          <p className="text-sm mt-1">Přidejte HTML soubory do složky <code className="bg-gray-100 px-1 rounded">public/priprava/</code></p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {quizzes.map((quiz) => (
            <Link
              key={quiz.slug}
              href={`/priprava/${quiz.slug}`}
              className="group flex flex-col p-5 bg-white rounded-xl border border-gray-200 hover:border-blue-400 hover:shadow-md transition-all duration-150"
            >
              <div className="flex items-start justify-between mb-4">
                <span className="inline-block px-2.5 py-1 bg-blue-100 text-blue-700 text-xs font-bold rounded-md uppercase tracking-wide">
                  {quiz.slug}
                </span>
                <ArrowRight className="w-4 h-4 text-gray-300 group-hover:text-blue-500 group-hover:translate-x-0.5 transition-all" />
              </div>
              <div className="flex items-start space-x-2 mt-auto">
                <BookOpen className="w-4 h-4 text-gray-400 flex-shrink-0 mt-0.5" />
                <span className="text-sm font-medium text-gray-700 leading-snug line-clamp-2">
                  {quiz.title}
                </span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  )
}
