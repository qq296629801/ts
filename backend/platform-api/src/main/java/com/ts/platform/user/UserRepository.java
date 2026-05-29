package com.ts.platform.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    long countByCreatedAtAfter(LocalDateTime time);

    Optional<User> findByPhone(String phone);

    Optional<User> findByEmail(String email);

    Optional<User> findByInviteCode(String inviteCode);

    boolean existsByPhone(String phone);

    boolean existsByEmail(String email);
}
