package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.domain.entity.Product;
import com.freshmart.smartstock.dto.inventory.LowStockItem;
import com.freshmart.smartstock.repository.ProductRepository;
import com.freshmart.smartstock.service.impl.InventoryServiceImpl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private InventoryServiceImpl service;

    @Test
    void lowStockMapsProductsToLowStockItemsCorrectly() {
        Product p1 = new Product();
        p1.setId(1L);
        p1.setSku("RC-001");
        p1.setName("Cà chua bi");
        p1.setUnit("hộp");
        p1.setQuantityInStock(2);
        p1.setMinStock(10);
        p1.setActive(true);

        Product p2 = new Product();
        p2.setId(2L);
        p2.setSku("TT-005");
        p2.setName("Thịt thăn bò Úc");
        p2.setUnit("khay");
        p2.setQuantityInStock(4);
        p2.setMinStock(5);
        p2.setActive(true);

        when(productRepository.findLowStock()).thenReturn(List.of(p1, p2));

        List<LowStockItem> result = service.lowStock();

        assertEquals(2, result.size());

        LowStockItem item1 = result.get(0);
        assertEquals(1L, item1.productId());
        assertEquals("RC-001", item1.sku());
        assertEquals("Cà chua bi", item1.name());
        assertEquals("hộp", item1.unit());
        assertEquals(2, item1.quantityInStock());
        assertEquals(10, item1.minStock());

        LowStockItem item2 = result.get(1);
        assertEquals(2L, item2.productId());
        assertEquals("TT-005", item2.sku());
        assertEquals("Thịt thăn bò Úc", item2.name());
        assertEquals("khay", item2.unit());
        assertEquals(4, item2.quantityInStock());
        assertEquals(5, item2.minStock());

        verify(productRepository).findLowStock();
    }

    @Test
    void lowStockReturnsEmptyListWhenNoProductsLowOnStock() {
        when(productRepository.findLowStock()).thenReturn(List.of());

        List<LowStockItem> result = service.lowStock();

        assertTrue(result.isEmpty());
        verify(productRepository).findLowStock();
    }

    @Test
    void lowStockPreservesAscendingOrderOfStock() {
        Product outOfStock = new Product();
        outOfStock.setId(10L);
        outOfStock.setSku("TC-001");
        outOfStock.setName("Táo Envy");
        outOfStock.setUnit("kg");
        outOfStock.setQuantityInStock(0);
        outOfStock.setMinStock(20);

        Product criticalStock = new Product();
        criticalStock.setId(11L);
        criticalStock.setSku("TC-002");
        criticalStock.setName("Nho xanh Autumn Crisp");
        criticalStock.setUnit("hộp");
        criticalStock.setQuantityInStock(3);
        criticalStock.setMinStock(15);

        Product boundaryStock = new Product();
        boundaryStock.setId(12L);
        boundaryStock.setSku("TC-003");
        boundaryStock.setName("Cam Cara ruột đỏ");
        boundaryStock.setUnit("kg");
        boundaryStock.setQuantityInStock(10);
        boundaryStock.setMinStock(10);

        when(productRepository.findLowStock()).thenReturn(List.of(outOfStock, criticalStock, boundaryStock));

        List<LowStockItem> result = service.lowStock();

        assertEquals(3, result.size());
        assertEquals(0, result.get(0).quantityInStock());
        assertEquals(3, result.get(1).quantityInStock());
        assertEquals(10, result.get(2).quantityInStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenStockEqualsMinStock() {
        Product product = new Product();
        product.setQuantityInStock(15);
        product.setMinStock(15);

        assertTrue(product.isLowStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenStockExceedsMinStockByOne() {
        Product product = new Product();
        product.setQuantityInStock(16);
        product.setMinStock(15);

        assertFalse(product.isLowStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenStockIsZero() {
        Product product = new Product();
        product.setQuantityInStock(0);
        product.setMinStock(10);

        assertTrue(product.isLowStock());
    }

    @Test
    void isLowStockBoundaryRuleWhenMinStockIsZero() {
        Product product = new Product();
        product.setMinStock(0);

        product.setQuantityInStock(0);
        assertTrue(product.isLowStock());

        product.setQuantityInStock(1);
        assertFalse(product.isLowStock());
    }
}
