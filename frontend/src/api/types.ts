import type { components } from './schema'

type S = components['schemas']

/**
 * springdoc marks every response field optional, so the generated types force `?.` everywhere.
 * These aliases make the fields the API always sends required and keep the genuinely nullable ones optional.
 * They are derived from `schema.ts` (regenerate with `npm run gen:api`), so a renamed field is a compile error.
 */
export type Role = 'ADMIN' | 'STAFF'
export type DocumentStatus = 'DRAFT' | 'CONFIRMED' | 'CANCELLED'
export type IssueReason = 'SALE' | 'DAMAGED' | 'INTERNAL'

export type User = Required<S['UserResponse']>
export type LoginResult = Omit<Required<S['LoginResponse']>, 'user'> & { user: User }
export type Category = Required<S['CategoryResponse']>
export type Supplier = Omit<Required<S['SupplierResponse']>, 'phone' | 'address'> & {
  phone?: string | null
  address?: string | null
}
export type Product = Required<S['ProductResponse']>
export type LowStockItem = Required<S['LowStockItem']>
export type StockSummaryItem = Required<S['StockSummaryItem']>
export type ProductHistoryItem = Required<S['ProductHistoryItem']>

export type ReceiptLine = Required<S['ReceiptLineResponse']>
export type Receipt = Omit<Required<S['ReceiptResponse']>, 'note' | 'confirmedAt' | 'lines'> & {
  note?: string | null
  confirmedAt?: string | null
  lines: ReceiptLine[]
}

export type IssueLine = Required<S['IssueLineResponse']>
export type Issue = Omit<Required<S['IssueResponse']>, 'note' | 'confirmedAt' | 'lines'> & {
  note?: string | null
  confirmedAt?: string | null
  lines: IssueLine[]
}
export type IssueConfirmResult = { issue: Issue; newlyLowStock: LowStockItem[] }

export type ParsedLine = Omit<Required<S['ParsedReceiptLine']>, 'unitPrice'> & { unitPrice: number | null }
export type UnmatchedLine = Required<S['UnmatchedLine']>
export type ParseResult = Omit<Required<S['ParseReceiptResponse']>, 'suggestedSupplierId' | 'lines'> & {
  suggestedSupplierId: number | null
  lines: ParsedLine[]
}

/** Body of the 409 returned when confirming an issue that exceeds stock (not part of the OpenAPI schema). */
export interface ShortageItem {
  productId: number
  sku: string
  name: string
  requested: number
  available: number
}

export interface Page<T> {
  items: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ProductRequest = S['ProductRequest']
export type ReceiptRequest = S['ReceiptRequest']
export type IssueRequest = S['IssueRequest']
