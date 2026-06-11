package com.services.api.admin.category.controller;

import com.services.api.admin.category.dto.CategoryCreateRequest;
import com.services.api.admin.category.service.AdminCategoryService;
import com.services.core.applydays.entity.Category;
import com.services.core.common.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoryController {

  private final AdminCategoryService adminCategoryService;

  @PostMapping
  public BaseResponse<Category> createCategory(@RequestBody CategoryCreateRequest request) {
    try {
      String name = request.name();
      Long parentId = request.parentId();
      int depth = request.depth() != null ? request.depth() : 1;

      log.info("Creating category: name={}, parentId={}, depth={}", name, parentId, depth);
      return BaseResponse.of(
          HttpStatus.CREATED, adminCategoryService.createCategory(name, parentId, depth));
    } catch (Exception e) {
      log.error("Failed to create category", e);
      throw e;
    }
  }

  @DeleteMapping("/{id}")
  public BaseResponse<Void> deleteCategory(@PathVariable Long id) {
    adminCategoryService.deleteCategory(id);
    return BaseResponse.of(HttpStatus.OK, null);
  }
}
