import React from 'react'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { AuthProvider, useAuth } from '@/contexts/AuthContext'
import apiClient from '@/lib/api'

jest.mock('@/lib/api', () => ({
  __esModule: true,
  default: {
    login: jest.fn(),
    loginCustomer: jest.fn(),
    validateSession: jest.fn(),
    ensureCsrfCookie: jest.fn().mockResolvedValue(undefined),
    logout: jest.fn().mockResolvedValue(undefined),
    setOnUnauthorized: jest.fn(),
  },
}))

const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>

const AuthProbe = () => {
  const { user, isLoading, loginStaff, loginCustomer, setUserFromAuthResponse, logout } = useAuth()

  return (
    <div>
      <div data-testid="loading">{String(isLoading)}</div>
      <div data-testid="username">{user?.username ?? 'none'}</div>
      <div data-testid="role">{user?.role ?? 'none'}</div>
      <button onClick={() => loginStaff('admin', 'admin123')}>login-staff</button>
      <button onClick={() => loginCustomer('ORD-1', '777888999')}>login-customer</button>
      <button
        onClick={() =>
          setUserFromAuthResponse({
            userId: 7,
            username: 'customer-link',
            role: 'CUSTOMER',
            fullName: null,
            email: null,
          })
        }
      >
        set-user-from-auth-response
      </button>
      <button onClick={logout}>logout</button>
    </div>
  )
}

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear()
    sessionStorage.clear()
    jest.clearAllMocks()
    mockedApiClient.validateSession.mockRejectedValue(new Error('no session'))
  })

  it('stores user after successful staff login with ADMIN role', async () => {
    mockedApiClient.login.mockResolvedValue({
      userId: 1,
      username: 'admin',
      role: 'ADMIN',
      fullName: 'Admin User',
      email: 'admin@example.com',
    })

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('login-staff'))

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('admin')
      expect(screen.getByTestId('role')).toHaveTextContent('ADMIN')
    })

    const stored = sessionStorage.getItem('ski_session_user')
    expect(stored).toContain('"username":"admin"')
    expect(stored).toContain('"role":"ADMIN"')
  })

  it('returns false and keeps user empty when login role is CUSTOMER', async () => {
    mockedApiClient.login.mockResolvedValue({
      userId: 2,
      username: 'customer-like',
      role: 'CUSTOMER',
      fullName: null,
      email: null,
    })

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('login-staff'))

    await waitFor(() => {
      expect(mockedApiClient.login).toHaveBeenCalledWith('admin', 'admin123')
    })
    expect(screen.getByTestId('username')).toHaveTextContent('none')
    expect(sessionStorage.getItem('ski_session_user')).toBeNull()
  })

  it('clears user and storage on logout', async () => {
    sessionStorage.setItem('ski_session_user', JSON.stringify({ username: 'admin', role: 'ADMIN' }))

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('admin')
    })

    fireEvent.click(screen.getByText('logout'))

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('none')
    })
    expect(sessionStorage.getItem('ski_session_user')).toBeNull()
  })

  it('rethrows 429 error from loginStaff', async () => {
    const rateLimitError = Object.assign(new Error('Too many attempts'), {
      status: 429,
      retryAfter: 30,
    })
    mockedApiClient.login.mockRejectedValue(rateLimitError)

    let capturedLoginStaff: ((username: string, password: string) => Promise<boolean>) | null = null
    const Probe = () => {
      const { loginStaff } = useAuth()
      capturedLoginStaff = loginStaff
      return null
    }

    render(
      <AuthProvider>
        <Probe />
      </AuthProvider>
    )

    await expect(capturedLoginStaff?.('admin', 'admin123')).rejects.toMatchObject({
      status: 429,
      retryAfter: 30,
    })
  })

  it('logs customer in when CUSTOMER role is returned', async () => {
    mockedApiClient.loginCustomer.mockResolvedValue({
      userId: 21,
      username: 'customer-user',
      role: 'CUSTOMER',
      fullName: 'Customer Name',
      email: 'customer@example.com',
    })

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('login-customer'))

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('customer-user')
      expect(screen.getByTestId('role')).toHaveTextContent('CUSTOMER')
    })
  })

  it('setUserFromAuthResponse sets only CUSTOMER user', async () => {
    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    fireEvent.click(screen.getByText('set-user-from-auth-response'))

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('customer-link')
      expect(screen.getByTestId('role')).toHaveTextContent('CUSTOMER')
    })
  })

  it('registers unauthorized callback and callback logs user out', async () => {
    mockedApiClient.login.mockResolvedValue({
      userId: 99,
      username: 'admin2',
      role: 'ADMIN',
      fullName: null,
      email: null,
    })

    render(
      <AuthProvider>
        <AuthProbe />
      </AuthProvider>
    )

    await waitFor(() => {
      expect(mockedApiClient.setOnUnauthorized).toHaveBeenCalled()
    })

    fireEvent.click(screen.getByText('login-staff'))

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('admin2')
    })

    const firstCall = mockedApiClient.setOnUnauthorized.mock.calls.find(
      (args) => typeof args[0] === 'function'
    )
    const onUnauthorized = firstCall?.[0] as (() => void) | undefined
    expect(onUnauthorized).toBeDefined()

    await act(async () => {
      onUnauthorized?.()
    })

    await waitFor(() => {
      expect(screen.getByTestId('username')).toHaveTextContent('none')
    })
    expect(sessionStorage.getItem('ski_session_user')).toBeNull()
  })
})
