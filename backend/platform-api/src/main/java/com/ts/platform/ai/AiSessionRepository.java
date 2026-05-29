package com.ts.platform.ai;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiSessionRepository extends JpaRepository<AiSession, String> {

    List<AiSession> findByUserIdOrderByUpdatedAtDesc(Long userId);

    Optional<AiSession> findByIdAndUserId(String id, Long userId);
}
