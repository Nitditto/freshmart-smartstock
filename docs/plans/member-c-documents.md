# Kế hoạch Thành viên C – Phiếu xuất (Issue), xác nhận an toàn, đồng thời, mã chứng từ

> Đọc trước: `00-shared.md` (phân chia, helper có sẵn, luật không chờ nhau), `docs/api-contract.md` §2, §5.
> **Phần của C = nặng điểm nhất (Logic nghiệp vụ 20đ).** Phiếu nhập đã chuyển sang D → C tập trung làm *thật sâu* một chứng từ.
> Slide: **6** (xuất kho + transaction + đồng thời; D trình bày nửa đầu), cùng chia slide 11.

## 1. Phạm vi

| Endpoint | Quyền | Mã |
|---|---|---|
| POST `/api/issues` | mọi vai trò | 201, 400, 401, 404 |
| GET `/api/issues` (`status`, `from`, `to`, phân trang) | mọi vai trò | 200, 400, 401 |
| GET `/api/issues/{id}` | mọi vai trò | 200, 401, 404 |
| PUT `/api/issues/{id}` | chủ phiếu / ADMIN | 200, 400, 401, 403, 404, 409 |
| PATCH `/api/issues/{id}/confirm` | chủ phiếu / ADMIN; `DAMAGED` chỉ ADMIN | 200, 401, 403, 404, 409 |
| PATCH `/api/issues/{id}/cancel` | chủ phiếu / ADMIN | 200, 401, 403, 404, 409 |

Ngoài phạm vi C: phiếu nhập (D), sản phẩm/low-stock endpoint (B), seed (A). C **không** gọi `ReceiptService`.

## 2. Bạn có sẵn gì (không cần chờ ai)
- Entity `StockIssue`, `StockIssueDetail`, `Product`, `User`; `StockIssueRepository` (`findById` đã EntityGraph, `search(status,from,to,pageable)`); `ProductRepository.findAllByIdForUpdate`.
- DTO `IssueRequest/IssueLineRequest/IssueResponse/IssueConfirmResponse`, `ShortageItem`, `LowStockItem` (đã có `from(Product)`).
- `DocumentGuard`, `PageableGuard`, `DocumentCodeGenerator.next("PX")`, `SecurityUtils`, `Clock`, `TestData`.
- Controller `IssueController` đã map đủ route – bạn **chỉ cài `IssueServiceImpl`**.

## 3. Definition of Done
- [ ] 6 endpoint đúng mã HTTP như bảng trên, có `@Operation`.
- [ ] `confirm`: một `@Transactional`; **kiểm tra TẤT CẢ dòng trước, trừ sau**; thiếu → 409 + `data=List<ShortageItem>`, **không dòng nào bị trừ**.
- [ ] DAMAGED: STAFF → 403 (kể cả chủ phiếu); ADMIN → OK.
- [ ] `IssueConfirmResponse.newlyLowStock` = các sản phẩm **của phiếu** có tồn sau xuất ≤ MinStock (contract §5.9).
- [ ] Hai request confirm đồng thời trên cùng sản phẩm không bao giờ làm tồn âm (test đa luồng).
- [ ] `DocumentCodeGenerator` không trùng mã khi tạo đồng thời (retry – xem 5.6).
- [ ] ≥ 15 unit test (Mockito) + ≥ 3 integration test (Testcontainers) + 1 test đồng thời.
- [ ] Slide 6 (phần xuất kho) + trả lời được mục 8. ≥ 3 PR merged.

## 4. Thiết kế (đã quyết, làm theo)

**Thứ tự kiểm tra trong `confirm`** (đúng contract §5.12): `404` → `403 owner` → `403 DAMAGED` → `409 status` → `409 shortages`.

**Vì sao khóa dòng:** hai confirm song song cùng đọc `stock=10`, mỗi cái trừ 8 → âm. `SELECT … FOR UPDATE` (`findAllByIdForUpdate`) buộc cái thứ hai chờ rồi đọc giá trị mới → bị chặn 409. Luôn khóa theo thứ tự `id` (repository đã `order by id`) để không deadlock với phiếu nhập của D (D cũng khóa theo id).

**Bẫy REPEATABLE READ (MySQL mặc định) – đọc kỹ:** `SELECT` thường đọc *snapshot* cũ; chỉ `SELECT … FOR UPDATE` mới đọc *giá trị mới nhất đã commit*. Hibernate lại **không nạp lại** entity đã nằm trong persistence context. Hệ quả: nếu bạn `findById` phiếu (EntityGraph nạp luôn `Product`) **rồi mới** khóa sản phẩm, tồn kho bạn thấy có thể là bản cũ → vẫn bán lố. Vì vậy thứ tự **bắt buộc**:
1. Khóa **chính dòng phiếu**: `issueRepository.findByIdForUpdate(id)` (không EntityGraph). Hai người cùng confirm *một phiếu* → người sau chờ, rồi thấy `CONFIRMED` → 409.
2. Lấy danh sách `productId` từ `issue.getDetails()` (proxy chưa khởi tạo → không nạp Product).
3. Khóa sản phẩm: `productRepository.findAllByIdForUpdate(ids)` (đọc tồn mới nhất, sắp theo id).
4. Mới kiểm tra trạng thái / tồn kho / trừ tồn.
`update` và `cancel` cũng dùng `findByIdForUpdate` để không đua với `confirm`.

## 5. Việc cần làm – từng PR (mỗi PR độc lập)

### PR #1 `feat: issue create, get, search` (21/11)
```java
@Service @RequiredArgsConstructor
public class IssueServiceImpl implements IssueService {
    private static final Set<String> SORTABLE = Set.of("id", "code", "createdAt", "confirmedAt");
    private final StockIssueRepository issueRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DocumentCodeGenerator codeGenerator;
    private final Clock clock;

    @Override @Transactional
    public IssueResponse create(IssueRequest request) {
        AuthUser me = SecurityUtils.currentUser();
        List<Long> ids = request.lines().stream().map(IssueLineRequest::productId).toList();
        DocumentGuard.assertNoDuplicateProducts(ids);                       // 400
        Map<Long, Product> products = DocumentGuard.loadProductsForLines(ids, productRepository); // 404 / 400

        StockIssue issue = new StockIssue();
        issue.setCode(codeGenerator.next(DocumentCodeGenerator.ISSUE_PREFIX));
        issue.setReason(request.reason());
        issue.setNote(trimToNull(request.note()));
        issue.setStatus(DocumentStatus.DRAFT);
        issue.setCreatedBy(userRepository.getReferenceById(me.id()));        // id từ token, KHÔNG từ body
        for (IssueLineRequest l : request.lines()) {
            StockIssueDetail d = new StockIssueDetail();
            d.setProduct(products.get(l.productId()));
            d.setQuantity(l.quantity());
            issue.addDetail(d);
        }
        StockIssue saved = issueRepository.save(issue);
        return IssueResponse.from(issueRepository.findById(saved.getId()).orElseThrow()); // nạp đủ quan hệ cho DTO
    }

    @Override @Transactional(readOnly = true)
    public PageResponse<IssueResponse> search(DocumentStatus status, LocalDate from, LocalDate to, Pageable pageable) {
        if (from != null && to != null && from.isAfter(to)) throw ApiException.badRequest("from phải ≤ to");
        Pageable safe = PageableGuard.sanitize(pageable, SORTABLE);
        Page<StockIssue> page = issueRepository.search(status,
                DocumentGuard.startOfDay(from, clock), DocumentGuard.startOfNextDay(to, clock), safe);
        return PageResponse.from(page, IssueResponse::from);
    }
    // get(id): findById -> notFound("Không tìm thấy phiếu xuất " + id)
}
```
⚠ `StockIssueRepository.search` không có EntityGraph → `IssueResponse.from` truy cập `details`/`createdBy` (LAZY) – chạy trong `@Transactional(readOnly = true)` nên không lỗi; nếu thấy N+1, thêm `@EntityGraph(attributePaths = {"createdBy"})` lên `search` (file của C).
**Test:** tạo thành công (mã `PX-yyyyMMdd-001`, `status=DRAFT`, `createdBy` = người gọi dù body giả mạo); không dòng → 400 (validation); dòng trùng → 400; sản phẩm không có → 404; sản phẩm ẩn → 400; `from > to` → 400; sort theo `passwordHash` → 400; `size=1000` → 400.

### PR #2 `feat: issue update and cancel` (22/11)
```java
@Override @Transactional
public IssueResponse update(Long id, IssueRequest request) {
    AuthUser me = SecurityUtils.currentUser();
    StockIssue issue = issueRepository.findByIdForUpdate(id).orElseThrow(() -> notFound(id));
    DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);   // 403
    DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());      // 409
    // validate dòng mới như create
    issue.setReason(request.reason());
    issue.setNote(trimToNull(request.note()));
    issue.clearDetails();
    issueRepository.saveAndFlush(issue);      // ← BẮT BUỘC: xóa dòng cũ TRƯỚC khi thêm dòng mới
    for (...) issue.addDetail(newDetail);     // (Hibernate chạy INSERT trước DELETE → vi phạm UNIQUE (issue_id, product_id))
    issueRepository.save(issue);
    return IssueResponse.from(issueRepository.findById(id).orElseThrow());
}
```
`cancel`: owner → draft → `status=CANCELLED`. **Test:** STAFF sửa phiếu người khác → 403; ADMIN sửa được; sửa CONFIRMED/CANCELLED → 409; **sửa phiếu giữ nguyên một sản phẩm** (bẫy UNIQUE) → 200; hủy hai lần → 409.

### PR #3 `feat: issue confirm with all-or-nothing stock check` (23/11 – để kịp v0.1.0)
Thêm vào `StockIssueRepository` (file của C):
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select i from StockIssue i where i.id = :id")
Optional<StockIssue> findByIdForUpdate(@Param("id") Long id);
```
```java
@Override @Transactional
public IssueConfirmResponse confirm(Long id) {
    AuthUser me = SecurityUtils.currentUser();
    StockIssue issue = issueRepository.findByIdForUpdate(id)                                 // KHÓA phiếu, đọc mới nhất
            .orElseThrow(() -> ApiException.notFound("Không tìm thấy phiếu xuất " + id));   // 404
    DocumentGuard.assertCanModify(issue.getCreatedBy().getId(), me);                         // 403 owner
    if (issue.getReason() == IssueReason.DAMAGED && me.role() != Role.ADMIN)
        throw ApiException.forbidden("Chỉ ADMIN được xác nhận phiếu xuất lý do DAMAGED");     // 403
    DocumentGuard.assertDraft(issue.getStatus(), issue.getCode());                           // 409

    List<Long> ids = issue.getDetails().stream().map(d -> d.getProduct().getId()).sorted().toList();
    Map<Long, Product> locked = productRepository.findAllByIdForUpdate(ids).stream()         // KHÓA sản phẩm
            .collect(Collectors.toMap(Product::getId, Function.identity()));

    List<ShortageItem> shortages = new ArrayList<>();
    for (StockIssueDetail d : issue.getDetails()) {                                          // 1) KIỂM TRA hết
        Product p = locked.get(d.getProduct().getId());
        if (d.getQuantity() > p.getQuantityInStock())
            shortages.add(new ShortageItem(p.getId(), p.getSku(), p.getName(), d.getQuantity(), p.getQuantityInStock()));
    }
    if (!shortages.isEmpty()) throw ApiException.conflict("Không đủ tồn kho", shortages);    // không trừ gì

    List<Product> touched = new ArrayList<>();
    for (StockIssueDetail d : issue.getDetails()) {                                          // 2) rồi mới TRỪ
        Product p = locked.get(d.getProduct().getId());
        p.setQuantityInStock(p.getQuantityInStock() - d.getQuantity());
        touched.add(p);
    }
    issue.setStatus(DocumentStatus.CONFIRMED);
    issue.setConfirmedAt(Instant.now(clock));
    List<LowStockItem> newlyLow = touched.stream().filter(Product::isLowStock).map(LowStockItem::from).toList();
    return new IssueConfirmResponse(IssueResponse.from(issue), newlyLow);
}
```
Lưu ý: `IssueResponse.from(issue)` đọc `details.product` và `createdBy` – các proxy này khởi tạo lười trong cùng transaction (an toàn). Với `product` đã được nạp bởi query khóa, bạn nhận đúng đối tượng đã trừ tồn.
**Test (Mockito):** đủ tồn → tồn giảm đúng; thiếu 1 dòng trong 3 → ném 409, `data` = đúng 1 `ShortageItem`, **cả 3 sản phẩm giữ nguyên tồn**; DAMAGED + STAFF → 403; DAMAGED + ADMIN → OK; STAFF confirm phiếu người khác → 403; confirm lần hai → 409; tồn sau xuất = MinStock → vào `newlyLowStock` (biên `≤`); tồn sau xuất > MinStock → không vào.

### PR #4 `test: issue integration + rollback` (24–25/11)
Dùng `AbstractIntegrationTest` + `TestData` (không cần D/B xong):
1. Tạo phiếu bằng repository → gọi `PATCH /confirm` bằng token STAFF chủ phiếu → 200, tồn trong DB giảm.
2. Xuất vượt tồn → 409, body `data[0].requested/available` đúng, DB không đổi.
3. **Rollback:** phiếu 2 dòng; dòng 2 gây lỗi (ví dụ sản phẩm có `CHECK` vi phạm do bạn dựng tồn thấp) → toàn bộ không đổi.
4. STAFF + DAMAGED → 403; ADMIN → 200.
Lấy token: gọi `POST /api/auth/login` với user seed hoặc tạo `JwtService` token trực tiếp (xem `AuthAndRoleIntegrationTest` mẫu).

### PR #5 `test: concurrent confirm` (26/11)
```java
// sản phẩm tồn 10; hai phiếu xuất khác nhau, mỗi phiếu 8; chạy confirm song song
ExecutorService pool = Executors.newFixedThreadPool(2);
CountDownLatch ready = new CountDownLatch(2), go = new CountDownLatch(1);
// mỗi luồng: set SecurityContext (AuthUser của owner) -> ready.countDown(); go.await(); service.confirm(id)
// assert: đúng 1 thành công, 1 ném ApiException(409), tồn cuối = 2
```
Thêm: **hai luồng cùng confirm một phiếu** → đúng 1 thành công, tồn chỉ giảm một lần. Lưu ý `SecurityContext` là `ThreadLocal`: phải set trong từng luồng.

### PR #6 `fix: document code race` (26/11)
`DocumentCodeGenerator.next`: người đầu tiên trong ngày có thể đua insert dòng counter. Bắt `DataIntegrityViolationException` ở `saveAndFlush` → đọc lại dòng đã được luồng khác tạo (1 lần retry). Cách chắc chắn hơn: `@Modifying @Query(nativeQuery) INSERT … ON DUPLICATE KEY UPDATE last_seq = last_seq + 1` rồi `SELECT`. **Test:** 20 luồng gọi `next("PX")` → 20 mã khác nhau, liên tiếp `001..020`.
> `DocumentCodeGenerator` dùng chung với D (mã `PN`): **không đổi chữ ký** `next(String)`, nên D không bị ảnh hưởng.

## 6. Lịch
| Ngày | Việc |
|---|---|
| 20/11 | Đọc `00-shared`, tạo `feature/c-issues`, bắt đầu PR #1 |
| 21–22/11 | PR #1, #2 |
| 23/11 | PR #3 (confirm) → v0.1.0 |
| 24–26/11 | PR #4, #5, #6 |
| 27/11 | Sửa lỗi, tăng coverage; viết slide 6 |
| 28/11 | Freeze; chuẩn bị đoạn code để giải thích |
| 29/11 | Tập dượt demo (bước 5–6 trong 00-shared §7) |

## 7. Phụ thuộc
- **Không phụ thuộc ai.** Cần xem mẫu của D (receipt) chỉ để thống nhất phong cách – không bắt buộc.
- **Cung cấp:** ngữ nghĩa `confirmedAt` (B, E dựa vào); `IssueConfirmResponse` cho slide demo.

## 8. Câu hỏi "vì sao" C phải trả lời được
1. Transaction là gì? `@Transactional` rollback khi nào? (RuntimeException/`ApiException`; checked exception thì không.)
2. Vì sao kiểm tra **tất cả** dòng trước khi trừ? (UX: trả đủ danh sách thiếu; nếu trừ dần rồi rollback thì chỉ biết dòng đầu tiên lỗi.)
3. Hai nhân viên xuất cùng mặt hàng cùng lúc – xử lý thế nào? Pessimistic lock vs optimistic `@Version` + retry: ưu/nhược?
4. Vì sao khóa theo thứ tự `id`? (tránh deadlock với phiếu nhập.)
5. Vì sao không sửa phiếu đã xác nhận? (sổ kho bất biến, điều chỉnh bằng phiếu mới.)
6. `CHECK (quantity_in_stock >= 0)` còn cần không khi code đã kiểm tra? (lớp phòng thủ cuối.)
7. Vì sao `createdBy` lấy từ token? Vì sao DAMAGED chỉ ADMIN xác nhận?
8. Mã phiếu reset mỗi ngày – đảm bảo không trùng bằng cách nào?

## 9. Rủi ro
| Rủi ro | Giảm |
|---|---|
| INSERT-trước-DELETE khi thay dòng | `saveAndFlush` sau `clearDetails()`; test "giữ nguyên 1 sản phẩm" |
| `LazyInitializationException` | map DTO **trong** service, trong `@Transactional` |
| Đọc tồn cũ do REPEATABLE READ + persistence context | Luôn khóa phiếu → khóa sản phẩm **trước** khi nạp `Product` bằng EntityGraph (mục 4) |
| Test đồng thời flaky | `CountDownLatch` bắt đầu cùng lúc; chạy 20 lần cục bộ; không dùng `sleep` |
| Quá tải ngày 23/11 | PR #3 chỉ cần *đúng*, chưa cần test đồng thời (để PR #5) |
