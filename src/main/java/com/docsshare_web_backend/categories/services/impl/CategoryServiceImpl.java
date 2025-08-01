package com.docsshare_web_backend.categories.services.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.docsshare_web_backend.categories.dto.requests.CategoryFilterRequest;
import com.docsshare_web_backend.categories.dto.requests.CategoryRequest;
import com.docsshare_web_backend.categories.dto.responses.CategoryResponse;
import com.docsshare_web_backend.categories.filters.CategoryFilter;
import com.docsshare_web_backend.categories.models.Category;
import com.docsshare_web_backend.categories.repositories.CategoryRepository;
import com.docsshare_web_backend.categories.services.CategoryService;
import com.docsshare_web_backend.commons.services.ToxicService;
import com.docsshare_web_backend.documents.filters.DocumentFilter;
import com.docsshare_web_backend.documents.models.Document;

import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService{
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    ToxicService toxicService;

    public static class CategoryMapper {
        public static CategoryResponse toCategoryResponse(Category category) {
            return CategoryResponse.builder()
                    .id(category.getId())
                    .name(category.getName())
                    .description(category.getDescription())
                    .createdAt(category.getCreatedAt())
                    .updatedAt(category.getUpdateAt())
                    .parentId(category.getParent() != null ? category.getParent().getId() : null)
                    .children(category.getChildren() != null
                            ? category.getChildren().stream()
                                    .map(CategoryMapper::toCategoryResponse)
                                    .collect(Collectors.toList())
                            : null)
                    .build();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllRootCategories(CategoryFilterRequest request) {
        Specification<Category> spec = CategoryFilter.filterByRequest(request)
                .and((root, query, cb) -> cb.isNull(root.get("parent")));

        List<Category> rootCategories = categoryRepository.findAll(spec);

        return rootCategories.stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getSubCategories(long parentId) {
        Category parentCategory = categoryRepository.findById(parentId)
                .orElseThrow(() -> new EntityNotFoundException("Parent category not found with id: " + parentId));
        return parentCategory.getChildren().stream()
                .map(CategoryMapper::toCategoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("category not found with id: " + categoryId));
        return CategoryMapper.toCategoryResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        Category parentCategory = null;
        if (request.getParentId() != null) {
            parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent category not found with id: " + request.getParentId()));
        }
        toxicService.validateTextSafety(request.getName(), "Name");
        Category newCategory = Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .parent(parentCategory)
                .createdAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build();

        Category savedCategory = categoryRepository.save(newCategory);
        return CategoryMapper.toCategoryResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(long categoryId, CategoryRequest request) {
        Category existingCategory = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

        Category parentCategory = null;
        if (request.getParentId() != null) {
            parentCategory = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Parent category not found with id: " + request.getParentId()));
        }

        existingCategory.setName(request.getName());
        existingCategory.setDescription(request.getDescription());
        existingCategory.setParent(parentCategory);
        existingCategory.setUpdateAt(LocalDateTime.now());

        Category updatedCategory = categoryRepository.save(existingCategory);
        return CategoryMapper.toCategoryResponse(updatedCategory);
    }

    @Override
        @Transactional
        public CategoryResponse deleteCategory(long categoryId) {
                Category existingCategory = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new EntityNotFoundException("Category not found with id: " + categoryId));

                if (!existingCategory.getChildren().isEmpty()) {
                        throw new IllegalStateException("Cannot delete category because it has subcategories.");
                }

                if (!existingCategory.getDocuments().isEmpty()) {
                        throw new IllegalStateException("Cannot delete category because it has associated documents.");
                }

                categoryRepository.delete(existingCategory);
                return CategoryMapper.toCategoryResponse(existingCategory);
        }
}
