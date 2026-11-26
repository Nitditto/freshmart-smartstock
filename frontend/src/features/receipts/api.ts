import { useMutation } from '@tanstack/react-query'
import type { ParseResult, Receipt, ReceiptRequest } from '../../api/types'
import { apiPatch, apiPost } from '../../shared/api/client'
import { toast } from '../../shared/ui/toast'
import { useRefreshAfterDocumentChange } from '../documents/api'

export function useCreateReceipt() {
  const refresh = useRefreshAfterDocumentChange('receipts')
  return useMutation({
    mutationFn: (body: ReceiptRequest) => apiPost<Receipt>('/receipts', body),
    onSuccess: async (receipt) => {
      await refresh()
      toast.success(`Đã tạo phiếu nhập ${receipt.code} (nháp)`)
    },
  })
}

export function useConfirmReceipt() {
  const refresh = useRefreshAfterDocumentChange('receipts')
  return useMutation({
    mutationFn: (id: number) => apiPatch<Receipt>(`/receipts/${id}/confirm`),
    onSuccess: async (receipt) => {
      await refresh()
      toast.success(`Đã xác nhận ${receipt.code}: tồn kho đã được cộng`)
    },
  })
}

/** The model only SUGGESTS lines; nothing is saved until the user creates the receipt. */
export function useParseInvoice() {
  return useMutation({
    mutationFn: (text: string) => apiPost<ParseResult>('/receipts/parse', { text }),
  })
}
