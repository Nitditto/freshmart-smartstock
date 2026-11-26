import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Product } from '../../api/types'
import { useCategories } from '../../shared/api/lookups'
import { Button } from '../../shared/ui/Button'
import { Dialog } from '../../shared/ui/Dialog'
import { SelectField, TextField } from '../../shared/ui/Field'
import { useSaveProduct } from './api'

const NUMBER_ERROR = 'Vui lòng nhập số'

const schema = z.object({
  sku: z.string().trim().min(1, 'Vui lòng nhập mã SKU').max(50, 'Tối đa 50 ký tự'),
  name: z.string().trim().min(1, 'Vui lòng nhập tên sản phẩm').max(200, 'Tối đa 200 ký tự'),
  categoryId: z.number({ error: 'Vui lòng chọn danh mục' }).int().positive('Vui lòng chọn danh mục'),
  unit: z.string().trim().min(1, 'Vui lòng nhập đơn vị').max(30, 'Tối đa 30 ký tự'),
  price: z.number({ error: NUMBER_ERROR }).min(0, 'Giá phải từ 0 trở lên'),
  minStock: z.number({ error: NUMBER_ERROR }).int('Phải là số nguyên').min(0, 'Tồn tối thiểu phải từ 0 trở lên'),
})
type ProductForm = z.infer<typeof schema>

interface ProductFormDialogProps {
  open: boolean
  product: Product | null
  onClose: () => void
}

export function ProductFormDialog({ open, product, onClose }: ProductFormDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} title={product ? 'Sửa sản phẩm' : 'Thêm sản phẩm'}>
      {/* Mounted only while open, so the form always starts from the product being edited. */}
      {open && <ProductForm product={product} onDone={onClose} />}
    </Dialog>
  )
}

function ProductForm({ product, onDone }: { product: Product | null; onDone: () => void }) {
  const categories = useCategories()
  const save = useSaveProduct(product?.id ?? null)
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<ProductForm>({
    resolver: zodResolver(schema),
    defaultValues: product
      ? {
          sku: product.sku,
          name: product.name,
          categoryId: product.categoryId,
          unit: product.unit,
          price: product.price,
          minStock: product.minStock,
        }
      : { sku: '', name: '', unit: '', minStock: 0, price: 0 },
  })

  const onSubmit = (values: ProductForm) => save.mutate(values, { onSuccess: onDone })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="grid grid-cols-1 gap-4 sm:grid-cols-2">
      <TextField label="Mã SKU" error={errors.sku?.message} {...register('sku')} />
      <TextField label="Tên sản phẩm" error={errors.name?.message} {...register('name')} />
      <SelectField
        label="Danh mục"
        error={errors.categoryId?.message}
        {...register('categoryId', { setValueAs: (v) => (v === '' ? NaN : Number(v)) })}
      >
        <option value="">Chọn danh mục</option>
        {categories.data?.map((c) => (
          <option key={c.id} value={c.id}>
            {c.name}
          </option>
        ))}
      </SelectField>
      <TextField label="Đơn vị tính" error={errors.unit?.message} {...register('unit')} />
      <TextField
        label="Giá bán (VND)"
        type="number"
        inputMode="numeric"
        min={0}
        error={errors.price?.message}
        {...register('price', { valueAsNumber: true })}
      />
      <TextField
        label="Tồn tối thiểu"
        type="number"
        inputMode="numeric"
        min={0}
        hint="Dưới mức này sẽ báo tồn thấp"
        error={errors.minStock?.message}
        {...register('minStock', { valueAsNumber: true })}
      />
      <p className="text-xs text-muted-foreground sm:col-span-2">
        Số lượng tồn không sửa trực tiếp: chỉ thay đổi khi xác nhận phiếu nhập hoặc phiếu xuất.
      </p>
      <div className="flex justify-end gap-2 sm:col-span-2">
        <Button variant="secondary" onClick={onDone} disabled={save.isPending}>
          Hủy
        </Button>
        <Button type="submit" loading={save.isPending}>
          Lưu
        </Button>
      </div>
    </form>
  )
}
