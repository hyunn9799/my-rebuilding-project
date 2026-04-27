package com.aicc.silverlink.domain.admin.repository;

import com.aicc.silverlink.domain.admin.entity.OfflineRegistrationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfflineRegistrationLogRepository extends JpaRepository<OfflineRegistrationLog, Long> {
}
