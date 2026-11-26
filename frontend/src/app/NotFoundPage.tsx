import { Link } from 'react-router-dom'
import { EmptyState } from '../shared/ui/States'

export function NotFoundPage() {
  return (
    <EmptyState
      message="Không tìm thấy trang này."
      action={
        <Link to="/" className="text-sm font-medium text-accent underline">
          Về trang tổng quan
        </Link>
      }
    />
  )
}
