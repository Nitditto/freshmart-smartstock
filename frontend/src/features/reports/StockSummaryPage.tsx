import { useMutation, useQuery } from '@tanstack/react-query'
import { Download } from 'lucide-react'
import { useState } from 'react'
import type { StockSummaryItem } from '../../api/types'
import { apiDownload, apiGet } from '../../shared/api/client'
import { businessDate, firstDayOfMonth, formatNumber } from '../../shared/lib/format'
import { Button } from '../../shared/ui/Button'
import { Card } from '../../shared/ui/Card'
import { TextField } from '../../shared/ui/Field'
import { PageHeader } from '../../shared/ui/PageHeader'
import { EmptyState, ErrorState, Loading } from '../../shared/ui/States'
import { toast } from '../../shared/ui/toast'
import { TABLE, TD, TD_NUM, TH, TH_NUM, TD_CODE } from '../../shared/ui/table'

type ExportFormat = 'csv' | 'xlsx'

function saveBlob(blob: Blob, fileName: string) {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}

export function StockSummaryPage() {
  const today = new Date()
  const [from, setFrom] = useState(firstDayOfMonth(today))
  const [to, setTo] = useState(businessDate(today))
  const rangeValid = from !== '' && to !== '' && from <= to

  const report = useQuery({
    queryKey: ['stock-summary', from, to],
    queryFn: () => apiGet<StockSummaryItem[]>('/reports/stock-summary', { from, to }),
    enabled: rangeValid,
  })

  const exportFile = useMutation({
    mutationFn: (format: ExportFormat) =>
      apiDownload('/reports/stock-summary/export', { from, to, format }).then((blob) => ({ blob, format })),
    onSuccess: ({ blob, format }) => {
      saveBlob(blob, `stock-summary_${from}_${to}.${format}`)
      toast.success(format === 'xlsx' ? 'Đã tải file Excel' : 'Đã tải file CSV')
    },
  })

  const rows = report.data ?? []

  return (
    <>
      <PageHeader
        title="Báo cáo biến động tồn kho"
        description="Tồn đầu kỳ + nhập - xuất = tồn cuối kỳ, chỉ tính chứng từ đã xác nhận (theo ngày xác nhận)."
        actions={
          <div className="flex gap-2">
            {(['csv', 'xlsx'] as const).map((format) => (
              <Button
                key={format}
                variant="secondary"
                icon={<Download className="size-4" aria-hidden="true" />}
                loading={exportFile.isPending && exportFile.variables === format}
                disabled={!rangeValid || exportFile.isPending}
                onClick={() => exportFile.mutate(format)}
              >
                {format === 'xlsx' ? 'Xuất Excel' : 'Xuất CSV'}
              </Button>
            ))}
          </div>
        }
      />

      <Card className="overflow-hidden">
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-2 lg:max-w-xl">
          <TextField label="Từ ngày" type="date" value={from} max={to || undefined} onChange={(e) => setFrom(e.target.value)} />
          <TextField
            label="Đến ngày (tính trọn ngày)"
            type="date"
            value={to}
            min={from || undefined}
            onChange={(e) => setTo(e.target.value)}
            error={from && to && from > to ? 'Ngày bắt đầu phải trước ngày kết thúc' : undefined}
          />
        </div>

        {report.isLoading && <Loading />}
        {report.error && <ErrorState message={report.error.message} onRetry={() => report.refetch()} />}
        {report.data && rows.length === 0 && <EmptyState message="Không có sản phẩm nào." />}
        {rows.length > 0 && (
          <div className="overflow-x-auto">
            <table className={TABLE}>
              <thead>
                <tr>
                  <th className={TH}>SKU</th>
                  <th className={TH}>Sản phẩm</th>
                  <th className={TH_NUM}>Tồn đầu</th>
                  <th className={TH_NUM}>Nhập</th>
                  <th className={TH_NUM}>Xuất</th>
                  <th className={TH_NUM}>Tồn cuối</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((r) => (
                  <tr key={r.productId}>
                    <td className={TD_CODE}>{r.sku}</td>
                    <td className={TD}>{r.name}</td>
                    <td className={TD_NUM}>{formatNumber(r.openingQty)}</td>
                    <td className={TD_NUM}>{formatNumber(r.totalIn)}</td>
                    <td className={TD_NUM}>{formatNumber(r.totalOut)}</td>
                    <td className={`${TD_NUM} font-semibold`}>{formatNumber(r.closingQty)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </Card>
    </>
  )
}
