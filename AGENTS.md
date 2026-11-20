# AGENTS.md – SmartStock (FreshMart Warehouse API)

Quy tắc dùng chung cho **mọi AI agent và mọi thành viên** (Claude Code, Copilot, Cursor, Codex…). File này là nguồn sự thật; `CLAUDE.md` chỉ bổ sung phần riêng của Claude Code.
Ưu tiên khi có mâu thuẫn: lời người dùng > `docs/api-contract.md` > `CLAUDE.md` (rule chi tiết: code, pattern, commit, comment) > file này.

## 1. Dự án
REST API quản lý kho cho FreshMart. Mọi thay đổi tồn kho đi qua **phiếu nhập / phiếu xuất**; vai trò `ADMIN` / `STAFF`; có tính năng LLM gợi ý phiếu nhập từ hóa đơn text.
Stack: Java 17 · Spring Boot 3.3 · Spring Data JPA · MySQL 8 · Flyway · Spring Security + JWT (jjwt) · springdoc (Swagger) · JUnit 5 / Mockito / Testcontainers · Maven · Docker Compose.
Tài liệu: `docs/api-contract.md` (hợp đồng), `docs/erd.md`, `docs/architecture.md`, `docs/git-flow.md`, `docs/plans/00-shared.md` (phân công + helper dùng chung), `docs/plans/member-*.md`.

## 2. Lệnh thường dùng
```bash
mvn -B verify                                              # build + test + coverage (như CI) – chạy trước mỗi PR
mvn test -Dtest=IssueServiceImplTest                       # một lớp test
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local   # chạy local (cần MySQL + application-local.yml)
docker compose up --build                                  # chạy cả API + MySQL
docker compose down -v && docker compose up --build        # reset sạch dữ liệu (demo)
```
Windows: PowerShell 5.1 không có `&&` – dùng Git Bash hoặc `;`; `cp` → `Copy-Item`. Line ending luôn **LF** (`.gitattributes`).
Integration test dùng Testcontainers → cần Docker; không có Docker thì tự bỏ qua (CI có Docker nên vẫn chạy).

## 3. Kiến trúc & cấu trúc
`Controller → Service (interface + impl) → Repository → Entity`, DTO ở biên. Gói `com.freshmart.smartstock`:
`controller/` · `service/` + `service/impl/` · `repository/` · `domain/{entity,enums}/` · `dto/<feature>/` · `security/` · `llm/` · `config/` · `common/{api,exception}/`.
- Controller **mỏng**: nhận request, `@Valid`, gọi service, bọc `ApiResponse`. Không chứa logic nghiệp vụ, không gọi repository.
- Service giữ logic + `@Transactional`; **map entity → DTO bên trong service**, trong transaction.
- Service **không gọi service của feature khác** (tránh phụ thuộc vòng và phụ thuộc người); cần dữ liệu thì dùng repository.
- Mẫu chuẩn để copy: `CategoryServiceImpl` + `CategoryServiceImplTest`. Helper dùng chung: `DocumentGuard`, `PageableGuard`, `SecurityUtils`, `ApiException`.

## 4. Quy tắc code
- **DTO là `record`**, request có Bean Validation (`@NotNull`, `@Size`, `@Min`…). **Không bao giờ trả entity** ra API. Không có `passwordHash` trong bất kỳ response/log nào.
- Lỗi nghiệp vụ → `ApiException.badRequest|unauthorized|forbidden|notFound|conflict(msg[, data])`; không tự `ResponseEntity` lỗi, không `catch (Exception)` rồi nuốt.
- Mã HTTP đúng nghĩa: 200 · 201 (tạo) · 400 (dữ liệu sai) · 401 (chưa xác thực) · 403 (không đủ quyền) · 404 · 409 (xung đột nghiệp vụ/trạng thái) · 503 (AI sập). Mọi phản hồi dùng envelope `{status, message, data}`.
- REST: URL là **danh từ số nhiều** (`/api/receipts`), hành động trạng thái dùng `PATCH /{id}/confirm`; đúng method; không `POST` cho mọi thứ.
- **Danh tính từ token** (`SecurityUtils.currentUser()`), tuyệt đối không nhận `userId`/`createdBy` từ body/query.
- Phân quyền: `@PreAuthorize` ở controller **chỉ** cho endpoint có role cố định (`/api/users`, `POST /api/products`, `stock-summary`…). Quyền **phụ thuộc dữ liệu** (sở hữu chứng từ, phiếu xuất `DAMAGED` chỉ ADMIN xác nhận) kiểm ở service (`DocumentGuard.assertCanModify`, `SecurityUtils.isAdmin()`), ném `ApiException.forbidden`. **Không** đặt `@PreAuthorize(ADMIN)` lên `confirm` của phiếu xuất.
- Thời gian: lưu `Instant` (UTC); múi giờ nghiệp vụ `Asia/Ho_Chi_Minh` qua bean `Clock`; **không** `Instant.now()`/`LocalDate.now()` trực tiếp ở service/seeder/test (dùng `Instant.now(clock)`). Ngoại lệ duy nhất: callback `@PrePersist/@PreUpdate` trong entity (không inject được bean). Tiền: `BigDecimal`. Khóa: `Long`.
- Không `System.out`/`printStackTrace`; dùng SLF4J; không log secret, token, mật khẩu, nội dung hóa đơn đầy đủ.
- Injection: constructor injection (`@RequiredArgsConstructor`), không `@Autowired` field. Không magic number/string lặp lại → hằng số hoặc enum.
- Tên: lớp `PascalCase`, method/biến `camelCase`, hằng `UPPER_SNAKE`, bảng/cột SQL `snake_case`. Thông báo lỗi tiếng Việt, code/comment kỹ thuật tiếng Anh hoặc Việt đều được nhưng **nhất quán trong file**.
- Comment giải thích **vì sao**, không giải thích **cái gì**. Không để code chết, không commit code bị comment-out, `TODO` phải ghi `TODO[Owner]`.

## 5. Dữ liệu & đồng thời (khu vực dễ sai nhất)
- Schema **chỉ đổi bằng Flyway** (`db/migration/V{n}__mo_ta.sql`). **Không sửa migration đã merge** – thêm version mới. Số version đã cấp phát trong `docs/git-flow.md`. `ddl-auto` luôn `none`.
- `quantityInStock` chỉ đổi khi **xác nhận chứng từ**, trong **một transaction**. Không có API sửa tồn trực tiếp.
- Xác nhận chứng từ: thứ tự **khóa dòng phiếu (`findByIdForUpdate`) → khóa sản phẩm (`findAllByIdForUpdate`, sắp theo id) → mới kiểm tra/ghi**. Lý do: MySQL REPEATABLE READ + persistence context của Hibernate có thể trả tồn cũ nếu nạp entity trước khi khóa.
- Xác nhận phiếu xuất: kiểm tra **tất cả** dòng trước, rồi mới trừ; thiếu → 409 + danh sách, không trừ gì.
- Thay danh sách dòng của chứng từ: `clearDetails()` → `saveAndFlush` → thêm dòng mới (tránh vi phạm UNIQUE do Hibernate chạy INSERT trước DELETE).
- Chống N+1 / `LazyInitializationException`: `@EntityGraph` hoặc map DTO trong transaction; list endpoint phải `PageableGuard.sanitize` (whitelist sort, `size ≤ 100`).
- Chứng từ `CONFIRMED`/`CANCELLED` là **bất biến**; sai sót sửa bằng chứng từ điều chỉnh mới.

## 6. Bảo mật
- Mật khẩu BCrypt; JWT access token 60 phút, claims `sub`, `email`, `role`; sai email/mật khẩu/tài khoản khóa → cùng một thông báo 401.
- **Không commit** secret: `.env`, `application-local.yml`, JWT secret, API key, connection string thật. Lỡ commit = coi như lộ → **đổi secret** (xóa lịch sử là chưa đủ).
- Không đưa dữ liệu nhạy cảm/cá nhân/giá nhập vào prompt LLM. Đầu ra LLM chỉ là **đề xuất**, luôn kiểm chứng ở backend, không tự lưu.
- Không tin input: validate ở biên, whitelist sort/filter, không nối chuỗi SQL, không trả stack trace cho client.

## 7. Kiểm thử
- Mỗi tính năng có test **cả nhánh lỗi** (400/403/404/409), không chỉ nhánh thành công. Sửa bug → thêm test tái hiện trước.
- Unit test service: Mockito, không Spring context (mẫu `CategoryServiceImplTest`). Tên test mô tả hành vi: `confirmRejectsWhenStockInsufficientWith409`.
- Integration test: `AbstractIntegrationTest` + `TestData` (dựng dữ liệu bằng repository, **không** gọi API/service của người khác). Test đồng thời dùng `CountDownLatch`, không dùng `sleep`.
- **Viết unit test Mockito đủ mọi nhánh lỗi TRƯỚC**, rồi mới thêm integration test: máy/CI không có Docker thì integration bị bỏ qua, coverage vẫn phải đạt nhờ unit test.
- Coverage phần của mình ≥ 70%. Test phải tất định (không phụ thuộc giờ hệ thống, mạng, thứ tự chạy, API key thật).

## 8. Git & PR
- Nhánh: `main` (phát hành) ← `develop` (tích hợp) ← `feature/<chữ-cái>-<việc>`. **Không push thẳng** `main`/`develop`.
- Commit: `feat:` `fix:` `docs:` `refactor:` `test:` (+ `chore:`); thể mệnh lệnh, **tiêu đề ≤ 72 ký tự; thân (nếu có) 1 dòng ≤ 72 ký tự, cách tiêu đề 1 dòng trống**, một thay đổi logic một commit (chi tiết: `CLAUDE.md` §4).
- PR nhỏ, một mục đích, ≥ 1 người khác review, `mvn -B verify` xanh, điền template. Mỗi thành viên ≥ 3 PR merged. Đổi hợp đồng API → sửa `docs/api-contract.md` trong cùng PR.
- Không commit `target/`, `.idea/`, file lớn, dữ liệu thật. Tag: `v0.1.0` (23/11), `v1.0.0` (29/11).

## 9. Ranh giới làm việc (cho người và agent)
- **Chỉ sửa file thuộc phần của mình** (bảng trong `docs/plans/00-shared.md` §2). Cần sửa file người khác → mở issue/nhắn, không tự sửa.
- Helper dùng chung (`DocumentGuard`, `PageableGuard`, `TestData`, `common/*`): chỉ **thêm** method, không đổi chữ ký; thêm test.
- Không cài thư viện mới, không đổi version Spring/Java, không đổi `pom.xml` nếu chưa báo nhóm.
- Không đổi hợp đồng API (đường dẫn, tên trường, mã lỗi, tên trạng thái) ngoài `docs/api-contract.md` + PR có cả nhóm review.
- Không "tiện tay" refactor phạm vi ngoài task; không xóa/đổi test của người khác để làm test mình xanh.

## 10. Quy trình cho agent
1. Đọc `docs/plans/00-shared.md` + plan của người dùng + phần liên quan của `api-contract.md` **trước khi** viết code.
2. Tìm mẫu có sẵn (`CategoryServiceImpl`, helper) rồi bắt chước; không phát minh kiến trúc mới.
3. Làm đúng phạm vi task; thay đổi nhỏ nhất đủ để đạt mục tiêu. Nếu yêu cầu mơ hồ hoặc mâu thuẫn hợp đồng → hỏi, đừng đoán.
4. Viết/cập nhật test cùng lúc; chạy `mvn -B verify` (hoặc nói rõ nếu không chạy được) và **báo trung thực** kết quả, kể cả test fail hay bước bị bỏ qua.
5. Không tuyên bố "xong" khi chưa kiểm chứng. Không commit/push khi chưa được yêu cầu; không bao giờ commit secret.

## 11. Definition of Done (mỗi PR) – nguồn gốc duy nhất
`CLAUDE.md` §7, `docs/plans/00-shared.md` §5 và `.github/pull_request_template.md` chỉ **trỏ về đây**; khi sửa checklist thì sửa ở đây trước. Thêm: số migration đúng bảng cấp phát trong `docs/git-flow.md`.
- [ ] Build + test xanh, có test nhánh lỗi · [ ] đúng hợp đồng API & mã HTTP · [ ] không trả entity/`passwordHash` · [ ] có `@Operation` Swagger
- [ ] không secret, không sửa file người khác · [ ] tài liệu (`api-contract`, `docs/`) cập nhật nếu đổi hành vi · [ ] đã điền PR template

## 12. Frontend (`frontend/`)
Kế hoạch: `docs/plans/ui-shared.md`. Stack: Vite + React 18 + TypeScript + React Router + TanStack Query + axios + React Hook Form/Zod + Tailwind (shadcn/ui) + MSW + Vitest.
- UI **chỉ gọi endpoint có trong `docs/api-contract.md`**; kiểu API sinh bằng `npm run gen:api` từ `/v3/api-docs` (không sửa `src/api/schema.ts` bằng tay). Không đổi hợp đồng từ phía UI.
- Cấu trúc theo feature: `src/features/<tên>/` (một màn hình một thư mục, một PR). `src/shared/**`, router, layout chỉ **thêm**, đổi chữ ký phải báo nhóm.
- Mọi gọi API đi qua một axios instance duy nhất: gắn token, bóc envelope `{status,message,data}`, `401` → về `/login`, `403` → toast, `409` giữ `data` (ví dụ danh sách thiếu tồn).
- Dữ liệu server dùng TanStack Query (không `useEffect + fetch` thủ công); form dùng React Hook Form + Zod. TypeScript `strict`, **không `any`**, không `@ts-ignore` nếu chưa có lý do ghi bên cạnh.
- Ẩn nút/route theo vai trò chỉ để tiện dùng; **quyền thật do backend**. Token ở `sessionStorage`, không `localStorage` lâu dài; không `dangerouslySetInnerHTML`; không hard-code URL/secret (dùng `import.meta.env`).
- Mọi thao tác ghi: có loading, disable nút, toast thành công/lỗi. Hiển thị `message` tiếng Việt từ server. Tiền `vi-VN`, giờ hiển thị `Asia/Ho_Chi_Minh`.
- Đặt tên: component `PascalCase.tsx`, hook `useXxx`, file khác `camelCase.ts`; một component một file; component ≲ 200 dòng, tách khi lớn.
- Trước PR: `npm run lint && npm run test && npm run build` xanh. Commit/PR theo quy ước chung (`CLAUDE.md` §4).
