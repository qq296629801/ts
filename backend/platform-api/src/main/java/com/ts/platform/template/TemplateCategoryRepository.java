package com.ts.platform.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TemplateCategoryRepository extends JpaRepository<TemplateCategory, Long> {

    List<TemplateCategory> findByStatusOrderBySortOrderAsc(Integer status);
}
