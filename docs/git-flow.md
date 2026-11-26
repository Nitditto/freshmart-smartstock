# Git Flow - quy tắc của nhóm

## Nhánh
| Nhánh | Mục đích |
|---|---|
| `main` | Bản phát hành cuối. Chỉ merge từ `develop` khi chốt (tag `v1.0.0`). |
| `develop` | Nhánh tích hợp. Mọi PR vào đây. |
| `feature/{ten}` | Mỗi người một nhánh, ví dụ `feature/a-erd-seed`, `feature/b-products`, `feature/c-documents`, `feature/d-auth`, `feature/e-llm`. Có thể tách thêm `feature/b-low-stock`... để có >= 3 PR/người. |

## Quy tắc
- **Không push thẳng** vào `main`/`develop` (bật branch protection: bắt buộc PR + 1 approval).
- Mỗi PR vào `develop`, **>= 1 thành viên khác review**, CI (`mvn verify`) phải xanh.
- **Mỗi thành viên >= 3 PR đã merge** -> chia việc thành các PR nhỏ (một endpoint / một nhóm test / một tài liệu).
- Commit theo quy ước: `feat:`, `fix:`, `docs:`, `refactor:`, `test:` (ví dụ `feat: add low-stock endpoint`).
- Tag: **`v0.1.0` cuối tuần 1** (dự kiến thứ Hai 23/11), **`v1.0.0` khi nộp**.
- Không bao giờ commit: `.env`, `application-local.yml`, `target/`, JWT secret, API key, connection string thật.
  Lỡ commit secret -> coi như lộ, **đổi secret** (xóa khỏi lịch sử không đủ).

## Quy trình một PR
```bash
git checkout develop && git pull
git checkout -b feature/b-products
# ... code + test ...
mvn -B verify
git add -p && git commit -m "feat: implement product search with paging"
git push -u origin feature/b-products
# mở PR vào develop, điền template, gắn reviewer
```
Sau khi merge: xóa nhánh, `git checkout develop && git pull`.

## Migration (Flyway) - chỉ 1 người tạo tại một thời điểm
Vì hai người cùng tạo `V2__...sql` sẽ xung đột, **số version được cấp phát trước**:

| Version | Dành cho | Dùng khi |
|---|---|---|
| `V1` | Member A | Schema ban đầu (đã có) |
| `V2`-`V3` | Member A | Chỉnh schema sau review ERD |
| `V4` | Member B | Index/ cột bổ sung cho sản phẩm (nếu cần) |
| `V5` | Member C | Ràng buộc/ index cho chứng từ (nếu cần) |
| `V6` | Member D | Bảng/cột liên quan tài khoản (nếu cần) |
| `V7` | Member E | Bảng log LLM (nếu cần) |
| `V8+` | Ai cần thì báo nhóm trước | |

Quy tắc: **không sửa migration đã merge** - muốn đổi thì thêm version mới. Tên file: `V{n}__{mo_ta_ngan}.sql`.

## Lịch gợi ý (30/11 là ngày thuyết trình dự kiến)
| Ngày | Mốc |
|---|---|
| Thứ Sáu 20/11 | Kickoff, chốt ERD + api-contract, mọi người clone skeleton, tạo nhánh |
| 21-22/11 | Mỗi người làm phần lõi của mình, PR đầu tiên |
| Thứ Hai 23/11 | Merge hết vào `develop`, tag **v0.1.0** |
| 24-27/11 | Hoàn thiện nghiệp vụ, unit test, tích hợp, LLM + 8 hóa đơn |
| 28/11 | Đóng băng tính năng (feature freeze), làm slide, README, docs |
| 29/11 | Tập dượt + demo Swagger, tag **v1.0.0** |
| 30/11 | Thuyết trình |
