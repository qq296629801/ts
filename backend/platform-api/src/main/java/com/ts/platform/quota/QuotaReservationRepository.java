package com.ts.platform.quota;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuotaReservationRepository extends JpaRepository<QuotaReservation, Long> {

    Optional<QuotaReservation> findByIdAndUserId(Long id, Long userId);
}
