package com.docsshare_web_backend.categories.services;

import com.docsshare_web_backend.categories.dto.requests.CategoryRequest;
import com.docsshare_web_backend.categories.dto.responses.CategoryResponse;

import java.util.List;

import org.springframework.stereotype.Service;

@Service
public interface CategoryService {
    List<CategoryResponse> getAllRootCategories();
    List<CategoryResponse> getSubCategories(long parentId);
    CategoryResponse createCategory(CategoryRequest request);
    CategoryResponse updateCategory(long categoryId, CategoryRequest request);
    CategoryResponse deleteCategory(long categoryId);
}
