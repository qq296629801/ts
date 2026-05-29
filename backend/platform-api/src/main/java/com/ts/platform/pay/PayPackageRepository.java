package com.ts.platform.pay;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayPackageRepository extends JpaRepository<PayPackage, Long> {

    List<PayPackage> findByStatusOrderBySortOrderAsc(Integer status);
}
