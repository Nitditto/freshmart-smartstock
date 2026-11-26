import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, ClipboardList, Package, PackageMinus } from 'lucide-react'
import type { ComponentType } from 'react'
import type { DocumentStatus, LowStockItem, Page } from '../../api/types'
import { apiGet } from '../../shared/api/client'
import { formatNumber } from '../../shared/lib/format'
import { Badge } from '../../shared/ui/Badge'
import { Card } from '../../shared/ui/Card'
import { PageHeader } from '../../shared/ui/PageHeader'
import { ErrorState, Loading } from '../../shared/ui/States'
import { TABLE, TD, TD_NUM, TH, TH_NUM, TD_CODE } from '../../shared/ui/table'

/** Only the total is needed, so ask for a single row. */
async function countOf(path: string, params: Record<string, string> = {}) {
  const page = await apiGet<Page<unknown>>(path, { size: 1, ...params })
  return page.totalElements
}

function useDashboard() {
  return useQuery({
    queryKey: ['dashboard'],
    queryFn: async () => {
      const draft: { status: DocumentStatus } = { status: 'DRAFT' }
      const [lowStock, products, draftReceipts, draftIssues] = await Promise.all([
        apiGet<LowStockItem[]>('/inventory/low-stock'),
        countOf('/products'),
        countOf('/receipts', draft),
        countOf('/issues', draft),
      ])
      return { lowStock, products, draftReceipts, draftIssues }
    },
  })
}

interface KpiProps {
  label: string
  value: number
  icon: ComponentType<{ className?: string }>
  tone?: 'default' | 'warning'
}

function Kpi({ label, value, icon: Icon, tone = 'default' }: KpiProps) {
  return (
    <Card className="flex items-center gap-3 p-4">
      <span className={tone === 'warning' ? 'rounded-md bg-warning-soft p-2 text-warning' : 'rounded-md bg-muted p-2 text-primary'}>
        <Icon className="size-5" />
      </span>
      <div>
        <p className="text-2xl font-semibold tabular-nums">{formatNumber(value)}</p>
        <p className="text-sm text-muted-foreground">{label}</p>
      </div>
    </Card>
  )
}

export function DashboardPage() {
  const { data, isLoading, error, refetch } = useDashboard()

  return (
    <>
      <PageHeader title="Tổng quan kho" description="Tồn kho, cảnh báo và chứng từ đang chờ xử lý." />
      {isLoading && <Loading />}
      {error && <ErrorState message={error.message} onRetry={() => refetch()} />}
      {data && (
        <>
          <div className="mb-5 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <Kpi label="Sản phẩm đang bán" value={data.products} icon={Package} />
            <Kpi label="Sản phẩm tồn thấp" value={data.lowStock.length} icon={AlertTriangle} tone="warning" />
            <Kpi label="Phiếu nhập nháp" value={data.draftReceipts} icon={ClipboardList} />
            <Kpi label="Phiếu xuất nháp" value={data.draftIssues} icon={PackageMinus} />
          </div>

          <Card className="overflow-hidden">
            <div className="flex items-center justify-between px-4 py-3">
              <h2 className="font-semibold">Cảnh báo tồn kho thấp</h2>
              <span className="text-xs text-muted-foreground">Tồn nhỏ hơn hoặc bằng mức tối thiểu</span>
            </div>
            {data.lowStock.length === 0 ? (
              <p className="px-4 pb-4 text-sm text-muted-foreground">Không có sản phẩm nào sắp hết hàng.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className={TABLE}>
                  <thead>
                    <tr>
                      <th className={TH}>SKU</th>
                      <th className={TH}>Sản phẩm</th>
                      <th className={TH}>Đơn vị</th>
                      <th className={TH_NUM}>Tồn kho</th>
                      <th className={TH_NUM}>Tối thiểu</th>
                      <th className={TH}>Mức độ</th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.lowStock.map((p) => (
                      <tr key={p.productId}>
                        <td className={TD_CODE}>{p.sku}</td>
                        <td className={TD}>{p.name}</td>
                        <td className={TD}>{p.unit}</td>
                        <td className={TD_NUM}>{formatNumber(p.quantityInStock)}</td>
                        <td className={TD_NUM}>{formatNumber(p.minStock)}</td>
                        <td className={TD}>
                          {p.quantityInStock === 0 ? <Badge tone="danger">Hết hàng</Badge> : <Badge tone="warning">Sắp hết</Badge>}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </Card>
        </>
      )}
    </>
  )
}
