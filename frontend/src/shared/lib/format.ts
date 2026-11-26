const TIME_ZONE = 'Asia/Ho_Chi_Minh'

const vnd = new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND', maximumFractionDigits: 0 })
const plainNumber = new Intl.NumberFormat('vi-VN')
const dateTime = new Intl.DateTimeFormat('vi-VN', {
  timeZone: TIME_ZONE,
  day: '2-digit',
  month: '2-digit',
  year: 'numeric',
  hour: '2-digit',
  minute: '2-digit',
})
const dateOnly = new Intl.DateTimeFormat('en-CA', { timeZone: TIME_ZONE })

export function formatVnd(value: number | null | undefined): string {
  return value == null ? '-' : vnd.format(value)
}

export function formatNumber(value: number | null | undefined): string {
  return value == null ? '-' : plainNumber.format(value)
}

/** The server sends UTC instants; the business time zone is Asia/Ho_Chi_Minh. */
export function formatDateTime(value: string | null | undefined): string {
  return value ? dateTime.format(new Date(value)) : '-'
}

/** Calendar date (yyyy-MM-dd) in the business time zone, e.g. for date inputs and report ranges. */
export function businessDate(date: Date): string {
  return dateOnly.format(date)
}

export function firstDayOfMonth(date: Date): string {
  return `${businessDate(date).slice(0, 8)}01`
}

export const STATUS_LABEL = {
  DRAFT: 'Nháp',
  CONFIRMED: 'Đã xác nhận',
  CANCELLED: 'Đã hủy',
} as const

export const REASON_LABEL = {
  SALE: 'Bán hàng',
  DAMAGED: 'Hàng hỏng',
  INTERNAL: 'Dùng nội bộ',
} as const
