package com.freshmart.smartstock.support;

import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockIssue;
import com.freshmart.smartstock.domain.entity.StockIssueDetail;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.IssueReason;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.repository.StockIssueRepository;
import com.freshmart.smartstock.repository.StockReceiptRepository;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Builds test data DIRECTLY through repositories, so no member's test depends on another member's service.
 * Usage in an integration test (extends AbstractIntegrationTest):
 * <pre>
 *   @Autowired UserRepository users; ...
 *   TestData data = new TestData(users, categories, products, suppliers, receipts, issues);
 *   Product p = data.product("T-001", 10, 2);
 * </pre>
 * Every call generates unique values (email, sku, code) so tests can share one container without clashing.
 * NOTE: these factories do NOT change product stock when they create a CONFIRMED document - set the stock
 * yourself in the test so the scenario is explicit.
 */
public class TestData {

    private static final AtomicInteger SEQ = new AtomicInteger();

    private final UserRepository users;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final SupplierRepository suppliers;
    private final StockReceiptRepository receipts;
    private final StockIssueRepository issues;

    public TestData(UserRepository users, CategoryRepository categories, ProductRepository products,
                    SupplierRepository suppliers, StockReceiptRepository receipts, StockIssueRepository issues) {
        this.users = users;
        this.categories = categories;
        this.products = products;
        this.suppliers = suppliers;
        this.receipts = receipts;
        this.issues = issues;
    }

    public User user(Role role) {
        int n = SEQ.incrementAndGet();
        User u = new User();
        u.setFullName("Test " + role + " " + n);
        u.setEmail("test" + n + "@freshmart.test");
        u.setPasswordHash("{not-a-real-hash}");
        u.setRole(role);
        u.setActive(true);
        return users.save(u);
    }

    public Category category() {
        Category c = new Category();
        c.setName("Cat-" + SEQ.incrementAndGet());
        return categories.save(c);
    }

    public Supplier supplier() {
        Supplier s = new Supplier();
        s.setName("Supplier-" + SEQ.incrementAndGet());
        return suppliers.save(s);
    }

    /** Active product with the given stock and minimum stock. */
    public Product product(int stock, int minStock) {
        int n = SEQ.incrementAndGet();
        Product p = new Product();
        p.setSku("T-" + n);
        p.setName("Product " + n);
        p.setCategory(category());
        p.setUnit("kg");
        p.setPrice(BigDecimal.valueOf(10000));
        p.setQuantityInStock(stock);
        p.setMinStock(minStock);
        p.setActive(true);
        return products.save(p);
    }

    public Product hiddenProduct() {
        Product p = product(0, 0);
        p.setActive(false);
        return products.save(p);
    }

    /** One-line receipt in the given status. confirmedAt is set only for CONFIRMED. */
    public StockReceipt receipt(User creator, Product product, int quantity, DocumentStatus status,
                                Instant confirmedAt) {
        StockReceipt r = new StockReceipt();
        r.setCode("PN-TEST-" + SEQ.incrementAndGet());
        r.setSupplier(supplier());
        r.setStatus(status);
        r.setCreatedBy(creator);
        r.setConfirmedAt(status == DocumentStatus.CONFIRMED ? confirmedAt : null);
        StockReceiptDetail d = new StockReceiptDetail();
        d.setProduct(product);
        d.setQuantity(quantity);
        d.setUnitPrice(BigDecimal.valueOf(9000));
        r.addDetail(d);
        return receipts.save(r);
    }

    /** One-line issue in the given status. confirmedAt is set only for CONFIRMED. */
    public StockIssue issue(User creator, Product product, int quantity, IssueReason reason,
                            DocumentStatus status, Instant confirmedAt) {
        StockIssue i = new StockIssue();
        i.setCode("PX-TEST-" + SEQ.incrementAndGet());
        i.setReason(reason);
        i.setStatus(status);
        i.setCreatedBy(creator);
        i.setConfirmedAt(status == DocumentStatus.CONFIRMED ? confirmedAt : null);
        StockIssueDetail d = new StockIssueDetail();
        d.setProduct(product);
        d.setQuantity(quantity);
        i.addDetail(d);
        return issues.save(i);
    }
}
