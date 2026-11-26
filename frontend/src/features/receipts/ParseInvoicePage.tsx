import { AlertTriangle, FilePlus2, Sparkles } from 'lucide-react'
import { useState } from 'react'
import type { ParseResult } from '../../api/types'
import { useSuppliers } from '../../shared/api/lookups'
import { formatNumber, formatVnd } from '../../shared/lib/format'
import { Badge } from '../../shared/ui/Badge'
import { Button } from '../../shared/ui/Button'
import { Card } from '../../shared/ui/Card'
import { TextAreaField } from '../../shared/ui/Field'
import { PageHeader } from '../../shared/ui/PageHeader'
import { TABLE, TD, TD_NUM, TH, TH_NUM } from '../../shared/ui/table'
import { useParseInvoice } from './api'
import { ReceiptFormDialog, type ReceiptInitial } from './ReceiptFormDialog'

const MAX_TEXT_LENGTH = 8000
const SAMPLE_INVOICE = 'NCC Nông sản Xanh - Cà chua 40kg x 26.000; Khoai tây 60kg x 20.000; Bánh mì sandwich 20 gói x 25.000'

export function ParseInvoicePage() {
  const [text, setText] = useState('')
  const [result, setResult] = useState<ParseResult | null>(null)
  const [formOpen, setFormOpen] = useState(false)
  const suppliers = useSuppliers()
  const parse = useParseInvoice()

  const trimmed = text.trim()
  const tooLong = text.length > MAX_TEXT_LENGTH

  const onParse = () => parse.mutate(trimmed, { onSuccess: setResult })

  const suggestedSupplier = suppliers.data?.find((s) => s.id === result?.suggestedSupplierId)
  const initial: ReceiptInitial | undefined = result
    ? {
        supplierId: result.suggestedSupplierId ?? undefined,
        note: 'Tạo từ gợi ý hóa đơn (AI), đã kiểm tra thủ công',
        lines: result.lines.map((l) => ({ productId: l.productId, quantity: l.quantity, unitPrice: l.unitPrice ?? 0 })),
      }
    : undefined

  return (
    <>
      <PageHeader
        title="Nhập phiếu từ hóa đơn (AI)"
        description="Dán nội dung hóa đơn của nhà cung cấp. Hệ thống chỉ gợi ý dòng hàng; bạn kiểm tra rồi mới tạo phiếu nháp."
      />

      <Card className="mb-5 p-4">
        <TextAreaField
          label="Nội dung hóa đơn"
          rows={5}
          value={text}
          maxLength={MAX_TEXT_LENGTH + 1}
          placeholder="Ví dụ: NCC Nông sản Xanh - Cà chua 40kg x 26.000; Khoai tây 60kg x 20.000"
          hint={`${text.length}/${MAX_TEXT_LENGTH} ký tự. Không gửi giá nhập hay dữ liệu cá nhân của người dùng cho AI.`}
          error={tooLong ? `Hóa đơn quá dài (tối đa ${MAX_TEXT_LENGTH} ký tự)` : undefined}
          onChange={(e) => setText(e.target.value)}
        />
        <div className="mt-3 flex flex-wrap gap-2">
          <Button
            icon={<Sparkles className="size-4" aria-hidden="true" />}
            loading={parse.isPending}
            disabled={!trimmed || tooLong}
            onClick={onParse}
          >
            Phân tích hóa đơn
          </Button>
          <Button variant="secondary" onClick={() => setText(SAMPLE_INVOICE)}>
            Dùng hóa đơn mẫu
          </Button>
        </div>
        {parse.isPending && (
          <p role="status" className="mt-3 text-sm text-muted-foreground">
            Đang phân tích, model chạy cục bộ có thể mất vài chục giây...
          </p>
        )}
      </Card>

      {result && (
        <Card className="overflow-hidden">
          <div className="flex flex-wrap items-center gap-3 border-b border-border px-4 py-3">
            <h2 className="font-semibold">Gợi ý từ hóa đơn</h2>
            {result.mock ? <Badge tone="neutral">Dữ liệu mô phỏng (chưa dùng model thật)</Badge> : <Badge tone="info">Gợi ý từ AI</Badge>}
            <span className="text-sm text-muted-foreground">Chỉ là đề xuất, cần kiểm tra trước khi lưu.</span>
          </div>

          <div className="px-4 py-3 text-sm">
            Nhà cung cấp trong hóa đơn: <strong>{result.supplierNameInText ?? 'Không nhận ra'}</strong>
            {suggestedSupplier ? (
              <>
                {' '}
                khớp với <strong>{suggestedSupplier.name}</strong>
              </>
            ) : (
              <span className="text-muted-foreground"> (chưa khớp nhà cung cấp nào, hãy chọn tay ở bước tạo phiếu)</span>
            )}
          </div>

          {result.lines.length > 0 ? (
            <div className="overflow-x-auto">
              <table className={TABLE}>
                <thead>
                  <tr>
                    <th className={TH}>Sản phẩm</th>
                    <th className={TH_NUM}>Số lượng</th>
                    <th className={TH_NUM}>Đơn giá</th>
                    <th className={TH}>Dòng trong hóa đơn</th>
                  </tr>
                </thead>
                <tbody>
                  {result.lines.map((line) => (
                    <tr key={line.productId}>
                      <td className={TD}>{line.productName}</td>
                      <td className={TD_NUM}>{formatNumber(line.quantity)}</td>
                      <td className={TD_NUM}>{line.unitPrice == null ? 'Chưa có giá' : formatVnd(line.unitPrice)}</td>
                      <td className={`${TD} text-muted-foreground`}>{line.sourceText}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="px-4 pb-3 text-sm text-muted-foreground">Không có dòng nào được nhận dạng chắc chắn.</p>
          )}

          {result.unmatched.length > 0 && (
            <div className="border-t border-border bg-warning-soft px-4 py-3">
              <p className="flex items-center gap-2 text-sm font-medium text-warning">
                <AlertTriangle className="size-4" aria-hidden="true" />
                {result.unmatched.length} dòng chưa nhận dạng được, cần nhập tay
              </p>
              <ul className="mt-2 list-disc pl-6 text-sm">
                {result.unmatched.map((u, index) => (
                  <li key={`${u.sourceText}-${index}`}>
                    <span className="font-medium">{u.sourceText}</span>
                    <span className="text-muted-foreground">: {u.reason}</span>
                  </li>
                ))}
              </ul>
            </div>
          )}

          <div className="flex justify-end border-t border-border px-4 py-3">
            <Button
              icon={<FilePlus2 className="size-4" aria-hidden="true" />}
              disabled={result.lines.length === 0}
              onClick={() => setFormOpen(true)}
            >
              Kiểm tra và tạo phiếu nháp
            </Button>
          </div>
        </Card>
      )}

      <ReceiptFormDialog open={formOpen} initial={initial} onClose={() => setFormOpen(false)} />
    </>
  )
}
