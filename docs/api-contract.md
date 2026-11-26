# API Contract (nguồn sự thật chung của cả nhóm)

> Mọi thay đổi ở file này phải được cả nhóm đồng ý (qua PR có >= 1 review). Swagger trong code phải khớp với file này.

## 1. Quy ước chung

**Envelope** (mọi response, kể cả lỗi):
```json
{ "status": 200, "message": "success", "data": { } }
```
- `status` luôn bằng HTTP status code thực tế. `data` = `null` khi không có nội dung.
- Lỗi validation (400): `data` = `{ "tenTruong": "thông báo lỗi" }`.
- Lỗi 409 thiếu tồn kho: `data` = danh sách `ShortageItem` (xem mục 4).

**Mã trạng thái dùng trong dự án**

| Mã | Khi nào |
|----|---------|
| 200 | Thành công (GET/PUT/PATCH/DELETE) |
| 201 | Tạo mới thành công (POST tạo tài nguyên) |
| 400 | Dữ liệu sai (validation, JSON hỏng, LLM trả JSON không hợp lệ) |
| 401 | Thiếu/sai/hết hạn token; sai email/mật khẩu; tài khoản bị khóa (cùng một thông báo) |
| 403 | Token hợp lệ nhưng không đủ quyền (sai role, hoặc STAFF đụng chứng từ của người khác) |
| 404 | Không tìm thấy tài nguyên |
| 409 | Xung đột nghiệp vụ: trùng SKU/email/tên, đổi trạng thái chứng từ đã CONFIRMED/CANCELLED, vượt tồn kho |
| 503 | Dịch vụ AI không khả dụng (chỉ `/api/receipts/parse`) |

**Đặt tên / REST**: danh từ số nhiều (`/api/products`), động từ HTTP đúng nghĩa, hành động trạng thái dùng `PATCH /{id}/confirm|cancel`. Request/response luôn là DTO (record), **không bao giờ** trả thẳng entity. Không trả `passwordHash`.

**Phân trang**: `?page=0&size=20&sort=name,asc` -> `data = { items, page, size, totalElements, totalPages }`.

**Ngày/giờ**: `Instant` ISO-8601 UTC trong JSON (`2026-11-20T03:00:00Z`); tham số ngày `from`/`to` dạng `yyyy-MM-dd` (theo múi giờ `Asia/Ho_Chi_Minh`, `to` tính cả ngày đó).

**Xác thực**: `Authorization: Bearer <JWT>`; token 60 phút; claims `sub`=UserId, `email`, `role`. UserId luôn lấy từ token.

## 2. Ma trận phân quyền

| Chức năng | ADMIN | STAFF |
|-----------|:-----:|:-----:|
| Đăng nhập, xem `/auth/me`, đổi mật khẩu | Có | Có |
| Quản lý tài khoản (`/api/users`) | Có | Không (403) |
| Xem danh mục / sản phẩm / nhà cung cấp / tồn kho / low-stock / lịch sử | Có | Có |
| Tạo/sửa/ẩn danh mục, sản phẩm, nhà cung cấp | Có | Không (403) |
| Tạo phiếu nhập / xuất (DRAFT) | Có | Có |
| Sửa / xác nhận / hủy phiếu | Có (mọi phiếu) | Chỉ phiếu **do mình tạo** (403 nếu khác) |
| Xác nhận phiếu xuất lý do `DAMAGED` | Có | Không (403) |
| Xem phiếu (GET) | Có | Có |
| `POST /api/receipts/parse` (LLM) | Có | Có |
| `GET /api/reports/stock-summary` | Có | Không (403) |

## 3. Danh sách endpoint

Cột **Owner**: A/B/C/D/E theo `docs/plans/member-*.md`. Cột Trạng thái: `Xong` = đã chạy được, `Chưa` = còn khung, trả 501 cho tới khi owner làm xong.

| Method & Path | Mô tả | Role | Mã trả về | Owner | Trạng thái |
|---|---|---|---|---|---|
| POST `/api/auth/login` | Cấp JWT | public | 200, 400, 401 | D | Xong |
| GET `/api/auth/me` | User hiện tại | any | 200, 401 | D | Xong |
| PATCH `/api/auth/password` | Đổi mật khẩu `{currentPassword,newPassword}`; sai mật khẩu cũ hoặc mới trùng cũ -> 400; mới 8-72 ký tự | any | 200, 400, 401 | D | Xong |
| GET `/api/users` | Danh sách tài khoản | ADMIN | 200, 401, 403 | D | Xong |
| POST `/api/users` | Tạo tài khoản | ADMIN | 201, 400, 401, 403, 409 | D | Xong |
| PATCH `/api/users/{id}/status` | Khóa/mở khóa | ADMIN | 200, 400, 401, 403, 404, 409 | D | Xong |
| GET `/api/categories` | Danh sách danh mục | any | 200, 401 | B | Xong |
| POST `/api/categories` | Tạo danh mục | ADMIN | 201, 400, 401, 403, 409 | B | Xong |
| GET `/api/products` | Tìm/lọc/sắp xếp/phân trang | any | 200, 400, 401 | B | Chưa |
| GET `/api/products/{id}` | Chi tiết + tồn kho | any | 200, 401, 404 | B | Chưa |
| POST `/api/products` | Tạo sản phẩm | ADMIN | 201, 400, 401, 403, 404, 409 | B | Chưa |
| PUT `/api/products/{id}` | Sửa sản phẩm | ADMIN | 200, 400, 401, 403, 404, 409 | B | Chưa |
| DELETE `/api/products/{id}` | Ẩn (soft delete) | ADMIN | 200, 401, 403, 404 | B | Chưa |
| GET `/api/suppliers` | Danh sách NCC | any | 200, 401 | B | Chưa |
| POST `/api/suppliers` | Tạo NCC | ADMIN | 201, 400, 401, 403 | B | Chưa |
| PUT `/api/suppliers/{id}` | Sửa NCC | ADMIN | 200, 400, 401, 403, 404 | B | Chưa |
| POST `/api/receipts` | Tạo phiếu nhập nhiều dòng (DRAFT) | any | 201, 400, 401, 404 | D | Xong |
| GET `/api/receipts` | Danh sách (lọc ngày/trạng thái) | any | 200, 401 | D | Xong |
| GET `/api/receipts/{id}` | Chi tiết phiếu | any | 200, 401, 404 | D | Xong |
| PUT `/api/receipts/{id}` | Sửa phiếu DRAFT | owner/ADMIN | 200, 400, 401, 403, 404, 409 | D | Xong |
| PATCH `/api/receipts/{id}/confirm` | Xác nhận -> tăng tồn | owner/ADMIN | 200, 401, 403, 404, 409 | D | Xong |
| PATCH `/api/receipts/{id}/cancel` | Hủy phiếu DRAFT | owner/ADMIN | 200, 401, 403, 404, 409 | D | Xong |
| POST `/api/receipts/parse` | LLM: gợi ý phiếu từ hóa đơn text; dòng không hợp lệ vào `unmatched` | any | 200, 400, 401, 503 | E | Xong |
| POST `/api/issues` | Tạo phiếu xuất (DRAFT) | any | 201, 400, 401, 404 | C | Xong |
| GET `/api/issues` | Danh sách phiếu xuất (lọc `status`, `from`/`to`; sort whitelist) | any | 200, 400, 401 | C | Xong |
| GET `/api/issues/{id}` | Chi tiết phiếu xuất | any | 200, 401, 404 | C | Xong |
| PUT `/api/issues/{id}` | Sửa phiếu DRAFT | owner/ADMIN | 200, 400, 401, 403, 404, 409 | C | Xong |
| PATCH `/api/issues/{id}/confirm` | Xác nhận -> giảm tồn | owner/ADMIN (DAMAGED: ADMIN) | 200, 401, 403, 404, 409 | C | Xong |
| PATCH `/api/issues/{id}/cancel` | Hủy phiếu DRAFT | owner/ADMIN | 200, 401, 403, 404, 409 | C | Xong |
| GET `/api/inventory/low-stock` | Sản phẩm tồn <= MinStock | any | 200, 401 | B | Chưa |
| GET `/api/products/{id}/history` | Lịch sử nhập/xuất (chỉ phiếu CONFIRMED, mới nhất trước, tối đa 500 dòng; `direction` IN/OUT, `documentType` RECEIPT/ISSUE) | any | 200, 401, 404 | B | Chưa |
| GET `/api/reports/stock-summary?from=&to=` | Báo cáo biến động tồn: mọi sản phẩm (kể cả đã ẩn), tính theo `confirmedAt`, `to` trọn ngày | ADMIN | 200, 400, 401, 403 | E | Xong |
| GET `/api/reports/stock-summary/export?from=&to=` | Tải báo cáo biến động tồn dạng CSV cho Excel (UTF-8 có BOM, chống CSV injection); lỗi vẫn trả JSON | ADMIN | 200, 400, 401, 403 | E | Xong |

## 4. Payload mẫu quan trọng

**Login**
```json
// POST /api/auth/login
{ "email": "admin@freshmart.local", "password": "FreshMart@2026" }
// 200
{ "status": 200, "message": "success", "data": {
  "accessToken": "eyJ...", "tokenType": "Bearer", "expiresInSeconds": 3600,
  "user": { "id": 1, "fullName": "Admin FreshMart", "email": "admin@freshmart.local", "role": "ADMIN", "active": true, "createdAt": "..." } } }
```

**Tạo phiếu nhập** (người tạo lấy từ token)
```json
// POST /api/receipts
{ "supplierId": 1, "note": "Nhập rau sáng thứ Hai",
  "lines": [ { "productId": 3, "quantity": 50, "unitPrice": 20000 },
             { "productId": 4, "quantity": 30, "unitPrice": 18000 } ] }
```

**Xác nhận phiếu xuất vượt tồn -> 409, không trừ dòng nào**
```json
{ "status": 409, "message": "Không đủ tồn kho", "data": [
  { "productId": 3, "sku": "RCQ-003", "name": "Cà rốt", "requested": 40, "available": 25 } ] }
```

**Xác nhận phiếu xuất thành công** - kèm cảnh báo tồn thấp
```json
{ "status": 200, "message": "success", "data": {
  "issue": { "id": 9, "code": "PX-20261120-001", "status": "CONFIRMED", "...": "..." },
  "newlyLowStock": [ { "productId": 3, "sku": "RCQ-003", "name": "Cà rốt", "unit": "kg", "quantityInStock": 8, "minStock": 25 } ] } }
```

**LLM parse** (chỉ là gợi ý, không lưu)
```json
// POST /api/receipts/parse
{ "text": "NCC Nông sản Xanh - Cà chua 40kg x 26.000; Khoai tây 60kg x 20.000" }
// 200
{ "status": 200, "message": "success", "data": {
  "suggestedSupplierId": 1, "supplierNameInText": "Nông sản Xanh",
  "lines": [ { "productId": 1, "productName": "Cà chua", "quantity": 40, "unitPrice": 26000, "sourceText": "Cà chua 40kg x 26.000" } ],
  "unmatched": [], "mock": false } }
```

## 5. Quy tắc nghiệp vụ dùng chung (mọi người phải thống nhất)

1. Trạng thái chứng từ: `DRAFT -> CONFIRMED`, `DRAFT -> CANCELLED`. Mọi thao tác sửa/xác nhận/hủy trên CONFIRMED/CANCELLED -> **409**. Sai sót sau xác nhận -> tạo chứng từ điều chỉnh mới.
2. Tồn kho chỉ đổi khi **xác nhận** chứng từ, trong **một transaction**; không có API sửa tồn trực tiếp.
3. Xác nhận phiếu xuất: kiểm tra **tất cả** dòng trước; có dòng vượt tồn -> 409 + danh sách thiếu, không trừ gì.
4. Chứng từ >= 1 dòng; `quantity > 0`; một sản phẩm không lặp 2 lần trong 1 chứng từ; không thêm sản phẩm đã ẩn vào chứng từ mới.
5. Mã chứng từ: `PN-yyyyMMdd-xxx` / `PX-yyyyMMdd-xxx`, bộ đếm reset mỗi ngày (múi giờ `Asia/Ho_Chi_Minh`).
6. Low-stock: `QuantityInStock <= MinStock`, chỉ sản phẩm đang hoạt động.
7. Sai email / sai mật khẩu / tài khoản khóa -> cùng một thông báo 401.
8. Thời điểm biến động tồn trong **lịch sử** và **báo cáo** = `confirmed_at` của chứng từ (không phải `created_at`); chỉ tính chứng từ CONFIRMED.
9. **[Đã chốt]** "Sản phẩm vừa giảm xuống dưới mức tối thiểu" (`newlyLowStock` khi xác nhận phiếu xuất) = các sản phẩm **trong phiếu** có tồn sau xuất <= MinStock (tồn đúng bằng MinStock cũng tính).
10. **[Đã chốt 20/11 - không cần họp lại]** Sản phẩm **ẩn**: `GET /api/products/{id}` vẫn trả 200 với `active=false` (để lịch sử còn đọc được); không hiện trong danh sách `GET /api/products`, không hiện trong low-stock.
11. **[Đã chốt]** Thêm sản phẩm vào chứng từ: id **không tồn tại -> 404**; sản phẩm **đã ẩn -> 400**; NCC không tồn tại -> 404; dòng lặp sản phẩm -> 400. (Cài trong `DocumentGuard`.)
12. **[Đã chốt]** Phiếu xuất `DAMAGED`: ai cũng **tạo** được (DRAFT); chỉ ADMIN **xác nhận** (STAFF -> 403, kể cả chủ phiếu). Thứ tự kiểm tra khi confirm: tồn tại(404) -> quyền sở hữu(403) -> DAMAGED(403) -> trạng thái(409) -> tồn kho(409).
13. **[Đã chốt]** Bộ lọc `from`/`to` của danh sách chứng từ theo **ngày tạo** (`createdAt`), `to` tính **trọn ngày**, múi giờ `Asia/Ho_Chi_Minh` (xem `DocumentGuard.startOfDay/startOfNextDay`). Lịch sử + báo cáo tồn thì theo `confirmedAt` (mục 8).
14. **[Đã chốt]** `POST /api/receipts/parse`: JSON **sai cú pháp/sai schema -> 400**; dòng đúng cú pháp nhưng `productId` không có/ẩn hoặc `quantity <= 0` -> **chuyển vào `unmatched`** với `reason` (không làm hỏng cả response); AI sập/timeout -> 503.
15. **[Đã chốt]** Sắp xếp list: chỉ cho phép whitelist (dùng `PageableGuard.sanitize`), `size` <= 100; sai -> 400. Product: `id,sku,name,price,quantityInStock`. Receipt/Issue: `id,code,createdAt,confirmedAt`.
16. **[Đã chốt]** `confirmedAt` = `Instant.now(clock)` (inject `Clock`), đặt đúng lúc xác nhận trong cùng transaction.
17. **[Đã chốt]** Khóa tồn kho khi xác nhận: **luôn** `productRepository.findAllByIdForUpdate(ids)` (sắp theo id) cho **cả** phiếu nhập lẫn phiếu xuất -> mọi transaction khóa cùng thứ tự, không deadlock.
18. **[Đã chốt]** ADMIN không được tự khóa tài khoản của mình (409). Đổi trạng thái idempotent (khóa người đã khóa -> 200).
