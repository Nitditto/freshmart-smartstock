# ERD – SmartStock

> Owner: Member A. Schema thật nằm ở `src/main/resources/db/migration/V1__init_schema.sql` (Flyway). Xuất hình `docs/erd.png` từ sơ đồ này (hoặc dbdiagram.io / MySQL Workbench) để nộp.

```mermaid
erDiagram
    USERS ||--o{ STOCK_RECEIPTS : "created_by"
    USERS ||--o{ STOCK_ISSUES : "created_by"
    SUPPLIERS ||--o{ STOCK_RECEIPTS : "supplier_id"
    CATEGORIES ||--o{ PRODUCTS : "category_id"
    STOCK_RECEIPTS ||--|{ STOCK_RECEIPT_DETAILS : "receipt_id"
    STOCK_ISSUES ||--|{ STOCK_ISSUE_DETAILS : "issue_id"
    PRODUCTS ||--o{ STOCK_RECEIPT_DETAILS : "product_id"
    PRODUCTS ||--o{ STOCK_ISSUE_DETAILS : "product_id"

    USERS {
        bigint id PK
        varchar full_name
        varchar email UK
        varchar password_hash
        varchar role "ADMIN|STAFF"
        boolean is_active
        datetime created_at
    }
    CATEGORIES {
        bigint id PK
        varchar name UK
    }
    SUPPLIERS {
        bigint id PK
        varchar name
        varchar phone
        varchar address
    }
    PRODUCTS {
        bigint id PK
        varchar sku UK
        varchar name
        bigint category_id FK
        varchar unit
        decimal price
        int quantity_in_stock "CHECK >= 0"
        int min_stock
        boolean is_active "soft delete"
        datetime created_at
        datetime updated_at
    }
    STOCK_RECEIPTS {
        bigint id PK
        varchar code UK "PN-yyyyMMdd-xxx"
        bigint supplier_id FK
        varchar status "DRAFT|CONFIRMED|CANCELLED"
        varchar note
        bigint created_by FK
        datetime created_at
        datetime confirmed_at
    }
    STOCK_RECEIPT_DETAILS {
        bigint id PK
        bigint receipt_id FK
        bigint product_id FK
        int quantity "CHECK > 0"
        decimal unit_price
    }
    STOCK_ISSUES {
        bigint id PK
        varchar code UK "PX-yyyyMMdd-xxx"
        varchar reason "SALE|DAMAGED|INTERNAL"
        varchar status "DRAFT|CONFIRMED|CANCELLED"
        varchar note
        bigint created_by FK
        datetime created_at
        datetime confirmed_at
    }
    STOCK_ISSUE_DETAILS {
        bigint id PK
        bigint issue_id FK
        bigint product_id FK
        int quantity "CHECK > 0"
    }
    DOCUMENT_COUNTERS {
        bigint id PK
        varchar prefix "PN|PX"
        date counter_date
        int last_seq "UK(prefix, counter_date)"
    }
```

## Quan hệ và lý do thiết kế (chuẩn bị cho slide 3)

| Quan hệ | Kiểu | Vì sao |
|---|---|---|
| Category → Product | 1 – N | Mỗi sản phẩm thuộc đúng 1 danh mục; lọc theo `categoryId`. |
| Supplier → StockReceipt | 1 – N | Mỗi phiếu nhập từ 1 NCC; NCC có thể có nhiều phiếu. |
| StockReceipt → StockReceiptDetail | 1 – N (bắt buộc ≥ 1) | Tách **header/detail** để một phiếu có nhiều dòng (chuẩn hóa, không lặp cột). `ON DELETE CASCADE`. |
| StockIssue → StockIssueDetail | 1 – N | Như trên. |
| Product → *Detail | 1 – N | Truy vết mọi biến động của 1 sản phẩm (lịch sử, báo cáo). |
| User → StockReceipt / StockIssue (`created_by`) | 1 – N | Truy vết ai tạo; cơ sở của quy tắc "STAFF chỉ thao tác phiếu của mình". |
| (receipt_id, product_id), (issue_id, product_id) | UNIQUE | Chặn lặp sản phẩm trong cùng chứng từ ở tầng DB, không chỉ ở code. |

## Quyết định thiết kế cần giải thích được

1. **Vì sao có `quantity_in_stock` trong `products` (denormalize) thay vì tính từ tổng nhập – xuất?**
   Đọc tồn kho là thao tác nóng (danh sách sản phẩm, low-stock, chặn xuất quá tồn); cộng dồn hàng nghìn dòng mỗi lần là lãng phí. Đổi lại phải đảm bảo nhất quán: chỉ đổi trong transaction khi xác nhận chứng từ, khóa dòng sản phẩm (`SELECT … FOR UPDATE`), thêm `CHECK (quantity_in_stock >= 0)` làm lớp chặn cuối. Lịch sử vẫn tính lại được từ các dòng chứng từ CONFIRMED.
2. **Vì sao soft delete (`is_active`) thay vì xóa sản phẩm?** Sản phẩm đã xuất hiện trong chứng từ cũ; xóa cứng làm hỏng khóa ngoại và lịch sử. Ẩn thì không thêm được vào chứng từ mới nhưng chứng từ cũ vẫn đọc được.
3. **Vì sao có bảng `document_counters`?** Sinh mã `PN-yyyyMMdd-xxx` reset mỗi ngày mà không cần job định kỳ: khóa theo `(prefix, ngày)`, tăng `last_seq` trong cùng transaction tạo chứng từ → không trùng mã khi nhiều người tạo đồng thời.
4. **Vì sao không sửa chứng từ đã xác nhận?** Để sổ kho bất biến (audit). Sai sót → tạo chứng từ điều chỉnh mới.

> Đề có nhắc "trả lời 3 câu hỏi thiết kế" ở slide 3 nhưng file đề không liệt kê 3 câu cụ thể. Bốn câu trên là phần nhóm tự chuẩn bị; **hỏi lại BTC/doanh nghiệp** để chắc chắn câu nào nằm trong 3 câu đó.

## Index đã có trong V1
`products(category_id)`, `products(name)`, `products(is_active, quantity_in_stock, min_stock)` (low-stock), `stock_receipts(status, created_at)`, `stock_receipts(created_by)`, tương tự cho `stock_issues`, và index theo `product_id` trên hai bảng detail (lịch sử sản phẩm).
