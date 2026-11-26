# Khung bài thuyết trình SmartStock (11 slide)

Đề (CLT, Bước 5) quy định 11 slide, thuyết trình 10 phút cộng 3-5 phút hỏi đáp, ngày 30/11/2026, demo trực tiếp qua Swagger. Mỗi người nói một cụm slide liền nhau, theo thứ tự mạch chuyện: **A, D, C, B, E**. Thứ tự này trùng với thứ tự slide trong đề.

| Cụm | Slide | Người nói | Thời gian |
|---|---|---|---|
| Dữ liệu | 1 Bìa, 2 Vấn đề + User Story, 3 ERD | A | 1 phút 45 giây |
| Truy cập | 4 API và kiến trúc, 5 JWT và phân quyền | D | 2 phút |
| Nghiệp vụ | 6 Nhập kho và xuất kho | C | 1 phút 30 giây |
| Tồn kho | 7 Tồn kho và cảnh báo | B | 1 phút |
| LLM, demo, Git | 8 LLM, 9 Demo Swagger, 10 Git Flow | E | 3 phút |
| Tổng kết | 11 Đóng góp cá nhân | cả nhóm, theo thứ tự A, D, C, B, E | 30 giây |

## Quy ước chung
- Mỗi slide một ý chính, tối đa 5 dòng chữ; hình và số liệu thay cho đoạn văn. Không đọc nguyên slide (đề trừ điểm cách này).
- Tên gọi thống nhất toàn bộ: phiếu nhập (receipt), phiếu xuất (issue), tồn kho (quantity in stock), mức tối thiểu (MinStock), ADMIN, STAFF, DRAFT, CONFIRMED, CANCELLED.
- Số liệu lấy từ tài liệu trong repo, không tự nghĩ thêm. Mỗi slide ghi sẵn nguồn bên dưới.
- Mỗi người chuẩn bị 1 ảnh chụp dự phòng cho phần của mình, phòng khi demo trực tiếp lỗi.
- Mọi thành viên đều có thể bị hỏi về phần của mình, nên đọc mục "Câu hỏi dự kiến" của slide và mục "vì sao" trong plan của mình.
- Mốc: khung chốt 24/11 (xong), bản nháp 27/11, freeze 28/11, tập dượt có bấm giờ 29/11, thuyết trình 30/11.

---

## Slide 1. Bìa (A)
- Tên đề tài: SmartStock, REST API quản lý kho cho FreshMart. Doanh nghiệp: CLT.
- Tên nhóm và 5 thành viên kèm phần phụ trách một dòng:
  - A: ERD, user story, dữ liệu mẫu
  - D: JWT, phân quyền, tài khoản, phiếu nhập, Swagger
  - C: phiếu xuất, giao dịch, đồng thời
  - B: sản phẩm, nhà cung cấp, cảnh báo tồn kho
  - E: LLM, báo cáo, Git, PPT
- Công nghệ một dòng: Java 17, Spring Boot 3.3, MySQL 8, Flyway, JWT, Swagger, Docker Compose.
- Việc cần điền: tên nhóm, tên đầy đủ từng người.

## Slide 2. Xác định vấn đề + User Story chính (A)
- **Vấn đề thực tế:** FreshMart ghi nhập/xuất kho bằng Excel; nhiều người cùng sửa một file nên tồn kho lệch thực tế; không truy vết được ai nhập hay xuất, lúc nào; chỉ biết hết hàng khi kệ đã trống.
- **Mục tiêu:** mọi thay đổi tồn kho đi qua phiếu nhập hoặc phiếu xuất; mỗi vai trò chỉ làm đúng thao tác được phép; cảnh báo sớm khi sắp hết hàng.
- **Phạm vi:** một kho, khoảng 200 sản phẩm, 2 vai ADMIN và STAFF, 33 endpoint, tính năng LLM gợi ý phiếu nhập. Ngoài phạm vi: giao diện người dùng, thanh toán, nhiều kho.
- **User Story chính (chọn 4-5 trong 11):** phiếu nhập và xác nhận (US-05, US-06), phiếu xuất không để tồn âm (US-07), cảnh báo tồn thấp (US-08), nhập phiếu từ hóa đơn bằng LLM (US-11).
- Nguồn: `docs/user-stories.md`, `docs/plans/member-a-erd-data.md` mục 1.
- Hình: không cần hình phức tạp; có thể dùng 2 cột "trước (Excel)" và "sau (SmartStock)".
- Câu hỏi dự kiến: vì sao chọn phạm vi này; ngoài phạm vi gồm những gì và vì sao.

## Slide 3. Thiết kế ERD (A)
- Hình `docs/erd.png`: 9 bảng (users, categories, suppliers, products, stock_receipts, stock_receipt_details, stock_issues, stock_issue_details, document_counters), mọi quan hệ 1-N.
- Trả lời 3 câu hỏi thiết kế (chọn trong 4 quyết định ở `docs/erd.md`):
  1. Vì sao tách phiếu thành header và detail, và vì sao có UNIQUE `(receipt_id, product_id)` ở tầng DB.
  2. Vì sao lưu `quantity_in_stock` trong `products` thay vì cộng dồn từ phiếu: đọc nhanh, đổi lại phải khóa dòng và có CHECK `>= 0`.
  3. Vì sao xóa mềm (`is_active`) thay vì xóa cứng: giữ khóa ngoại và lịch sử.
  - Dự phòng: bảng `document_counters` giúp mã PN/PX reset mỗi ngày mà không trùng.
- Một câu về kiểm chứng: test `validate` đối chiếu entity với migration trên MySQL thật; `EXPLAIN` cho thấy chưa cần thêm index.
- Nguồn: `docs/erd.md`, `docs/plans/member-a-erd-data.md` mục 7 (8 câu "vì sao").
- Câu hỏi dự kiến: vì sao `BigDecimal` cho tiền, `Instant` UTC cho thời gian; index quan trọng nhất là gì (index `product_id` trên hai bảng detail).

## Slide 4. Tổng quan API và kiến trúc (D)
- 33 endpoint chia theo nhóm: auth, users, categories, products, suppliers, receipts, issues, inventory, reports (ghi sẵn trong `docs/api-contract.md` mục 3).
- **Nguyên tắc RESTful đã áp dụng:** URL là danh từ số nhiều (`/api/receipts`); hành động trạng thái dùng `PATCH /{id}/confirm`; đúng method; mã HTTP có ý nghĩa (200, 201, 400, 401, 403, 404, 409, 503); DTO thay vì trả entity; mọi phản hồi cùng khuôn `{status, message, data}`; tài liệu Swagger kèm nút Authorize.
- **Kiến trúc:** controller -> service -> repository, DTO ở biên, phụ thuộc một chiều. Hình: sơ đồ Mermaid ở đầu `docs/architecture.md` (xuất thành ảnh).
- **Vì sao MVC (monolith) mà không phải MSA:** một kho khoảng 200 sản phẩm, nhóm 5 người, nghiệp vụ cốt lõi cần một transaction ACID; ranh giới module rõ nên có thể tách sau (ứng viên đầu tiên là `llm` và `report`).
- Nguồn: `docs/architecture.md` mục 1-4, `docs/api-contract.md`.
- Câu hỏi dự kiến: vì sao PATCH chứ không phải POST cho confirm; vì sao 409 chứ không phải 400 khi thiếu tồn.

## Slide 5. JWT và phân quyền (D)
- **Đăng nhập:** `POST /api/auth/login` cấp access token hết hạn sau 60 phút, claims `sub`, `email`, `role`. Mật khẩu băm BCrypt, không bao giờ trả ra API. Sai email, sai mật khẩu hoặc tài khoản bị khóa cùng nhận một thông báo 401, không lộ email có tồn tại hay không.
- **Mỗi request:** bộ lọc đọc `Authorization: Bearer`, kiểm token và kiểm tài khoản còn hoạt động, nên khóa tài khoản có hiệu lực ngay chứ không đợi hết 60 phút. 401 là chưa xác thực; 403 là đã đăng nhập nhưng không đủ quyền.
- **Ma trận quyền:** ADMIN toàn quyền, kể cả báo cáo và xác nhận phiếu xuất DAMAGED. STAFF xem dữ liệu, tạo chứng từ, chỉ sửa, xác nhận, hủy chứng từ do chính mình tạo.
- **Hai tầng kiểm quyền:** quyền theo vai trò cố định đặt ở controller (`@PreAuthorize`); quyền phụ thuộc dữ liệu (chủ phiếu, DAMAGED) đặt ở service.
- Danh tính luôn lấy từ token, không bao giờ từ dữ liệu client gửi.
- Hình: bảng ma trận quyền (`docs/api-contract.md` mục 2); ảnh Swagger `/api/auth/me` trả 200 sau Authorize.
- Nguồn: `docs/plans/member-d-security-platform.md`, `docs/api-contract.md` mục 1-2.
- Câu hỏi dự kiến: token bị lộ thì sao (hạn 60 phút, tài khoản khóa là hết hiệu lực ngay); vì sao BCrypt; vì sao không nhận `userId` từ body.

## Slide 6. Nhập kho và xuất kho (C)
- **Trạng thái chứng từ:** DRAFT -> CONFIRMED hoặc DRAFT -> CANCELLED; hai trạng thái cuối bất biến, sai sót sửa bằng chứng từ điều chỉnh mới. Sửa hoặc hủy chứng từ đã chốt trả 409.
- **Xác nhận phiếu trong đúng một transaction:** khóa dòng phiếu, rồi khóa các sản phẩm theo thứ tự id (tránh deadlock), rồi mới kiểm tra và ghi.
  - Phiếu nhập: cộng `quantity` vào tồn từng dòng.
  - Phiếu xuất: kiểm tra **tất cả** dòng trước; có dòng vượt tồn thì trả 409 kèm danh sách thiếu và không trừ dòng nào; đủ thì trừ hết.
- **Quy tắc quyền theo dữ liệu:** STAFF chỉ xác nhận phiếu của mình; phiếu xuất lý do DAMAGED chỉ ADMIN xác nhận. Thứ tự kiểm tra khi confirm: tồn tại (404), chủ phiếu (403), DAMAGED (403), trạng thái (409), tồn kho (409).
- **Đồng thời:** hai người xác nhận cùng lúc hai phiếu cùng sản phẩm thì một thành công, một bị 409, tồn không âm (có test đa luồng). Mã PN/PX sinh bằng một câu upsert nguyên tử; code cũ từng deadlock khi 20 người tạo phiếu đầu ngày cùng lúc, đã sửa và có test.
- Hình: sơ đồ trạng thái chứng từ (vẽ nhanh) và đoạn code ngắn của `confirm` (khóa, kiểm tra, trừ) trong `IssueServiceImpl`.
- Nguồn: `docs/plans/member-c-documents.md` mục 4 và 8, `docs/architecture.md` mục 5.
- Câu hỏi dự kiến: vì sao kiểm tra tất cả dòng trước khi trừ; khóa bi quan so với `@Version`; vì sao khóa theo thứ tự id; `CHECK >= 0` còn cần không khi code đã kiểm tra (là lớp chặn cuối).
- Lưu ý: phiếu nhập do D làm nhưng dùng cùng quy tắc; C chỉ cần nói một câu "phiếu nhập làm cùng cách, chỉ khác là cộng thay vì trừ".

## Slide 7. Tồn kho và cảnh báo (B)
- **Cảnh báo tồn thấp:** `GET /api/inventory/low-stock` trả sản phẩm đang hoạt động có tồn `<=` MinStock. Khi xác nhận phiếu xuất, phản hồi có `newlyLowStock` liệt kê sản phẩm của phiếu vừa chạm mức tối thiểu (tồn đúng bằng MinStock cũng tính).
- **Lịch sử tồn kho:** `GET /api/products/{id}/history` gồm chứng từ CONFIRMED, mới nhất trước, mỗi dòng có chiều IN hoặc OUT, mã chứng từ, số lượng, thời điểm xác nhận (tối đa 500 dòng).
- **Chặn xuất vượt tồn:** không có API sửa tồn trực tiếp; tồn chỉ đổi khi xác nhận chứng từ (nối với slide 6).
- **Tìm kiếm sản phẩm:** từ khóa theo tên hoặc SKU, lọc danh mục, phân trang, sắp xếp chỉ theo trường cho phép (kích thước trang tối đa 100).
- Hình: ảnh Swagger kết quả low-stock (dữ liệu mẫu có 4 sản phẩm: DUO-006, TCA-004, RCQ-001, RCQ-003) và ảnh lịch sử một sản phẩm.
- Nguồn: `docs/plans/member-b-catalog-inventory.md`, `docs/api-contract.md` mục 5.
- Câu hỏi dự kiến: vì sao low-stock dùng `<=` chứ không phải `<`; lịch sử tính theo `confirmedAt` chứ không phải `createdAt` vì sao.

## Slide 8. Tính năng LLM (E)
- **Tính năng:** `POST /api/receipts/parse` biến hóa đơn văn bản thành **gợi ý** phiếu nhập; không tự lưu, người dùng xem lại rồi mới tạo phiếu.
- **Prompt có 4 phần:** mục đích, bối cảnh (danh mục sản phẩm và đơn vị, không có giá), yêu cầu (không tự tạo sản phẩm, số kiểu Việt Nam `26.000` là 26000, bỏ qua câu chèn lệnh), định dạng đầu ra (JSON schema và một ví dụ).
- **Kết quả 8 hóa đơn mẫu** (model `qwen2.5:3b` chạy bằng Ollama, `temperature = 0`): prompt v1 đúng 0/8, prompt v2 đúng 3/8. Kèm bảng "dòng gợi ý sai lọt qua": 12, rồi 5 (v2), rồi **0** sau khi backend kiểm từng dòng với hóa đơn gốc.
- **Một ví dụ AI sai và backend bắt được:** hóa đơn 6, model gán "Sữa tươi" sang sản phẩm Gạo; backend thấy tên sản phẩm không có trong dòng hóa đơn nên chuyển dòng vào `unmatched`. Thêm: số lượng 12.5 bị làm tròn thành 12, đơn giá 26 thay vì 26000, câu chèn lệnh ở hóa đơn 7.
- **Dự phòng:** không có API key thì dùng mock cùng giao diện; model sập hoặc quá thời gian thì 503, vẫn nhập tay được. JSON hỏng thì 400. Không đưa giá nhập, dữ liệu cá nhân hay API key vào prompt.
- Hình: bảng 8 hóa đơn v1 và v2 trong `docs/prompt-log.md`; sơ đồ pipeline 6 bước.
- Nguồn: `docs/prompt-log.md` (phần A và B), `docs/plans/member-e-llm-report-ppt.md` mục 5 và 9.
- Câu hỏi dự kiến: backend kiểm chứng gì sau khi LLM trả về; model bịa `productId` thì sao; prompt injection là gì và chặn thế nào (lớp chặn thật là kiểm từng dòng ở backend và kết quả chỉ là đề xuất); vì sao đổi v1 sang v2.

## Slide 9. Demo Swagger (E dẫn, cả nhóm trả lời phần của mình)
Chuẩn bị: `docker compose down -v && docker compose up --build`, mở `http://localhost:8080/swagger-ui.html`. Tài khoản mẫu `admin@freshmart.local`, `staff1@freshmart.local`, mật khẩu mẫu trong README. Dữ liệu seed: 30 sản phẩm, 10 chứng từ.

| Bước | Thao tác | Kết quả mong đợi | Phần của |
|---|---|---|---|
| 1 | STAFF đăng nhập, bấm Authorize, gọi `GET /api/auth/me` | 200, không có `passwordHash` | D |
| 2 | STAFF gọi `POST /api/products` | **403** | D |
| 3 | `POST /api/receipts/parse` với `NCC Nông sản Xanh - Cà chua 40kg x 26.000; Khoai tây 60kg x 20.000` | 2 dòng gợi ý, có `suggestedSupplierId`, `"mock"` đúng với chế độ đang chạy | E |
| 4 | Tạo phiếu nhập từ gợi ý, rồi `PATCH .../confirm` | 200, tồn Cà chua (RCQ-001) tăng 15 lên 55 | D |
| 5 | Tạo phiếu xuất 100 sản phẩm RCQ-005 (Chuối, tồn 30), confirm | **409**, `data` có `requested 100`, `available 30`, tồn không đổi | C |
| 6 | Tạo phiếu xuất 21 sản phẩm RCQ-005, confirm | 200, `newlyLowStock` có RCQ-005 (tồn 9, MinStock 10) | C |
| 7 | `GET /api/inventory/low-stock` và `GET /api/products/{id}/history` | RCQ-005 xuất hiện ở low-stock; lịch sử có dòng OUT | B |
| 8 | ADMIN gọi `GET /api/reports/stock-summary` (và bản `export` tải Excel hoặc CSV) | tồn đầu + nhập - xuất = tồn cuối | E |

- Số liệu tồn ở bảng trên lấy từ dữ liệu seed hiện tại; chạy thử một lần trước ngày 30/11 để xác nhận và sửa số nếu seed đổi.
- Việc cần làm: chụp ảnh mọi bước làm bản dự phòng; tập dượt có bấm giờ để demo gói trong khoảng 1 phút.
- Nguồn: `docs/plans/00-shared.md` mục 7.

## Slide 10. Git Flow (E)
- **Cấu trúc nhánh:** `main` là bản phát hành, `develop` là nhánh tích hợp, `feature/...` hoặc `fix/...` là nhánh làm việc; không push thẳng vào `main` hoặc `develop`.
- **Quy tắc:** mọi thay đổi vào `develop` qua Pull Request; CI `build-test` phải xanh; commit theo `feat:`, `fix:`, `docs:`, `refactor:`, `test:`; không commit secret; mỗi thành viên có PR đã merge; một người tạo migration tại một thời điểm (đã dùng V1, không cần thêm).
- **Thẻ phát hành:** `v0.1.0` (bản tích hợp đầu) và `v1.0.0` (khi nộp).
- **Vấn đề phối hợp đã gặp (kể thật):**
  1. Phần của một thành viên dựng thành project riêng, khác phiên bản và cấu trúc so với khung chung nên phải đối chiếu và tách thành các PR nhỏ.
  2. Một test đồng thời trên `develop` bị lỗi làm đỏ CI của mọi nhánh; phải merge bản sửa trước rồi cập nhật các nhánh còn lại (bài học về thứ tự merge).
  3. Hai PR cùng sửa các dòng sát nhau trong `api-contract.md` gây xung đột; giải bằng cách giữ cả hai thay đổi.
- Hình: ảnh chụp danh sách PR thật, trang Actions CI xanh, đồ thị nhánh.
- Nguồn: `docs/git-flow.md`, danh sách PR trên GitHub.
- Việc cần điền: số PR đã merge của từng người (lấy lúc gần ngày thuyết trình, sau khi chốt).

## Slide 11. Đóng góp cá nhân (cả nhóm, thứ tự A, D, C, B, E)
Mỗi người 2-3 dòng: đã làm gì (kèm một con số hoặc một sản phẩm cụ thể) và một bài học chính. Gợi ý nội dung để điền:

| Người | Đã làm | Bài học (điền) |
|---|---|---|
| A | ERD 9 bảng, 11 user story có mã HTTP, dữ liệu mẫu 10 chứng từ tạo qua service thật, test entity khớp schema | |
| D | JWT, ma trận quyền, quản lý tài khoản, phiếu nhập, định dạng phản hồi chung, Swagger | |
| C | Phiếu xuất, xác nhận an toàn khi đồng thời, sinh mã chứng từ không trùng, test đa luồng | |
| B | Sản phẩm, nhà cung cấp, tìm kiếm có phân trang, cảnh báo tồn thấp, lịch sử | |
| E | LLM parse và đo 8 hóa đơn, báo cáo tồn kho và xuất Excel/CSV, Git, tài liệu | |

---

## Việc cần làm theo người
| Người | Việc | Hạn |
|---|---|---|
| A | Điền slide 1-3, xuất `erd.png` lên slide, chọn 3 câu hỏi ERD | 27/11 |
| D | Điền slide 4-5, chụp ảnh Authorize và 403 | 27/11 |
| C | Điền slide 6, vẽ sơ đồ trạng thái, chọn đoạn code `confirm` | 27/11 |
| B | Điền slide 7, chụp ảnh low-stock và lịch sử | 27/11 |
| E | Điền slide 8-10, ghép bộ PPT, kiểm tên gọi thống nhất và số liệu | 27/11 |
| Cả nhóm | Ghi nhật ký dùng AI của mình vào `docs/prompt-log.md` mục B (mỗi người, kèm ít nhất một ví dụ AI sai) | 28/11 |
| Cả nhóm | Tập dượt bấm giờ 10 phút, mỗi người trả lời thử câu hỏi dự kiến | 29/11 |

## Checklist nộp bài (đề, trang 15)
- File PPT (tối đa 16 slide; bộ này 11 slide), có đủ sơ đồ bắt buộc.
- Link GitHub: nhánh, PR, lịch sử commit.
- README: cấu hình MySQL, cách chạy migration, tài khoản mẫu cho từng vai.
- `docs/`: `user-stories.md`, `erd.md` kèm `erd.png`, `architecture.md`, `prompt-log.md`.
- Dữ liệu mẫu: 1 ADMIN, 2 STAFF, 5 danh mục, 30 sản phẩm, 3 nhà cung cấp, 10 chứng từ.
