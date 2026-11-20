package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.security.AuthUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared, stateless rules for receipts (Member D) and issues (Member C). Both members use it AS-IS, so
 * neither has to wait for the other. Do not change a method signature without telling the group;
 * additive changes only (new methods) and every change needs a unit test.
 *
 * Error policy (docs/api-contract.md section 5):
 * - not the owner and not ADMIN            -> 403
 * - document is not DRAFT                  -> 409
 * - the same product twice in one document -> 400
 * - product id does not exist              -> 404
 * - product exists but is hidden           -> 400
 */
public final class DocumentGuard {

    private DocumentGuard() {
    }

    /** ADMIN may touch any document; STAFF only documents they created. */
    public static void assertCanModify(Long ownerId, AuthUser me) {
        if (me.role() != Role.ADMIN && !Objects.equals(ownerId, me.id())) {
            throw ApiException.forbidden("Bạn chỉ được thao tác trên chứng từ do chính mình tạo");
        }
    }

    /** Only DRAFT documents can be edited, confirmed or cancelled. */
    public static void assertDraft(DocumentStatus status, String code) {
        if (status != DocumentStatus.DRAFT) {
            throw ApiException.conflict("Chứng từ " + code + " đang ở trạng thái " + status
                    + ", chỉ được sửa/xác nhận/hủy khi còn DRAFT");
        }
    }

    /** One product may appear only once per document (also enforced by a UNIQUE key in the DB). */
    public static void assertNoDuplicateProducts(Collection<Long> productIds) {
        Set<Long> seen = new HashSet<>();
        for (Long id : productIds) {
            if (!seen.add(id)) {
                throw ApiException.badRequest("Sản phẩm id=" + id + " bị lặp trong cùng một chứng từ");
            }
        }
    }

    /**
     * Loads the products for the lines of a NEW or EDITED document (no lock; plain read).
     * 404 if an id does not exist, 400 if the product is hidden. Returns productId -> Product.
     */
    public static Map<Long, Product> loadProductsForLines(Collection<Long> productIds, ProductRepository repository) {
        List<Product> found = repository.findAllById(productIds);
        Map<Long, Product> byId = found.stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        for (Long id : productIds) {
            if (!byId.containsKey(id)) {
                throw ApiException.notFound("Không tìm thấy sản phẩm id=" + id);
            }
        }
        for (Product p : found) {
            if (!p.isActive()) {
                throw ApiException.badRequest("Sản phẩm '" + p.getName() + "' đã bị ẩn, không thể thêm vào chứng từ");
            }
        }
        return byId;
    }

    /** Start of the given local day in the application time zone (inclusive lower bound). */
    public static Instant startOfDay(LocalDate day, Clock clock) {
        return day == null ? null : day.atStartOfDay(clock.getZone()).toInstant();
    }

    /** Start of the day AFTER the given one (exclusive upper bound, so "to" covers the whole day). */
    public static Instant startOfNextDay(LocalDate day, Clock clock) {
        return day == null ? null : day.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
    }
}
