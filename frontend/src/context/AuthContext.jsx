import { useCallback, useMemo, useState } from 'react'
import { login as loginRequest, logout as logoutRequest, register as registerRequest } from '../services/authService'
import { setAccessToken } from '../services/apiClient'
import AuthStateContext from './AuthStateContext'

export function AuthProvider({ children }) {
  const [session, setSession] = useState(null)

  const acceptSession = useCallback((response) => {
    setAccessToken(response.accessToken)
    setSession({ user: response.user, expiresAt: response.expiresAt })
    return response.user
  }, [])

  const login = useCallback(async (credentials) => {
    const response = await loginRequest(credentials)
    return acceptSession(response)
  }, [acceptSession])

  const register = useCallback(async (credentials) => {
    const response = await registerRequest(credentials)
    return acceptSession(response)
  }, [acceptSession])

  const logout = useCallback(async () => {
    try {
      if (session) {
        await logoutRequest()
      }
    } finally {
      setAccessToken(null)
      setSession(null)
    }
  }, [session])

  const value = useMemo(() => ({
    user: session?.user ?? null,
    expiresAt: session?.expiresAt ?? null,
    login,
    register,
    logout,
  }), [session, login, register, logout])

  return <AuthStateContext.Provider value={value}>{children}</AuthStateContext.Provider>
}
