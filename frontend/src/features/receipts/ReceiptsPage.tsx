import { Check, Eye, Plus, Sparkles, XCircle } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router-dom'
import type { Receipt } from '../../api/types'
import { useAuth } from '../../shared/auth/AuthContext'
import { formatDateTime } from '../../shared/lib/format'
import { StatusBadge } from '../../shared/ui/Badge'
import { Button } from '../../shared/ui/Button'
import { Card } from '../../shared/ui/Card'
import { ConfirmDialog } from '../../shared/ui/ConfirmDialog'
import { PageHeader } from '../../shared/ui/PageHeader'
import { Pagination } from '../../shared/ui/Pagination'
import { EmptyState, ErrorState, Loading } from '../../shared/ui/States'
import { TABLE, TD, TH, TD_CODE } from '../../shared/ui/table'
import { EMPTY_DOCUMENT_QUERY, useCancelDocument, useDocuments, type DocumentQuery } from '../documents/api'
import { DocumentFilters } from '../documents/DocumentFilters'
import { canModifyDocument } from '../documents/permissions'
import { useConfirmReceipt } from './api'
import { ReceiptDetailDialog } from './ReceiptDetailDialog'
import { ReceiptFormDialog } from './ReceiptFormDialog'

export function ReceiptsPage() {
  const { user } = useAuth()
  const [query, setQuery] = useState<DocumentQuery>(EMPTY_DOCUMENT_QUERY)
  const [formOpen, setFormOpen] = useState(false)
  const [detail, setDetail] = useState<Receipt | null>(null)
  const [toCancel, setToCancel] = useState<Receipt | null>(null)

  const receipts = useDocuments<Receipt>('receipts', query)
  const confirm = useConfirmReceipt()
  const cancel = useCancelDocument('receipts')
  const data = receipts.data

  return (
    <>
      <PageHeader
        title="Phiếu nhập kho"
        description="Tạo phiếu nháp, kiểm tra rồi xác nhận để cộng tồn kho."
        actions={
          <>
            <Link
              to="/receipts/parse"
              className="inline-flex min-h-9 items-center gap-2 rounded-md border border-border bg-card px-3 py-1.5 text-sm font-medium hover:bg-muted"
            >
              <Sparkles className="size-4" aria-hidden="true" />
              Nhập từ hóa đơn (AI)
            </Link>
            <Button icon={<Plus className="size-4" aria-hidden="true" />} onClick={() => setFormOpen(true)}>
              Tạo phiếu nhập
            </Button>
          </>
        }
      />

      <Card className="overflow-hidden">
        <DocumentFilters query={query} onChange={setQuery} />
        {receipts.isLoading && <Loading />}
        {receipts.error && <ErrorState message={receipts.error.message} onRetry={() => receipts.refetch()} />}
        {data && data.items.length === 0 && <EmptyState message="Chưa có phiếu nhập nào khớp bộ lọc." />}
        {data && data.items.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className={TABLE}>
                <thead>
                  <tr>
                    <th className={TH}>Mã phiếu</th>
                    <th className={TH}>Nhà cung cấp</th>
                    <th className={TH}>Trạng thái</th>
                    <th className={TH}>Người tạo</th>
                    <th className={TH}>Ngày tạo</th>
                    <th className={TH}>
                      <span className="sr-only">Thao tác</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((r) => {
                    const editable = canModifyDocument(user, r)
                    return (
                      <tr key={r.id}>
                        <td className={TD_CODE}>{r.code}</td>
                        <td className={TD}>{r.supplierName}</td>
                        <td className={TD}>
                          <StatusBadge status={r.status} />
                        </td>
                        <td className={TD}>{r.createdByName}</td>
                        <td className={TD}>{formatDateTime(r.createdAt)}</td>
                        <td className={TD}>
                          <div className="flex justify-end gap-1">
                            <Button variant="ghost" aria-label={`Xem ${r.code}`} onClick={() => setDetail(r)}>
                              <Eye className="size-4" aria-hidden="true" />
                            </Button>
                            {editable && (
                              <>
                                <Button
                                  variant="ghost"
                                  aria-label={`Xác nhận ${r.code}`}
                                  loading={confirm.isPending && confirm.variables === r.id}
                                  onClick={() => confirm.mutate(r.id)}
                                  icon={<Check className="size-4 text-accent" aria-hidden="true" />}
                                >
                                  Xác nhận
                                </Button>
                                <Button variant="ghost" aria-label={`Hủy ${r.code}`} onClick={() => setToCancel(r)}>
                                  <XCircle className="size-4 text-danger" aria-hidden="true" />
                                </Button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              totalElements={data.totalElements}
              onChange={(page) => setQuery({ ...query, page })}
            />
          </>
        )}
      </Card>

      <ReceiptFormDialog open={formOpen} onClose={() => setFormOpen(false)} />
      <ReceiptDetailDialog receipt={detail} onClose={() => setDetail(null)} />
      <ConfirmDialog
        open={toCancel !== null}
        title="Hủy phiếu nhập"
        message={`Hủy phiếu ${toCancel?.code ?? ''}? Phiếu đã hủy không thể mở lại và không làm thay đổi tồn kho.`}
        confirmLabel="Hủy phiếu"
        loading={cancel.isPending}
        onCancel={() => setToCancel(null)}
        onConfirm={() => toCancel && cancel.mutate(toCancel.id, { onSuccess: () => setToCancel(null) })}
      />
    </>
  )
}
