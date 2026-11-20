# Kế hoạch Thành viên D – Phiếu nhập, Tài khoản, JWT & phân quyền, Swagger, ngoại lệ

> Đọc trước: `00-shared.md`, `docs/api-contract.md` §1–2, §5.
> Điểm liên quan: **REST API 15đ + JWT/phân quyền 15đ** + một phần Logic nghiệp vụ (phiếu nhập).
> Slide: **5** (JWT & phân quyền), **nửa đầu slide 6** (quy tắc trạng thái chứng từ + phiếu nhập; C trình bày phiếu xuất), phối hợp E ở slide 4 (RESTful) và 9 (demo 403).

## 1. Phạm vi (đã chia lại)

| Nhóm | Việc | Ghi chú |
|---|---|---|
| **Phiếu nhập** (mới nhận từ C) | `ReceiptServiceImpl`: create / get / search / update / cancel / **confirm** | Controller `ReceiptController` đã map đủ; **không đụng `/parse`** (của E) |
| Tài khoản | `UserServiceImpl`: list, create, updateStatus | |
| Nền tảng | ma trận 401/403, ngoại lệ, Swagger | |
| Thêm (chỉ khi xong hết) | `PATCH /api/auth/password` | bỏ refresh token |

## 2. Bạn có sẵn gì (không cần chờ ai)
- `StockReceipt`, `StockReceiptDetail`, `Supplier`, `User`, `Product`; `StockReceiptRepository` (`findById` có EntityGraph, `search`); `SupplierRepository`; `ProductRepository.findAllByIdForUpdate`.
- DTO `ReceiptRequest/LineRequest/Response`, `UserResponse`, `CreateUserRequest`, `UpdateUserStatusRequest`.
- `DocumentGuard`, `PageableGuard`, `DocumentCodeGenerator.next("PN")`, `SecurityUtils`, `Clock`, `TestData`.
- Phần C làm phiếu xuất **hoàn toàn riêng**; hai bên chỉ chung helper đã viết sẵn. Hai bên khóa sản phẩm cùng thứ tự `id` nên an toàn khi chạy song song.

## 3. Definition of Done
- [ ] Phiếu nhập: 6 endpoint đúng mã; `confirm` cộng tồn trong **một transaction**, `confirmedAt` đúng lúc xác nhận.
- [ ] `UserServiceImpl` đủ 3 chức năng; email lưu `trim().toLowerCase()`; không response nào chứa `passwordHash` (có test).
- [ ] Test ma trận 401/403 cho **từng endpoint × từng vai trò** (tham số hóa) + token hết hạn / sai chữ ký / rác → 401.
- [ ] Swagger: Authorize hoạt động; mọi endpoint có `@Operation`; 400/401/403/404/409 hiển thị; có ví dụ request.
- [ ] `GlobalExceptionHandler` có test cho từng nhánh (mục 5.6).
- [ ] ≥ 12 unit test + ≥ 3 integration test; slide 5 + nửa slide 6; ≥ 3 PR merged.

## 4. Thiết kế phiếu nhập (đã quyết)
Giống phiếu xuất nhưng **đơn giản hơn** vì cộng tồn không bao giờ thiếu. Vẫn bắt buộc khóa đúng thứ tự (xem bẫy REPEATABLE READ):
1. `receiptRepository.findByIdForUpdate(id)` – khóa dòng phiếu (đọc mới nhất, không EntityGraph).
2. Lấy `productId` từ `receipt.getDetails()` (proxy chưa khởi tạo).
3. `productRepository.findAllByIdForUpdate(ids)` – khóa sản phẩm, sắp theo id.
4. Kiểm tra trạng thái → cộng tồn → `status=CONFIRMED`, `confirmedAt=Instant.now(clock)`.

Thứ tự kiểm tra: `404` → `403 owner` → `409 status`. Thêm vào `StockReceiptRepository` (file của D):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select r from StockReceipt r where r.id = :id")
Optional<StockReceipt> findByIdForUpdate(@Param("id") Long id);
```
Vì sao cộng tồn vẫn phải khóa? Hai transaction cùng `stock = stock + q` đọc snapshot cũ → mất cập nhật (lost update); và phiếu nhập chạy song song với phiếu xuất của C trên cùng sản phẩm.

## 5. Việc cần làm – từng PR (mỗi PR độc lập)

### PR #1 `feat: user management` (21/11)
```java
@Service @RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }
    @Transactional
    public UserResponse create(CreateUserRequest r) {
        String email = r.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) throw ApiException.conflict("Email đã được sử dụng");
        User u = new User();
        u.setFullName(r.fullName().trim()); u.setEmail(email); u.setRole(r.role()); u.setActive(true);
        u.setPasswordHash(passwordEncoder.encode(r.password()));
        return UserResponse.from(userRepository.save(u));
    }
    @Transactional
    public UserResponse updateStatus(Long id, UpdateUserStatusRequest r) {
        User u = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Không tìm thấy tài khoản " + id));
        if (!r.active() && u.getId().equals(SecurityUtils.currentUserId()))
            throw ApiException.conflict("Không thể tự khóa tài khoản của chính mình");
        u.setActive(r.active());          // idempotent: khóa người đã khóa vẫn 200
        return UserResponse.from(u);
    }
}
```
**Test (Mockito):** email trùng khác hoa/thường → 409; hash ≠ mật khẩu gốc và bắt đầu `$2`; tự khóa → 409; id không có → 404; khóa người khác → 200 `active=false`; `UserResponse` không có trường hash (reflection hoặc kiểm JSON).

### PR #2 `feat: receipt create, get, search` (21–22/11)
Mô phỏng `create/search` của phiếu xuất (xem `member-c` mục PR #1) nhưng:
```java
Supplier supplier = supplierRepository.findById(request.supplierId())
        .orElseThrow(() -> ApiException.notFound("Không tìm thấy nhà cung cấp " + request.supplierId()));   // 404
r.setCode(codeGenerator.next(DocumentCodeGenerator.RECEIPT_PREFIX));
detail.setUnitPrice(line.unitPrice());     // BigDecimal, ≥ 0 đã validate ở DTO
```
`search`: `PageableGuard.sanitize(pageable, Set.of("id","code","createdAt","confirmedAt"))`, `from > to` → 400, lọc ngày bằng `DocumentGuard.startOfDay/startOfNextDay`.
**Test:** tạo OK (mã `PN-yyyyMMdd-001`, `createdBy` = người gọi dù body giả mạo); NCC không có → 404; sản phẩm ẩn → 400; dòng trùng → 400; sort sai → 400; list lọc `status`.

### PR #3 `feat: receipt update and cancel` (22/11)
`findByIdForUpdate` → `assertCanModify` → `assertDraft`. Thay dòng: `clearDetails()` → `saveAndFlush` → `addDetail(...)` (tránh vi phạm `UNIQUE (receipt_id, product_id)` do Hibernate chạy INSERT trước DELETE). `cancel` → `CANCELLED`.
**Test:** STAFF sửa phiếu người khác → 403; ADMIN sửa được; sửa/hủy CONFIRMED hoặc CANCELLED → 409; sửa giữ nguyên một sản phẩm → 200; đổi NCC.

### PR #4 `feat: receipt confirm increases stock` (23/11 – để v0.1.0)
```java
@Transactional
public ReceiptResponse confirm(Long id) {
    AuthUser me = SecurityUtils.currentUser();
    StockReceipt r = receiptRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    DocumentGuard.assertCanModify(r.getCreatedBy().getId(), me);
    DocumentGuard.assertDraft(r.getStatus(), r.getCode());
    List<Long> ids = r.getDetails().stream().map(d -> d.getProduct().getId()).sorted().toList();
    Map<Long, Product> locked = productRepository.findAllByIdForUpdate(ids).stream()
            .collect(Collectors.toMap(Product::getId, Function.identity()));
    for (StockReceiptDetail d : r.getDetails()) {
        Product p = locked.get(d.getProduct().getId());
        p.setQuantityInStock(Math.addExact(p.getQuantityInStock(), d.getQuantity()));   // chống tràn int
    }
    r.setStatus(DocumentStatus.CONFIRMED);
    r.setConfirmedAt(Instant.now(clock));
    return ReceiptResponse.from(r);
}
```
**Test:** tồn tăng đúng cho phiếu nhiều dòng; confirm lần 2 → 409 và tồn **không** tăng thêm; STAFF confirm phiếu người khác → 403; phiếu CANCELLED → 409; rollback khi một dòng lỗi (Mockito ném lỗi giữa chừng → tồn không đổi, kiểm bằng integration); `confirmedAt` ≠ null.

### PR #5 `test: security matrix` (24/11)
Mở rộng `AuthAndRoleIntegrationTest` (đã có 6 test mẫu) bằng `@ParameterizedTest`/`@MethodSource`: danh sách `(method, path, body, role được phép)` lấy từ `api-contract.md` §3. Với mỗi endpoint: không token → 401; STAFF trên endpoint ADMIN → 403; vai trò được phép → **không phải 401/403** (501 chấp nhận tạm). Thêm: token hết hạn (`JwtService` với `Clock` cố định – mẫu `JwtServiceTest`), sai chữ ký, token rác → 401; user bị khóa → login 401 **cùng thông báo** với sai mật khẩu; response `/auth/login`, `/auth/me`, `/users` không chứa `passwordHash`.
27/11: đổi "không phải 401/403" thành khẳng định mã thật khi các phần đã merge.

### PR #6 `docs: swagger polish + exception tests` (25–26/11)
- `OperationCustomizer` để mọi endpoint hiển thị 400/401/403/404/409 với schema `ApiResponse`; `@ExampleObject` cho login, tạo user, lỗi validation, 409 thiếu tồn kho.
- Kiểm: **Authorize** → dán token → `/api/auth/me` = 200. Chụp ảnh cho slide 5.
- Test từng nhánh `GlobalExceptionHandler`: validation (400 + lỗi theo trường), JSON hỏng (400), sai kiểu tham số (400), `AccessDeniedException` (403), `DataIntegrityViolationException` (409), route không có (404), method sai (405), lỗi bất ngờ (500, **không lộ stack trace**). Lỗi sinh ra ở filter/entry point cũng dùng envelope (đã có `SecurityResponseWriter`).

### PR #7 (tùy chọn, sau 27/11) `feat: change password`
`PATCH /api/auth/password` `{currentPassword,newPassword}`: sai mật khẩu cũ → 400; mật khẩu mới 8–72 ký tự; BCrypt. Chỉ làm khi mọi thứ trên đã merge.

## 6. Lịch
| Ngày | Việc |
|---|---|
| 20/11 | Chạy login/Swagger; đọc `security/*`; tạo `feature/d-users-receipts` |
| 21–22/11 | PR #1, #2, #3 |
| 23/11 | PR #4 (confirm nhập) → v0.1.0 |
| 24–26/11 | PR #5, #6; review PR của cả nhóm |
| 27/11 | Siết test ma trận; slide 5 + nửa slide 6 |
| 28/11 | Freeze; chuẩn bị demo 403 |
| 29/11 | Tập dượt |

## 7. Phụ thuộc
- **Không phụ thuộc ai.** Test dùng `TestData`/Mockito. Seed của A không đụng service của D.
- **Cung cấp:** `SecurityUtils`, `DocumentGuard` (đã có), mẫu `ReceiptServiceImpl` cho nhóm tham khảo.

## 8. Câu hỏi "vì sao" D phải trả lời được
1. Luồng JWT từ đăng nhập đến gọi API? Token gồm gì? Payload có mã hóa không? (không – chỉ ký.)
2. 401 vs 403 – ví dụ trong dự án. Vì sao sai email và sai mật khẩu trả **cùng** thông báo?
3. Vì sao BCrypt mà không SHA-256/MD5? Vì sao stateless + tắt CSRF?
4. HS256 vs RS256; secret lưu ở đâu; lộ secret thì sao?
5. Quyền theo vai trò kiểm ở controller (`@PreAuthorize`), quyền theo sở hữu kiểm ở service (cần DB) – vì sao tách vậy?
6. Token còn hạn của user bị khóa: xử lý thế nào, đánh đổi gì? (kiểm `active` mỗi request tốn 1 truy vấn / cache ngắn.)
7. Vì sao khóa phiếu rồi mới khóa sản phẩm? Vì sao confirm phiếu nhập vẫn cần khóa?
8. RESTful trong dự án: danh từ số nhiều, đúng method, mã trạng thái có nghĩa, DTO thay vì entity.

## 9. Rủi ro
| Rủi ro | Giảm |
|---|---|
| Test ma trận phụ thuộc endpoint người khác chưa xong | Chỉ khẳng định "không phải 401/403" cho vai trò được phép; siết 27/11 |
| 401/403 trả HTML thay vì JSON | Đã có entry point/handler; test kiểm body |
| Quá nhiều phần nhỏ dồn vào tuần cuối | PR #1–#4 xong **trước 23/11**; Swagger/test ma trận làm sau |
| Rò secret | `git log -p` không có secret; `.gitignore` chặn `.env`, `application-local.yml` |
