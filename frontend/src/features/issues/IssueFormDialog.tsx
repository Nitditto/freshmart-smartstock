import { zodResolver } from '@hookform/resolvers/zod'
import { Plus, Trash2 } from 'lucide-react'
import { useFieldArray, useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { useProductOptions } from '../../shared/api/lookups'
import { REASON_LABEL, formatNumber } from '../../shared/lib/format'
import { Button } from '../../shared/ui/Button'
import { Dialog } from '../../shared/ui/Dialog'
import { SelectField, TextAreaField, TextField } from '../../shared/ui/Field'
import { useCreateIssue } from './api'

const NUMBER_ERROR = 'Vui lòng nhập số'
const PICK_PRODUCT = 'Chọn sản phẩm'
const REASONS = Object.keys(REASON_LABEL) as Array<keyof typeof REASON_LABEL>

const lineSchema = z.object({
  productId: z.number({ error: PICK_PRODUCT }).int().positive(PICK_PRODUCT),
  quantity: z.number({ error: NUMBER_ERROR }).int('Phải là số nguyên').min(1, 'Số lượng phải lớn hơn 0'),
})

const schema = z
  .object({
    reason: z.enum(['SALE', 'DAMAGED', 'INTERNAL'], { error: 'Vui lòng chọn lý do' }),
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

type IssueForm = z.infer<typeof schema>

const asNumber = (v: string) => (v === '' ? NaN : Number(v))

export function IssueFormDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  return (
    <Dialog open={open} onClose={onClose} title="Tạo phiếu xuất" wide>
      {open && <IssueFormBody onDone={onClose} />}
    </Dialog>
  )
}

function IssueFormBody({ onDone }: { onDone: () => void }) {
  const products = useProductOptions()
  const create = useCreateIssue()
  const {
    register,
    control,
    handleSubmit,
    formState: { errors },
  } = useForm<IssueForm>({
    resolver: zodResolver(schema),
    defaultValues: { reason: 'SALE', note: '', lines: [{ productId: NaN, quantity: 1 }] },
  })
  const { fields, append, remove } = useFieldArray({ control, name: 'lines' })
  const watched = useWatch({ control, name: 'lines' })

  const stockOf = (productId: number | undefined) => products.data?.find((p) => p.id === productId)?.quantityInStock

  const onSubmit = (values: IssueForm) =>
    create.mutate({ reason: values.reason, note: values.note?.trim() || undefined, lines: values.lines }, { onSuccess: onDone })

  return (
    <form onSubmit={handleSubmit(onSubmit)} noValidate className="flex flex-col gap-4">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <SelectField label="Lý do xuất" error={errors.reason?.message} {...register('reason')}>
          {REASONS.map((r) => (
            <option key={r} value={r}>
              {REASON_LABEL[r]}
            </option>
          ))}
        </SelectField>
        <TextAreaField label="Ghi chú" rows={1} error={errors.note?.message} {...register('note')} />
      </div>
      <p className="text-xs text-muted-foreground">
        Phiếu lý do &quot;Hàng hỏng&quot; chỉ ADMIN được xác nhận. Việc kiểm tra đủ tồn xảy ra khi xác nhận, không phải lúc tạo.
      </p>

      <fieldset className="flex flex-col gap-3">
        <legend className="mb-1 text-sm font-medium">Các dòng hàng</legend>
        {fields.map((field, index) => {
          const lineErrors = errors.lines?.[index]
          const line = watched?.[index]
          const stock = stockOf(Number(line?.productId))
          const exceeds = stock !== undefined && Number(line?.quantity) > stock
          return (
            <div key={field.id} className="grid grid-cols-1 items-start gap-3 rounded-md border border-border p-3 sm:grid-cols-[1fr_8rem_auto]">
              <SelectField
                label="Sản phẩm"
                error={lineErrors?.productId?.message}
                {...register(`lines.${index}.productId`, { setValueAs: asNumber })}
              >
                <option value="">Chọn sản phẩm</option>
                {products.data?.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.sku} - {p.name} (tồn {formatNumber(p.quantityInStock)})
                  </option>
                ))}
              </SelectField>
              <TextField
                label="Số lượng"
                type="number"
                min={1}
                inputMode="numeric"
                error={lineErrors?.quantity?.message}
                hint={exceeds ? `Vượt tồn hiện có (${formatNumber(stock)}): xác nhận sẽ bị từ chối` : undefined}
                {...register(`lines.${index}.quantity`, { valueAsNumber: true })}
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
            onClick={() => append({ productId: NaN, quantity: 1 })}
          >
            Thêm dòng
          </Button>
        </div>
      </fieldset>

      <div className="flex justify-end gap-2 border-t border-border pt-3">
        <Button variant="secondary" onClick={onDone} disabled={create.isPending}>
          Hủy
        </Button>
        <Button type="submit" loading={create.isPending}>
          Tạo phiếu nháp
        </Button>
      </div>
    </form>
  )
}
