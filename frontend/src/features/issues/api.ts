import { useMutation } from '@tanstack/react-query'
import type { Issue, IssueConfirmResult, IssueRequest, ShortageItem } from '../../api/types'
import { ApiError, apiPatch, apiPost } from '../../shared/api/client'
import { toast } from '../../shared/ui/toast'
import { useRefreshAfterDocumentChange } from '../documents/api'

export function useCreateIssue() {
  const refresh = useRefreshAfterDocumentChange('issues')
  return useMutation({
    mutationFn: (body: IssueRequest) => apiPost<Issue>('/issues', body),
    onSuccess: async (issue) => {
      await refresh()
      toast.success(`Đã tạo phiếu xuất ${issue.code} (nháp)`)
    },
  })
}

function isShortageList(data: unknown): data is ShortageItem[] {
  return (
    Array.isArray(data) &&
    data.length > 0 &&
    data.every((d) => typeof d === 'object' && d !== null && 'requested' in d && 'available' in d)
  )
}

interface ConfirmHandlers {
  onConfirmed: (result: IssueConfirmResult) => void
  onShortage: (items: ShortageItem[]) => void
}

/**
 * Confirming an issue can fail with 409 + the list of products that lack stock; that case is shown in a dialog
 * instead of a toast. Any other failure is reported like every other mutation.
 */
export function useConfirmIssue({ onConfirmed, onShortage }: ConfirmHandlers) {
  const refresh = useRefreshAfterDocumentChange('issues')
  return useMutation({
    mutationFn: (id: number) => apiPatch<IssueConfirmResult>(`/issues/${id}/confirm`),
    onSuccess: async (result) => {
      await refresh()
      toast.success(`Đã xác nhận ${result.issue.code}: tồn kho đã được trừ`)
      onConfirmed(result)
    },
    onError: (error) => {
      if (error instanceof ApiError && error.status === 409 && isShortageList(error.data)) {
        onShortage(error.data)
      } else if (!(error instanceof ApiError && error.handled)) {
        toast.error(error.message)
      }
    },
  })
}
