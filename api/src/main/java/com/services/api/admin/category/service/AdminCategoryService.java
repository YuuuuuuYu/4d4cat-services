package com.services.api.admin.category.service;

import com.services.core.applydays.entity.Category;
import com.services.core.applydays.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

  private final CategoryRepository categoryRepository;

  @Transactional
  public Category createCategory(String name, Long parentId, int depth) {
    Category category = Category.builder().name(name).parentId(parentId).depth(depth).build();
    return categoryRepository.save(category);
  }

  @Transactional
  public void deleteCategory(Long id) {
    categoryRepository.deleteById(id);
  }
}
