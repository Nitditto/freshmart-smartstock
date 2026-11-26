import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import type { Page, Product, ProductHistoryItem, ProductRequest } from '../../api/types'
import { apiDelete, apiGet, apiPost, apiPut } from '../../shared/api/client'
import { toast } from '../../shared/ui/toast'

/** Sort fields the backend accepts for products (ProductServiceImpl whitelist). */
export type ProductSortField = 'id' | 'sku' | 'name' | 'price' | 'quantityInStock'

export interface ProductQuery {
  keyword: string
  categoryId: number | null
  page: number
  size: number
  sortField: ProductSortField
  sortDir: 'asc' | 'desc'
}

export function useProducts(query: ProductQuery) {
  return useQuery({
    queryKey: ['products', 'list', query],
    queryFn: () =>
      apiGet<Page<Product>>('/products', {
        keyword: query.keyword,
        categoryId: query.categoryId,
        page: query.page,
        size: query.size,
        sort: `${query.sortField},${query.sortDir}`,
      }),
    placeholderData: keepPreviousData,
  })
}

export function useProductHistory(productId: number | null) {
  return useQuery({
    queryKey: ['products', 'history', productId],
    queryFn: () => apiGet<ProductHistoryItem[]>(`/products/${productId}/history`),
    enabled: productId !== null,
  })
}

/** Stock changes elsewhere (documents) also stale these, so every product-related cache is invalidated together. */
function useInvalidateProducts() {
  const queryClient = useQueryClient()
  return () => Promise.all([
    queryClient.invalidateQueries({ queryKey: ['products'] }),
    queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
  ])
}

export function useSaveProduct(editingId: number | null) {
  const invalidate = useInvalidateProducts()
  return useMutation({
    mutationFn: (body: ProductRequest) =>
      editingId === null ? apiPost<Product>('/products', body) : apiPut<Product>(`/products/${editingId}`, body),
    onSuccess: async () => {
      await invalidate()
      toast.success(editingId === null ? 'Đã tạo sản phẩm' : 'Đã cập nhật sản phẩm')
    },
  })
}

export function useHideProduct() {
  const invalidate = useInvalidateProducts()
  return useMutation({
    mutationFn: (id: number) => apiDelete(`/products/${id}`),
    onSuccess: async () => {
      await invalidate()
      toast.success('Đã ẩn sản phẩm')
    },
  })
}
