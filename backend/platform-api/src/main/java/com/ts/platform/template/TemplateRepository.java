package com.ts.platform.template;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TemplateRepository extends JpaRepository<Template, Long> {

    long countByStatus(String status);

    Page<Template> findByStatus(String status, Pageable pageable);

    @Query("""
            SELECT t FROM Template t WHERE t.status = 'APPROVED'
            AND (:categoryId IS NULL OR t.categoryId = :categoryId)
            AND (:keyword IS NULL OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                 OR LOWER(t.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<Template> searchApproved(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
