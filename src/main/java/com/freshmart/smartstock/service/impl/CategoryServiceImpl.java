package com.freshmart.smartstock.service.impl;

import com.freshmart.smartstock.common.exception.ApiException;
import com.freshmart.smartstock.domain.entity.Category;
import com.freshmart.smartstock.dto.category.CategoryRequest;
import com.freshmart.smartstock.dto.category.CategoryResponse;
import com.freshmart.smartstock.repository.CategoryRepository;
import com.freshmart.smartstock.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * REFERENCE SLICE - copy this pattern (Controller -> Service interface/impl -> Repository, DTOs in/out,
 * ApiException for errors, @Transactional on service methods) for every other feature.
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return categoryRepository.findAll(Sort.by("name")).stream().map(CategoryResponse::from).toList();
    }

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw ApiException.conflict("Danh mục '" + name + "' đã tồn tại");
        }
        Category category = new Category();
        category.setName(name);
        return CategoryResponse.from(categoryRepository.save(category));
    }
}
