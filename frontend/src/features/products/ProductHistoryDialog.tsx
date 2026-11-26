import type { Product } from '../../api/types'
import { formatDateTime, formatNumber } from '../../shared/lib/format'
import { Badge } from '../../shared/ui/Badge'
import { Dialog } from '../../shared/ui/Dialog'
import { EmptyState, ErrorState, Loading } from '../../shared/ui/States'
import { TABLE, TD, TD_NUM, TH, TH_NUM, TD_CODE } from '../../shared/ui/table'
import { useProductHistory } from './api'

const HISTORY_LIMIT_NOTE = 'Hiển thị tối đa 500 biến động mới nhất, chỉ từ chứng từ đã xác nhận.'

export function ProductHistoryDialog({ product, onClose }: { product: Product | null; onClose: () => void }) {
  const history = useProductHistory(product?.id ?? null)

  return (
    <Dialog open={product !== null} onClose={onClose} title={product ? `Lịch sử nhập/xuất: ${product.name}` : 'Lịch sử'} wide>
      {history.isLoading && <Loading />}
      {history.error && <ErrorState message={history.error.message} onRetry={() => history.refetch()} />}
      {history.data && history.data.length === 0 && <EmptyState message="Sản phẩm này chưa có biến động nào." />}
      {history.data && history.data.length > 0 && (
        <>
          <div className="overflow-x-auto rounded-md border border-border">
            <table className={TABLE}>
              <thead>
                <tr>
                  <th className={TH}>Thời điểm xác nhận</th>
                  <th className={TH}>Chiều</th>
                  <th className={TH}>Chứng từ</th>
                  <th className={TH_NUM}>Số lượng</th>
                </tr>
              </thead>
              <tbody>
                {history.data.map((item) => (
                  <tr key={`${item.documentType}-${item.documentCode}`}>
                    <td className={TD}>{formatDateTime(item.occurredAt)}</td>
                    <td className={TD}>
                      {item.direction === 'IN' ? <Badge tone="success">Nhập</Badge> : <Badge tone="warning">Xuất</Badge>}
                    </td>
                    <td className={TD_CODE}>{item.documentCode}</td>
                    <td className={TD_NUM}>
                      {item.direction === 'IN' ? '+' : '-'}
                      {formatNumber(item.quantity)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <p className="mt-2 text-xs text-muted-foreground">{HISTORY_LIMIT_NOTE}</p>
        </>
      )}
    </Dialog>
  )
}
