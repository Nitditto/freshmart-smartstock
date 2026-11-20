# Prompt Log (Member E chủ trì; mọi người ghi phần AI mình dùng)

> Mục đích: chứng minh nhóm dùng AI **có kiểm chứng**. Ghi trung thực, kể cả khi AI sai.

## A. Prompt của tính năng LLM trong sản phẩm (`POST /api/receipts/parse`)

File prompt: `src/main/resources/prompts/receipt-parse-v1.system.txt` và `.user.txt`.

| Thành phần bắt buộc | Nằm ở đâu trong prompt |
|---|---|
| Mục đích | Dòng đầu system prompt |
| Bối cảnh (danh sách sản phẩm + đơn vị) | `{{PRODUCT_CATALOG}}` trong user prompt |
| Yêu cầu (không tự tạo sản phẩm) | Quy tắc 2–4 trong system prompt |
| Định dạng đầu ra (JSON schema) | Cuối system prompt |

### Kết quả trên 8 hóa đơn mẫu
| # | Hóa đơn (mô tả ngắn) | v1: đúng/sai | v2: đúng/sai | Ghi chú lỗi |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |
| 4 | | | | |
| 5 | | | | |
| 6 | | | | |
| 7 | | | | |
| 8 | | | | |
| | **Tỷ lệ chính xác** | **…/8** | **…/8** | |

Thay đổi prompt v1 → v2 (cụ thể thêm/bớt gì và vì sao): …

Tiêu chí "đúng": mọi dòng khớp `productId`, `quantity`, `unitPrice`; không có sản phẩm bịa; dòng không chắc nằm trong `unmatched`.

## B. Nhật ký dùng AI khi phát triển

| Ngày | Người | Hỏi AI gì | AI trả lời (tóm tắt) | Nhóm sửa / từ chối gì & vì sao |
|---|---|---|---|---|
| | | | | |

Ví dụ AI sai/thiếu và cách nhóm kiểm chứng (ít nhất 1 mục để trình bày): …
