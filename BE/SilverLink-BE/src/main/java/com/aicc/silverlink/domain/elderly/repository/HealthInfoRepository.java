package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HealthInfoRepository extends JpaRepository<ElderlyHealthInfo, Long> {
    // 식별 관계이므로 기본 deleteById(elderlyUserId)로 탈퇴 처리가 가능합니다.
}