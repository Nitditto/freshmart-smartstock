# User Stories – SmartStock (FreshMart)

> Owner: Member A. Bản nháp dưới đây bám sát đề; nhóm rà soát, sửa lời và bổ sung. Yêu cầu: ≥ 8 story, mỗi story 2–3 tiêu chí chấp nhận **kiểm thử được** (có thể viết thành test/Swagger demo).

## US-01 Đăng nhập
**Với vai trò** nhân viên/quản trị, **tôi muốn** đăng nhập bằng email và mật khẩu **để** truy cập hệ thống an toàn.
- [ ] Đăng nhập đúng → 200 và nhận access token hết hạn sau 60 phút.
- [ ] Sai email, sai mật khẩu hoặc tài khoản bị khóa → 401 với cùng một thông báo.
- [ ] Không phản hồi nào chứa `passwordHash`.

## US-02 Phân quyền theo vai trò
**Với vai trò** ADMIN, **tôi muốn** hệ thống giới hạn thao tác theo vai trò **để** nhân viên không thay đổi dữ liệu danh mục.
- [ ] Không có token / token hết hạn → 401.
- [ ] STAFF gọi API chỉ dành cho ADMIN (tạo sản phẩm, báo cáo, quản lý user) → 403.
- [ ] ADMIN gọi được tất cả API.

## US-03 Quản lý sản phẩm
**Với vai trò** ADMIN, **tôi muốn** tạo, sửa và ẩn sản phẩm **để** danh mục hàng luôn chính xác.
- [ ] SKU trùng → 409; giá < 0 hoặc MinStock < 0 → 400.
- [ ] "Xóa" sản phẩm chỉ ẩn (soft delete); sản phẩm ẩn không xuất hiện trong danh sách và không thêm được vào chứng từ mới.
- [ ] Không có API nào sửa trực tiếp số lượng tồn kho.

## US-04 Tìm kiếm sản phẩm
**Với vai trò** nhân viên kho, **tôi muốn** tìm sản phẩm theo từ khóa và danh mục, có phân trang **để** tra cứu nhanh.
- [ ] `keyword` khớp tên hoặc SKU, không phân biệt hoa thường.
- [ ] Lọc `categoryId`, sắp xếp theo trường cho phép, trả `totalElements/totalPages`.
- [ ] Chi tiết sản phẩm hiển thị tồn kho hiện tại.

## US-05 Lập phiếu nhập
**Với vai trò** nhân viên kho, **tôi muốn** tạo phiếu nhập nhiều dòng từ nhà cung cấp **để** ghi nhận hàng về.
- [ ] Phiếu mới ở trạng thái DRAFT, mã dạng `PN-yyyyMMdd-xxx`, đếm lại từ 001 mỗi ngày.
- [ ] Phiếu phải có ≥ 1 dòng, `Quantity > 0`, một sản phẩm không lặp hai lần.
- [ ] Người tạo lấy từ token, không từ dữ liệu client gửi.

## US-06 Xác nhận phiếu nhập
**Với vai trò** nhân viên kho, **tôi muốn** xác nhận phiếu nhập **để** tồn kho được cộng đúng.
- [ ] Xác nhận → tồn của từng dòng tăng đúng `Quantity`, trạng thái CONFIRMED.
- [ ] Xác nhận/sửa/hủy phiếu đã CONFIRMED hoặc CANCELLED → 409.
- [ ] STAFF không xác nhận được phiếu của người khác (403); ADMIN thì được.

## US-07 Lập và xác nhận phiếu xuất
**Với vai trò** nhân viên kho, **tôi muốn** xuất hàng qua phiếu xuất **để** tồn kho không bao giờ âm.
- [ ] Có dòng vượt tồn → 409 kèm danh sách sản phẩm thiếu; **không** dòng nào bị trừ.
- [ ] Lỗi giữa chừng → rollback toàn bộ (transaction).
- [ ] Phiếu lý do `DAMAGED` chỉ ADMIN xác nhận được; STAFF → 403.

## US-08 Cảnh báo tồn kho thấp
**Với vai trò** quản lý, **tôi muốn** thấy các sản phẩm sắp hết **để** nhập thêm trước khi kệ trống.
- [ ] `GET /api/inventory/low-stock` trả sản phẩm có tồn ≤ MinStock.
- [ ] Kết quả xác nhận phiếu xuất nêu các sản phẩm vừa tụt xuống mức tối thiểu.

## US-09 Lịch sử nhập – xuất của sản phẩm
**Với vai trò** quản lý, **tôi muốn** xem lịch sử nhập/xuất một sản phẩm **để** truy vết ai làm gì, lúc nào.
- [ ] Chỉ gồm chứng từ đã CONFIRMED, mới nhất trước.
- [ ] Mỗi dòng cho biết chiều (IN/OUT), mã chứng từ, số lượng, thời điểm.

## US-10 Báo cáo biến động tồn kho
**Với vai trò** ADMIN, **tôi muốn** báo cáo tồn đầu – nhập – xuất – tồn cuối theo khoảng ngày **để** đối chiếu sổ kho.
- [ ] `from > to` → 400; STAFF → 403.
- [ ] Với mỗi sản phẩm: tồn đầu + nhập − xuất = tồn cuối.

## US-11 Nhập phiếu từ hóa đơn bằng AI
**Với vai trò** nhân viên kho, **tôi muốn** dán nội dung hóa đơn để hệ thống gợi ý phiếu nhập **để** đỡ gõ tay.
- [ ] Kết quả chỉ là đề xuất; hệ thống không tự lưu phiếu.
- [ ] Mọi `productId` phải tồn tại và `quantity > 0`; JSON sai định dạng → 400.
- [ ] Không có API key → chạy bằng mock với cùng giao diện; AI không khả dụng → 503 rõ ràng.
