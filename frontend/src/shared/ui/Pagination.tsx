import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from './Button'

interface PaginationProps {
  page: number
  totalPages: number
  totalElements: number
  onChange: (page: number) => void
}

/** Server-side paging: `page` is zero-based like the API. */
export function Pagination({ page, totalPages, totalElements, onChange }: PaginationProps) {
  return (
    <div className="flex flex-wrap items-center justify-between gap-2 border-t border-border px-4 py-2 text-sm text-muted-foreground">
      <span>{totalElements} kết quả</span>
      <div className="flex items-center gap-2">
        <Button variant="secondary" aria-label="Trang trước" disabled={page <= 0} onClick={() => onChange(page - 1)}>
          <ChevronLeft className="size-4" aria-hidden="true" />
        </Button>
        <span>
          Trang {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </span>
        <Button
          variant="secondary"
          aria-label="Trang sau"
          disabled={page + 1 >= totalPages}
          onClick={() => onChange(page + 1)}
        >
          <ChevronRight className="size-4" aria-hidden="true" />
        </Button>
      </div>
    </div>
  )
}
