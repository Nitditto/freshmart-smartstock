package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.api.PageResponse;
import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.dto.product.ProductRequest;
import com.freshmart.smartstock.dto.product.ProductResponse;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.service.impl.ProductServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductServiceImpl service;

    @Test
    void searchReturnsPagedResponseMatchingKeywordAndCategory() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Rau củ");

        Product p1 = new Product();
        p1.setId(1L);
        p1.setSku("RC-001");
        p1.setName("Cà chua bi");
        p1.setCategory(category);
        p1.setUnit("hộp");
        p1.setPrice(BigDecimal.valueOf(25000));
        p1.setQuantityInStock(20);
        p1.setMinStock(5);
        p1.setActive(true);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setSku("RC-002");
        p2.setName("Cà chua beef");
        p2.setCategory(category);
        p2.setUnit("kg");
        p2.setPrice(BigDecimal.valueOf(35000));
        p2.setQuantityInStock(15);
        p2.setMinStock(5);
        p2.setActive(true);

        Pageable pageable = PageRequest.of(0, 10, Sort.by("price").descending());
        when(productRepository.search("cà chua", 1L, true, pageable))
                .thenReturn(new PageImpl<>(List.of(p1, p2), pageable, 2));

        PageResponse<ProductResponse> response = service.search("  cà chua  ", 1L, pageable);

        assertEquals(2, response.items().size());
        assertEquals(0, response.page());
        assertEquals(10, response.size());
        assertEquals(2L, response.totalElements());
        assertEquals(1, response.totalPages());
        assertEquals("RC-001", response.items().get(0).sku());
        assertEquals("RC-002", response.items().get(1).sku());
    }

    @Test
    void searchConvertsBlankKeywordToNull() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.search(isNull(), eq(2L), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ProductResponse> response = service.search("   ", 2L, pageable);

        assertTrue(response.items().isEmpty());
        verify(productRepository).search(isNull(), eq(2L), eq(true), any(Pageable.class));
    }

    @Test
    void searchRejectsSortFieldNotInWhitelistWith400() {
        Pageable unsafePageable = PageRequest.of(0, 20, Sort.by("passwordHash"));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.search("rau", null, unsafePageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(productRepository, never()).search(any(), any(), any(Boolean.class), any());
    }

    @Test
    void searchRejectsSortFieldLikeCategoryWith400() {
        Pageable unsafePageable = PageRequest.of(0, 20, Sort.by("category"));

        ApiException exception = assertThrows(ApiException.class, () ->
                service.search(null, null, unsafePageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(productRepository, never()).search(any(), any(), any(Boolean.class), any());
    }

    @Test
    void searchRejectsPageSizeExceedingMaxWith400() {
        Pageable oversizePageable = PageRequest.of(0, 101);

        ApiException exception = assertThrows(ApiException.class, () ->
                service.search(null, null, oversizePageable));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertEquals("size tối đa là 100", exception.getMessage());
        verify(productRepository, never()).search(any(), any(), any(Boolean.class), any());
    }

    @Test
    void searchAllowsValidSortFieldsInWhitelist() {
        Pageable sortPrice = PageRequest.of(0, 20, Sort.by("price").descending());
        when(productRepository.search(isNull(), isNull(), eq(true), eq(sortPrice)))
                .thenReturn(new PageImpl<>(List.of(), sortPrice, 0));

        PageResponse<ProductResponse> response = service.search(null, null, sortPrice);
        assertTrue(response.items().isEmpty());

        Pageable sortStock = PageRequest.of(0, 20, Sort.by("quantityInStock").ascending());
        when(productRepository.search(isNull(), isNull(), eq(true), eq(sortStock)))
                .thenReturn(new PageImpl<>(List.of(), sortStock, 0));

        PageResponse<ProductResponse> responseStock = service.search(null, null, sortStock);
        assertTrue(responseStock.items().isEmpty());
    }

    @Test
    void searchAlwaysPassesOnlyActiveTrue() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.search(eq("thịt"), isNull(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        service.search("thịt", null, pageable);

        verify(productRepository).search(eq("thịt"), isNull(), eq(true), any(Pageable.class));
    }

    @Test
    void searchAcceptsUpperBoundaryPageSize100() {
        Pageable maxPageable = PageRequest.of(0, 100);
        when(productRepository.search(isNull(), isNull(), eq(true), eq(maxPageable)))
                .thenReturn(new PageImpl<>(List.of(), maxPageable, 0));

        PageResponse<ProductResponse> response = service.search(null, null, maxPageable);

        assertEquals(0, response.page());
        assertEquals(100, response.size());
    }

    @Test
    void searchHandlesNullAndEmptyKeywordWithoutNpe() {
        Pageable pageable = PageRequest.of(0, 20);
        when(productRepository.search(isNull(), eq(1L), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        PageResponse<ProductResponse> responseNull = service.search(null, 1L, pageable);
        assertTrue(responseNull.items().isEmpty());

        PageResponse<ProductResponse> responseEmpty = service.search("", 1L, pageable);
        assertTrue(responseEmpty.items().isEmpty());
    }

    @Test
    void searchHandlesUnpagedByEnforcingMaxPageSize() {
        Pageable expectedPageable = PageRequest.of(0, 100);
        when(productRepository.search(isNull(), isNull(), eq(true), eq(expectedPageable)))
                .thenReturn(new PageImpl<>(List.of(), expectedPageable, 0));

        PageResponse<ProductResponse> response = service.search(null, null, Pageable.unpaged());

        assertEquals(0, response.page());
        assertEquals(100, response.size());
        verify(productRepository).search(isNull(), isNull(), eq(true), eq(expectedPageable));
    }

    @Test
    void searchAllowsAllSortFieldsInWhitelistAscAndDesc() {
        List<String> sortFields = List.of("id", "sku", "name", "price", "quantityInStock");
        for (String field : sortFields) {
            Pageable asc = PageRequest.of(0, 10, Sort.by(field).ascending());
            when(productRepository.search(isNull(), isNull(), eq(true), eq(asc)))
                    .thenReturn(new PageImpl<>(List.of(), asc, 0));
            service.search(null, null, asc);

            Pageable desc = PageRequest.of(0, 10, Sort.by(field).descending());
            when(productRepository.search(isNull(), isNull(), eq(true), eq(desc)))
                    .thenReturn(new PageImpl<>(List.of(), desc, 0));
            service.search(null, null, desc);
        }
    }

    @Test
    void getReturnsProductWhenExistsIncludingInactive() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Rau củ");

        Product product = new Product();
        product.setId(10L);
        product.setSku("RCQ-001");
        product.setName("Cà chua bi");
        product.setCategory(category);
        product.setUnit("hộp");
        product.setPrice(BigDecimal.valueOf(25000));
        product.setQuantityInStock(15);
        product.setMinStock(5);
        product.setActive(false);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        ProductResponse response = service.get(10L);

        assertEquals(10L, response.id());
        assertEquals("RCQ-001", response.sku());
        assertEquals("Cà chua bi", response.name());
        assertEquals(15, response.quantityInStock());
        assertFalse(response.active());
    }

    @Test
    void getThrows404WhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.get(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy sản phẩm 999", exception.getMessage());
    }

    @Test
    void createRejectsDuplicateSkuIgnoringCaseWith409() {
        when(productRepository.existsBySkuIgnoreCase("rcq-001")).thenReturn(true);

        ProductRequest request = new ProductRequest("  rcq-001  ", "Cà chua bi", 1L, "hộp",
                BigDecimal.valueOf(25000), 5);
        ApiException exception = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Mã SKU 'rcq-001' đã tồn tại", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createThrows404WhenCategoryNotFound() {
        when(productRepository.existsBySkuIgnoreCase("RCQ-002")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest("RCQ-002", "Bắp cải tím", 99L, "bắp",
                BigDecimal.valueOf(18000), 10);
        ApiException exception = assertThrows(ApiException.class, () -> service.create(request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy danh mục 99", exception.getMessage());
        verify(productRepository, never()).save(any());
    }

    @Test
    void createSuccessInitializesStockToZeroAndActiveTrue() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Rau củ");

        when(productRepository.existsBySkuIgnoreCase("RCQ-003")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(20L);
            return p;
        });

        ProductRequest request = new ProductRequest("  RCQ-003  ", "  Khoai tây Đà Lạt  ", 1L, "  kg  ",
                BigDecimal.valueOf(30000), 20);
        ProductResponse response = service.create(request);

        assertEquals(20L, response.id());
        assertEquals("RCQ-003", response.sku());
        assertEquals("Khoai tây Đà Lạt", response.name());
        assertEquals("kg", response.unit());
        assertEquals(0, response.quantityInStock());
        assertTrue(response.active());
        assertEquals(20, response.minStock());
    }

    @Test
    void createSuccessWithBoundaryZeroPriceAndZeroMinStock() {
        Category category = new Category();
        category.setId(3L);
        category.setName("Gia vị");

        when(productRepository.existsBySkuIgnoreCase("GV-000")).thenReturn(false);
        when(categoryRepository.findById(3L)).thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(21L);
            return p;
        });

        ProductRequest request = new ProductRequest("GV-000", "Gói muối mẫu dùng thử", 3L, "gói", BigDecimal.ZERO, 0);
        ProductResponse response = service.create(request);

        assertEquals(21L, response.id());
        assertEquals(BigDecimal.ZERO, response.price());
        assertEquals(0, response.minStock());
        assertEquals(0, response.quantityInStock());
        assertTrue(response.active());
    }

    @Test
    void validationRejectsNegativePrice() {
        ProductRequest request = new ProductRequest("RCQ-001", "Cà chua", 1L, "kg", BigDecimal.valueOf(-1000), 10);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("price")));
    }

    @Test
    void validationRejectsNegativeMinStock() {
        ProductRequest request = new ProductRequest("RCQ-001", "Cà chua", 1L, "kg", BigDecimal.valueOf(10000), -5);
        Set<ConstraintViolation<ProductRequest>> violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("minStock")));
    }

    @Test
    void updateThrows404WhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest("TH-001", "Thịt ba chỉ", 2L, "kg", BigDecimal.valueOf(140000), 15);
        ApiException exception = assertThrows(ApiException.class, () -> service.update(999L, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy sản phẩm 999", exception.getMessage());
    }

    @Test
    void updateRejectsDuplicateSkuWithAnotherProductWith409() {
        Product product = new Product();
        product.setId(5L);
        product.setSku("TH-001");

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("th-002", 5L)).thenReturn(true);

        ProductRequest request = new ProductRequest("  th-002  ", "Thịt thăn heo", 2L, "kg",
                BigDecimal.valueOf(150000), 10);
        ApiException exception = assertThrows(ApiException.class, () -> service.update(5L, request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertEquals("Mã SKU 'th-002' đã tồn tại", exception.getMessage());
    }

    @Test
    void updateAllowsKeepingSameSkuForSameProduct() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Thịt tươi");

        Product product = new Product();
        product.setId(5L);
        product.setSku("TH-001");
        product.setName("Thịt ba chỉ cũ");
        product.setCategory(category);
        product.setUnit("kg");
        product.setPrice(BigDecimal.valueOf(130000));
        product.setQuantityInStock(50);
        product.setMinStock(10);
        product.setActive(true);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("TH-001", 5L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest("TH-001", "Thịt ba chỉ chuẩn VietGAP", 2L, "kg",
                BigDecimal.valueOf(145000), 12);
        ProductResponse response = service.update(5L, request);

        assertEquals(5L, response.id());
        assertEquals("TH-001", response.sku());
        assertEquals("Thịt ba chỉ chuẩn VietGAP", response.name());
        assertEquals(BigDecimal.valueOf(145000), response.price());
        assertEquals(12, response.minStock());
        assertEquals(50, response.quantityInStock());
    }

    @Test
    void updateSuccessWithNewUnusedSkuAndNewCategory() {
        Category oldCategory = new Category();
        oldCategory.setId(1L);
        oldCategory.setName("Rau củ");

        Category newCategory = new Category();
        newCategory.setId(4L);
        newCategory.setName("Trái cây");

        Product product = new Product();
        product.setId(6L);
        product.setSku("RC-010");
        product.setName("Dưa lưới xanh");
        product.setCategory(oldCategory);
        product.setUnit("quả");
        product.setPrice(BigDecimal.valueOf(65000));
        product.setQuantityInStock(30);
        product.setMinStock(5);
        product.setActive(true);

        when(productRepository.findById(6L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("TC-010", 6L)).thenReturn(false);
        when(categoryRepository.findById(4L)).thenReturn(Optional.of(newCategory));

        ProductRequest request = new ProductRequest("TC-010", "Dưa lưới ruột cam Nhật Bản", 4L, "quả",
                BigDecimal.valueOf(85000), 8);
        ProductResponse response = service.update(6L, request);

        assertEquals(6L, response.id());
        assertEquals("TC-010", response.sku());
        assertEquals("Dưa lưới ruột cam Nhật Bản", response.name());
        assertEquals(4L, response.categoryId());
        assertEquals("Trái cây", response.categoryName());
        assertEquals(30, response.quantityInStock());
    }

    @Test
    void updateTrimsSkuNameAndUnit() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Thịt tươi");

        Product product = new Product();
        product.setId(7L);
        product.setSku("BO-001");
        product.setName("Thịt bò Úc");
        product.setCategory(category);
        product.setUnit("khay");
        product.setPrice(BigDecimal.valueOf(200000));
        product.setQuantityInStock(10);
        product.setMinStock(2);

        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("BO-001-NEW", 7L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest("  BO-001-NEW  ", "  Thịt bò Wagyu A5  ", 2L, "  khay  ",
                BigDecimal.valueOf(500000), 3);
        ProductResponse response = service.update(7L, request);

        assertEquals("BO-001-NEW", response.sku());
        assertEquals("Thịt bò Wagyu A5", response.name());
        assertEquals("khay", response.unit());
    }

    @Test
    void updatePreservesExistingQuantityInStockExplicitly() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Rau củ");

        Product product = new Product();
        product.setId(12L);
        product.setSku("RC-099");
        product.setName("Nấm đùi gà");
        product.setCategory(category);
        product.setUnit("gói");
        product.setPrice(BigDecimal.valueOf(22000));
        product.setQuantityInStock(88);
        product.setMinStock(10);

        when(productRepository.findById(12L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("RC-099", 12L)).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        ProductRequest request = new ProductRequest("RC-099", "Nấm đùi gà baby hữu cơ", 1L, "gói",
                BigDecimal.valueOf(25000), 15);
        ProductResponse response = service.update(12L, request);

        assertEquals(88, product.getQuantityInStock());
        assertEquals(88, response.quantityInStock());
    }

    @Test
    void updateThrows404WhenCategoryNotFound() {
        Product product = new Product();
        product.setId(5L);
        product.setSku("TH-001");

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuIgnoreCaseAndIdNot("TH-001", 5L)).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest("TH-001", "Thịt ba chỉ", 99L, "kg", BigDecimal.valueOf(140000), 15);
        ApiException exception = assertThrows(ApiException.class, () -> service.update(5L, request));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy danh mục 99", exception.getMessage());
    }

    @Test
    void hideSetsActiveFalseIdempotently() {
        Product product = new Product();
        product.setId(8L);
        product.setActive(true);

        when(productRepository.findById(8L)).thenReturn(Optional.of(product));

        service.hide(8L);
        assertFalse(product.isActive());

        service.hide(8L);
        assertFalse(product.isActive());
    }

    @Test
    void hideThrows404WhenProductNotFound() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () -> service.hide(999L));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy sản phẩm 999", exception.getMessage());
    }

    @Test
    void searchEscapesLikeWildcardsInTheKeyword() {
        Pageable pageable = PageRequest.of(0, 10, Sort.by("id"));
        when(productRepository.search(eq("RCQ!_001"), isNull(), eq(true), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.search(" RCQ_001 ", null, pageable);

        verify(productRepository).search(eq("RCQ!_001"), isNull(), eq(true), any(Pageable.class));
    }
}

