package com.freshmart.smartstock.service;

import com.freshmart.smartstock.dto.category.CategoryRequest;
import com.freshmart.smartstock.dto.category.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> findAll();

    /** Name must be unique, case-insensitive (409). */
    CategoryResponse create(CategoryRequest request);
}
