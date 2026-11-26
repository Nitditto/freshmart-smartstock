import axios, { AxiosError } from 'axios'
import { AUTH_EXPIRED_EVENT, authStorage } from '../auth/authStorage'
import { toast } from '../ui/toast'

export interface Envelope<T> {
  status: number
  message: string
  data: T
}

/** Normalised API failure: components never see axios or HTTP details (Adapter pattern). */
export class ApiError extends Error {
  readonly status: number
  /** Body `data` of the envelope, e.g. the shortage list of a 409. */
  readonly data: unknown
  /** True when the interceptor already told the user (403), so callers must not toast a second time. */
  readonly handled: boolean

  constructor(status: number, message: string, data?: unknown, handled = false) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.data = data
    this.handled = handled
  }
}

const FALLBACK_MESSAGE = 'Không kết nối được máy chủ. Vui lòng thử lại.'
const FORBIDDEN_MESSAGE = 'Bạn không có quyền thực hiện thao tác này.'
const LOGIN_PATH = '/auth/login'

export const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE ?? '/api' })

http.interceptors.request.use((config) => {
  const auth = authStorage.read()
  if (auth) {
    config.headers.set('Authorization', `Bearer ${auth.token}`)
  }
  return config
})

/** A failed blob download carries its JSON error envelope as a Blob; read it back into an object. */
async function readBody(body: unknown): Promise<Partial<Envelope<unknown>> | undefined> {
  try {
    const parsed = body instanceof Blob ? JSON.parse(await body.text()) : body
    return typeof parsed === 'object' && parsed !== null ? (parsed as Partial<Envelope<unknown>>) : undefined
  } catch {
    return undefined
  }
}

export async function toApiError(error: unknown): Promise<ApiError> {
  if (!(error instanceof AxiosError)) {
    return new ApiError(0, FALLBACK_MESSAGE)
  }
  const status = error.response?.status ?? 0
  const body = await readBody(error.response?.data)
  const message = typeof body?.message === 'string' && body.message ? body.message : FALLBACK_MESSAGE
  return new ApiError(status, message, body?.data, status === 403)
}

http.interceptors.response.use(
  (response) => response,
  async (error: unknown) => {
    const apiError = await toApiError(error)
    const isLogin = error instanceof AxiosError && error.config?.url === LOGIN_PATH
    if (apiError.status === 401 && !isLogin) {
      authStorage.clear()
      window.dispatchEvent(new Event(AUTH_EXPIRED_EVENT))
    }
    if (apiError.status === 403) {
      toast.error(FORBIDDEN_MESSAGE)
    }
    throw apiError
  },
)

type Params = Record<string, string | number | boolean | undefined | null>

/** Drops empty filters so the URL never carries `keyword=` or `status=undefined`. */
function clean(params?: Params) {
  if (!params) return undefined
  return Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''))
}

export async function apiGet<T>(url: string, params?: Params): Promise<T> {
  const response = await http.get<Envelope<T>>(url, { params: clean(params) })
  return response.data.data
}

export async function apiPost<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.post<Envelope<T>>(url, body)
  return response.data.data
}

export async function apiPut<T>(url: string, body: unknown): Promise<T> {
  const response = await http.put<Envelope<T>>(url, body)
  return response.data.data
}

export async function apiPatch<T>(url: string, body?: unknown): Promise<T> {
  const response = await http.patch<Envelope<T>>(url, body)
  return response.data.data
}

export async function apiDelete(url: string): Promise<void> {
  await http.delete(url)
}

export async function apiDownload(url: string, params?: Params): Promise<Blob> {
  const response = await http.get<Blob>(url, { params: clean(params), responseType: 'blob' })
  return response.data
}
