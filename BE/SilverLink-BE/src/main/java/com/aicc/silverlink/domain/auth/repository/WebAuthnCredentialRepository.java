package com.aicc.silverlink.domain.auth.repository;

import com.aicc.silverlink.domain.auth.entity.WebAuthnCredential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WebAuthnCredentialRepository extends JpaRepository<WebAuthnCredential, Long> {
    List<WebAuthnCredential> findAllByUser_IdAndRevokedAtIsNull(Long userId);
    Optional<WebAuthnCredential> findByCredentialId(String credentialId);
    Optional<WebAuthnCredential> findByCredentialIdAndUser_Id(String credentialId, Long userId);

}
