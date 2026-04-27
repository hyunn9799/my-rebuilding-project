package com.aicc.silverlink.domain.policy.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PolicyType {
    // 1. [기본] 서비스 이용약관 (필수)
    TERMS_OF_SERVICE("서비스 이용약관"),

    // 2. [기본] 개인정보 처리방침 (필수)

    PRIVACY_POLICY("개인정보 처리방침"),

    // 3. [특화] 민감정보 처리 동의 (필수)

    SENSITIVE_INFO_CONSENT("민감정보 처리 동의"),

    // 4. [특화] 개인정보 제3자 제공 동의 (필수/선택)

    THIRD_PARTY_PROVISION_CONSENT("개인정보 제3자 제공 동의"),

    // 5. [안전] 위치기반 서비스 이용약관 (선택/필수)

    LOCATION_BASED_SERVICE("위치기반 서비스 이용약관"),

    // 6. [알림] 복지 정보 및 혜택 수신 동의 (선택)

    WELFARE_BENEFITS_NOTIFICATION("복지 정보 및 혜택 알림 수신 동의");

    private final String description;
}