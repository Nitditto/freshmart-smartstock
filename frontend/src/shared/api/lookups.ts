import { useQuery } from '@tanstack/react-query'
import type { Category, Page, Product, Supplier } from '../../api/types'
import { apiGet } from './client'

/** Largest page the API allows (PageableGuard); enough for the ~200-SKU FreshMart catalogue. */
const MAX_PAGE_SIZE = 100
const FIVE_MINUTES_MS = 5 * 60 * 1000

export function useCategories() {
  return useQuery({
    queryKey: ['categories'],
    queryFn: () => apiGet<Category[]>('/categories'),
    staleTime: FIVE_MINUTES_MS,
  })
}

export function useSuppliers() {
  return useQuery({
    queryKey: ['suppliers'],
    queryFn: () => apiGet<Supplier[]>('/suppliers'),
    staleTime: FIVE_MINUTES_MS,
  })
}

/** Active products for the line pickers of receipts and issues. */
export function useProductOptions() {
  return useQuery({
    queryKey: ['products', 'options'],
    queryFn: async () => (await apiGet<Page<Product>>('/products', { size: MAX_PAGE_SIZE, sort: 'name,asc' })).items,
    staleTime: FIVE_MINUTES_MS,
  })
}
