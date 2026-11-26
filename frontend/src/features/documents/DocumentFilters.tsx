import type { DocumentStatus } from '../../api/types'
import { STATUS_LABEL } from '../../shared/lib/format'
import { Button } from '../../shared/ui/Button'
import { SelectField, TextField } from '../../shared/ui/Field'
import { EMPTY_DOCUMENT_QUERY, type DocumentQuery } from './api'

const STATUSES = Object.keys(STATUS_LABEL) as DocumentStatus[]

interface DocumentFiltersProps {
  query: DocumentQuery
  onChange: (query: DocumentQuery) => void
}

/** Status and creation-date filters; changing any filter goes back to the first page. */
export function DocumentFilters({ query, onChange }: DocumentFiltersProps) {
  const update = (patch: Partial<DocumentQuery>) => onChange({ ...query, ...patch, page: 0 })
  const hasFilter = query.status !== '' || query.from !== '' || query.to !== ''

  return (
    <div className="grid grid-cols-1 items-end gap-3 p-4 sm:grid-cols-2 lg:grid-cols-[14rem_1fr_1fr_auto]">
      <SelectField
        label="Trạng thái"
        value={query.status}
        onChange={(e) => update({ status: e.target.value as DocumentStatus | '' })}
      >
        <option value="">Tất cả</option>
        {STATUSES.map((s) => (
          <option key={s} value={s}>
            {STATUS_LABEL[s]}
          </option>
        ))}
      </SelectField>
      <TextField
        label="Từ ngày tạo"
        type="date"
        value={query.from}
        max={query.to || undefined}
        onChange={(e) => update({ from: e.target.value })}
      />
      <TextField
        label="Đến ngày tạo"
        type="date"
        value={query.to}
        min={query.from || undefined}
        onChange={(e) => update({ to: e.target.value })}
      />
      <Button variant="secondary" disabled={!hasFilter} onClick={() => onChange(EMPTY_DOCUMENT_QUERY)}>
        Xóa lọc
      </Button>
    </div>
  )
}
