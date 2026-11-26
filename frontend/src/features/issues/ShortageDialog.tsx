import type { ShortageItem } from '../../api/types'
import { formatNumber } from '../../shared/lib/format'
import { Button } from '../../shared/ui/Button'
import { Dialog } from '../../shared/ui/Dialog'
import { TABLE, TD, TD_NUM, TH, TH_NUM } from '../../shared/ui/table'

interface ShortageDialogProps {
  items: ShortageItem[] | null
  onClose: () => void
}

/** The 409 of an issue confirmation: every product that lacks stock, and nothing was deducted. */
export function ShortageDialog({ items, onClose }: ShortageDialogProps) {
  return (
    <Dialog open={items !== null} onClose={onClose} title="Không đủ tồn kho để xuất">
      <p className="mb-3 text-sm">
        Phiếu <strong>chưa được xác nhận</strong> và tồn kho <strong>không bị trừ</strong>. Giảm số lượng ở các sản phẩm dưới
        đây hoặc nhập thêm hàng rồi thử lại.
      </p>
      <div className="overflow-x-auto rounded-md border border-border">
        <table className={TABLE}>
          <thead>
            <tr>
              <th className={TH}>Sản phẩm</th>
              <th className={TH_NUM}>Cần xuất</th>
              <th className={TH_NUM}>Hiện có</th>
              <th className={TH_NUM}>Thiếu</th>
            </tr>
          </thead>
          <tbody>
            {items?.map((item) => (
              <tr key={item.productId}>
                <td className={TD}>
                  {item.name} <span className="text-muted-foreground">({item.sku})</span>
                </td>
                <td className={TD_NUM}>{formatNumber(item.requested)}</td>
                <td className={TD_NUM}>{formatNumber(item.available)}</td>
                <td className={`${TD_NUM} font-semibold text-danger`}>{formatNumber(item.requested - item.available)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-4 flex justify-end">
        <Button onClick={onClose}>Đã hiểu</Button>
      </div>
    </Dialog>
  )
}
