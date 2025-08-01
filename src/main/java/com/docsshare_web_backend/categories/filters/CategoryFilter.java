package com.docsshare_web_backend.categories.filters;

import com.docsshare_web_backend.commons.filters.CommonFilter;
import com.docsshare_web_backend.categories.dto.requests.CategoryFilterRequest;
import com.docsshare_web_backend.categories.models.Category;

import org.springframework.data.jpa.domain.Specification;

public class CategoryFilter {
    public static Specification<Category> filterByRequest(CategoryFilterRequest request) {
        return CommonFilter.filter(request, Category.class);
    }
}
