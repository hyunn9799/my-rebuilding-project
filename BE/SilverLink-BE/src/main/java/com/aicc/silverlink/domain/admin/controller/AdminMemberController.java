package com.aicc.silverlink.domain.admin.controller;

import com.aicc.silverlink.domain.admin.dto.AdminMemberDtos;
import com.aicc.silverlink.domain.admin.service.AdminMemberService;
import com.aicc.silverlink.domain.admin.service.OfflineRegistrationService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "관리자-회원관리", description = "오프라인 대면 회원가입 및 관리")
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

    private final OfflineRegistrationService registrationService;
    private final AdminMemberService memberService;

    @Operation(summary = "어르신 오프라인 등록", description = "센터 방문 어르신을 대면 등록합니다.")
    @PostMapping("/elderly")
    public ResponseEntity<Long> registerElderly(@Valid @RequestBody AdminMemberDtos.RegisterElderlyRequest req) {
        Long userId = registrationService.registerElderly(req);
        return ResponseEntity.ok(userId);
    }

    @Operation(summary = "보호자 오프라인 등록", description = "센터 방문 보호자를 대면 등록하고 어르신과 연결합니다.")
    @PostMapping("/guardian")
    public ResponseEntity<Long> registerGuardian(@Valid @RequestBody AdminMemberDtos.RegisterGuardianRequest req) {
        Long userId = registrationService.registerGuardian(req);
        return ResponseEntity.ok(userId);
    }

    @Operation(summary = "회원 삭제", description = "회원을 삭제합니다. 역할에 따라 관련 관계도 함께 삭제됩니다.")
    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        memberService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "회원 수정", description = "회원 정보(이름, 전화번호, 이메일)를 수정합니다.")
    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<AdminMemberDtos.UpdateMemberResponse>> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody AdminMemberDtos.UpdateMemberRequest req) {
        AdminMemberDtos.UpdateMemberResponse response = memberService.updateUser(userId, req);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
