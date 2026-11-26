import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { DocumentStatus, Page } from '../../api/types'
import { apiGet, apiPatch } from '../../shared/api/client'
import { toast } from '../../shared/ui/toast'

/** Receipts and issues share the list/filter/cancel API shape, so the two screens share these hooks. */
export type DocumentResource = 'receipts' | 'issues'

export interface DocumentQuery {
  status: DocumentStatus | ''
  from: string
  to: string
  page: number
}

export const EMPTY_DOCUMENT_QUERY: DocumentQuery = { status: '', from: '', to: '', page: 0 }
export const DOCUMENT_PAGE_SIZE = 10

export function useDocuments<T>(resource: DocumentResource, query: DocumentQuery) {
  return useQuery({
    queryKey: [resource, 'list', query],
    queryFn: () =>
      apiGet<Page<T>>(`/${resource}`, {
        status: query.status,
        from: query.from,
        to: query.to,
        page: query.page,
        size: DOCUMENT_PAGE_SIZE,
        sort: 'createdAt,desc',
      }),
    placeholderData: keepPreviousData,
  })
}

/** A confirmed document changes stock, so products, the dashboard and the report refresh together with the list. */
export function useRefreshAfterDocumentChange(resource: DocumentResource) {
  const queryClient = useQueryClient()
  return () =>
    Promise.all(
      [resource, 'products', 'dashboard', 'stock-summary'].map((key) => queryClient.invalidateQueries({ queryKey: [key] })),
    )
}

export function useCancelDocument(resource: DocumentResource) {
  const refresh = useRefreshAfterDocumentChange(resource)
  return useMutation({
    mutationFn: (id: number) => apiPatch(`/${resource}/${id}/cancel`),
    onSuccess: async () => {
      await refresh()
      toast.success('Đã hủy phiếu')
    },
  })
}
