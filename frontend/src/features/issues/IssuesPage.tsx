import { Check, Eye, Plus, XCircle } from 'lucide-react'
import { useState } from 'react'
import type { Issue, LowStockItem, ShortageItem } from '../../api/types'
import { useAuth } from '../../shared/auth/AuthContext'
import { REASON_LABEL, formatDateTime } from '../../shared/lib/format'
import { Badge, StatusBadge } from '../../shared/ui/Badge'
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
import { useConfirmIssue } from './api'
import { IssueDetailDialog } from './IssueDetailDialog'
import { IssueFormDialog } from './IssueFormDialog'
import { NewlyLowStockDialog } from './NewlyLowStockDialog'
import { ShortageDialog } from './ShortageDialog'

export function IssuesPage() {
  const { user } = useAuth()
  const [query, setQuery] = useState<DocumentQuery>(EMPTY_DOCUMENT_QUERY)
  const [formOpen, setFormOpen] = useState(false)
  const [detail, setDetail] = useState<Issue | null>(null)
  const [toCancel, setToCancel] = useState<Issue | null>(null)
  const [shortages, setShortages] = useState<ShortageItem[] | null>(null)
  const [lowStock, setLowStock] = useState<LowStockItem[] | null>(null)

  const issues = useDocuments<Issue>('issues', query)
  const cancel = useCancelDocument('issues')
  const confirm = useConfirmIssue({
    onShortage: setShortages,
    onConfirmed: (result) => setLowStock(result.newlyLowStock.length > 0 ? result.newlyLowStock : null),
  })
  const data = issues.data

  return (
    <>
      <PageHeader
        title="Phiếu xuất kho"
        description="Xác nhận kiểm tra đủ tồn cho tất cả dòng trước khi trừ; thiếu một dòng thì không trừ dòng nào."
        actions={
          <Button icon={<Plus className="size-4" aria-hidden="true" />} onClick={() => setFormOpen(true)}>
            Tạo phiếu xuất
          </Button>
        }
      />

      <Card className="overflow-hidden">
        <DocumentFilters query={query} onChange={setQuery} />
        {issues.isLoading && <Loading />}
        {issues.error && <ErrorState message={issues.error.message} onRetry={() => issues.refetch()} />}
        {data && data.items.length === 0 && <EmptyState message="Chưa có phiếu xuất nào khớp bộ lọc." />}
        {data && data.items.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className={TABLE}>
                <thead>
                  <tr>
                    <th className={TH}>Mã phiếu</th>
                    <th className={TH}>Lý do</th>
                    <th className={TH}>Trạng thái</th>
                    <th className={TH}>Người tạo</th>
                    <th className={TH}>Ngày tạo</th>
                    <th className={TH}>
                      <span className="sr-only">Thao tác</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((i) => {
                    const editable = canModifyDocument(user, i)
                    const needsAdmin = i.reason === 'DAMAGED' && user?.role !== 'ADMIN'
                    return (
                      <tr key={i.id}>
                        <td className={TD_CODE}>{i.code}</td>
                        <td className={TD}>
                          {i.reason === 'DAMAGED' ? <Badge tone="warning">{REASON_LABEL[i.reason]}</Badge> : REASON_LABEL[i.reason]}
                        </td>
                        <td className={TD}>
                          <StatusBadge status={i.status} />
                        </td>
                        <td className={TD}>{i.createdByName}</td>
                        <td className={TD}>{formatDateTime(i.createdAt)}</td>
                        <td className={TD}>
                          <div className="flex items-center justify-end gap-1">
                            <Button variant="ghost" aria-label={`Xem ${i.code}`} onClick={() => setDetail(i)}>
                              <Eye className="size-4" aria-hidden="true" />
                            </Button>
                            {editable && needsAdmin && <span className="text-xs text-muted-foreground">Chờ ADMIN xác nhận</span>}
                            {editable && !needsAdmin && (
                              <Button
                                variant="ghost"
                                aria-label={`Xác nhận ${i.code}`}
                                loading={confirm.isPending && confirm.variables === i.id}
                                onClick={() => confirm.mutate(i.id)}
                                icon={<Check className="size-4 text-accent" aria-hidden="true" />}
                              >
                                Xác nhận
                              </Button>
                            )}
                            {editable && (
                              <Button variant="ghost" aria-label={`Hủy ${i.code}`} onClick={() => setToCancel(i)}>
                                <XCircle className="size-4 text-danger" aria-hidden="true" />
                              </Button>
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

      <IssueFormDialog open={formOpen} onClose={() => setFormOpen(false)} />
      <IssueDetailDialog issue={detail} onClose={() => setDetail(null)} />
      <ShortageDialog items={shortages} onClose={() => setShortages(null)} />
      <NewlyLowStockDialog items={lowStock} onClose={() => setLowStock(null)} />
      <ConfirmDialog
        open={toCancel !== null}
        title="Hủy phiếu xuất"
        message={`Hủy phiếu ${toCancel?.code ?? ''}? Phiếu đã hủy không thể mở lại và không làm thay đổi tồn kho.`}
        confirmLabel="Hủy phiếu"
        loading={cancel.isPending}
        onCancel={() => setToCancel(null)}
        onConfirm={() => toCancel && cancel.mutate(toCancel.id, { onSuccess: () => setToCancel(null) })}
      />
    </>
  )
}
