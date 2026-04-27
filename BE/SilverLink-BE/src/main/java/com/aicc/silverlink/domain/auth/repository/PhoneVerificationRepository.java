package com.aicc.silverlink.domain.auth.repository;

import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;


public interface PhoneVerificationRepository extends JpaRepository<PhoneVerification,Long> {

    Optional<PhoneVerification> findByIdAndStatus(Long id, PhoneVerification.Status status);

    long countByPhoneE164AndPurposeAndCreatedAtAfter(String phoneE164, PhoneVerification.Purpose purpose, LocalDateTime after);

}
