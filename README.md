# SmartStock – Warehouse Management REST API (FreshMart)

Backend quản lý kho cho FreshMart: mọi thay đổi tồn kho đi qua **phiếu nhập / phiếu xuất**, mỗi vai trò chỉ làm đúng thao tác được phép, kèm tính năng **LLM** gợi ý phiếu nhập từ hóa đơn văn bản.

**Stack:** Java 17 · Spring Boot 3.3 · Spring Data JPA (Hibernate) · MySQL 8 · Flyway (migration) · Spring Security + JWT (jjwt) · springdoc-openapi (Swagger) · JUnit 5 / Mockito / Testcontainers · Docker Compose.

> Khung dự án đã chạy được phần đăng nhập, phân quyền, Swagger, định dạng phản hồi chung, xử lý lỗi, migration và danh mục (làm mẫu). Các endpoint còn lại trả **501** cho tới khi owner hoàn thành – xem `docs/plans/`.

## 1. Chạy nhanh bằng Docker Compose (khuyến nghị)

```bash
cp .env.example .env          # rồi sửa mật khẩu + JWT_SECRET (>= 32 ký tự)
docker compose up --build
```
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

## 2. Chạy local (IDE / Maven)

Yêu cầu: JDK 17+, Maven 3.9+, MySQL 8 (hoặc chỉ chạy DB bằng `docker compose up mysql`).

```bash
# 1) Tạo DB và user (nếu dùng MySQL cài sẵn)
mysql -uroot -p -e "CREATE DATABASE smartstock CHARACTER SET utf8mb4; \
  CREATE USER 'smartstock'@'%' IDENTIFIED BY 'change-me'; \
  GRANT ALL ON smartstock.* TO 'smartstock'@'%';"

# 2) Cấu hình bí mật (không commit!)
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml
#    -> điền DB password, app.jwt.secret (>= 32 ký tự), (tùy chọn) app.llm.api-key

# 3) Chạy
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local
```

### LLM thật cho `POST /api/receipts/parse` (Ollama, chạy trên máy, không cần API key)
Mặc định không có key → `MockLlmService`. Muốn dùng model thật:
```bash
ollama pull qwen2.5:3b                                            # một lần
mvn spring-boot:run -Dspring-boot.run.profiles=dev,local,ollama   # profile `ollama` = application-ollama.yml
```
- Đổi model/địa chỉ bằng biến môi trường `LLM_MODEL`, `LLM_BASE_URL` (Docker: `http://host.docker.internal:11434/v1`).
- Phản hồi có `"mock": false` là đang dùng model thật. Ollama tắt → API trả **503**, nhập tay vẫn dùng được.
- Dùng nhà cung cấp trên mạng (OpenAI, Groq, Gemini...) thì đặt `app.llm.api-key/base-url/model` trong `application-local.yml` hoặc biến `LLM_API_KEY`; **không bao giờ commit key**.

### Migration (thay cho `dotnet ef database update`)
Flyway **tự chạy** các file trong `src/main/resources/db/migration/` mỗi lần ứng dụng khởi động, nên chỉ cần khởi động ứng dụng là DB được tạo/cập nhật. Quy tắc phiên bản migration: xem `docs/git-flow.md`.

### Tài khoản mẫu (chỉ profile `dev`, mật khẩu mặc định `FreshMart@2026`)

| Vai trò | Email |
|---|---|
| ADMIN | `admin@freshmart.local` |
| STAFF | `staff1@freshmart.local` |
| STAFF | `staff2@freshmart.local` |

Dữ liệu seed: 1 ADMIN, 2 STAFF, 5 danh mục, 30 sản phẩm, 3 nhà cung cấp (+ 10 chứng từ – xem `plans/member-a`).

## 3. Dùng Swagger
1. `POST /api/auth/login` → copy `data.accessToken`.
2. Bấm **Authorize** (góc phải trên) → dán token (không cần gõ chữ `Bearer`).
3. Gọi các API. STAFF gọi API của ADMIN sẽ nhận **403**.

## 4. Test
```bash
mvn test            # unit test + integration test (cần Docker cho Testcontainers; tự bỏ qua nếu không có Docker)
mvn verify          # như CI; báo cáo coverage: target/site/jacoco/index.html
```

## 5. Cấu hình (biến môi trường)

| Biến | Ý nghĩa | Bắt buộc |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Kết nối MySQL | có |
| `JWT_SECRET` | Khóa ký JWT, ≥ 32 ký tự | **có** (thiếu → app không chạy) |
| `LLM_API_KEY`, `LLM_BASE_URL`, `LLM_MODEL` | LLM tương thích OpenAI; trống key → dùng mock | không |
| `SPRING_PROFILES_ACTIVE` | `dev` để seed dữ liệu mẫu | không |

## 6. Cấu trúc thư mục
Xem `docs/architecture.md`. Tóm tắt: `controller → service → repository`, DTO ở `dto/`, entity ở `domain/`, migration ở `resources/db/migration/`, prompt ở `resources/prompts/`.

## 7. Tài liệu
| File | Nội dung |
|---|---|
| `docs/user-stories.md` | User story + tiêu chí chấp nhận |
| `docs/erd.md` | ERD (Mermaid) + lý do thiết kế |
| `docs/architecture.md` | Kiến trúc, MVC vs MSA, luồng, giao dịch/khóa |
| `docs/api-contract.md` | **Hợp đồng API chung** (endpoint, role, mã lỗi, payload) |
| `docs/git-flow.md` | Nhánh, PR, commit, tag, cấp phát số migration, lịch |
| `docs/prompt-log.md` | Prompt LLM + kết quả 8 hóa đơn + nhật ký dùng AI |
| `docs/plans/member-a … e.md` | Kế hoạch chi tiết cho từng thành viên |

## 8. Nhóm
| Thành viên | Phụ trách | Kế hoạch |
|---|---|---|
| A | ERD, user story, entity, migration, seed | `docs/plans/member-a-erd-data.md` |
| B | Category/Product/Supplier API, low-stock, lịch sử | `docs/plans/member-b-catalog-inventory.md` |
| C | Phiếu nhập/xuất, transaction, unit test | `docs/plans/member-c-documents.md` |
| D | JWT, phân quyền, user, response chung, Swagger | `docs/plans/member-d-security-platform.md` |
| E | LLM, prompt log, báo cáo, Git, PPT | `docs/plans/member-e-llm-report-ppt.md` |
