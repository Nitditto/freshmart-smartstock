import type { User } from '../../api/types'

export interface StoredAuth {
  token: string
  user: User
}

const KEY = 'smartstock.auth'

/** sessionStorage, not localStorage: the session ends with the tab (AGENTS.md section 12). Every access is guarded. */
export const authStorage = {
  read(): StoredAuth | null {
    try {
      const raw = sessionStorage.getItem(KEY)
      return raw ? (JSON.parse(raw) as StoredAuth) : null
    } catch {
      return null
    }
  },
  write(auth: StoredAuth) {
    try {
      sessionStorage.setItem(KEY, JSON.stringify(auth))
    } catch {
      /* storage unavailable: the session just will not survive a reload */
    }
  },
  clear() {
    try {
      sessionStorage.removeItem(KEY)
    } catch {
      /* nothing to clear */
    }
  },
}

export const AUTH_EXPIRED_EVENT = 'smartstock:auth-expired'
