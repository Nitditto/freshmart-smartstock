# Kế hoạch UI chung (frontend) – cả nhóm cùng làm

> Đọc kèm `00-shared.md`, `docs/api-contract.md`. UI **không nằm trong bảng chấm** của đề nhưng nhóm quyết định làm đầy đủ. Hai nguyên tắc: (1) UI **không bao giờ chặn** phần backend; (2) UI chỉ gọi endpoint đã có trong `api-contract.md` – không tự thêm/đổi hợp đồng.

## 1. Cách làm chung mà không phải phân người
Không giao người, nhưng để **không đạp nhau** thì dùng quy tắc "ai làm API nào thì tự nhiên làm màn hình của API đó, khi đã xong phần backend của mình". Đây chỉ là gợi ý để tránh sửa cùng file, không phải phân công cứng; ai rảnh thì nhận màn hình còn trống (ghi tên vào bảng §4 trước khi bắt đầu).

Luật chống xung đột:
- Mỗi màn hình = **một thư mục feature riêng** (`src/features/<tên>/`), một PR riêng. Không sửa thư mục feature của người khác.
- File dùng chung (`src/shared/**`, `App.tsx`, routes, theme) chỉ **thêm**, PR nhỏ, merge nhanh; đổi chữ ký phải báo cả nhóm.
- Một người tạo bộ khung (PR #0, mục 3) rồi cả nhóm mới tách nhánh – **đây là điểm duy nhất cần chờ**, và chỉ chờ ~1 buổi.

## 2. Công nghệ (đã quyết, không bàn lại)
| Hạng mục | Chọn | Lý do |
|---|---|---|
| Build/dev | **Vite + React 18 + TypeScript** | nhẹ, nhanh, ít cấu hình |
| Router | React Router 6 | route theo vai trò |
| Dữ liệu server | **TanStack Query** | cache, phân trang, loading/error có sẵn |
| HTTP | axios (một instance) | gắn token, xử lý envelope + 401 một chỗ |
| Form + validate | React Hook Form + Zod | khớp validation backend |
| UI kit | **Tailwind CSS + shadcn/ui** (hoặc Ant Design nếu cả nhóm quen hơn – chọn **một**) | bảng, dialog, toast có sẵn |
| Kiểu API | `openapi-typescript` sinh từ `/v3/api-docs` | không gõ tay DTO, lệch hợp đồng là lỗi biên dịch |
| Dữ liệu giả | **MSW** (Mock Service Worker) | UI chạy được khi backend còn 501 |
| Test | Vitest + Testing Library; (tùy chọn) Playwright cho kịch bản demo | |
| Triển khai | Build tĩnh phục vụ bằng nginx trong Docker Compose | `docker compose up` chạy cả API + DB + UI |

## 3. Bộ khung (PR #0 – làm trước, ~1 buổi, một người)
Thư mục `frontend/` ở gốc repo:
```
frontend/
  src/
    app/            # App.tsx, router, providers (QueryClient), layout
    shared/         # api client (axios), auth store, ui components dùng chung, utils
    features/       # auth, dashboard, products, catalog, receipts, issues, reports, users
    mocks/          # MSW handlers theo api-contract.md
    api/schema.ts   # sinh bằng `npm run gen:api` (không sửa tay)
  index.html, vite.config.ts, tsconfig.json, package.json
```
Bộ khung phải có sẵn:
1. **API client**: `baseURL=/api`, gắn `Authorization: Bearer`, **bóc envelope** `{status,message,data}`; `401` → xóa token + về `/login`; `403` → toast "Bạn không có quyền"; `409` giữ nguyên `data` (để hiển thị danh sách thiếu tồn).
2. **Auth**: lưu token (memory + `sessionStorage`; **không** `localStorage` lâu dài), `useAuth()` trả `{user, role}`, `<RequireRole roles={['ADMIN']}>` ẩn menu/route theo vai trò. *Ẩn nút chỉ để tiện – quyền thật vẫn do backend (403).*
3. Layout (sidebar + header + toast) và trang `Login`.
4. MSW bật bằng `VITE_USE_MOCKS=true`.
5. Vite proxy `/api` → `http://localhost:8080`.
6. CI thêm job `frontend`: `npm ci && npm run lint && npm run test && npm run build`.
7. **Backend (D, file `SecurityConfig`)**: bật CORS cho `http://localhost:5173` ở profile `dev` (không cần nếu dùng proxy; bắt buộc nếu gọi trực tiếp). Mở 1 PR nhỏ.

## 4. Màn hình & endpoint (mỗi dòng = một PR độc lập)

| Màn hình | Endpoint dùng | Chi tiết bắt buộc | Quyền | Người nhận |
|---|---|---|---|---|
| Đăng nhập | `POST /auth/login`, `GET /auth/me` | lỗi 401 hiển thị đúng 1 thông báo chung | mọi người | |
| Dashboard | `GET /inventory/low-stock`, `GET /products` | thẻ KPI (tổng SP, số SP tồn thấp), bảng tồn thấp | mọi người | |
| Sản phẩm | `GET/POST/PUT/DELETE /products`, `GET /categories` | tìm kiếm, lọc danh mục, phân trang, sắp xếp (đúng whitelist); form tạo/sửa; nút Ẩn có xác nhận; **drawer lịch sử** `GET /products/{id}/history` | xem: mọi người; ghi: ADMIN | |
| Danh mục & NCC | `/categories`, `/suppliers` | bảng + form | ghi: ADMIN | |
| Phiếu nhập | `/receipts`, `/receipts/{id}`, `confirm`, `cancel` | danh sách lọc trạng thái/ngày; form nhiều dòng (chọn sản phẩm, SL, giá); chặn dòng trùng ở client; nút Xác nhận/Hủy chỉ hiện khi `DRAFT` và đúng chủ phiếu/ADMIN | mọi người | |
| **Nhập từ hóa đơn (AI)** | `POST /receipts/parse` | dán text → hiển thị **gợi ý** + danh sách `unmatched` → người dùng sửa → bấm "Tạo phiếu nháp"; nhãn "AI gợi ý – cần kiểm tra"; xử lý 503 ("nhập tay") và cờ `mock` | mọi người | |
| Phiếu xuất | `/issues`, …, `confirm`, `cancel` | như phiếu nhập + chọn lý do; **409 thiếu tồn → dialog liệt kê `ShortageItem`**; sau confirm thành công hiển thị `newlyLowStock` bằng cảnh báo; `DAMAGED` ẩn nút Xác nhận với STAFF | mọi người | |
| Báo cáo tồn | `GET /reports/stock-summary` | chọn khoảng ngày, bảng opening/in/out/closing, (nếu có) nút xuất CSV | ADMIN | |
| Tài khoản | `GET/POST /users`, `PATCH /users/{id}/status` | bảng, tạo user, khóa/mở khóa; không cho tự khóa mình | ADMIN | |

> Màn hình chưa có người nhận: ai rảnh ghi tên. Nếu hết 26/11 vẫn trống, nhóm chia lại trong buổi họp – **không cắt**.

## 5. Quy ước giao diện
- **Mọi thao tác ghi** có trạng thái loading (disable nút), thông báo thành công/lỗi bằng toast, và vô hiệu hóa thao tác không hợp lệ.
- **Lỗi từ envelope** hiển thị `message` tiếng Việt từ server; lỗi validation (400) hiển thị theo từng trường nếu server trả.
- Bảng: phân trang server-side, sắp xếp chỉ trên cột được whitelist, trạng thái rỗng/lỗi/đang tải rõ ràng.
- Tiền định dạng `vi-VN` (VND), ngày giờ hiển thị theo `Asia/Ho_Chi_Minh` (server trả UTC `Instant`).
- Nhãn trạng thái bằng màu + chữ (không chỉ màu): DRAFT / CONFIRMED / CANCELLED.
- Responsive tối thiểu laptop + tablet; phím tắt/accessibility cơ bản (label cho input, focus hợp lý).
- Không hard-code URL/secret; cấu hình qua `import.meta.env`.

## 6. Lịch (không cắt phạm vi, chỉ xếp song song)
| Ngày | Việc UI |
|---|---|
| 21–22/11 | **PR #0** bộ khung + MSW + CI (1 người); mọi người còn lại làm backend như plan |
| 23/11 | v0.1.0 (backend lõi); UI chạy được Login + Dashboard bằng mock |
| 24–26/11 | Mỗi người, khi xong backend của mình, làm màn hình tương ứng (mock → chuyển sang API thật) |
| 27/11 | Ghép toàn bộ, chạy kịch bản demo trên UI; sửa lỗi |
| **28/11** | **Freeze cả UI và backend** |
| 29/11 | Tập dượt: demo trên UI, Swagger là phương án dự phòng; tag v1.0.0 |

Quy tắc sống còn: **Swagger vẫn là kênh demo chính được chấm**; UI là phần thêm. Nếu 27/11 UI chưa ổn thì demo bằng Swagger và UI chỉ là phần giới thiệu – không để UI làm chậm hạn nộp backend.

## 7. Kiểm thử UI
- Component quan trọng (form phiếu nhiều dòng, dialog thiếu tồn, parse hóa đơn) có Vitest + Testing Library.
- Kịch bản demo 8 bước (`00-shared.md` §7) viết thành 1 test Playwright chạy với backend thật (tùy chọn nhưng nên có để tập dượt).
- `npm run lint && npm run test && npm run build` xanh trước mỗi PR.

## 8. Rủi ro
| Rủi ro | Giảm |
|---|---|
| Cả nhóm đồng thời sửa file khung → xung đột | PR #0 xong trước; thư mục feature riêng; file shared chỉ thêm |
| UI lệch hợp đồng API | Kiểu sinh từ OpenAPI; MSW bám `api-contract.md` |
| UI kéo chậm backend | Backend là ưu tiên 1; UI làm sau khi phần backend của mình merge |
| Đổi hợp đồng API làm vỡ UI | Đổi API phải qua `api-contract.md` + cả nhóm review (đã có luật) |
| Token bị đánh cắp (XSS) | Không `dangerouslySetInnerHTML`; token trong `sessionStorage`; CSP khi build |
| Demo UI hỏng | Swagger dự phòng + ảnh chụp từng bước |
