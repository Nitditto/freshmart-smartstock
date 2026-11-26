import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { describe, expect, it } from 'vitest'
import { ApiError, toApiError } from './client'

function failure(status: number, data: unknown): AxiosError {
  const config = { url: '/x' } as InternalAxiosRequestConfig
  const response = { status, data, statusText: '', headers: {}, config } as AxiosResponse
  return new AxiosError('failed', 'ERR_BAD_REQUEST', config, undefined, response)
}

describe('toApiError', () => {
  it('reads the Vietnamese message and data out of the envelope', async () => {
    const shortages = [{ productId: 5, requested: 100, available: 30 }]

    const error = await toApiError(failure(409, { status: 409, message: 'Không đủ tồn kho', data: shortages }))

    expect(error).toBeInstanceOf(ApiError)
    expect(error.status).toBe(409)
    expect(error.message).toBe('Không đủ tồn kho')
    expect(error.data).toEqual(shortages)
    expect(error.handled).toBe(false)
  })

  it('marks 403 as already handled so it is not toasted twice', async () => {
    const error = await toApiError(failure(403, { status: 403, message: 'Không đủ quyền', data: null }))

    expect(error.handled).toBe(true)
  })

  it('reads the envelope of a failed file download (a Blob)', async () => {
    const blob = new Blob([JSON.stringify({ status: 400, message: 'from phải trước to', data: null })], {
      type: 'application/json',
    })

    const error = await toApiError(failure(400, blob))

    expect(error.message).toBe('from phải trước to')
  })

  it('falls back to a generic message when the server is unreachable', async () => {
    const error = await toApiError(new AxiosError('Network Error'))

    expect(error.status).toBe(0)
    expect(error.message).toContain('Không kết nối được máy chủ')
  })
})
