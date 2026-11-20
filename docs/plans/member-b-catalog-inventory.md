# Kế hoạch Thành viên B – Danh mục, Sản phẩm, Nhà cung cấp, Low-stock, Lịch sử tồn kho

> Đọc trước: `00-shared.md`, `docs/api-contract.md` §3 và §5 (mục 6, 8, 10, 15). Slide: **7** (Tồn kho & cảnh báo; C bổ sung phần chặn xuất vượt tồn), cùng chia slide 11.

## 1. Phạm vi

| Endpoint | Quyền | Việc |
|---|---|---|
| GET/POST `/api/categories` | GET: mọi người; POST: ADMIN | ✅ đã có (mẫu) – rà soát + test |
| GET/POST/PUT `/api/suppliers` | GET: mọi người; ghi: ADMIN | `SupplierServiceImpl` |
| GET `/api/products` (phân trang/sắp xếp/từ khóa/`categoryId`) | mọi người | `ProductServiceImpl.search` |
| GET `/api/products/{id}` | mọi người | `get` (cả sản phẩm ẩn → `active=false`) |
| POST/PUT/DELETE `/api/products` | ADMIN | `create/update/hide` |
| GET `/api/inventory/low-stock` | mọi người | `InventoryServiceImpl.lowStock` |
| GET `/api/products/{id}/history` | mọi người | `InventoryServiceImpl.history` |

Không thuộc B: phiếu nhập (D), phiếu xuất + `newlyLowStock` (C), `stock-summary` (E), seed (A).

## 2. Bạn có sẵn gì
`ProductRepository` (`existsBySkuIgnoreCase[AndIdNot]`, `search`, `findLowStock`), `SupplierRepository`, `CategoryRepository`, DTO `ProductRequest/Response`, `SupplierRequest/Response`, `LowStockItem`, `ProductHistoryItem`, `PageResponse`, **`PageableGuard`**, `TestData` (tạo phiếu CONFIRMED trực tiếp để thử lịch sử – **không cần chờ C/D**).
`ProductRepository.search` đang trả `Page<Product>`; `ProductResponse.from` đọc `category` (LAZY) → xem 5.3.

## 3. Definition of Done
- [ ] Mọi endpoint đúng mã trong `api-contract.md`, có `@Operation`, mô tả tham số `keyword/categoryId/page/size/sort`.
- [ ] SKU trùng → 409; `price<0`/`minStock<0` → 400; category không có → 404; **không có API sửa trực tiếp tồn kho**.
- [ ] Ẩn sản phẩm = `active=false` (idempotent); sản phẩm ẩn **không** hiện trong list/low-stock; `GET /{id}` vẫn trả 200.
- [ ] Sort whitelist `id,sku,name,price,quantityInStock`; `size ≤ 100` (dùng `PageableGuard`).
- [ ] Low-stock đúng `≤`; lịch sử chỉ chứng từ **CONFIRMED**, mới nhất trước, theo `confirmedAt`.
- [ ] Coverage phần B ≥ 70%; slide 7 (phần B); ≥ 3 PR merged.

## 4. File B sở hữu
`service/impl/{Product,Supplier,Inventory,Category}ServiceImpl`, `repository/{Product,Supplier,Category}Repository`, **`repository/StockReceiptDetailRepository` và `StockIssueDetailRepository`** (query lịch sử), `controller/{Product,Supplier,Category,Inventory}Controller` (trừ `stock-summary`), test tương ứng.
> E làm báo cáo trong `ReportRepository` **riêng** → hai người không sửa chung file.

## 5. Việc cần làm – từng PR

### PR #1 `feat: supplier api` (20–21/11)
```java
@Service @RequiredArgsConstructor
public class SupplierServiceImpl implements SupplierService {
    private final SupplierRepository repo;
    @Transactional(readOnly = true) public List<SupplierResponse> findAll() { return repo.findAll(Sort.by("name")).stream().map(SupplierResponse::from).toList(); }
    @Transactional public SupplierResponse create(SupplierRequest r) { Supplier s = new Supplier(); apply(s, r); return SupplierResponse.from(repo.save(s)); }
    @Transactional public SupplierResponse update(Long id, SupplierRequest r) {
        Supplier s = repo.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy nhà cung cấp " + id));
        apply(s, r); return SupplierResponse.from(s);
    }
    private static void apply(Supplier s, SupplierRequest r) { s.setName(r.name().trim()); s.setPhone(trimToNull(r.phone())); s.setAddress(trimToNull(r.address())); }
}
```
**Test:** tạo OK (đã trim); cập nhật id không có → 404; `findAll` sắp theo tên.

### PR #2 `feat: product crud` (21–22/11)
- `create`: `existsBySkuIgnoreCase(sku.trim())` → 409; category tồn tại (404); `quantityInStock = 0`, `active = true`.
- `update`: `existsBySkuIgnoreCaseAndIdNot` → 409; **không đụng `quantityInStock`**; `404` nếu không có.
- `hide`: `active=false`, idempotent (ẩn lần hai vẫn 200); `get`: trả cả sản phẩm ẩn.
**Test:** SKU trùng khác hoa/thường → 409; update giữ nguyên SKU của chính nó → OK; update **không** thay đổi tồn; ẩn hai lần → cả hai 200.

### PR #3 `feat: product search paging sorting` (22/11)
```java
private static final Set<String> SORTABLE = Set.of("id", "sku", "name", "price", "quantityInStock");

@Transactional(readOnly = true)
public PageResponse<ProductResponse> search(String keyword, Long categoryId, Pageable pageable) {
    Pageable safe = PageableGuard.sanitize(pageable, SORTABLE);               // 400 nếu sort sai / size > 100
    String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
    return PageResponse.from(productRepository.search(kw, categoryId, true, safe), ProductResponse::from);
}
```
Tránh `LazyInitializationException` + N+1: thêm `@EntityGraph(attributePaths = "category")` lên `search` (đếm trang bằng `countQuery` riêng nếu Hibernate cảnh báo), hoặc dựa vào `@Transactional(readOnly = true)` và chấp nhận N+1 với 200 sản phẩm – ghi lý do vào PR.
**Test:** keyword khớp tên và SKU, không phân biệt hoa/thường; lọc category; trang 0/1 + `totalElements`; sort `price,desc`; sort `passwordHash` → 400; `size=1000` → 400; sản phẩm ẩn không hiện.

### PR #4 `feat: low-stock endpoint` (23/11 → v0.1.0)
`lowStock()` → `productRepository.findLowStock()` → `LowStockItem::from`. **Test biên:** tồn **=** MinStock vẫn thấp; tồn = MinStock + 1 không thấp; sản phẩm ẩn không xuất hiện; sắp theo tồn tăng dần.
> `newlyLowStock` của phiếu xuất do **C** tính trong `IssueServiceImpl` bằng `Product::isLowStock` → B **không** cần cung cấp gì.

### PR #5 `feat: product history` (24–25/11 – dùng `TestData`, không chờ C/D)
Thêm vào hai `*DetailRepository` (file của B):
```java
// StockReceiptDetailRepository
@Query("""
   select d from StockReceiptDetail d join fetch d.receipt r
   where d.product.id = :productId and r.status = com.freshmart.smartstock.domain.enums.DocumentStatus.CONFIRMED
   order by r.confirmedAt desc, d.id desc
   """)
List<StockReceiptDetail> findConfirmedByProductId(@Param("productId") Long productId);
// StockIssueDetailRepository: tương tự, join fetch d.issue i
```
```java
@Transactional(readOnly = true)
public List<ProductHistoryItem> history(Long productId) {
    if (!productRepository.existsById(productId)) throw ApiException.notFound("Không tìm thấy sản phẩm " + productId); // kể cả đã ẩn
    List<ProductHistoryItem> out = new ArrayList<>();
    receiptDetailRepository.findConfirmedByProductId(productId).forEach(d -> out.add(new ProductHistoryItem(
            d.getReceipt().getConfirmedAt(), "IN", "RECEIPT", d.getReceipt().getCode(), d.getQuantity())));
    issueDetailRepository.findConfirmedByProductId(productId).forEach(d -> out.add(new ProductHistoryItem(
            d.getIssue().getConfirmedAt(), "OUT", "ISSUE", d.getIssue().getCode(), d.getQuantity())));
    out.sort(Comparator.comparing(ProductHistoryItem::occurredAt).reversed());
    return out;
}
```
(Phương án thay thế: native `UNION ALL` – so sánh và ghi lý do chọn vào PR.)
**Test (`TestData`):** sản phẩm có 2 phiếu nhập + 1 phiếu xuất CONFIRMED + 1 phiếu DRAFT + 1 CANCELLED → chỉ 3 dòng, đúng thứ tự thời gian giảm dần, đúng `direction/documentType`; sản phẩm ẩn vẫn có lịch sử; id không có → 404.

### PR #6 `test: catalog integration` (26/11)
`AbstractIntegrationTest`: STAFF `POST /api/products` → 403; ADMIN → 201; SKU trùng → 409; list có phân trang; low-stock; history. Rà Swagger (mô tả tham số).

## 6. Lịch
| Ngày | Việc |
|---|---|
| 20–21/11 | PR #1 |
| 21–22/11 | PR #2, #3 |
| 23/11 | PR #4 → v0.1.0 |
| 24–26/11 | PR #5, #6 |
| 27/11 | Tăng coverage, sửa lỗi; slide 7 |
| 28/11 | Freeze; chuẩn bị demo bước 7 |
| 29/11 | Tập dượt |

## 7. Phụ thuộc
**Không phụ thuộc ai.** Lịch sử thử bằng `TestData`. Cung cấp: quy ước `confirmedAt` cho E; cùng thống nhất sort whitelist (`PageableGuard`).

## 8. Câu hỏi "vì sao"
1. Vì sao ẩn (soft delete) thay vì xóa? Ẩn rồi còn thêm vào phiếu mới được không? (không → 400.)
2. Vì sao không có API sửa tồn trực tiếp?
3. Vì sao whitelist sort + giới hạn `size`? (bảo mật, hiệu năng.)
4. Phân trang `Pageable` hoạt động thế nào; `LazyInitializationException` và N+1 là gì, xử lý ra sao?
5. Low-stock: vì sao `≤` chứ không `<`?
6. Lịch sử lấy từ đâu, vì sao chỉ CONFIRMED, vì sao `confirmedAt`?
7. Vì sao `GET /api/products/{id}/history` nằm dưới `/products`? (tài nguyên con.)

## 9. Rủi ro
| Rủi ro | Giảm |
|---|---|
| `LazyInitializationException` khi map DTO | `@Transactional(readOnly=true)` hoặc `@EntityGraph`, có test |
| Tham số `null` trong JPQL | Đã dùng `:x is null or …`; nếu lỗi → `Specification` |
| Lịch sử sai thứ tự | Sắp sau khi ghép; test nhiều thời điểm |
