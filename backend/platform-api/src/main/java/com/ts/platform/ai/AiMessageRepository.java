package com.ts.platform.ai;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    Page<AiMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId, Pageable pageable);
}
