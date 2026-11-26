import { ArrowDown, ArrowUp, EyeOff, History, Pencil, Plus, Search } from 'lucide-react'
import { useState } from 'react'
import type { Product } from '../../api/types'
import { useCategories } from '../../shared/api/lookups'
import { useAuth } from '../../shared/auth/AuthContext'
import { formatNumber, formatVnd } from '../../shared/lib/format'
import { useDebounced } from '../../shared/lib/useDebounced'
import { Badge } from '../../shared/ui/Badge'
import { Button } from '../../shared/ui/Button'
import { Card } from '../../shared/ui/Card'
import { ConfirmDialog } from '../../shared/ui/ConfirmDialog'
import { TextField, SelectField } from '../../shared/ui/Field'
import { PageHeader } from '../../shared/ui/PageHeader'
import { Pagination } from '../../shared/ui/Pagination'
import { EmptyState, ErrorState, Loading } from '../../shared/ui/States'
import { TABLE, TD, TD_NUM, TH, TH_NUM, TD_CODE } from '../../shared/ui/table'
import { useHideProduct, useProducts, type ProductSortField } from './api'
import { ProductFormDialog } from './ProductFormDialog'
import { ProductHistoryDialog } from './ProductHistoryDialog'

const PAGE_SIZE = 10

interface SortHeaderProps {
  field: ProductSortField
  label: string
  active: ProductSortField
  dir: 'asc' | 'desc'
  numeric?: boolean
  onSort: (field: ProductSortField) => void
}

function SortHeader({ field, label, active, dir, numeric, onSort }: SortHeaderProps) {
  const isActive = active === field
  return (
    <th className={numeric ? TH_NUM : TH} aria-sort={isActive ? (dir === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button type="button" onClick={() => onSort(field)} className="inline-flex items-center gap-1 uppercase">
        {label}
        {isActive && (dir === 'asc' ? <ArrowUp className="size-3" aria-hidden="true" /> : <ArrowDown className="size-3" aria-hidden="true" />)}
      </button>
    </th>
  )
}

export function ProductsPage() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const categories = useCategories()

  const [keywordInput, setKeywordInput] = useState('')
  const keyword = useDebounced(keywordInput.trim())
  const [categoryId, setCategoryId] = useState<number | null>(null)
  const [page, setPage] = useState(0)
  const [sortField, setSortField] = useState<ProductSortField>('id')
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('asc')

  const [formOpen, setFormOpen] = useState(false)
  const [editing, setEditing] = useState<Product | null>(null)
  const [historyOf, setHistoryOf] = useState<Product | null>(null)
  const [toHide, setToHide] = useState<Product | null>(null)

  const products = useProducts({ keyword, categoryId, page, size: PAGE_SIZE, sortField, sortDir })
  const hide = useHideProduct()

  const onSort = (field: ProductSortField) => {
    if (field === sortField) {
      setSortDir((dir) => (dir === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortField(field)
      setSortDir('asc')
    }
    setPage(0)
  }

  const openForm = (product: Product | null) => {
    setEditing(product)
    setFormOpen(true)
  }

  const data = products.data

  return (
    <>
      <PageHeader
        title="Sản phẩm"
        description="Tra cứu theo tên hoặc SKU, lọc theo danh mục, xem lịch sử nhập/xuất."
        actions={
          isAdmin && (
            <Button icon={<Plus className="size-4" aria-hidden="true" />} onClick={() => openForm(null)}>
              Thêm sản phẩm
            </Button>
          )
        }
      />

      <Card className="overflow-hidden">
        <div className="grid grid-cols-1 gap-3 p-4 sm:grid-cols-[1fr_16rem]">
          <div className="relative">
            <Search className="pointer-events-none absolute top-9 left-3 size-4 text-muted-foreground" aria-hidden="true" />
            <TextField
              label="Tìm kiếm"
              placeholder="Tên hoặc mã SKU"
              value={keywordInput}
              className="pl-9"
              onChange={(e) => {
                setKeywordInput(e.target.value)
                setPage(0)
              }}
            />
          </div>
          <SelectField
            label="Danh mục"
            value={categoryId ?? ''}
            onChange={(e) => {
              setCategoryId(e.target.value ? Number(e.target.value) : null)
              setPage(0)
            }}
          >
            <option value="">Tất cả danh mục</option>
            {categories.data?.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </SelectField>
        </div>

        {products.isLoading && <Loading />}
        {products.error && <ErrorState message={products.error.message} onRetry={() => products.refetch()} />}
        {data && data.items.length === 0 && <EmptyState message="Không có sản phẩm nào khớp bộ lọc." />}
        {data && data.items.length > 0 && (
          <>
            <div className="overflow-x-auto">
              <table className={TABLE}>
                <thead>
                  <tr>
                    <SortHeader field="sku" label="SKU" active={sortField} dir={sortDir} onSort={onSort} />
                    <SortHeader field="name" label="Tên sản phẩm" active={sortField} dir={sortDir} onSort={onSort} />
                    <th className={TH}>Danh mục</th>
                    <th className={TH}>Đơn vị</th>
                    <SortHeader field="price" label="Giá" active={sortField} dir={sortDir} numeric onSort={onSort} />
                    <SortHeader field="quantityInStock" label="Tồn kho" active={sortField} dir={sortDir} numeric onSort={onSort} />
                    <th className={TH_NUM}>Tối thiểu</th>
                    <th className={TH}>
                      <span className="sr-only">Thao tác</span>
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {data.items.map((p) => (
                    <tr key={p.id}>
                      <td className={TD_CODE}>{p.sku}</td>
                      <td className={TD}>{p.name}</td>
                      <td className={TD}>{p.categoryName}</td>
                      <td className={TD}>{p.unit}</td>
                      <td className={TD_NUM}>{formatVnd(p.price)}</td>
                      <td className={TD_NUM}>
                        <span className="inline-flex items-center gap-2">
                          {p.quantityInStock <= p.minStock && <Badge tone={p.quantityInStock === 0 ? 'danger' : 'warning'}>Tồn thấp</Badge>}
                          {formatNumber(p.quantityInStock)}
                        </span>
                      </td>
                      <td className={TD_NUM}>{formatNumber(p.minStock)}</td>
                      <td className={TD}>
                        <div className="flex justify-end gap-1">
                          <Button variant="ghost" aria-label={`Lịch sử của ${p.name}`} onClick={() => setHistoryOf(p)}>
                            <History className="size-4" aria-hidden="true" />
                          </Button>
                          {isAdmin && (
                            <>
                              <Button variant="ghost" aria-label={`Sửa ${p.name}`} onClick={() => openForm(p)}>
                                <Pencil className="size-4" aria-hidden="true" />
                              </Button>
                              <Button variant="ghost" aria-label={`Ẩn ${p.name}`} onClick={() => setToHide(p)}>
                                <EyeOff className="size-4" aria-hidden="true" />
                              </Button>
                            </>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination page={data.page} totalPages={data.totalPages} totalElements={data.totalElements} onChange={setPage} />
          </>
        )}
      </Card>

      <ProductFormDialog open={formOpen} product={editing} onClose={() => setFormOpen(false)} />
      <ProductHistoryDialog product={historyOf} onClose={() => setHistoryOf(null)} />
      <ConfirmDialog
        open={toHide !== null}
        title="Ẩn sản phẩm"
        message={`Ẩn "${toHide?.name ?? ''}"? Sản phẩm sẽ không còn trong danh sách và không thêm được vào chứng từ mới, nhưng lịch sử vẫn giữ nguyên.`}
        confirmLabel="Ẩn sản phẩm"
        loading={hide.isPending}
        onCancel={() => setToHide(null)}
        onConfirm={() => toHide && hide.mutate(toHide.id, { onSuccess: () => setToHide(null) })}
      />
    </>
  )
}
