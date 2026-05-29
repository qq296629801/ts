package com.ts.platform.template;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TemplateFavoriteRepository extends JpaRepository<TemplateFavorite, Long> {

    boolean existsByUserIdAndTemplateId(Long userId, Long templateId);

    Optional<TemplateFavorite> findByUserIdAndTemplateId(Long userId, Long templateId);
}
