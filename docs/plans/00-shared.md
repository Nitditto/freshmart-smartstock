# 00 – Bối cảnh chung & luật "không ai chờ ai"

Đọc file này **một lần**, sau đó chỉ đọc plan của mình. Mọi quyết định còn mở đã được chốt ở đây và ở `docs/api-contract.md` §5 (mục 10–18) → **không cần họp để chờ quyết định**.

## 1. Dự án
SmartStock – REST API quản lý kho cho FreshMart (đề CLT, OJT KITS 2026). Nhóm 5 người, thuyết trình **30/11/2026**, demo trực tiếp qua Swagger. Stack: Java 17, Spring Boot 3.3, JPA/Hibernate, MySQL 8 + Flyway, Spring Security + JWT, springdoc, JUnit 5 / Mockito / Testcontainers. Quy tắc code: xem `CLAUDE.md`/`AGENTS.md` ở thư mục gốc. Quy trình Git: `docs/git-flow.md`.

## 2. Phân chia lại (C ↔ D) và ma trận sở hữu

| Mảng | Trước | **Bây giờ** | Lý do |
|---|---|---|---|
| Phiếu nhập (receipt) – toàn bộ 6 endpoint | C | **D** | C quá tải (20đ logic dồn một người) |
| Phiếu xuất (issue) – toàn bộ 6 endpoint + `newlyLowStock` | C | **C** | phần khó nhất (409 + shortages + đồng thời) |
| `DocumentCodeGenerator` (retry), test đồng thời | C | **C** | cùng một vấn đề "đồng thời" |
| Quản lý user, JWT, Swagger, ma trận 401/403 | D | **D** | giữ nguyên |
| Điểm cộng refresh token | D | **Hủy / chỉ làm khi mọi thứ xong** (đổi mật khẩu thì được) | D đã nhận phiếu nhập |
| Seed 10 chứng từ | A (gọi service C) | **A, gọi `ReceiptService` (D) + `IssueService` (C)** sau khi hai bên merge create/confirm (~24/11) | Giữ nguyên plan A; C, D chỉ cần ổn định `create`/`confirm` trước 23/11 |
| Query lịch sử | B + E dùng chung repo | **B** (2 `*DetailRepository`); E dùng `ReportRepository` mới | tránh sửa cùng một file |

Slide: C = slide 6 (xuất kho + transaction + đồng thời); D = slide 5 (JWT) + **nửa đầu slide 6** (quy tắc trạng thái + phiếu nhập).

### Bảng file sở hữu (chỉ sửa file của mình; cần sửa file người khác → mở issue/nhắn, không tự sửa)

| Người | Sở hữu |
|---|---|
| **A** | `db/migration/V1–V3`, `domain/entity/*`, `domain/enums/*`, `config/DataSeeder.java`, `docs/{user-stories,erd}.md`, `erd.png` |
| **B** | `service/impl/{Product,Supplier,Inventory,Category}ServiceImpl`, `repository/{Product,Supplier,Category}Repository`, **`repository/Stock{Receipt,Issue}DetailRepository`**, `controller/{Product,Supplier,Category,Inventory}Controller` (trừ `stock-summary`) |
| **C** | `service/impl/IssueServiceImpl`, `service/DocumentCodeGenerator`, `repository/StockIssueRepository`, `controller/IssueController`, `dto/issue/*`, test liên quan |
| **D** | `service/impl/{Receipt,User}ServiceImpl`, `repository/{StockReceipt,User}Repository`, `controller/{Receipt (trừ /parse),User,Auth}Controller`, `security/*`, `config/{SecurityConfig,OpenApiConfig}`, `common/*`, `dto/{receipt,user,auth}/*` |
| **E** | `llm/*`, `service/impl/{ReceiptParse,Report}ServiceImpl`, `repository/ReportRepository` (mới), `resources/prompts/*`, `src/test/resources/invoices/*`, `docs/{prompt-log,git-flow}.md`, `.github/*`, PPT |
| **Dùng chung (tạo sẵn, chỉ thêm, không đổi chữ ký)** | `service/impl/DocumentGuard`, `common/api/PageableGuard`, `support/TestData` (test), `common/*` |

Migration: `V4`=B, `V5`=C, `V6`=D, `V7`=E (chỉ khi thật sự cần; thông báo trước, không sửa file đã merge).

## 3. Những thứ đã có sẵn để bạn không phải chờ ai

- **Helper dùng chung (đã viết):**
  - `DocumentGuard` – `assertCanModify(ownerId, me)` (403), `assertDraft(status, code)` (409), `assertNoDuplicateProducts(ids)` (400), `loadProductsForLines(ids, repo)` (404 / 400 ẩn), `startOfDay/startOfNextDay(date, clock)`.
  - `PageableGuard.sanitize(pageable, allowedSortProps)` – whitelist sort + size ≤ 100 (400).
  - `TestData` (test) – tạo user/category/product/supplier/receipt/issue **trực tiếp bằng repository** (không qua service của người khác).
- **Mẫu hoàn chỉnh:** `CategoryServiceImpl` + `CategoryServiceImplTest`.
- **Nhận diện người gọi:** `SecurityUtils.currentUser()` → `AuthUser(id, email, role)`.
- **Lỗi:** `ApiException.{badRequest,unauthorized,forbidden,notFound,conflict(msg,data)}`; `GlobalExceptionHandler` đã bọc envelope.
- **Thời gian:** inject `Clock` (bean trong `ClockConfig`, múi giờ `Asia/Ho_Chi_Minh`) → dùng `Instant.now(clock)`, **không** `Instant.now()` trực tiếp.

## 4. Luật "không chờ nhau"

1. **Chỉ phụ thuộc vào interface/DTO/entity đã có**, không phụ thuộc vào *triển khai* của người khác. Service A gọi service B khác người → **cấm**; nếu cần, tự query bằng repository của mình.
2. **Test của mình tự dựng dữ liệu** bằng `TestData` hoặc Mockito; không gọi `POST` của người khác để chuẩn bị dữ liệu.
3. **Ngoại lệ có chủ đích:** seed phần 2 của A gọi service của C/D (đã hẹn mốc 23/11). Nếu C/D trễ, A tạm seed bằng repository để kịp demo – không chặn ai.
4. **Mỗi PR nhỏ, độc lập, merge được riêng** (≥ 3 PR/người). PR không được làm hỏng build của người khác (CI xanh).
5. Đổi **hợp đồng** (endpoint, tên trạng thái, DTO, mã lỗi) → sửa `docs/api-contract.md` **trong cùng PR** + gắn tên cả nhóm làm reviewer. Đổi chữ ký helper dùng chung → xin phép cả nhóm.
6. Trong lúc chờ code người khác, test ma trận quyền chỉ khẳng định **"không phải 401/403"** cho vai trò được phép (501 vẫn chấp nhận); siết lại ngày 27/11.
7. Gặp lỗi do code người khác: mở issue có bước tái hiện; **không sửa hộ** trong PR của mình.

## 5. Quy trình mỗi PR (checklist)
- [ ] Nhánh `feature/<chữ-cái>-<việc>` từ `develop`; commit `feat:/fix:/docs:/refactor:/test:`.
- [ ] `mvn -B verify` xanh ở máy mình.
- [ ] Có test cho nhánh lỗi, không chỉ nhánh thành công.
- [ ] Swagger: `@Operation` + mô tả tham số; không trả entity; không lộ `passwordHash`.
- [ ] Không có secret; không sửa file của người khác; cập nhật `api-contract.md` nếu đổi hợp đồng.
- [ ] Điền `.github/pull_request_template.md`, gắn ≥ 1 reviewer.

## 6. Lịch chung (30/11 thuyết trình)
| Ngày | Mốc |
|---|---|
| 20/11 | Mọi người clone, tạo nhánh, PR đầu tiên (nhỏ). E bật branch protection |
| 21–22/11 | Phần lõi của từng người |
| **23/11** | Merge hết vào `develop`, tag **v0.1.0** |
| 24–26/11 | Hoàn thiện nghiệp vụ + test, tích hợp |
| 27/11 | Test ma trận quyền siết lại; PPT bản nháp |
| **28/11** | **Feature freeze**, chỉ sửa lỗi; slide hoàn chỉnh |
| 29/11 | Tập dượt demo + bấm giờ; tag **v1.0.0** |
| 30/11 | Thuyết trình |

Ngày đệm: 27/11 chiều và 29/11 sáng chỉ để sửa lỗi/tập dượt, **không nhận tính năng mới**.

## 7. Kịch bản demo Swagger (cả nhóm cùng nhắm tới)
1. STAFF đăng nhập → Authorize. 2. STAFF `POST /api/products` → **403** (D). 3. `POST /api/receipts/parse` với hóa đơn mẫu → gợi ý (E). 4. Tạo phiếu nhập từ gợi ý → xác nhận → tồn tăng (D). 5. Tạo phiếu xuất vượt tồn → xác nhận → **409** + danh sách thiếu, tồn không đổi (C). 6. Xuất hợp lệ → `newlyLowStock` (C). 7. `low-stock`, `history` (B). 8. ADMIN `stock-summary` (E). Dữ liệu reset bằng `docker compose down -v && up --build` (A bảo đảm seed chạy lại được).

## 8. UI (frontend)
Cả nhóm cùng làm, **không cắt phạm vi, không phân người cứng** – xem `docs/plans/ui-shared.md` (bộ khung PR #0, danh sách màn hình, luật chống xung đột, lịch song song). UI không chặn backend: dùng MSW khi backend còn 501; freeze cùng ngày **28/11**; Swagger vẫn là kênh demo chính được chấm.
