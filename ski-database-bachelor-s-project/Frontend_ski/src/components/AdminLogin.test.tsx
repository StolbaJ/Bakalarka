import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import AdminLogin from '@/components/AdminLogin'
import { useAuth } from '@/contexts/AuthContext'
import { apiClient } from '@/lib/api'

jest.mock('@/contexts/AuthContext', () => ({
  useAuth: jest.fn(),
}))

jest.mock('@/contexts/LanguageContext', () => ({
  useLanguage: () => ({
    t: (key: string) => key,
  }),
}))

jest.mock('@/lib/api', () => ({
  apiClient: {
    getCredentialsHint: jest.fn(),
  },
}))

const mockedUseAuth = useAuth as jest.MockedFunction<typeof useAuth>
const mockedApiClient = apiClient as jest.Mocked<typeof apiClient>

describe('AdminLogin', () => {
  beforeEach(() => {
    mockedApiClient.getCredentialsHint.mockResolvedValue({ showHint: false })
    mockedUseAuth.mockReturnValue({
      user: null,
      isLoading: false,
      loginStaff: jest.fn(),
      loginCustomer: jest.fn(),
      setUserFromAuthResponse: jest.fn(),
      logout: jest.fn(),
      refreshUser: jest.fn(),
    })
  })

  it('calls onClose after successful staff login', async () => {
    const onClose = jest.fn()
    const loginStaff = jest.fn().mockResolvedValue(true)
    mockedUseAuth.mockReturnValue({
      user: null,
      isLoading: false,
      loginStaff,
      loginCustomer: jest.fn(),
      setUserFromAuthResponse: jest.fn(),
      logout: jest.fn(),
      refreshUser: jest.fn(),
    })

    render(<AdminLogin onClose={onClose} />)

    fireEvent.change(screen.getByLabelText('adminLogin.username'), { target: { value: 'admin' } })
    fireEvent.change(screen.getByLabelText('adminLogin.password'), { target: { value: 'admin123' } })
    fireEvent.click(screen.getByRole('button', { name: 'adminLogin.submit' }))

    await waitFor(() => {
      expect(loginStaff).toHaveBeenCalledWith('admin', 'admin123')
      expect(onClose).toHaveBeenCalled()
    })
  })

  it('shows invalid credentials message when login fails', async () => {
    const loginStaff = jest.fn().mockResolvedValue(false)
    mockedUseAuth.mockReturnValue({
      user: null,
      isLoading: false,
      loginStaff,
      loginCustomer: jest.fn(),
      setUserFromAuthResponse: jest.fn(),
      logout: jest.fn(),
      refreshUser: jest.fn(),
    })

    render(<AdminLogin onClose={jest.fn()} />)

    fireEvent.change(screen.getByLabelText('adminLogin.username'), { target: { value: 'admin' } })
    fireEvent.change(screen.getByLabelText('adminLogin.password'), { target: { value: 'wrong' } })
    fireEvent.click(screen.getByRole('button', { name: 'adminLogin.submit' }))

    expect(await screen.findByText('adminLogin.invalidCredentials')).toBeInTheDocument()
  })
})
