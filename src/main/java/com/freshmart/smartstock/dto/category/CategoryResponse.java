package com.freshmart.smartstock.dto.category;

import com.freshmart.smartstock.domain.entity.Category;

public record CategoryResponse(Long id, String name) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName());
    }
}
