import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import type { LoginResult, Role, User } from '../../api/types'
import { apiPost } from '../api/client'
import { AUTH_EXPIRED_EVENT, authStorage, type StoredAuth } from './authStorage'

interface AuthValue {
  user: User | null
  login: (email: string, password: string) => Promise<User>
  logout: () => void
}

const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [auth, setAuth] = useState<StoredAuth | null>(() => authStorage.read())

  useEffect(() => {
    const onExpired = () => setAuth(null)
    window.addEventListener(AUTH_EXPIRED_EVENT, onExpired)
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, onExpired)
  }, [])

  const login = useCallback(async (email: string, password: string) => {
    const result = await apiPost<LoginResult>('/auth/login', { email, password })
    const next = { token: result.accessToken, user: result.user }
    authStorage.write(next)
    setAuth(next)
    return result.user
  }, [])

  const logout = useCallback(() => {
    authStorage.clear()
    setAuth(null)
  }, [])

  const value = useMemo(() => ({ user: auth?.user ?? null, login, logout }), [auth, login, logout])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthValue {
  const value = useContext(AuthContext)
  if (!value) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return value
}

/** Sends anonymous visitors to the login page and remembers where they wanted to go. */
export function RequireAuth({ children }: { children: ReactNode }) {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />
  }
  return children
}

/**
 * Hides routes and menus the role cannot use. This is convenience only: the backend still answers 403
 * (AGENTS.md section 12), so nothing here is a security boundary.
 */
export function RequireRole({ roles, children }: { roles: Role[]; children: ReactNode }) {
  const { user } = useAuth()
  if (!user || !roles.includes(user.role)) {
    return <Navigate to="/" replace />
  }
  return children
}
