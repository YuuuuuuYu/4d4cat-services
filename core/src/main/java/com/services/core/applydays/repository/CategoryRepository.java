package com.services.core.applydays.repository;

import com.services.core.applydays.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {}
