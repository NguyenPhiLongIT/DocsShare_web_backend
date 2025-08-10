package com.docsshare_web_backend.categories.domain;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.docsshare_web_backend.categories.dto.requests.CategoryFilterRequest;
import com.docsshare_web_backend.categories.dto.requests.CategoryRequest;
import com.docsshare_web_backend.categories.dto.responses.CategoryResponse;
import com.docsshare_web_backend.categories.services.CategoryService;
import com.docsshare_web_backend.commons.services.ExcelExportService;
import com.docsshare_web_backend.documents.dto.requests.DocumentFilterRequest;
import com.docsshare_web_backend.documents.dto.responses.DocumentResponse;

import jakarta.servlet.http.HttpServletResponse;

@Slf4j
@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {
    @Autowired
    private CategoryService categoryService;
    @Autowired
    private ExcelExportService excelExportService;

    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllRootCategories(@ModelAttribute CategoryFilterRequest request) {
        List<CategoryResponse> rootCategories = categoryService.getAllRootCategories(request);
        return ResponseEntity.ok(rootCategories);
    }

    @GetMapping("/{parentId}/subcategories")
    public ResponseEntity<List<CategoryResponse>> getSubCategories(@PathVariable long parentId) {
        List<CategoryResponse> subCategories = categoryService.getSubCategories(parentId);
        return ResponseEntity.ok(subCategories);
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable long categoryId) {
        CategoryResponse category = categoryService.getCategoryById(categoryId);
        return ResponseEntity.ok(category);
    }

    @PostMapping("/create")
    public ResponseEntity<CategoryResponse> createCategory(@RequestBody CategoryRequest request) {
        CategoryResponse createdCategory = categoryService.createCategory(request);
        return ResponseEntity.ok(createdCategory);
    }

    @PutMapping("/update/{categoryId}")
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable long categoryId,
                                                           @RequestBody CategoryRequest request) {
        CategoryResponse updatedCategory = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(updatedCategory);
    }

    @DeleteMapping("/delete/{categoryId}")
    public ResponseEntity<CategoryResponse> deleteCategory(@PathVariable long categoryId) {
        CategoryResponse deletedCategory = categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok(deletedCategory);
    }

    @GetMapping("/export")
    public void exportExcel(@ModelAttribute CategoryFilterRequest filterRequest,
        HttpServletResponse response
    ) {
        List<CategoryResponse> data = categoryService.getAllRootCategories(filterRequest);
        new ExcelExportService<CategoryResponse>().export(response, "Category_export", data);
    }
}
