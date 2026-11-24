# Kế hoạch Thành viên A - ERD, User Story, Entity, Migration, Dữ liệu mẫu

**Vai trò thêm:** "người giữ cổng migration" (mọi file `V*.sql` phải qua A).
**Slide phụ trách:** 2 (Xác định vấn đề + User Story chính), 3 (Thiết kế ERD). Cùng chia slide 11 (đóng góp cá nhân).

> Bối cảnh chung, phân chia C/D, helper dùng chung và luật "không chờ nhau": xem `00-shared.md`.

---

## 0. Bối cảnh chung (đọc file nào cũng đủ)

- **Dự án:** SmartStock - REST API quản lý kho cho FreshMart (đề CLT, OJT KITS 2026). Nhóm 5 người; nộp GitHub + PPT; thuyết trình dự kiến **30/11/2026**, demo trực tiếp qua Swagger.
- **Stack:** Java 17, Spring Boot 3.3, JPA/Hibernate, MySQL 8 + Flyway, Spring Security + JWT, springdoc (Swagger), JUnit 5 / Mockito / Testcontainers.
- **Khung đã có sẵn (đừng viết lại):** `ApiResponse`/`PageResponse`, `ApiException` (factory `notFound/conflict/...`), `GlobalExceptionHandler`, `SecurityConfig` + JWT, `AuthService` (login, me), `CategoryService` (**mẫu hoàn chỉnh**), toàn bộ entity, repository, DTO, controller, service interface. Service chưa làm đang ném `ApiException.notImplemented(...)` -> trả 501.
- **Chạy thử:** xem `README.md` (Docker Compose hoặc `mvn spring-boot:run -Dspring-boot.run.profiles=dev,local`). Tài khoản dev: `admin@freshmart.local`, `staff1@freshmart.local`, `staff2@freshmart.local` / `FreshMart@2026`.
- **Luật Git:** nhánh `feature/...` -> PR vào `develop`, >= 1 người khác review, CI xanh, **mỗi người >= 3 PR đã merge**, commit `feat:/fix:/docs:/refactor:/test:`, tuyệt đối không commit secret. Chi tiết: `docs/git-flow.md`.
- **Phải đọc trước khi code:** `docs/api-contract.md` (hợp đồng chung), `docs/erd.md`, `docs/architecture.md`, `docs/git-flow.md`.
- **Quy ước code:** xem `AGENTS.md` / `CLAUDE.md`; mô phỏng `CategoryServiceImpl` + `CategoryServiceImplTest`.

---

## 1. Mục tiêu & phạm vi của A

1. Chốt **ERD** và giải thích được từng quan hệ (tiêu chí đánh giá 10đ).
2. Viết **>= 8 user story** (khung đã có 11 story nháp trong `docs/user-stories.md`) mỗi story 2-3 tiêu chí chấp nhận kiểm thử được (10đ).
3. Đảm bảo **entity <-> migration khớp tuyệt đối**; mọi bảng tạo bằng Flyway (tương đương EF Migrations).
4. **Dữ liệu mẫu:** 1 ADMIN, 2 STAFF, 5 danh mục, 30 sản phẩm, 3 NCC, **10 chứng từ**.
5. Điều phối **kickoff** hôm nay để cả nhóm chốt ERD/hợp đồng API trước khi code.

Không thuộc A: logic API (B, C, D), LLM/báo cáo (E).

## 2. Đầu ra (Definition of Done)

- [ ] `docs/user-stories.md` đã rà soát, >= 8 story, mỗi story 2-3 AC đo được.
- [ ] `docs/erd.md` + **`docs/erd.png`** (xuất từ Mermaid/dbdiagram.io/Workbench) khớp 100% `V1__init_schema.sql`.
- [ ] Ứng dụng khởi động sạch trên MySQL **trống** (Flyway tạo đủ bảng), không lỗi mapping khi gọi mọi repository.
- [ ] `DataSeeder` phần 1 (đã có) + **phần 2: 10 chứng từ** tạo qua service thật.
- [ ] Test repository (Testcontainers) cho các query tự viết.
- [ ] Slide 2, 3 xong, A trả lời được các câu "vì sao" ở mục 7.
- [ ] >= 3 PR đã merge.

## 3. Các file A sở hữu
`src/main/resources/db/migration/V1..V3`, `domain/entity/*`, `domain/enums/*`, `repository/*` (cấu trúc entity/quan hệ; query nghiệp vụ do B/C/D/E bổ sung trong file của họ), `config/DataSeeder.java`, `docs/user-stories.md`, `docs/erd.md`, `docs/erd.png`.

## 4. Việc cần làm (theo thứ tự)

### Bước 1 - Kickoff cả nhóm (Thứ Sáu 20/11, ~60 phút) - A điều hành
Chốt và ghi lại vào PR `docs: kickoff decisions`:
- [ ] ERD & tên cột (đã có sẵn - xem còn muốn đổi gì không).
- [ ] Danh sách endpoint + mã lỗi (`docs/api-contract.md`).
- [ ] Các quyết định còn mở: **đã được chốt sẵn ở `api-contract.md` mục 5 mục 10-18** (sản phẩm ẩn, `newlyLowStock`, `confirmedAt`, DAMAGED...). A chỉ xác nhận/ghi biên bản, đổi ý thì sửa mục đó.
- [ ] Phân công & ngày mốc (`docs/git-flow.md`).

### Bước 2 - Rà soát schema/entity (20-21/11) -> PR #1 `fix/docs`
- Đọc `V1__init_schema.sql` cùng `domain/entity/*`: tên cột, kiểu, nullable, unique, FK, CHECK.
- Khởi động app trên MySQL trống; mở Swagger; gọi `GET /api/categories`.
- Viết test `SchemaMappingTest` (Testcontainers) để **bắt lệch entity<->migration**; dùng `TestData` để chèn/đọc đủ các entity. Lưu ý: Hibernate 6 + enum có thể báo kiểu `enum` vs `varchar`; nếu vậy giữ `ddl-auto=none`.
- Nếu cần sửa schema: tạo `V2__...sql` (A là người duy nhất tạo V2-V3), **không sửa V1**.

### Bước 3 - Hoàn thiện tài liệu (21-22/11) -> PR #2
- Chỉnh `docs/user-stories.md`: viết lại theo giọng của nhóm; mỗi AC phải kiểm thử được bằng Swagger/test (ghi mã HTTP kỳ vọng).
- Xuất `docs/erd.png`. Kiểm tra: mọi FK trong hình có trong SQL, và ngược lại.
- Viết nội dung slide 2 (vấn đề FreshMart: Excel, số liệu lệch, không truy vết) và slide 3 (ERD + 3-4 quyết định thiết kế ở `docs/erd.md`).

### Bước 4 - Index & hiệu năng (22-23/11) -> PR #3
- Chạy `EXPLAIN` cho: tìm sản phẩm theo từ khóa/danh mục, low-stock, danh sách chứng từ theo trạng thái+ngày, lịch sử sản phẩm.
- Ghi kết quả vào `docs/erd.md` (mục "Index"). Thêm `V2__add_indexes.sql` nếu thiếu. Lưu ý: `LIKE '%kw%'` không dùng được B-tree - ghi nhận là giới hạn đã biết (200 sản phẩm nên chấp nhận được).

### Bước 5 - Seed phần 2: 10 chứng từ (sau khi C và D merge create/confirm, ~24-25/11) -> PR #4
- Trong `DataSeeder` (sau phần 1): đặt `SecurityContext` bằng `AuthUser` của admin/staff1/staff2 (seeder không có request), rồi gọi `ReceiptService.create/confirm` (**D**) và `IssueService.create/confirm` (**C**).
- Kịch bản: 5 phiếu nhập CONFIRMED (nhiều NCC), 2 phiếu xuất CONFIRMED, 1 phiếu nhập DRAFT, 1 phiếu CANCELLED, 1 phiếu xuất DRAFT lý do DAMAGED. **Cố ý** để vài sản phẩm ở mức tồn <= MinStock để demo cảnh báo.
- Seeder phải **idempotent** (chạy lần 2 không nhân đôi) và chỉ chạy khi `app.seed.enabled=true`.
- Phối hợp C và D: hẹn ngày merge create/confirm; nếu service phụ thuộc request context, báo họ điều chỉnh.

### Bước 6 - Test repository (24-26/11) -> PR #5
`@DataJpaTest`-style (Testcontainers) cho: `ProductRepository.search` (keyword/category/onlyActive), `findLowStock`, `findAllByIdForUpdate` (trả đủ, có thứ tự), `DocumentCounterRepository` (unique `(prefix,date)`), ràng buộc UNIQUE `(receipt_id, product_id)` và `CHECK quantity_in_stock >= 0` (vi phạm -> exception).

### Bước 7 - Cổng migration & hỗ trợ (liên tục)
- Khi ai cần migration: xem bảng cấp phát trong `docs/git-flow.md`; review file SQL (tên, thứ tự, có chạy được trên DB trống và DB đã có dữ liệu seed).
- Trước tag `v0.1.0` (23/11) và `v1.0.0`: chạy `docker compose down -v && docker compose up --build` để chắc app dựng được từ số 0.

## 5. Lịch của A
| Ngày | Việc |
|---|---|
| 20/11 | Kickoff, chốt quyết định, bắt đầu Bước 2 |
| 21-22/11 | Bước 2-3 (PR #1, #2), nội dung slide 2-3 |
| 23/11 | Bước 4, hỗ trợ merge -> tag v0.1.0 (E tag) |
| 24-26/11 | Bước 5-6 |
| 27/11 | Dọn tài liệu, kiểm `docker compose` từ số 0 |
| 28/11 | Freeze; hoàn thiện slide 2-3, tập nói |
| 29/11 | Tập dượt cả nhóm |

## 6. Checklist chất lượng (tự review trước khi mở PR)
- [ ] Mọi quan hệ có giải thích "vì sao" (1-N, header/detail, created_by).
- [ ] Tên bảng/cột/enum khớp giữa SQL, entity, `api-contract.md`.
- [ ] Không có `ddl-auto=update/create`.
- [ ] Không commit mật khẩu thật; mật khẩu seed chỉ ở profile `dev`.
- [ ] README nêu cách chạy migration + tài khoản mẫu.

## 7. Câu hỏi "vì sao" A phải trả lời được
1. Vì sao tách `StockReceipts` / `StockReceiptDetails` (header-detail)? -> chuẩn hóa 1NF/2NF: nhiều dòng/phiếu, không lặp cột; join theo `receipt_id`.
2. Vì sao `QuantityInStock` nằm trong `Products` dù có thể tính từ chứng từ? -> tốc độ đọc + chặn xuất quá tồn; đảm bảo nhất quán bằng transaction + khóa dòng + `CHECK >= 0`.
3. Vì sao soft delete? -> giữ khóa ngoại và lịch sử.
4. Vì sao có `document_counters`? -> mã reset mỗi ngày, không trùng khi đồng thời.
5. Vì sao Flyway/ migration thay vì `ddl-auto=update`? -> phiên bản hóa schema, tái lập được, review được qua PR.
6. Vì sao `BigDecimal` cho tiền, `Instant` UTC cho thời gian, khóa `BIGINT`?
7. Vì sao `UNIQUE (receipt_id, product_id)` ở DB khi code đã kiểm tra?
8. Index nào quan trọng nhất và tại sao?

## 8. Rủi ro
| Rủi ro | Cách giảm |
|---|---|
| Entity lệch SQL -> app lỗi khi chạy | Test mapping ở Bước 2, chạy CI |
| Hai người tạo cùng số migration | Bảng cấp phát version, A review |
| Seed phần 2 phụ thuộc C và D | Bắt đầu phần 1 trước; hẹn C, D ngày merge create/confirm (23/11). Nếu trễ, tạm seed bằng repository để kịp demo |
| `LIKE '%kw%'` chậm | Chấp nhận với 200 SP; nêu như giới hạn có chủ đích |
