package com.ts.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserQuotaRepository extends JpaRepository<UserQuota, Long> {

    Optional<UserQuota> findByUserId(Long userId);
}
