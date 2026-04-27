package com.aicc.silverlink.domain.counselor.controller;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.dto.CounselorUpdateRequest; // 💡 추가됨
import com.aicc.silverlink.domain.counselor.service.CounselorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "상담사", description = "상담사 등록/조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/counselors")
public class CounselorController {

    private final CounselorService counselorService;

    // 관리자: 상담사 신규 등록
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")


    public ResponseEntity<CounselorResponse> register(@RequestBody @Valid CounselorRequest request){
        CounselorResponse response = counselorService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 관리자: 특정 상담사 상세 정보 조회
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CounselorResponse> getCounselorByAdmin(@PathVariable("id") Long id) {
        CounselorResponse response = counselorService.getCounselor(id);

        return ResponseEntity.ok(response);
    }


    // 상담사: 본인 상세 정보 조회
    @GetMapping("/me")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<CounselorResponse> getCounselor(@AuthenticationPrincipal Long currentUserId) {
        CounselorResponse response = counselorService.getCounselor(currentUserId);
        return ResponseEntity.ok(response);
    }
    // ✅ 상담사: 본인 상세 정보 수정 (추가된 기능)
    @PutMapping("/me")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<CounselorResponse> updateMyProfile(
            @AuthenticationPrincipal Long currentUserId,
            @RequestBody @Valid CounselorUpdateRequest request) {

        CounselorResponse response = counselorService.updateCounselor(currentUserId, request);
        return ResponseEntity.ok(response);
    }

    // 관리자: 상담사 전체 목록 조회
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CounselorResponse>> getAllCounselors() {
        List<CounselorResponse> responses = counselorService.getAllCounselors();
        return ResponseEntity.ok(responses);
    }

}
