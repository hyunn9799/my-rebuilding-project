package com.aicc.silverlink.domain.guardian.controller;

import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianUpdateRequest;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.service.GuardianService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "보호자", description = "보호자 등록/조회/어르신 연결 API")
@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    // 보호자 회원가입 (누구나 가능)
    @PostMapping("/signup")
    public ResponseEntity<GuardianResponse> signup(@RequestBody GuardianRequest request){
        GuardianResponse response = guardianService.register(request);
        return ResponseEntity.created((URI.create("/api/guardians/" + response.getId()))).body(response);
    }

    // 보호자는 자신의 정보를 조회할 수 있다.
    @GetMapping("/me")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<GuardianResponse> getMyInfo(@AuthenticationPrincipal Long currentUserId){
        return ResponseEntity.ok(guardianService.getGuardian(currentUserId));
    }

    // 관리자는 특정 보호자의 정보를 ID로 조회할 수 있다.
    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuardianResponse> getGuardianByAdminId(@PathVariable Long id){
        return ResponseEntity.ok(guardianService.getGuardian(id));
    }

    // 상담사는 자신이 담당하는 어르신의 보호자 정보를 조회할 수 있다.
    @GetMapping("/counselor/{id}")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<GuardianResponse> getGuardianByCounselorId(
            @PathVariable Long id,
            @AuthenticationPrincipal Long currentUserId
    ){
        return ResponseEntity.ok(guardianService.getGuardianForCounselor(id, currentUserId));
    }

    // ✅ [추가] 관리자는 특정 어르신(Elderly)에게 연결된 보호자 정보를 조회할 수 있다.
    @GetMapping("/admin/elderly/{elderlyId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuardianResponse> getGuardianByElderlyForAdmin(@PathVariable Long elderlyId) {
        GuardianResponse guardian = guardianService.getGuardianByElderlyOrNull(elderlyId);
        if (guardian == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(guardian);
    }

    // 관리자는 특정 보호자에 연결된 노인 정보를 조회할 수 있다.
    @GetMapping("/admin/{id}/elderly")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GuardianElderlyResponse> getElderlyByGuardianForAdmin(@PathVariable("id") Long guardianId) {
        return ResponseEntity.ok(guardianService.getElderlyByGuardian(guardianId));
    }

    // 상담사는 본인이 담당하는 보호자에 연결된 노인 정보를 조회할 수 있다.
    @GetMapping("/counselor/{id}/elderly")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<GuardianElderlyResponse> getElderlyByGuardianForCounselor(
            @PathVariable("id") Long guardianId,
            @AuthenticationPrincipal Long currentUserId
    ) {
        return ResponseEntity.ok(guardianService.getElderlyByGuardianForCounselor(guardianId, currentUserId));
    }

    // 보호자는 자신이 모시는 어르신 정보를 조회할 수 있다.
    @GetMapping("/me/elderly")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<GuardianElderlyResponse> getMyElderly(@AuthenticationPrincipal Long currentUserId){
        return ResponseEntity.ok(guardianService.getElderlyByGuardian(currentUserId));
    }

    // 관리자는 전체 보호자 목록을 조회할 수 있다.
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GuardianResponse>> getAllGuardians(){
        return ResponseEntity.ok(guardianService.getAllGuardian());
    }

    // 관리자는 보호자와 어르신의 관계를 생성(매핑)할 수 있다.
    @PostMapping("/{id}/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> connectElderly(
            @PathVariable("id") Long guardianId,
            @RequestParam Long elderlyId,
            @RequestParam RelationType relationType
    ){
        guardianService.connectElderly(guardianId, elderlyId, relationType);
        return ResponseEntity.ok().build();
    }
    @PutMapping("/me")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<GuardianResponse> updateMe(@AuthenticationPrincipal Long id, @RequestBody @Valid GuardianUpdateRequest req) {
        return ResponseEntity.ok(guardianService.updateGuardianProfile(id, req));
    }

    @DeleteMapping("/me")
    @PreAuthorize("hasRole('GUARDIAN')")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long id) {
        guardianService.withdrawGuardian(id);
        return ResponseEntity.noContent().build();
    }
}