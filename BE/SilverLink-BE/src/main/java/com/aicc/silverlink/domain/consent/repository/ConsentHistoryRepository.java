package com.aicc.silverlink.domain.consent.repository;

import com.aicc.silverlink.domain.consent.entity.ConsentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsentHistoryRepository extends JpaRepository<ConsentHistory, Long> {
}
