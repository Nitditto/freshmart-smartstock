package com.freshmart.smartstock.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.freshmart.smartstock.common.api.LikePattern;
import com.freshmart.smartstock.domain.entity.DocumentCounter;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.domain.entity.StockReceipt;
import com.freshmart.smartstock.domain.entity.StockReceiptDetail;
import com.freshmart.smartstock.domain.entity.User;
import com.freshmart.smartstock.domain.enums.DocumentStatus;
import com.freshmart.smartstock.domain.enums.Role;
import com.freshmart.smartstock.support.AbstractIntegrationTest;
import com.freshmart.smartstock.support.TestData;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.transaction.support.TransactionTemplate;

/** Custom queries and DB constraints owned by Member A, checked on a real MySQL 8 (H2 would hide differences). */
class RepositoryQueryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private SupplierRepository supplierRepository;
    @Autowired
    private StockReceiptRepository receiptRepository;
    @Autowired
    private StockIssueRepository issueRepository;
    @Autowired
    private DocumentCounterRepository counterRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData(userRepository, categoryRepository, productRepository, supplierRepository,
                receiptRepository, issueRepository);
    }

    @Test
    void searchMatchesNameOrSkuIgnoringCaseAndFiltersCategoryAndHidden() {
        Product tomato = rename(data.product(5, 1), "Cà chua Searchable");
        Product sameCategory = rename(data.product(5, 1), "Khoai Searchable");
        sameCategory.setCategory(tomato.getCategory());
        productRepository.save(sameCategory);
        Product hidden = rename(data.hiddenProduct(), "Hidden Searchable");

        assertEquals(List.of(tomato.getId(), sameCategory.getId()),
                ids(productRepository.search("searchable", null, true, PageRequest.of(0, 10))));
        assertEquals(List.of(tomato.getId(), sameCategory.getId(), hidden.getId()),
                ids(productRepository.search("SEARCHABLE", null, false, PageRequest.of(0, 10))));
        assertEquals(List.of(tomato.getId()),
                ids(productRepository.search(tomato.getSku().toLowerCase(), null, true, PageRequest.of(0, 10))));
        assertEquals(List.of(tomato.getId(), sameCategory.getId()),
                ids(productRepository.search(null, tomato.getCategory().getId(), true, PageRequest.of(0, 10))));
    }

    @Test
    void searchTreatsEscapedUnderscoreAndPercentAsLiteralText() {
        Product literal = data.product(1, 0);
        literal.setSku("ZQ_100%-" + literal.getId());
        productRepository.save(literal);
        Product lookalike = data.product(1, 0);
        lookalike.setSku("ZQX1000-" + lookalike.getId());
        productRepository.save(lookalike);

        List<Long> literalOnly = ids(productRepository.search(LikePattern.escape("ZQ_100%"), null, true,
                PageRequest.of(0, 10)));
        List<Long> unescaped = ids(productRepository.search("ZQ_100%", null, true, PageRequest.of(0, 10)));

        assertEquals(List.of(literal.getId()), literalOnly);
        assertEquals(List.of(literal.getId(), lookalike.getId()), unescaped);
    }

    @Test
    void findLowStockIncludesEqualToMinimumExcludesHiddenAndSortsByStock() {
        Product atMinimum = data.product(4, 4);
        Product belowMinimum = data.product(1, 4);
        Product aboveMinimum = data.product(5, 4);
        Product hiddenLow = data.hiddenProduct();

        List<Long> lowStock = productRepository.findLowStock().stream().map(Product::getId).toList();

        assertTrue(lowStock.containsAll(List.of(atMinimum.getId(), belowMinimum.getId())));
        assertTrue(lowStock.indexOf(belowMinimum.getId()) < lowStock.indexOf(atMinimum.getId()));
        assertTrue(!lowStock.contains(aboveMinimum.getId()) && !lowStock.contains(hiddenLow.getId()));
    }

    @Test
    void findAllByIdForUpdateReturnsEveryRequestedRowOrderedById() {
        Product first = data.product(1, 0);
        Product second = data.product(2, 0);
        Product third = data.product(3, 0);

        List<Long> locked = transactionTemplate.execute(status ->
                productRepository.findAllByIdForUpdate(List.of(third.getId(), first.getId(), second.getId()))
                        .stream().map(Product::getId).toList());

        assertEquals(List.of(first.getId(), second.getId(), third.getId()), locked);
    }

    @Test
    void documentCounterAllowsOneRowPerPrefixAndDay() {
        LocalDate day = LocalDate.of(2026, 11, 25);
        counterRepository.saveAndFlush(new DocumentCounter("PX", day));

        assertThrows(DataIntegrityViolationException.class,
                () -> counterRepository.saveAndFlush(new DocumentCounter("PX", day)));
        counterRepository.saveAndFlush(new DocumentCounter("PN", day));
        counterRepository.saveAndFlush(new DocumentCounter("PX", day.plusDays(1)));
    }

    @Test
    void receiptCannotListTheSameProductTwice() {
        User staff = data.user(Role.STAFF);
        Product product = data.product(0, 0);
        StockReceipt receipt = data.receipt(staff, product, 3, DocumentStatus.DRAFT, null);

        receipt.addDetail(detail(product));

        assertThrows(DataIntegrityViolationException.class, () -> receiptRepository.saveAndFlush(receipt));
    }

    @Test
    void stockCanNeverBeNegativeEvenIfApplicationCodeTriesIt() {
        Product product = data.product(1, 0);
        product.setQuantityInStock(-1);

        // MySQL error 3819 (CHECK violated) is not in Spring's integrity-violation code list, so it surfaces as
        // JpaSystemException; services must therefore check stock themselves and never rely on this constraint.
        JpaSystemException ex = assertThrows(JpaSystemException.class, () -> productRepository.saveAndFlush(product));
        assertTrue(ex.getMessage().contains("ck_products_stock_non_negative"));
        assertEquals(1, productRepository.findById(product.getId()).orElseThrow().getQuantityInStock());
    }

    private Product rename(Product product, String name) {
        product.setName(name);
        return productRepository.save(product);
    }

    private static List<Long> ids(Page<Product> page) {
        return page.getContent().stream().map(Product::getId).sorted().toList();
    }

    private static StockReceiptDetail detail(Product product) {
        StockReceiptDetail detail = new StockReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(1);
        detail.setUnitPrice(BigDecimal.ONE);
        return detail;
    }
}
