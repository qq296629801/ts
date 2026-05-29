package com.ts.platform.image;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface ImageAssetRepository extends JpaRepository<ImageAsset, Long> {

    Page<ImageAsset> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    long countByCreatedAtAfter(LocalDateTime time);
}
