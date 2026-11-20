package com.freshmart.smartstock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.dto.category.CategoryRequest;
import com.freshmart.smartstock.dto.category.CategoryResponse;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/** Pattern for service unit tests: Mockito for repositories, no Spring context, no database. */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryServiceImpl service;

    @Test
    void createRejectsDuplicateNameWith409() {
        when(categoryRepository.existsByNameIgnoreCase("Rau củ")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class, () -> service.create(new CategoryRequest("Rau củ")));

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void createTrimsNameAndSaves() {
        when(categoryRepository.existsByNameIgnoreCase("Sữa")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> {
            Category c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CategoryResponse response = service.create(new CategoryRequest("  Sữa  "));

        assertEquals(10L, response.id());
        assertEquals("Sữa", response.name());
    }
}
