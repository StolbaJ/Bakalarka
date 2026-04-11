import { render, screen } from '@testing-library/react'
import ProtectedRoute from '@/components/ProtectedRoute'
import { useAuth } from '@/contexts/AuthContext'

const pushMock = jest.fn()

jest.mock('next/navigation', () => ({
  useRouter: () => ({
    push: pushMock,
  }),
}))

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: jest.fn(),
}))

const mockedUseAuth = useAuth as jest.MockedFunction<typeof useAuth>

describe('ProtectedRoute', () => {
  beforeEach(() => {
    pushMock.mockClear()
    mockedUseAuth.mockReset()
  })

  it('shows loading state while auth is loading', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isLoading: true,
      loginStaff: jest.fn(),
      loginCustomer: jest.fn(),
      setUserFromAuthResponse: jest.fn(),
      logout: jest.fn(),
      refreshUser: jest.fn(),
    })

    render(
      <ProtectedRoute>
        <div>Secret content</div>
      </ProtectedRoute>
    )

    expect(screen.getByText('Načítání…')).toBeInTheDocument()
    expect(pushMock).not.toHaveBeenCalled()
  })

  it('redirects unauthenticated user to home', () => {
    mockedUseAuth.mockReturnValue({
      user: null,
      isLoading: false,
      loginStaff: jest.fn(),
      loginCustomer: jest.fn(),
      setUserFromAuthResponse: jest.fn(),
      logout: jest.fn(),
      refreshUser: jest.fn(),
    })

    render(
      <ProtectedRoute>
        <div>Secret content</div>
      </ProtectedRoute>
    )

    expect(screen.getByText('Přístup odepřen')).toBeInTheDocument()
    expect(pushMock).toHaveBeenCalledWith('/')
  })

  it('renders children for technician when ADMIN_OR_TECHNICIAN is required', () => {
    mockedUseAuth.mockReturnValue({
      user: {
        username: 'tech',
        role: 'TECHNICIAN',
      },
      isLoading: false,
      loginStaff: jest.fn(),
      loginCustomer: jest.fn(),
      setUserFromAuthResponse: jest.fn(),
      logout: jest.fn(),
      refreshUser: jest.fn(),
    })

    render(
      <ProtectedRoute requiredRole="ADMIN_OR_TECHNICIAN">
        <div>Secret content</div>
      </ProtectedRoute>
    )

    expect(screen.getByText('Secret content')).toBeInTheDocument()
    expect(pushMock).not.toHaveBeenCalled()
  })
})
