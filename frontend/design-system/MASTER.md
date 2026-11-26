# Design system - SmartStock (frontend)

Nguồn quyết định: skill `ui-ux-pro-max` (truy vấn "warehouse inventory management dashboard", mật độ cao) cộng điều chỉnh của nhóm. Đây là nguồn chung cho mọi màn hình; đổi phong cách thì sửa file này trước.

## Hướng thiết kế
- **Phong cách:** Flat Design, nền sáng, không đổ bóng và không gradient. Phù hợp công cụ quản trị dữ liệu: dễ đọc, nhanh, ít phân tâm.
- **Mật độ:** cao (dashboard). Bảng gọn, khoảng cách 8-32 px; vẫn giữ vùng bấm tối thiểu 36 px cho nút trong bảng và 44 px cho nút chính trên màn hình nhỏ.
- **Ngôn ngữ:** toàn bộ chữ hiển thị bằng tiếng Việt; tiền `vi-VN` (VND), ngày giờ `Asia/Ho_Chi_Minh`.

## Màu (token CSS, khai báo trong `src/index.css`)
| Token | Giá trị | Dùng cho |
|---|---|---|
| `--color-primary` | `#334155` (slate 700) | sidebar, tiêu đề bảng, nút phụ |
| `--color-accent` | `#047857` (xanh lá đậm) | nút hành động chính, trạng thái tốt |
| `--color-background` | `#F8FAFC` | nền trang |
| `--color-card` | `#FFFFFF` | thẻ, bảng, dialog |
| `--color-foreground` | `#0F172A` | chữ chính |
| `--color-muted-foreground` | `#475569` | chữ phụ |
| `--color-border` | `#E2E8F0` | viền |
| `--color-danger` | `#DC2626` | lỗi, thiếu tồn, hủy |
| `--color-warning` | `#B45309` | cảnh báo tồn thấp |

- Skill đề xuất xanh `#059669` với chữ đen; nhóm đổi sang `#047857` để chữ trắng trên nút đạt tương phản AA (khoảng 5,5:1).
- Trạng thái chứng từ luôn là **màu + chữ**, không chỉ màu: DRAFT (xám, "Nháp"), CONFIRMED (xanh, "Đã xác nhận"), CANCELLED (đỏ, "Đã hủy").

## Chữ
- **Be Vietnam Pro** (gói `@fontsource/be-vietnam-pro`, có subset tiếng Việt), cài qua npm nên chạy được khi mất mạng lúc demo; dự phòng `system-ui`.
- Cỡ nền 16 px, dòng 1,5. Số liệu trong bảng dùng `tabular-nums` để cột số thẳng hàng.
- Skill gợi ý cặp Cormorant Garamond/Crimson Pro nhưng đó là phong cách học thuật, không hợp dashboard nên không dùng.

## Bố cục
- Sidebar cố định bên trái (thu gọn thành menu trên màn hình hẹp), header có tên người dùng, vai trò và nút đăng xuất. Nội dung tối đa khoảng 1280 px.
- Mỗi màn hình theo khuôn: tiêu đề + hành động chính ở góc phải, bộ lọc, bảng/biểu mẫu, trạng thái rỗng/đang tải/lỗi rõ ràng.
- Hỗ trợ laptop và tablet; không cuộn ngang trang (bảng rộng cuộn ngang trong khung riêng).

## Hành vi (checklist bắt buộc cho mọi màn hình)
- Mọi thao tác ghi: nút có trạng thái đang xử lý (disable), toast thành công hoặc lỗi, hiển thị `message` tiếng Việt từ server.
- Nút chỉ có biểu tượng phải có `aria-label`; con trỏ `pointer` cho phần tử bấm được; vòng focus nhìn thấy khi dùng bàn phím.
- Thao tác nguy hiểm (hủy phiếu, ẩn sản phẩm) có hộp xác nhận.
- Lỗi 409 thiếu tồn: dialog liệt kê từng sản phẩm thiếu (cần, hiện có).
- Biểu tượng dùng SVG (Lucide), không dùng emoji.
- Tôn trọng `prefers-reduced-motion`; chuyển trạng thái 150-200 ms.
- Ẩn nút hoặc menu theo vai trò chỉ để tiện dùng; quyền thật do backend (403).

## Điều cần tránh
Đổ bóng nặng, hiệu ứng 3D, trang trí thừa, chữ nhỏ hơn 12 px, màu là cách duy nhất truyền đạt trạng thái.
