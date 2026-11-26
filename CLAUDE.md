# CLAUDE.md

@AGENTS.md

`AGENTS.md` (import ở trên) là quy tắc chung cho cả nhóm và mọi agent. File này bổ sung **rule chi tiết** về code, design pattern, commit, comment và cách Claude Code làm việc. Khi có mâu thuẫn: lời người dùng > `docs/api-contract.md` > file này > `AGENTS.md`.

---

## 1. Cách làm việc với repo
- Bắt đầu task: đọc `docs/plans/00-shared.md` + plan của người đang làm (`docs/plans/member-<a-e>-*.md`) + phần liên quan của `docs/api-contract.md`. Chưa rõ người dùng là thành viên nào → hỏi, rồi **chỉ sửa file thuộc phần của họ**.
- Task nhiều bước/đụng nhiều file → lập kế hoạch ngắn rồi mới sửa; task nhỏ → làm thẳng. Làm đúng phạm vi, thay đổi nhỏ nhất đủ đạt mục tiêu.
- Tìm kiếm có mục tiêu (Grep/Glob), không đọc cả thư mục. Trước khi viết service mới, đọc `CategoryServiceImpl`, `DocumentGuard`, `GlobalExceptionHandler`.
- Xong mỗi nhóm việc: `mvn -B verify` (lặp nhanh: `mvn test -Dtest=<Lớp>`). Không có Maven/Docker → **nói rõ chưa chạy được**, không đoán kết quả. Test fail → báo nguyên văn, không báo "xong".
- Không tự `git commit/push`, mở PR, đổi nhánh khi chưa được yêu cầu. Không sửa `.env`, `application-local.yml`; không in secret ra màn hình/log.
- Không thêm dependency, không đổi version, không sửa migration đã merge, không đổi hợp đồng API nếu chưa được đồng ý.
- Trả lời bằng tiếng Việt, ngắn gọn, nêu file đã sửa (`path:line`) và phần chưa kiểm chứng; thuật ngữ kỹ thuật giữ tiếng Anh.

## 2. Quy tắc code Java

### Định dạng & đặt tên
- Java 17, indent **4 space**, tối đa **120 cột**, một lớp public một file, import không dùng dấu `*`, không import thừa (theo `.editorconfig`).
- Lớp/record/enum `PascalCase`; method/biến `camelCase`; hằng `UPPER_SNAKE_CASE`; package toàn chữ thường; bảng/cột SQL `snake_case`.
- Tên nói **ý nghĩa nghiệp vụ**, không viết tắt khó hiểu (`quantityInStock`, không `qty`/`qis`). Boolean đặt dạng `isX/hasX/canX`. Method là động từ (`confirm`, `findLowStock`); không `Util`/`Helper`/`Manager` chung chung.
- Hậu tố nhất quán: `XxxController`, `XxxService` + `XxxServiceImpl`, `XxxRepository`, `XxxRequest`, `XxxResponse`, `XxxException`.

### Cách viết
- Method **ngắn** (≈ ≤ 30 dòng), làm **một việc**, độ lồng ≤ 3; dùng *early return / guard clause* thay `if-else` lồng sâu. Quá 4 tham số → gói thành record.
- Ưu tiên **bất biến**: `final`, `record`, `List.of`, `stream().toList()`. Tránh `null` làm giá trị nghiệp vụ; dùng `Optional` cho **giá trị trả về** (không làm tham số/field).
- Không magic number/string: dùng hằng, enum hoặc `AppProperties`. Không code chết, không biến thừa, không `catch (Exception)` nuốt lỗi, không `e.printStackTrace()`.
- Collection/Stream: không `stream` cho việc đơn giản đọc kém hơn vòng `for`; không sửa collection khi đang lặp; so sánh `BigDecimal` bằng `compareTo`, không `equals`; so sánh chuỗi tránh NPE.
- Lombok: chỉ `@Getter/@Setter/@NoArgsConstructor/@RequiredArgsConstructor/@Slf4j` như codebase hiện có; không `@Data` trên entity; không `@Builder` trừ khi thật sự giúp.
- Log SLF4J (`@Slf4j`), tham số hóa (`log.info("Confirmed {}", code)`), đúng mức (`error` chỉ cho lỗi bất ngờ, không log 4xx do người dùng). Không log secret/token/mật khẩu/hóa đơn đầy đủ.

### Controller / Service / Repository / DTO
- **Controller**: chỉ `@Valid` + gọi service + bọc `ApiResponse`; có `@Operation`; không logic, không gọi repository, không trả entity.
- **Service**: constructor injection (`@RequiredArgsConstructor`, field `private final`), `@Transactional` mỗi method (đọc → `readOnly = true`), ném `ApiException`, map entity → DTO **trong** service. Không gọi service của feature khác.
- **Repository**: query rõ ràng (`@Query` JPQL/derived), tham số có tên, có `@EntityGraph` khi cần; không logic nghiệp vụ.
- **DTO**: `record`, validate ở request, có `static from(Entity)` ở response. Entity không lộ ra ngoài service.
- Phân tách trách nhiệm: quyền theo **vai trò cố định** ở controller (`@PreAuthorize`); quyền **phụ thuộc dữ liệu** (sở hữu, `DAMAGED`) ở service (`DocumentGuard`, `SecurityUtils.isAdmin()`).

## 3. Design pattern & nguyên tắc thiết kế
Dùng pattern **khi nó giải quyết vấn đề có thật**; không thêm lớp trừu tượng "cho đẹp".

| Pattern / nguyên tắc | Dùng ở đâu trong repo | Quy tắc |
|---|---|---|
| **Layered (MVC)** | Controller → Service → Repository | Phụ thuộc một chiều từ trên xuống, không gọi ngược |
| **Dependency Inversion / DI** | Service phụ thuộc interface; `LlmService` | Code vào interface; implement chọn bằng cấu hình |
| **Strategy** | `LlmService` ↔ `MockLlmService` / `HttpLlmService` | Thêm provider mới = thêm implement, không `if (mock)` rải rác |
| **Factory method** | `ApiException.notFound(..)`, `XxxResponse.from(..)` | Tạo đối tượng qua factory có tên, không `new ApiException(...)` lặp |
| **DTO / Assembler** | `dto/*`, `from(Entity)` | Ranh giới API tách khỏi mô hình dữ liệu |
| **Repository** | Spring Data | Che giấu truy cập dữ liệu; query thuộc repository |
| **Template/Guard** | `DocumentGuard`, `PageableGuard` | Luật dùng chung gom một nơi, stateless, có test |
| **State (đơn giản)** | `DocumentStatus` DRAFT→CONFIRMED/CANCELLED | Chuyển trạng thái chỉ qua service; trạng thái cuối bất biến |
| **Unit of Work** | `@Transactional` | Một nghiệp vụ = một transaction; lỗi → rollback toàn bộ |

- **SOLID** thực dụng: một lớp một lý do thay đổi (S); mở rộng bằng implement mới (O); phụ thuộc interface (D). Không tạo interface cho thứ chỉ có một cài đặt mãi mãi *trừ* mẫu `XxxService/XxxServiceImpl` của repo.
- **DRY có chừng mực**: lặp 3 lần mới trích hàm; nhưng **luật nghiệp vụ** (quyền sở hữu, trạng thái, khóa) phải ở một nơi.
- **YAGNI / KISS**: không thêm cấu hình, cờ, lớp trừu tượng cho nhu cầu chưa có. Ưu tiên giải pháp đơn giản, dễ đọc, dễ test.
- **Fail fast**: validate ở biên và dừng sớm; không để dữ liệu sai chạy sâu vào nghiệp vụ.
- **Tách đọc/ghi hợp lý**: method chỉ đọc `readOnly = true`; method ghi không trả về thứ đọc từ nơi khác một cách mập mờ.
- Tránh anti-pattern: *God service* (service > ~300 dòng → tách), *fat controller* (logic nghiệp vụ nằm ở controller), *shotgun surgery* (một luật nằm nhiều nơi), *primitive obsession* (dùng `String` cho trạng thái → dùng enum), *leaky abstraction* (trả entity/lỗi DB ra API).

## 4. Quy tắc commit
Định dạng **Conventional Commits**:
```
<type>: <mô tả ngắn, thể mệnh lệnh>

<dòng trống>
[1 dòng thân tùy chọn: giải thích VÌ SAO]
```
- **Tiêu đề 1 dòng ≤ 72 ký tự.** Nếu cần thân: **cách 1 dòng trống** rồi tối đa **1 dòng thân ≤ 72 ký tự** (tổng ≤ 2 dòng có nội dung; dòng trống bắt buộc để Git nhận ra thân commit).
- Type bắt buộc: `feat` `fix` `docs` `refactor` `test` (thêm `chore` cho build/CI/config). Không scope bắt buộc; nếu dùng: `feat(issue): ...`.
- Tiêu đề: thể mệnh lệnh, **chữ thường đầu câu**, không dấu chấm cuối, tiếng Anh (hoặc tiếng Việt không dấu – chọn một và giữ trong cả nhóm). Nói **cái gì đổi**; thân (nếu có) nói **vì sao**.
- **Cấm mọi trailer đồng tác giả/attribution** trong commit và mô tả PR: không `Co-Authored-By:`, không `Generated with Claude Code`, không tên/email công cụ AI. Commit chỉ mang tên người commit. (Đã tắt tự động trong `.claude/settings.json`; vẫn phải tự kiểm trước khi commit.)
- Một commit = một thay đổi logic, build vẫn xanh. Không gộp format + logic + đổi tên vào một commit. Không commit `WIP`, `fix`, `update`, `...`.
- Không commit secret, `target/`, `.idea/`, file sinh tự động, dữ liệu thật. Không `--no-verify`, không force-push lên `develop`/`main`.
- Ví dụ tốt:
  - `feat: add issue confirm with all-or-nothing stock check`
  - `fix: lock receipt row before reading product stock`
  - `test: cover concurrent confirm of two issues`
  - `docs: record kickoff decisions in api-contract`
- Ví dụ xấu: `update code` · `fix bug` · `feat: Added the new feature for confirming issues and also fixed a bug in receipts.` (dài, hai việc, quá 72 ký tự).
- Nhánh: `feature/<chữ-cái>-<việc>` (`feature/c-issue-confirm`). PR tiêu đề theo cùng quy ước commit, mô tả ngắn: *làm gì – vì sao – cách kiểm thử*.

## 5. Quy tắc comment & tài liệu trong code
**Nguyên tắc:** code tự giải thích **cái gì**; comment chỉ giải thích **vì sao** (lý do, ràng buộc, bẫy, đánh đổi). Nếu cần comment để giải thích đoạn code làm gì → đổi tên/tách hàm trước.
- **Cần comment:** lý do thiết kế không hiển nhiên (vì sao khóa theo id, vì sao `saveAndFlush`), bẫy đã biết (REPEATABLE READ, INSERT-trước-DELETE), luật nghiệp vụ lấy từ hợp đồng (`// api-contract §5.9`), giả định/ràng buộc, workaround kèm lý do và điều kiện gỡ bỏ.
- **Không comment:** lặp lại code (`// tăng i lên 1`), nhật ký sửa đổi (đã có Git), code bị comment-out (xóa đi), comment tên tác giả/ngày, tiếng lóng, than phiền.
- **Javadoc** (`/** ... */`) cho: interface service (hợp đồng: tham số, kết quả, mã lỗi ném ra), lớp helper dùng chung, method public có hành vi không hiển nhiên. Một câu mở đầu đủ nghĩa, kết thúc bằng dấu chấm. Không bắt buộc cho getter/setter, method hiển nhiên, `@Override` đã có Javadoc ở interface.
- **TODO:** `// TODO[Owner]: việc cần làm` (ví dụ `TODO[Member C]`), kèm điều kiện; không để TODO mồ côi và không merge `FIXME`. Khi làm xong → xóa.
- Comment **đồng bộ với code**: sửa code thì sửa comment, comment sai tệ hơn không có. Ngắn gọn: ≤ 1–2 dòng cho comment thường; ≤ 120 cột.
- Ngôn ngữ: nhất quán trong **một file** (Anh hoặc Việt); thông báo lỗi trả cho người dùng bằng **tiếng Việt**.
- Swagger: mọi endpoint có `@Operation(summary = "...")` ngắn, mô tả tham số lọc/sắp xếp; không nhét logic vào mô tả.
- Tài liệu: thay đổi hành vi/hợp đồng → cập nhật `docs/api-contract.md` trong cùng PR; quyết định thiết kế quan trọng → ghi lý do ở `docs/erd.md`/`architecture.md`.

## 5b. Quy tắc code Frontend (React + TypeScript)
Chi tiết kiến trúc ở `AGENTS.md` §12 và `docs/plans/ui-shared.md`.
- **Component:** function component + hooks; một component một file; props có kiểu rõ ràng (`interface XxxProps`); không logic gọi API trong component hiển thị – đặt trong hook feature (`useProducts`, `useConfirmIssue`).
- **State:** dữ liệu server → TanStack Query (key ổn định `['products', params]`, invalidate sau mutation); state UI cục bộ → `useState`; chỉ dùng store toàn cục cho auth. Không đồng bộ state thủ công với dữ liệu server.
- **Kiểu:** `strict`, không `any`; dùng kiểu sinh từ OpenAPI; union/enum cho trạng thái (`'DRAFT' | 'CONFIRMED' | 'CANCELLED'`), không `string` tùy ý.
- **Hiệu năng/độ đúng:** `key` ổn định trong danh sách (không dùng index nếu danh sách đổi thứ tự); `useMemo/useCallback` chỉ khi đo thấy cần; dependency của `useEffect` đầy đủ; dọn effect.
- **Pattern:** *Container/Hook + Presentational* (hook lấy dữ liệu, component thuần hiển thị); *Composition* thay vì kế thừa; *Adapter* ở API client (bóc envelope, chuẩn hóa lỗi) để component không biết chi tiết HTTP. Không thêm thư viện state/UI mới nếu chưa báo nhóm.
- **Comment:** cùng quy tắc §5 – chỉ giải thích "vì sao"; không comment-out code; `TODO[Owner]`.
- **Commit/PR:** như §4; PR UI nên kèm ảnh chụp màn hình hoặc GIF ngắn trong mô tả.
- **Thiết kế giao diện:** trước khi dựng màn hình mới, dùng skill `ui-ux-pro-max` (đã cài ở `.claude/skills/`) để chốt style, màu, font, bố cục; kết quả ghi vào `frontend/design-system/MASTER.md` để cả nhóm dùng chung một phong cách. Chỉ dùng skill `ui-styling` cho Tailwind/shadcn; không dùng skill `banner-design`, `brand`, `slides` cho UI ứng dụng.
- **An toàn:** escape mặc định của React, không chèn HTML thô; không log token; xác nhận trước thao tác hủy/ẩn/khóa tài khoản.

## 6. Bẫy đã biết (đừng lặp lại)
- Nạp entity bằng EntityGraph rồi mới khóa dòng → có thể thấy tồn cũ. Luôn khóa phiếu → khóa sản phẩm → rồi mới kiểm tra/ghi (`AGENTS.md` §5).
- `SecurityContext` là `ThreadLocal`: test đa luồng phải set lại trong từng luồng.
- `Pageable.unpaged()` làm `getPageSize()` ném lỗi → luôn qua `PageableGuard.sanitize`.
- Service chưa làm đang ném `ApiException.notImplemented(...)` (501): thay bằng logic thật và **xóa** lời gọi + comment `TODO[...]`.
- Hibernate chạy INSERT trước DELETE khi thay danh sách dòng → vi phạm UNIQUE; dùng `clearDetails()` + `saveAndFlush`.

## 7. Checklist trước khi báo "xong"
- [ ] Đúng phạm vi, đúng file của mình · [ ] theo mẫu `CategoryServiceImpl` · [ ] không trả entity/`passwordHash`
- [ ] test cả nhánh lỗi, `mvn -B verify` xanh (hoặc nói rõ chưa chạy) · [ ] comment chỉ giải thích "vì sao", không code chết/TODO mồ côi
- [ ] commit theo §4 (tiêu đề ≤ 72, thân ≤ 1 dòng cách 1 dòng trống, **không Co-Authored-By**) · [ ] hợp đồng/tài liệu đã cập nhật nếu đổi hành vi
