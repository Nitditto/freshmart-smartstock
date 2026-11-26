import type { Issue } from '../../api/types'
import { REASON_LABEL, formatDateTime, formatNumber } from '../../shared/lib/format'
import { StatusBadge } from '../../shared/ui/Badge'
import { Dialog } from '../../shared/ui/Dialog'
import { TABLE, TD, TD_NUM, TH, TH_NUM, TD_CODE } from '../../shared/ui/table'

export function IssueDetailDialog({ issue, onClose }: { issue: Issue | null; onClose: () => void }) {
  return (
    <Dialog open={issue !== null} onClose={onClose} title={issue ? `Phiếu xuất ${issue.code}` : 'Phiếu xuất'} wide>
      {issue && (
        <div className="flex flex-col gap-4">
          <dl className="grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Lý do</dt>
              <dd>{REASON_LABEL[issue.reason]}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Trạng thái</dt>
              <dd>
                <StatusBadge status={issue.status} />
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Người tạo</dt>
              <dd>
                {issue.createdByName} - {formatDateTime(issue.createdAt)}
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Xác nhận lúc</dt>
              <dd>{formatDateTime(issue.confirmedAt)}</dd>
            </div>
            {issue.note && (
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Ghi chú</dt>
                <dd>{issue.note}</dd>
              </div>
            )}
          </dl>
          <div className="overflow-x-auto rounded-md border border-border">
            <table className={TABLE}>
              <thead>
                <tr>
                  <th className={TH}>SKU</th>
                  <th className={TH}>Sản phẩm</th>
                  <th className={TH_NUM}>Số lượng</th>
                </tr>
              </thead>
              <tbody>
                {issue.lines.map((line) => (
                  <tr key={line.productId}>
                    <td className={TD_CODE}>{line.sku}</td>
                    <td className={TD}>{line.productName}</td>
                    <td className={TD_NUM}>{formatNumber(line.quantity)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </Dialog>
  )
}
