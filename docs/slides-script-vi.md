# Kịch bản đọc cho bài thuyết trình SmartStock

Slide trình bày bằng tiếng Anh (`docs/SmartStock-slides.pptx`); file này là lời nói tiếng Việt cho từng slide, phần quan trọng in **đậm**. Cùng nội dung nằm trong phần ghi chú (Notes) của từng slide trong file PPT, cũng có chữ đậm.

Thành viên: Vũ Đức Nam và Nguyễn Tiến Đạt (A), Ong Thế Minh (B), Đinh Văn Minh (C), Nguyễn Văn Trường (D và E). Nam và Đạt chia nhau slide 1-2 và 3, đổi chỗ được. Trường nói hai lần: slide 4-6 (phần D) và slide 11-15 (phần E). Các slide có chữ real screenshots dùng ảnh chụp thật từ hệ thống đang chạy.

Thời lượng chỉ là ước lượng theo số từ (khoảng 190 từ mỗi phút, tổng khoảng 17 phút 17 giây), không phải mốc cố định. Mỗi người nên đọc thử có bấm giờ và nói bằng lời của mình; nếu thiếu giờ thì đọc các cụm in đậm trước.

| Slide | Người nói | Ước lượng |
|---|---|---|
| 1. Title | Vũ Đức Nam (A) | 20 giây |
| 2. Problem and user stories | Vũ Đức Nam (A) | 35 giây |
| 3. ERD: 8 linked tables, 1 code counter | Nguyễn Tiến Đạt (A) | 1 phút 15 giây |
| 4. API overview and architecture | Nguyễn Văn Trường (D) | 1 phút 35 giây |
| 5. JWT and authorization | Nguyễn Văn Trường (D) | 1 phút 45 giây |
| 6. Swagger run: token works, role blocked (real screenshots) | Nguyễn Văn Trường (D) | 35 giây |
| 7. Confirming a document in one transaction | Đinh Văn Minh (C) | 90 giây |
| 8. Real run: a short stock is refused (real screenshots) | Đinh Văn Minh (C) | 45 giây |
| 9. Stock levels and early warning | Ong Thế Minh (B) | 1 phút 50 giây |
| 10. Real run: low-stock alerts and history (real screenshots) | Ong Thế Minh (B) | 40 giây |
| 11. LLM suggests receipts, the backend verifies | Nguyễn Văn Trường (E) | 1 phút 50 giây |
| 12. Real run: LLM suggestion on the UI (real screenshot) | Nguyễn Văn Trường (E) | 45 giây |
| 13. Live demo on Swagger | Nguyễn Văn Trường (E) | 1 phút 15 giây |
| 14. Real run: stock movement report (real screenshot) | Nguyễn Văn Trường (E) | 35 giây |
| 15. Git Flow | Nguyễn Văn Trường (E) | 1 phút 5 giây |
| 16. Individual contributions | Cả nhóm | 30 giây |

## Slide 1. Title

Người nói: Vũ Đức Nam (A) (ước lượng 20 giây)

Em chào thầy cô. Nhóm em làm **SmartStock**, một **REST API quản lý kho cho chuỗi cửa hàng FreshMart**. Nhóm có 5 người, chia 5 phần: **A** là ERD, user story, dữ liệu mẫu, do em và bạn Tiến Đạt làm; **B** là sản phẩm và cảnh báo tồn kho, bạn Thế Minh; **C** là phiếu xuất và xử lý đồng thời, bạn Văn Minh; **D và E** là JWT, phiếu nhập, LLM, báo cáo và Git, do bạn Văn Trường làm. Công nghệ là **Java 17, Spring Boot 3.3, MySQL 8**, chạy bằng **một lệnh Docker Compose**.

## Slide 2. Problem and user stories

Người nói: Vũ Đức Nam (A) (ước lượng 35 giây)

Bài toán: FreshMart đang ghi nhập xuất kho trong **một file Excel dùng chung**. Hai người sửa cùng lúc là **tồn kho lệch**, không biết ai nhập, ai xuất, lúc nào, và thường **kệ trống rồi mới biết hết hàng**. SmartStock đổi cách làm: **mọi thay đổi tồn kho đều đi qua phiếu nhập hoặc phiếu xuất**, không có API sửa số tồn trực tiếp. Có hai vai trò **ADMIN và STAFF**, và hệ thống tự cảnh báo khi tồn chạm mức tối thiểu. Tổng cộng **11 user story, 33 endpoint**; trọng tâm là phiếu nhập, **xuất kho không bao giờ làm tồn âm**, cảnh báo tồn thấp và **đọc hóa đơn bằng LLM**. Ngoài phạm vi: thanh toán và nhiều kho.

Câu hỏi dự kiến:
- Vì sao chọn phạm vi này? Một kho khoảng 200 sản phẩm là vừa sức nhóm 5 người; thanh toán và nhiều kho là hướng mở rộng sau.

## Slide 3. ERD: 8 linked tables, 1 code counter

Người nói: Nguyễn Tiến Đạt (A) (ước lượng 1 phút 15 giây)

Phần dữ liệu có **9 bảng**: **8 bảng nghiệp vụ liên kết với nhau bằng quan hệ một nhiều**, gồm users, categories, suppliers, products, hai bảng của phiếu nhập và hai bảng của phiếu xuất. Bảng thứ chín là **document_counters thì đứng riêng, không có khóa ngoại**, vì nó chỉ là bộ đếm để sinh mã phiếu như PN-20261126-001: mỗi dòng ứng với một cặp loại phiếu và ngày, nên sang ngày mới số đếm tự quay về 001. Phiếu chỉ chép con số đó vào cột code, không tham chiếu ngược lại. Em xin nêu ba quyết định thiết kế chính. Một, mỗi phiếu tách thành **header và detail**, và có ràng buộc **UNIQUE theo cặp phiếu và sản phẩm** ngay trong database, nên dù code có lỗi cũng không thể có hai dòng cùng một sản phẩm trong một phiếu. Hai, số tồn lưu thẳng ở cột **quantity_in_stock** của bảng products thay vì cộng dồn từ lịch sử phiếu: đọc rất nhanh, đổi lại phải khóa dòng khi ghi, và em thêm **CHECK lớn hơn hoặc bằng 0** làm chốt chặn cuối cùng. Ba, dùng **xóa mềm** bằng cột is_active, để sản phẩm ngừng bán vẫn còn trong các phiếu cũ. Dữ liệu mẫu có 30 sản phẩm, 3 nhà cung cấp và **10 chứng từ được tạo qua chính service thật**, không chèn tay vào bảng, nên số tồn luôn khớp với lịch sử phiếu.

Câu hỏi dự kiến:
- Vì sao document_counters không nối với bảng nào? Nó chỉ là bộ đếm theo (loại phiếu, ngày) để sinh mã; phiếu chép số vào cột code, không tham chiếu ngược, nên khóa ngoại sẽ gây phụ thuộc vòng.
- Vì sao BigDecimal cho tiền, Instant UTC cho thời gian? Tránh sai số số thực; UTC để không lệch múi giờ, khi hiển thị mới đổi sang Asia/Ho_Chi_Minh.
- Index quan trọng nhất? Index product_id trên hai bảng detail.

## Slide 4. API overview and architecture

Người nói: Nguyễn Văn Trường (D) (ước lượng 1 phút 35 giây)

Em phụ trách phần API, bảo mật, tài khoản và phiếu nhập. Trước hết là bức tranh chung: API có **33 endpoint**, chia theo các nhóm auth, users, categories, products, suppliers, receipts, issues, inventory và reports, và tất cả thử được ngay trên **Swagger**, có nút Authorize để dán token. Một request đi qua **bộ lọc JWT** trước, rồi tới controller, service, repository và cuối cùng là MySQL. Em muốn nhấn vào cách chia việc giữa các tầng: **controller chỉ nhận request, kiểm tra dữ liệu đầu vào bằng @Valid rồi trả kết quả; toàn bộ nghiệp vụ và transaction nằm ở service**; repository chỉ làm việc với dữ liệu. Service còn đổi entity sang DTO, nên entity và mật khẩu băm không bao giờ lọt ra ngoài API. Mọi phản hồi, kể cả lỗi, đều có cùng khuôn **status, message, data**: lỗi nghiệp vụ được ném thành ApiException, và chỉ **một chỗ duy nhất là GlobalExceptionHandler** đổi nó thành JSON, nên giao diện chỉ cần một cách xử lý lỗi. Về REST: URL là danh từ số nhiều như /api/receipts; **chuyển trạng thái dùng PATCH**, ví dụ PATCH /api/receipts/5/confirm; và mã HTTP đúng nghĩa: **400 là dữ liệu sai, 401 là chưa đăng nhập, 403 là đã đăng nhập nhưng không đủ quyền, 404 là không có, 409 là xung đột với trạng thái hiện tại, 503 là khi AI không chạy**. Nhóm chọn **MVC monolith, không dùng microservice**, vì chỉ có một kho khoảng 200 sản phẩm, và việc quan trọng nhất, tức là xác nhận phiếu rồi đổi tồn nhiều dòng, cần **một transaction ACID duy nhất**. Nếu tách service thì phải làm saga cho đúng việc mà MySQL đã làm sẵn. Ranh giới module vẫn rõ, nên nếu hệ thống lớn lên thì llm và report là hai phần tách ra trước.

Câu hỏi dự kiến:
- Vì sao PATCH chứ không POST cho confirm? PATCH là đổi một phần trạng thái của tài nguyên đã có, không tạo tài nguyên mới.
- Vì sao 409 chứ không 400 khi thiếu tồn? Dữ liệu request đúng, nhưng xung đột với trạng thái hiện tại của kho.

## Slide 5. JWT and authorization

Người nói: Nguyễn Văn Trường (D) (ước lượng 1 phút 45 giây)

Giờ em nói về bảo mật. Gọi /api/auth/login sẽ nhận **access token sống 60 phút**, trong token có id người dùng, email và role. Mật khẩu lưu bằng **BCrypt**, không dùng SHA-256 hay MD5, vì BCrypt cố ý chậm và có salt riêng cho từng mật khẩu, nên dò mật khẩu rất tốn thời gian. Hệ thống **stateless**: token nằm trong header chứ không nằm trong cookie, nên không cần session và có thể tắt CSRF. Em xin nói ba điểm cụ thể. Một, **sai email, sai mật khẩu hay tài khoản bị khóa đều trả cùng một câu 401**, để người ngoài không dò được email nào có tồn tại. Hai, mỗi request bộ lọc đều **kiểm lại tài khoản còn hoạt động**, nên khi ADMIN khóa ai thì người đó bị chặn ngay, không phải đợi token hết hạn. Ba, **danh tính luôn lấy từ token**, không bao giờ lấy từ body, vì client có thể gửi người tạo là ai cũng được. Bảng bên trái là ma trận quyền. ADMIN làm được hết. **STAFF chỉ sửa, xác nhận và hủy phiếu do chính mình tạo**, và không xem được báo cáo. Quyền theo vai trò cố định em đặt ở controller bằng PreAuthorize, còn quyền **phụ thuộc dữ liệu**, như chủ phiếu hay phiếu hàng hỏng, thì phải đọc database mới biết nên đặt ở service. Về tài khoản: ADMIN tạo tài khoản và khóa hoặc mở khóa, nhưng **không tự khóa chính mình**, nếu làm sẽ nhận 409; đổi mật khẩu phải nhập đúng mật khẩu hiện tại. Cuối cùng là phiếu nhập: nhân viên lập phiếu nháp, sửa hoặc hủy, và khi xác nhận thì cộng tồn. Dù chỉ là cộng, em vẫn **khóa dòng sản phẩm**, vì hai transaction cùng làm stock bằng stock cộng q có thể đọc cùng một số cũ rồi ghi đè nhau, gọi là lost update, nhất là khi phiếu nhập chạy song song với phiếu xuất của bạn Văn Minh. Em xin chuyển cho bạn Minh.

Câu hỏi dự kiến:
- Token bị lộ thì sao? Token chỉ sống 60 phút, và khóa tài khoản có hiệu lực ngay vì mỗi request đều kiểm tài khoản.
- Vì sao BCrypt mà không phải SHA-256? BCrypt cố ý chậm và có salt riêng, chống dò mật khẩu hàng loạt.
- Vì sao tắt được CSRF? Token nằm trong header Authorization, không phải cookie nên trình duyệt không tự gửi kèm.
- Vì sao không nhận userId từ body? Client có thể giả mạo; danh tính lấy từ token.
- Vì sao cộng tồn của phiếu nhập vẫn phải khóa? Hai transaction cùng đọc số cũ rồi ghi đè sẽ mất cập nhật (lost update).

## Slide 6. Swagger run: token works, role blocked (real screenshots)

Người nói: Nguyễn Văn Trường (D) (ước lượng 35 giây)

Đây là kết quả chạy thật trên Swagger, không phải ảnh minh họa. Bên trái, em đăng nhập bằng tài khoản **STAFF**, dán token vào nút Authorize rồi gọi **/api/auth/me**: kết quả **200**, trả hồ sơ người dùng với vai trò STAFF và **không có passwordHash**. Bên phải, vẫn dùng đúng token đó, em gọi **POST /api/products** thì nhận **403**, kèm thông báo tiếng Việt là bạn không có quyền thực hiện thao tác này. Hai ảnh cho thấy hai điều cùng lúc: **token hợp lệ thì vào được**, và **vai trò STAFF thì bị chặn ở việc của ADMIN**. Mọi lỗi đều trả về cùng khuôn status, message, data, nên giao diện xử lý được ngay.

## Slide 7. Confirming a document in one transaction

Người nói: Đinh Văn Minh (C) (ước lượng 90 giây)

Em phụ trách phiếu xuất, và phần khó nhất là **xác nhận phiếu**. Phiếu bắt đầu ở **DRAFT**, rồi sang **CONFIRMED** hoặc **CANCELLED**. Hai trạng thái cuối **không sửa được**, sai thì lập phiếu điều chỉnh. Khi bấm xác nhận, hệ thống làm bốn việc trong **một transaction**: khóa dòng phiếu, khóa các sản phẩm trong phiếu **theo thứ tự id**, kiểm tra **tất cả** các dòng, rồi mới trừ tồn. Ví dụ tồn Chuối đang là 30, em lập phiếu xuất 100, hệ thống trả **409** kèm danh sách 'cần 100, hiện có 30' và **không trừ gì cả**. Nếu phiếu có hai dòng, một dòng đủ, một dòng thiếu, thì dòng đủ cũng không bị trừ: **hoặc cả phiếu, hoặc không gì cả**. Thứ tự kiểm tra là: 404 nếu không tồn tại, 403 nếu không phải chủ phiếu, 403 nếu là **phiếu hàng hỏng mà người xác nhận không phải ADMIN**, rồi 409 sai trạng thái, 409 thiếu tồn. Về đồng thời, hai người xác nhận cùng lúc hai phiếu cùng một sản phẩm thì **một thành công, một bị 409, tồn không bao giờ âm**, và có test đa luồng chứng minh. Một lỗi nhóm đã gặp: bản đầu sinh mã phiếu PN, PX bị **deadlock khi 20 luồng cùng tạo phiếu**. Em thay bằng một câu **INSERT ... ON DUPLICATE KEY UPDATE** nguyên tử và thêm test 20 luồng. Phiếu nhập làm cùng cách, chỉ khác là cộng thay vì trừ.

Câu hỏi dự kiến:
- Vì sao kiểm tra tất cả dòng trước khi trừ? Để không trừ dở dang; thiếu một dòng thì không trừ dòng nào.
- Khóa bi quan so với @Version? Xung đột cao ở cùng một sản phẩm thì khóa dòng đơn giản và đúng ngay lần đầu, không phải thử lại.
- Vì sao khóa theo thứ tự id? Mọi transaction khóa cùng thứ tự nên không deadlock.
- CHECK >= 0 còn cần không? Cần, là lớp chặn cuối nếu code có lỗi.

## Slide 8. Real run: a short stock is refused (real screenshots)

Người nói: Đinh Văn Minh (C) (ước lượng 45 giây)

Đây là lần chạy thật của đúng ví dụ em vừa nói. Ảnh bên trái là form tạo phiếu xuất: em chọn **100 chuối trong khi tồn chỉ có 30**, giao diện hiện cảnh báo vàng dưới ô số lượng nhưng vẫn cho **lưu thành phiếu nháp**, vì tồn chỉ bị kiểm khi xác nhận. Ảnh bên phải là lúc bấm **Xác nhận**: hộp thoại liệt kê dòng thiếu, **cần 100, hiện có 30, thiếu 70**, và ghi rõ **phiếu chưa được xác nhận, tồn kho không bị trừ**. Ảnh nhỏ ở dưới là cùng quy tắc đó trên API: **409** kèm đúng danh sách thiếu gồm sản phẩm, số cần và số hiện có. Nhờ trả về danh sách thiếu mà người dùng biết ngay cần giảm bao nhiêu hoặc nhập thêm bao nhiêu.

## Slide 9. Stock levels and early warning

Người nói: Ong Thế Minh (B) (ước lượng 1 phút 50 giây)

Em phụ trách sản phẩm, nhà cung cấp và tồn kho. Trước hết là danh mục: **chỉ ADMIN được tạo, sửa và ẩn sản phẩm**, còn STAFF chỉ xem. SKU là duy nhất và không phân biệt hoa thường, trùng thì trả **409**. Khi sửa sản phẩm, **không thể sửa số tồn**: dù gửi kèm quantityInStock thì hệ thống bỏ qua, vì tồn chỉ đổi khi xác nhận chứng từ. Sản phẩm bị ẩn thì **không thể thêm vào phiếu mới**, thử sẽ nhận 400, nhưng vẫn xem được theo id và vẫn nằm trong lịch sử các phiếu cũ. Về tìm kiếm: gõ tên hoặc SKU, lọc theo danh mục, phân trang. Em có hai chốt bảo vệ: **chỉ sắp xếp theo các cột cho phép**, nên gửi sort theo một trường lạ như passwordHash sẽ bị 400; và **mỗi trang tối đa 100 dòng**. Ngoài ra em **escape ký tự % và _**, để người dùng gõ vào ô tìm không bị hiểu thành ký tự đại diện của SQL. Điểm em muốn nhấn nhất là **cảnh báo tồn thấp**. Endpoint low-stock trả các sản phẩm đang bán có **tồn nhỏ hơn hoặc bằng mức tối thiểu**. Em dùng dấu **bằng** chứ không phải nhỏ hơn, vì tồn đúng bằng mức tối thiểu là lúc cần đặt hàng rồi. Dữ liệu mẫu hiện có **4 sản phẩm** như vậy: nước cam ép đã hết, tôm sú, cà chua và cà rốt sắp hết. Khi xác nhận phiếu xuất, kết quả có thêm trường **newlyLowStock**, chỉ gồm những sản phẩm **vừa chạm** mức tối thiểu nhờ phiếu này, để nhân viên biết ngay mà báo, không phải mở danh sách ra tìm. Cuối cùng là lịch sử: lịch sử một sản phẩm lấy từ các phiếu **đã xác nhận**, mới nhất trước, mỗi dòng có chiều nhập hoặc xuất, mã phiếu và số lượng, tối đa 500 dòng. Em tính theo **thời điểm xác nhận** chứ không phải thời điểm tạo phiếu, vì đó mới là lúc tồn thật sự đổi. Em xin chuyển cho bạn Trường.

Câu hỏi dự kiến:
- Vì sao ẩn sản phẩm thay vì xóa? Giữ khóa ngoại và lịch sử các phiếu cũ; ẩn rồi thì không thêm vào phiếu mới được (400).
- Vì sao không có API sửa tồn trực tiếp? Mọi thay đổi tồn phải có chứng từ để truy vết được ai, lúc nào, vì sao.
- Vì sao low-stock dùng <= chứ không <? Tồn đúng bằng mức tối thiểu cũng cần đặt thêm hàng.
- Vì sao lịch sử theo confirmedAt? Đó là lúc tồn thật sự đổi; createdAt chỉ là lúc lập phiếu nháp.
- Vì sao whitelist sort và giới hạn size? Chặn sort theo trường nhạy cảm và chặn tải cả bảng một lần (bảo mật, hiệu năng).

## Slide 10. Real run: low-stock alerts and history (real screenshots)

Người nói: Ong Thế Minh (B) (ước lượng 40 giây)

Đây là các màn hình thật của phần tồn kho. Ảnh lớn bên trái là **Tổng quan kho**: hiện có **5 sản phẩm tồn thấp**. Bốn sản phẩm đầu là từ dữ liệu mẫu, còn **chuối** vào danh sách vì vừa xuất 21, tồn còn 9 trên mức tối thiểu 10. Ảnh trên bên phải là hộp thoại hiện ngay sau khi xác nhận phiếu xuất đó: đây chính là trường **newlyLowStock**, chỉ liệt kê sản phẩm **vừa chạm** mức tối thiểu nhờ phiếu này, chứ không phải cả danh sách tồn thấp. Ảnh dưới là **lịch sử của chuối**, chỉ gồm các phiếu đã xác nhận, mỗi dòng có chiều nhập hoặc xuất, mã phiếu và số lượng.

## Slide 11. LLM suggests receipts, the backend verifies

Người nói: Nguyễn Văn Trường (E) (ước lượng 1 phút 50 giây)

Phần em phụ trách là LLM, báo cáo và Git. Em bắt đầu với LLM. Người dùng dán **nội dung hóa đơn của nhà cung cấp** vào, hệ thống **gợi ý** các dòng phiếu nhập và **không tự lưu**; người dùng xem lại rồi mới tạo phiếu. Nhóm dùng model **qwen2.5 3b chạy bằng Ollama ngay trên máy**, nên không tốn tiền và dữ liệu không đi ra ngoài; nếu không cấu hình thì hệ thống dùng một bản mô phỏng cùng giao diện để vẫn chạy được. Backend gửi cho model **danh mục sản phẩm** cùng hóa đơn, nhưng **không gửi giá nhập hay dữ liệu cá nhân**. Prompt có bốn phần: mục đích, bối cảnh là danh mục sản phẩm, yêu cầu như số kiểu Việt Nam 26.000 là 26000 và bỏ qua câu chèn lệnh, và định dạng đầu ra là JSON kèm một ví dụ. Em còn đặt temperature bằng 0 cho kết quả ổn định, và **không giữ kết nối database trong lúc chờ model**, vì model có thể nghĩ vài chục giây. Em chạy thử **8 hóa đơn mẫu tự soạn**: prompt đầu đúng **0 trên 8**, sau khi sửa prompt đúng **3 trên 8**. Model nhỏ nên sai khá nhiều, ví dụ đọc 26.000 thành 26, hoặc làm tròn 12,5 ký thành 12. Vì vậy phần quan trọng nhất là **backend kiểm lại từng dòng với hóa đơn gốc**: tên sản phẩm có nằm trong dòng không, số lượng có nguyên vẹn không, đơn vị và đơn giá có khớp không. Số dòng sai lọt qua người dùng **giảm từ 12 xuống 0**, như biểu đồ bên trái. Ví dụ thật: model đọc dòng Sữa tươi nhưng gán sang **Gạo**; backend thấy chữ Sữa tươi không có trong dòng của Gạo nên **loại và chuyển sang nhập tay**. Một hóa đơn có câu chèn thêm 1000 cà phê: prompt đầu làm theo thật, bản sau thì bỏ qua. Còn nếu Ollama tắt thì API trả **503**, và nhân viên vẫn nhập phiếu tay như bình thường.

Câu hỏi dự kiến:
- Backend kiểm chứng gì sau khi LLM trả về? Tên sản phẩm có trong dòng, số lượng nguyên vẹn, đơn vị và đơn giá khớp, sản phẩm còn hoạt động.
- Model bịa productId thì sao? Bị loại vì không có trong danh mục, dòng chuyển sang nhập tay.
- Prompt injection chặn thế nào? Lớp chặn thật là kiểm từng dòng ở backend, và kết quả chỉ là đề xuất, không tự lưu.
- Vì sao đổi v1 sang v2? v2 thêm quy tắc số kiểu Việt Nam, bỏ qua câu chèn lệnh và có ví dụ đầu ra; kết quả đúng từ 0/8 lên 3/8.
- Vì sao parse không giữ transaction database? Model có thể mất vài chục giây; giữ kết nối trong lúc đó sẽ làm cạn pool kết nối.

## Slide 12. Real run: LLM suggestion on the UI (real screenshot)

Người nói: Nguyễn Văn Trường (E) (ước lượng 45 giây)

Đây là màn hình nhập từ hóa đơn khi chạy **model thật**: nhãn trên đầu ghi **Gợi ý từ AI**, không phải dữ liệu mô phỏng. Hóa đơn mẫu có ba dòng. Model nhận đúng **cà chua 40, giá 26.000** và nhà cung cấp là Nông sản Xanh. Hai dòng còn lại, khoai tây và bánh mì sandwich, **bị chuyển sang nhập tay**, vì backend không chắc chắn nên không đoán. Em nói thẳng với thầy cô: model 3 tỷ tham số **bỏ sót nhiều hơn mong muốn**, và phần lý do bằng tiếng Anh ở dưới là **do chính model tự viết ra**, không phải thông báo của backend. Nhưng điều quan trọng với kho là **không có dòng sai nào lọt vào phiếu**. Muốn model đầy đủ hơn thì chỉ cần đổi sang model lớn hơn trong cấu hình, phần kiểm tra của backend giữ nguyên.

## Slide 13. Live demo on Swagger

Người nói: Nguyễn Văn Trường (E) (ước lượng 1 phút 15 giây)

Bây giờ em demo tám bước trên Swagger, trên dữ liệu vừa dựng lại từ đầu. Bước một, đăng nhập bằng **STAFF** rồi bấm Authorize, gọi /auth/me thấy 200 và **không có passwordHash**. Bước hai, thử tạo sản phẩm thì bị **403**, vì đó là việc của ADMIN. Bước ba, dán hóa đơn mẫu vào /receipts/parse để lấy các dòng gợi ý, em để ý có kèm nhà cung cấp được đề xuất. Bước bốn, tạo **phiếu nhập từ gợi ý** rồi confirm: tồn cà chua tăng từ 15 lên 55, vì hóa đơn nhập 40. Sang phần xuất, bước năm: phiếu xuất **100 chuối khi tồn chỉ có 30**, bấm confirm bị **409** kèm danh sách cần 100, hiện có 30, và tồn vẫn nguyên. Bước sáu: giờ xuất **21**, thành công, và **newlyLowStock có chuối** vì tồn còn 9, dưới mức tối thiểu 10. Bước bảy: em kiểm tra ở low-stock thấy chuối đã xuất hiện, và lịch sử sản phẩm có dòng xuất vừa rồi. Bước tám, đăng nhập **ADMIN** và xem báo cáo tồn kho theo khoảng ngày. Công thức là **tồn đầu kỳ cộng nhập trừ xuất bằng tồn cuối kỳ**, chỉ tính phiếu đã xác nhận, theo ngày xác nhận, và ngày kết thúc tính trọn ngày. Em tải được cả file **Excel**, hoặc CSV nếu muốn, để mở thẳng trong Excel. Nếu máy hay mạng lỗi, nhóm có ảnh chụp từng bước làm dự phòng.

Câu hỏi dự kiến:
- Vì sao báo cáo tính theo ngày xác nhận và to tính trọn ngày? Tồn chỉ đổi khi xác nhận; tính trọn ngày để phiếu xác nhận lúc 23:59 của ngày kết thúc vẫn được tính.

## Slide 14. Real run: stock movement report (real screenshot)

Người nói: Nguyễn Văn Trường (E) (ước lượng 35 giây)

Đây là báo cáo tồn kho của **ADMIN**, trang này STAFF không vào được. Chọn khoảng ngày thì mỗi sản phẩm có **tồn đầu, nhập, xuất, tồn cuối**, theo công thức **tồn đầu cộng nhập trừ xuất bằng tồn cuối**. Chỉ tính các phiếu **đã xác nhận**, theo **ngày xác nhận**, và ngày kết thúc được tính **trọn ngày**. Có hai nút tải về là **Excel** và **CSV**. Em thêm bản Excel vì khi mở CSV, Excel trên máy tiếng Việt thường dồn cả dòng vào một cột do khác dấu phân cách; file xlsx thì luôn đúng cột, dòng đầu in đậm.

## Slide 15. Git Flow

Người nói: Nguyễn Văn Trường (E) (ước lượng 1 phút 5 giây)

Cuối cùng là Git. Nhóm dùng **Git Flow đơn giản**: **main** là bản phát hành, **develop** là nhánh tích hợp, mỗi việc làm trên một nhánh riêng đặt tên như feature gạch tên phần gạch việc, hoặc fix. **Không ai push thẳng vào main hay develop**: mọi thay đổi vào develop đều qua **Pull Request** theo mẫu có sẵn, và phải có **CI xanh**, gồm build, toàn bộ test backend và cả phần giao diện. Commit viết theo kiểu feat, fix, docs, test, tiêu đề ngắn, và tuyệt đối không commit secret như mật khẩu hay JWT secret. Hiện đã có **hơn 30 PR** được merge, tag **v0.1.0** đã có và **v1.0.0** sẽ gắn khi nộp bài. Ba vấn đề phối hợp nhóm đã gặp thật. Một, **có một phần được dựng thành project riêng**, khác cấu trúc chung, nên phải đối chiếu và tách thành nhiều PR nhỏ. Hai, **một test đồng thời bị lỗi** trên develop làm đỏ CI của mọi nhánh, bài học là phải merge bản sửa trước rồi mới cập nhật các nhánh còn lại. Ba, **hai PR sửa các dòng sát nhau** trong file api-contract gây xung đột, nhóm giải quyết bằng cách giữ cả hai thay đổi. Em xin kết thúc phần của mình ở đây.

Câu hỏi dự kiến:
- Vì sao Git Flow thay vì commit thẳng main? Có nhánh tích hợp và PR để CI chạy trước khi vào nhánh chính, lỗi của một người không làm hỏng bản phát hành.

## Slide 16. Individual contributions

Người nói: Cả nhóm (ước lượng 30 giây)

Cuối cùng là đóng góp của từng người, mỗi người nói một hai câu. **Nam và Đạt**: ERD 9 bảng, 11 user story, dữ liệu mẫu 10 chứng từ; bài học là **để chính database giữ luật** bằng UNIQUE và CHECK, không chỉ trông vào code. **Trường (phần D)**: JWT, ma trận quyền, phiếu nhập, Swagger; bài học là **quyền phụ thuộc dữ liệu phải kiểm ở service**. **Văn Minh**: phiếu xuất và xác nhận an toàn khi đồng thời; bài học là **khóa theo thứ tự id và phải có test đa luồng** thì mới tin được. **Thế Minh**: sản phẩm, tìm kiếm, cảnh báo tồn thấp, lịch sử; bài học là **escape ký tự LIKE và giới hạn kích thước trang**. **Trường (phần E)**: LLM, báo cáo Excel và CSV, Git; bài học là **không tin đầu ra của LLM, phải kiểm từng dòng ở backend**. Em xin cảm ơn thầy cô, nhóm sẵn sàng trả lời câu hỏi.

## Lưu ý khi demo (slide 9)

- Chạy sạch: `docker compose down -v` rồi `docker compose up --build`; mở Swagger tại `http://localhost:8080/swagger-ui.html`.
- Số tồn trong các bước demo (cà chua 15 lên 55, chuối tồn 30) lấy từ dữ liệu mẫu hiện tại; chạy thử một lần trước ngày thuyết trình.
- Nếu dùng model thật (Ollama), hóa đơn mẫu có thể chỉ ra 1 dòng gợi ý (cà chua) và đưa khoai tây vào nhập tay; hãy nói rõ đó là backend kiểm từng dòng.
- Chuẩn bị ảnh chụp từng bước làm bản dự phòng.
