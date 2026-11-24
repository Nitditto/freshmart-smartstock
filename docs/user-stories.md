# User Stories – SmartStock (FreshMart)

> Owner: Member A. Bản nháp dưới đây bám sát đề; nhóm rà soát, sửa lời và bổ sung. Yêu cầu: ≥ 8 story, mỗi story 2–3 tiêu chí chấp nhận **kiểm thử được** (có thể viết thành test/Swagger demo).

## US-01 Đăng nhập
**Với vai trò** nhân viên/quản trị, **tôi muốn** đăng nhập bằng email và mật khẩu **để** truy cập hệ thống an toàn.
- [ ] `POST /api/auth/login` đúng email/mật khẩu → 200 và nhận access token hết hạn sau 60 phút.
- [ ] Sai email, sai mật khẩu hoặc tài khoản bị khóa → 401 với cùng một thông báo.
- [ ] Không phản hồi nào chứa `passwordHash`.

## US-02 Phân quyền theo vai trò
**Với vai trò** ADMIN, **tôi muốn** hệ thống giới hạn thao tác theo vai trò **để** nhân viên không thay đổi dữ liệu danh mục.
- [ ] Không có token / token hết hạn → 401.
- [ ] STAFF gọi API chỉ dành cho ADMIN (tạo sản phẩm, báo cáo, quản lý user) → 403.
- [ ] ADMIN gọi các API đó → không bị 401/403 (200/201 tùy endpoint).

## US-03 Quản lý sản phẩm
**Với vai trò** ADMIN, **tôi muốn** tạo, sửa và ẩn sản phẩm **để** danh mục hàng luôn chính xác.
- [ ] SKU trùng → 409; giá < 0 hoặc MinStock < 0 → 400.
- [ ] `DELETE /api/products/{id}` → 200, sản phẩm chỉ bị ẩn (soft delete); không còn trong `GET /api/products`, thêm vào chứng từ mới → 400.
- [ ] Gửi `quantityInStock` trong `PUT /api/products/{id}` không làm đổi tồn (200, tồn giữ nguyên); STAFF tạo/sửa/ẩn → 403.

## US-04 Tìm kiếm sản phẩm
**Với vai trò** nhân viên kho, **tôi muốn** tìm sản phẩm theo từ khóa và danh mục, có phân trang **để** tra cứu nhanh.
- [ ] `GET /api/products?keyword=` khớp tên hoặc SKU, không phân biệt hoa thường → 200.
- [ ] Lọc `categoryId`, sắp xếp theo trường cho phép, trả `totalElements/totalPages`; sort ngoài whitelist hoặc `size > 100` → 400.
- [ ] `GET /api/products/{id}` → 200 kèm tồn hiện tại; id không tồn tại → 404.

## US-05 Lập phiếu nhập
**Với vai trò** nhân viên kho, **tôi muốn** tạo phiếu nhập nhiều dòng từ nhà cung cấp **để** ghi nhận hàng về.
- [ ] `POST /api/receipts` hợp lệ → 201, phiếu DRAFT, mã dạng `PN-yyyyMMdd-xxx`, đếm lại từ 001 mỗi ngày.
- [ ] Không có dòng, `quantity ≤ 0` hoặc lặp sản phẩm → 400; NCC/sản phẩm không tồn tại → 404.
- [ ] `createdById` trong phản hồi là người trong token, kể cả khi body gửi kèm `createdBy` khác.

## US-06 Xác nhận phiếu nhập
**Với vai trò** nhân viên kho, **tôi muốn** xác nhận phiếu nhập **để** tồn kho được cộng đúng.
- [ ] `PATCH /api/receipts/{id}/confirm` → 200, tồn của từng dòng tăng đúng `quantity`, trạng thái CONFIRMED.
- [ ] Xác nhận/sửa/hủy phiếu đã CONFIRMED hoặc CANCELLED → 409.
- [ ] STAFF không xác nhận được phiếu của người khác (403); ADMIN thì được.

## US-07 Lập và xác nhận phiếu xuất
**Với vai trò** nhân viên kho, **tôi muốn** xuất hàng qua phiếu xuất **để** tồn kho không bao giờ âm.
- [ ] `PATCH /api/issues/{id}/confirm` có dòng vượt tồn → 409 kèm danh sách sản phẩm thiếu; **không** dòng nào bị trừ.
- [ ] Hai phiếu cùng sản phẩm được xác nhận đồng thời khi tồn chỉ đủ cho một phiếu → một 200, một 409; tồn không âm.
- [ ] Phiếu lý do `DAMAGED` chỉ ADMIN xác nhận được; STAFF → 403.

## US-08 Cảnh báo tồn kho thấp
**Với vai trò** quản lý, **tôi muốn** thấy các sản phẩm sắp hết **để** nhập thêm trước khi kệ trống.
- [ ] `GET /api/inventory/low-stock` → 200, chỉ sản phẩm đang hoạt động có tồn ≤ MinStock (gồm cả bằng).
- [ ] Xác nhận phiếu xuất → 200, `newlyLowStock` liệt kê sản phẩm vừa tụt xuống mức tối thiểu.

## US-09 Lịch sử nhập – xuất của sản phẩm
**Với vai trò** quản lý, **tôi muốn** xem lịch sử nhập/xuất một sản phẩm **để** truy vết ai làm gì, lúc nào.
- [ ] `GET /api/products/{id}/history` → 200, chỉ gồm chứng từ CONFIRMED, mới nhất trước; id không tồn tại → 404.
- [ ] Mỗi dòng cho biết chiều (IN/OUT), mã chứng từ, số lượng, thời điểm xác nhận.

## US-10 Báo cáo biến động tồn kho
**Với vai trò** ADMIN, **tôi muốn** báo cáo tồn đầu – nhập – xuất – tồn cuối theo khoảng ngày **để** đối chiếu sổ kho.
- [ ] `from > to` → 400; STAFF → 403.
- [ ] `GET /api/reports/stock-summary` → 200; với mỗi sản phẩm: tồn đầu + nhập − xuất = tồn cuối, chỉ tính chứng từ CONFIRMED.

## US-11 Nhập phiếu từ hóa đơn bằng AI
**Với vai trò** nhân viên kho, **tôi muốn** dán nội dung hóa đơn để hệ thống gợi ý phiếu nhập **để** đỡ gõ tay.
- [ ] `POST /api/receipts/parse` → 200, kết quả chỉ là đề xuất; số phiếu nhập trong DB không đổi.
- [ ] `productId` không tồn tại/đã ẩn hoặc `quantity ≤ 0` → dòng vào `unmatched`; AI trả JSON sai định dạng → 400.
- [ ] Không có API key → chạy bằng mock với cùng giao diện; AI không khả dụng → 503 rõ ràng.
