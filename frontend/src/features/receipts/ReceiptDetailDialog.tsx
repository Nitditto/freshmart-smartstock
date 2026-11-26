import type { Receipt } from '../../api/types'
import { formatDateTime, formatNumber, formatVnd } from '../../shared/lib/format'
import { StatusBadge } from '../../shared/ui/Badge'
import { Dialog } from '../../shared/ui/Dialog'
import { TABLE, TD, TD_NUM, TH, TH_NUM, TD_CODE } from '../../shared/ui/table'

export function ReceiptDetailDialog({ receipt, onClose }: { receipt: Receipt | null; onClose: () => void }) {
  const total = receipt?.lines.reduce((sum, l) => sum + l.quantity * l.unitPrice, 0) ?? 0
  return (
    <Dialog open={receipt !== null} onClose={onClose} title={receipt ? `Phiếu nhập ${receipt.code}` : 'Phiếu nhập'} wide>
      {receipt && (
        <div className="flex flex-col gap-4">
          <dl className="grid grid-cols-1 gap-x-6 gap-y-2 text-sm sm:grid-cols-2">
            <div>
              <dt className="text-muted-foreground">Nhà cung cấp</dt>
              <dd>{receipt.supplierName}</dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Trạng thái</dt>
              <dd>
                <StatusBadge status={receipt.status} />
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Người tạo</dt>
              <dd>
                {receipt.createdByName} - {formatDateTime(receipt.createdAt)}
              </dd>
            </div>
            <div>
              <dt className="text-muted-foreground">Xác nhận lúc</dt>
              <dd>{formatDateTime(receipt.confirmedAt)}</dd>
            </div>
            {receipt.note && (
              <div className="sm:col-span-2">
                <dt className="text-muted-foreground">Ghi chú</dt>
                <dd>{receipt.note}</dd>
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
                  <th className={TH_NUM}>Đơn giá</th>
                  <th className={TH_NUM}>Thành tiền</th>
                </tr>
              </thead>
              <tbody>
                {receipt.lines.map((line) => (
                  <tr key={line.productId}>
                    <td className={TD_CODE}>{line.sku}</td>
                    <td className={TD}>{line.productName}</td>
                    <td className={TD_NUM}>{formatNumber(line.quantity)}</td>
                    <td className={TD_NUM}>{formatVnd(line.unitPrice)}</td>
                    <td className={TD_NUM}>{formatVnd(line.quantity * line.unitPrice)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="text-right text-sm">
            Tổng: <strong className="tabular-nums">{formatVnd(total)}</strong>
          </p>
        </div>
      )}
    </Dialog>
  )
}
