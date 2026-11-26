import type { LowStockItem } from '../../api/types'
import { formatNumber } from '../../shared/lib/format'
import { Button } from '../../shared/ui/Button'
import { Dialog } from '../../shared/ui/Dialog'
import { TABLE, TD, TD_NUM, TH, TH_NUM } from '../../shared/ui/table'

interface NewlyLowStockDialogProps {
  items: LowStockItem[] | null
  onClose: () => void
}

/** Shown after a successful confirmation when some products dropped to or below their minimum stock. */
export function NewlyLowStockDialog({ items, onClose }: NewlyLowStockDialogProps) {
  return (
    <Dialog open={items !== null} onClose={onClose} title="Cảnh báo tồn kho thấp">
      <p className="mb-3 text-sm">Các sản phẩm sau vừa xuống tới mức tồn tối thiểu, nên nhập thêm hàng:</p>
      <div className="overflow-x-auto rounded-md border border-border">
        <table className={TABLE}>
          <thead>
            <tr>
              <th className={TH}>Sản phẩm</th>
              <th className={TH_NUM}>Tồn hiện tại</th>
              <th className={TH_NUM}>Tối thiểu</th>
            </tr>
          </thead>
          <tbody>
            {items?.map((item) => (
              <tr key={item.productId}>
                <td className={TD}>
                  {item.name} <span className="text-muted-foreground">({item.sku})</span>
                </td>
                <td className={`${TD_NUM} font-semibold text-warning`}>
                  {formatNumber(item.quantityInStock)} {item.unit}
                </td>
                <td className={TD_NUM}>{formatNumber(item.minStock)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <div className="mt-4 flex justify-end">
        <Button onClick={onClose}>Đóng</Button>
      </div>
    </Dialog>
  )
}
