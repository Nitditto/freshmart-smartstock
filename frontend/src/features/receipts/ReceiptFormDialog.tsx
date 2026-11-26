import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { useProductOptions, useSuppliers } from '../../shared/api/lookups'
import { formatVnd } from '../../shared/lib/format'
import { Button } from '../../shared/ui/Button'
import { Dialog } from '../../shared/ui/Dialog'
import { SelectField, TextAreaField, TextField } from '../../shared/ui/Field'
import { useCreateReceipt } from './api'

const NUMBER_ERROR = 'Vui lòng nhập số'
const PICK_PRODUCT = 'Chọn sản phẩm'

const lineSchema = z.object({
  productId: z.number({ error: PICK_PRODUCT }).int().positive(PICK_PRODUCT),
  quantity: z.number({ error: NUMBER_ERROR }).int('Phải là số nguyên').min(1, 'Số lượng phải lớn hơn 0'),
  unitPrice: z.number({ error: NUMBER_ERROR }).min(0, 'Đơn giá phải từ 0 trở lên'),
})

const schema = z
  .object({
    supplierId: z.number({ error: 'Vui lòng chọn nhà cung cấp' }).int().positive('Vui lòng chọn nhà cung cấp'),
    note: z.string().max(500, 'Tối đa 500 ký tự').optional(),
    lines: z.array(lineSchema).min(1, 'Phiếu phải có ít nhất 1 dòng'),
  })
  .superRefine((value, ctx) => {
    const seen = new Set<number>()
    value.lines.forEach((line, index) => {
      if (seen.has(line.productId)) {
        ctx.addIssue({ code: 'custom', path: ['lines', index, 'productId'], message: 'Sản phẩm đã có ở dòng khác' })
      }
      seen.add(line.productId)
    })
  })

type ReceiptForm = z.infer<typeof schema>

export interface ReceiptInitial {
  supplierId?: number
  note?: string
  lines: Array<{ productId: number; quantity: number; unitPrice: number }>
}

const EMPTY_INITIAL: ReceiptInitial = { lines: [{ productId: NaN, quantity: 1, unitPrice: 0 }] }
const asNumber = (v: string) => (v === '' ? NaN : Number(v))

interface ReceiptFormDialogProps {
  open: boolean
  initial?: ReceiptInitial
  onClose: () => void
}

export function ReceiptFormDialog({ open, initial, onClose }: ReceiptFormDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} title="Tạo phiếu nhập" wide>
      {open && <ReceiptFormBody initial={initial ?? EMPTY_INITIAL} onDone={onClose} />}
    </Dialog>
  )
}

function ReceiptFormBody({ initial, onDone }: { initial: ReceiptInitial; onDone: () => void }) {
  const suppliers = useSuppliers()
  const products = useProductOptions()
  const create = useCreateReceipt()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<ReceiptForm>({
    resolver: zodResolver(schema),
    defaultValues: {
      supplierId: initial.supplierId ?? NaN,
      note: initial.note ?? '',
      lines: initial.lines.length > 0 ? initial.lines : EMPTY_INITIAL.lines,
    },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'lines' })
  const watched = useWatch({ control, name: 'lines' })
  const total = (watched ?? []).reduce((sum, l) => sum + (Number(l?.quantity) || 0) * (Number(l?.unitPrice) || 0), 0)

  const onSubmit = (values: ReceiptForm) =>
    create.mutate(
      { supplierId: values.supplierId, note: values.note?.trim() || undefined, lines: values.lines },
      { onSuccess: onDone },
    )

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField label="Nhà cung cấp" error={errors.supplierId?.message} {...register('supplierId', { setValueAs: asNumber })}>
          <option value="">Chọn nhà cung cấp</option>
          {suppliers.data?.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name}
            </option>
          ))}
        </SelectField>
        <TextAreaField label="Ghi chú" rows={1} error={errors.note?.message} {...register('note')} />
      </div>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">Các dòng hàng</legend>
        {fields.map((field, index) => {
          const lineErrors = errors.lines?.[index]
          return (
            <div key={field.id} className="grid grid-cols-1 items-start gap-3 rounded-md border border-border p-3 sm:grid-cols-[1fr_7rem_9rem_auto]">
              <SelectField
                label="Sản phẩm"
                error={lineErrors?.productId?.message}
                {...register(`lines.${index}.productId`, { setValueAs: asNumber })}
              >
                <option value="">Chọn sản phẩm</option>
                {products.data?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.sku} - {p.name}
                  </option>
                ))}
              </SelectField>
              <TextField
                label="Số lượng"
                type="number"
                min={1}
                inputMode="numeric"
                error={lineErrors?.quantity?.message}
                {...register(`lines.${index}.quantity`, { valueAsNumber: true })}
              />
              <TextField
                label="Đơn giá nhập"
                type="number"
                min={0}
                inputMode="numeric"
                error={lineErrors?.unitPrice?.message}
                {...register(`lines.${index}.unitPrice`, { valueAsNumber: true })}
              />
              <Button
                variant="ghost"
                className="sm:mt-6"
                aria-label={`Xóa dòng ${index + 1}`}
                disabled={fields.length === 1}
                onClick={() => remove(index)}
              >
                <Trash2 className="size-4" aria-hidden="true" />
              </Button>
            </div>
          )
        })}
        {errors.lines?.message && (
          <p role="alert" className="text-xs text-danger">
            {errors.lines.message}
          </p>
        )}
        <div>
          <Button
            variant="secondary"
            icon={<Plus className="size-4" aria-hidden="true" />}
            onClick={() => append({ productId: NaN, quantity: 1, unitPrice: 0 })}
          >
            Thêm dòng
          </Button>
        </div>
      </fieldset>

      <div className="flex flex-wrap items-center justify-between gap-3 border-t border-border pt-3">
        <p className="text-sm">
          Tổng giá trị: <strong className="tabular-nums">{formatVnd(total)}</strong>
        </p>
        <div className="flex gap-2">
          <Button variant="secondary" onClick={onDone} disabled={create.isPending}>
            Hủy
          </Button>
          <Button type="submit" loading={create.isPending}>
            Tạo phiếu nháp
          </Button>
        </div>
      </div>
    </form>
  )
}
