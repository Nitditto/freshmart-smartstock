package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Supplier;
import com.freshmart.smartstock.dto.supplier.SupplierRequest;
import com.freshmart.smartstock.dto.supplier.SupplierResponse;
import com.freshmart.smartstock.repository.SupplierRepository;
import com.freshmart.smartstock.service.impl.SupplierServiceImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;

@ExtendWith(MockitoExtension.class)
class SupplierServiceImplTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierServiceImpl service;

    @Test
    void findAllReturnsSuppliersSortedByName() {
        Supplier s1 = new Supplier();
        s1.setId(1L);
        s1.setName("Công ty Cổ phần Sữa Ba Vì");

        Supplier s2 = new Supplier();
        s2.setId(2L);
        s2.setName("Trang trại Rau sạch Đà Lạt");

        when(supplierRepository.findAll(Sort.by("name"))).thenReturn(List.of(s1, s2));

        List<SupplierResponse> result = service.findAll();

        assertEquals(2, result.size());
        assertEquals("Công ty Cổ phần Sữa Ba Vì", result.get(0).name());
        assertEquals("Trang trại Rau sạch Đà Lạt", result.get(1).name());
        verify(supplierRepository).findAll(Sort.by("name"));
    }

    @Test
    void createTrimsDataAndConvertsBlankToNull() {
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        SupplierRequest request = new SupplierRequest("  Công ty Nông sản Xanh  ", "   ", "  Đà Lạt, Lâm Đồng  ");
        SupplierResponse response = service.create(request);

        assertEquals(10L, response.id());
        assertEquals("Công ty Nông sản Xanh", response.name());
        assertNull(response.phone());
        assertEquals("Đà Lạt, Lâm Đồng", response.address());
    }

    @Test
    void createHandlesNullPhoneAndAddressWithoutNpe() {
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> {
            Supplier s = invocation.getArgument(0);
            s.setId(11L);
            return s;
        });

        SupplierRequest request = new SupplierRequest("  Hợp tác xã Nông nghiệp Mộc Châu  ", null, null);
        SupplierResponse response = service.create(request);

        assertEquals(11L, response.id());
        assertEquals("Hợp tác xã Nông nghiệp Mộc Châu", response.name());
        assertNull(response.phone());
        assertNull(response.address());
    }

    @Test
    void updateSuccessWhenSupplierExists() {
        Supplier existing = new Supplier();
        existing.setId(1L);
        existing.setName("Đại lý Đồ uống Miền Nam");
        existing.setPhone("0901000003");
        existing.setAddress("Thủ Đức, TP.HCM");

        when(supplierRepository.findById(1L)).thenReturn(Optional.of(existing));

        SupplierRequest request = new SupplierRequest("Công ty Thực phẩm Tươi Sống Việt", "0901000002",
                "Quận 7, TP.HCM");
        SupplierResponse response = service.update(1L, request);

        assertEquals(1L, response.id());
        assertEquals("Công ty Thực phẩm Tươi Sống Việt", response.name());
        assertEquals("0901000002", response.phone());
        assertEquals("Quận 7, TP.HCM", response.address());
    }

    @Test
    void updateTrimsDataAndConvertsBlankToNull() {
        Supplier existing = new Supplier();
        existing.setId(2L);
        existing.setName("Công ty TNHH Thủy sản Biển Đông");
        existing.setPhone("02838123456");
        existing.setAddress("Cảng cá Cát Lở, Vũng Tàu");

        when(supplierRepository.findById(2L)).thenReturn(Optional.of(existing));

        SupplierRequest request = new SupplierRequest("  Tập đoàn Thủy sản Minh Phú  ", "   ", "   ");
        SupplierResponse response = service.update(2L, request);

        assertEquals(2L, response.id());
        assertEquals("Tập đoàn Thủy sản Minh Phú", response.name());
        assertNull(response.phone());
        assertNull(response.address());
    }

    @Test
    void updateThrows404WhenSupplierNotFound() {
        when(supplierRepository.findById(999L)).thenReturn(Optional.empty());

        ApiException exception = assertThrows(ApiException.class, () ->
                service.update(999L, new SupplierRequest("Nhà cung cấp Không Tồn Tại", null, null)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertEquals("Không tìm thấy nhà cung cấp 999", exception.getMessage());
        verify(supplierRepository, never()).save(any());
    }
}
