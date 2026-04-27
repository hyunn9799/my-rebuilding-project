package com.aicc.silverlink.domain.policy.controller;

import com.aicc.silverlink.domain.policy.dto.PolicyRequest;
import com.aicc.silverlink.domain.policy.dto.PolicyResponse;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.policy.service.PolicyService;
import com.aicc.silverlink.global.common.response.ApiResponse; // ApiResponse 쓴다면 사용
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "약관/정책", description = "이용약관 조회/등록 API")
@Slf4j
@RestController
@RequestMapping("/api/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    /**
     * [공통/사용자] 최신 약관 조회
     * 회원가입 '전'에 호출되므로 로그인 없이 접근 가능해야 함
     */
    @GetMapping("/latest/{type}")
    public ResponseEntity<PolicyResponse> getLatestPolicy(@PathVariable PolicyType type) {
        log.info("GET /api/policies/latest/{} - 최신 약관 조회 요청", type);
        PolicyResponse response = policyService.getLatest(type);
        return ResponseEntity.ok(response);
    }

    /**
     * [관리자] 전체 정책 목록 조회
     * GET /api/policies
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        log.info("GET /api/policies - 전체 정책 목록 조회 요청");
        List<PolicyResponse> response = policyService.getAll();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PolicyResponse> createPolicy(
            @AuthenticationPrincipal Long adminId,
            @RequestBody @Valid PolicyRequest request) {
        log.info("POST /api/policies - 약관 생성 요청 (Admin ID: {})", adminId);

        // 토큰이 유효하지 않으면 adminId는 null일 수 있음 (필터 예외처리에 따라 다름)
        if (adminId == null) {
            throw new IllegalArgumentException("인증 정보가 유효하지 않습니다.");
        }

        PolicyResponse response = policyService.create(request, adminId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}