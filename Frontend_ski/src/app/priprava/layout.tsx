import ProtectedRoute from '@/components/ProtectedRoute'

export default function PripravaLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return <ProtectedRoute requiredRole="TECHNICIAN">{children}</ProtectedRoute>
}
