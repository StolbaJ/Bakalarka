'use client'

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function ServicePage() {
  const router = useRouter()

  useEffect(() => {
    router.replace('/orders')
  }, [router])

  return null
}
